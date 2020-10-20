package com.dolittle.ecom.customer;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import com.dolittle.ecom.customer.bo.CartItem;
import com.dolittle.ecom.customer.bo.Customer;
import com.dolittle.ecom.customer.bo.Order;
import com.dolittle.ecom.customer.bo.OrderContext;
import com.dolittle.ecom.customer.bo.ShippingAddress;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class OrderService {
    @Autowired
    JdbcTemplate jdbcTemplateObject;

    @Autowired
    CustomerCartService cartService;

    @PostMapping(value = "/orders", produces = "application/hal+json")
    @Transactional
    public Order createOrder(@RequestBody OrderContext orderContext, Principal principal)
    {
        // 1. Get the tax rate from tax profile
        // 2. Create order in db
        // 3. Add shipping address and link it to order
        // 4. Get the cart items and generate order items from them.
        // 5. Add order items to db and link them to order
        // 6. Change status of cart items in db to 'executed'

        log.info("Beginning to create an order for customer - "+orderContext.getCustomerId());
        assertAuthCustomerId(principal, orderContext.getCustomerId());
        Order preOrder = this.getPreOrder(orderContext.getCustomerId(), orderContext.getDeliveryAddressId(), principal);

        SimpleJdbcInsert jdbcInsert_Order = new SimpleJdbcInsert(jdbcTemplateObject)
                                            .usingColumns("cuid", "said", "taxproid", "tax_percent", "tax_type", "price", "discounted_price")
                                            .withTableName("item_order")
                                            .usingGeneratedKeyColumns("oid");

        Map<String, Object> parameters_insert_order = new HashMap<String, Object>(1);
        parameters_insert_order.put("cuid", orderContext.getCustomerId());
        parameters_insert_order.put("said", preOrder.getShippingAddressId());
        parameters_insert_order.put("taxproid", preOrder.getTaxProfileId());
        parameters_insert_order.put("tax_percent", preOrder.getTotalTaxRate());
        parameters_insert_order.put("tax_type", preOrder.getTaxType());        
        parameters_insert_order.put("price", preOrder.getOrderTotal());
        parameters_insert_order.put("discounted_price", preOrder.getDiscountedTotal());

        Number oid = jdbcInsert_Order.executeAndReturnKey(parameters_insert_order);

        Order order = preOrder;
        order.setId(oid.toString());

        // String insert_order_shipping_address = "insert into item_order_shipping_address (oid, shipping_first_name, shipping_last_name, "+
        //                                         "shipping_line1, shipping_line2, shipping_city, shipping_zip_code, shipping_mobile, "+
        //                                         "billing_first_name, billing_last_name, billing_line1, billing_line2, billing_city, billing_zip_code) "+
        //                                         "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        SimpleJdbcInsert jdbcInsert_shipping_address = new SimpleJdbcInsert(jdbcTemplateObject)
                                                        .usingColumns("oid", "shipping_first_name", "shipping_last_name", "shipping_line1", "shipping_line2",
                                                                    "shipping_city", "shipping_zip_code", "shipping_mobile", "billing_first_name", "billing_last_name",
                                                                    "billing_line1", "billing_line2", "billing_city", "billing_zip_code")
                                                        .withTableName("item_order_shipping_address");

        Map<String, Object> parameters_insert_shipping_address = new HashMap<String, Object>(1);
        parameters_insert_shipping_address.put("oid", oid);
        parameters_insert_shipping_address.put("shipping_first_name", order.getShippingAddress().getFirstName());
        parameters_insert_shipping_address.put("shipping_last_name", order.getShippingAddress().getLastName());
        parameters_insert_shipping_address.put("shipping_line1", order.getShippingAddress().getLine1());
        parameters_insert_shipping_address.put("shipping_line2", order.getShippingAddress().getLine2());
        parameters_insert_shipping_address.put("shipping_city", order.getShippingAddress().getCity());
        parameters_insert_shipping_address.put("shipping_zip_code", order.getShippingAddress().getZipcode());
        parameters_insert_shipping_address.put("shipping_mobile", order.getShippingAddress().getPhoneNumber());

        jdbcInsert_shipping_address.execute(parameters_insert_shipping_address);

        // Optimize: pull out the inner query to avoid repeating it several times in batch update                                           
        String insert_order_items = "insert into item_order_item (oid, iid, isvid, quantity, discounted_price, item_price, oisid) "+
                                    "values (?, ?, ?, ?, ?, ?, (select oisid from item_order_item_status where name like 'pending'))";


        
        BatchPreparedStatementSetter bpss = new BatchPreparedStatementSetter(){
            public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                ps.setInt(1, oid.intValue());
                ps.setInt(2, Integer.parseInt(order.getOrderItems().get(i).getProductId()));
                ps.setInt(3, Integer.parseInt(order.getOrderItems().get(i).getInsvid()));
                ps.setInt(4, order.getOrderItems().get(i).getQty());
                ps.setBigDecimal(5, order.getOrderItems().get(i).getPriceAfterDiscount());
                ps.setBigDecimal(6, order.getOrderItems().get(i).getOriginalPrice());
            };
            public int getBatchSize() {
                return order.getOrderItems().size();
            };
        };

        jdbcTemplateObject.batchUpdate(insert_order_items, bpss);

        String update_cart_items_to_executed = "update cart_item set cartisid = (select cartisid from cart_item_status where name like 'executed') "+
                                                "where cartid = (select cartid from cart where cuid=?) "+
                                                "and cartisid = (select cartisid from cart_item_status where name like 'active')";

        jdbcTemplateObject.update(update_cart_items_to_executed, orderContext.getCustomerId());
        log.debug("New order created - {} ",order.toString());
        log.info("Order "+oid+" created for customer - "+order.getCustomerId());
        return order;
    }

    @GetMapping(value = "/orders/preorder", produces = "application/hal+json")
    public Order getPreOrder(@RequestParam String customerId, @RequestParam String deliveryAddressId, Principal principal)
    {
        log.info("Beginning to create a preorder for customer - "+customerId);
        assertAuthCustomerId(principal, customerId);
        Order order = new Order();
        order.setCustomerId(customerId);
        CollectionModel<CartItem> cartItemsModel = cartService.getCartItems(customerId);
        cartItemsModel.forEach((cartItem) -> {
            order.addOrderItem(cartItem.getOrderItem());
            log.debug("Adding cart item to order "+cartItem.toString());
        });

        @Data class TaxProfile{
            private String taxproid;
            private String name;
            private String tax_type;
            private BigDecimal rate;
        }

        //Add promo code discounts to order
        // cartContext.getPromoCodes().stream().map((code) -> {
            //order.addDiscount(...)
            
            // });
        //order.addCharge(...)

        String query_tax_detail_sql = "select taxproid, name, rate, tax_type from tax_profile as t where t.default = 1";
        // Map<String, BigDecimal> tax_profile_rs_map = new HashMap<String, BigDecimal>();
        TaxProfile taxProfile = jdbcTemplateObject.queryForObject(query_tax_detail_sql, (rs, rowNum) ->{
            TaxProfile t = new TaxProfile();
            t.taxproid = rs.getString("taxproid");
            t.name = rs.getString("name");
            t.rate = rs.getBigDecimal("rate");
            t.tax_type = rs.getString("tax_type");
            return t;
        });
        order.setTaxProfileId(taxProfile.taxproid);
        order.setTaxType(taxProfile.tax_type);

        BigDecimal taxRate = taxProfile.rate;
        String taxDisplayName = taxProfile.name+"("+taxRate+")";
        order.addTax(taxDisplayName, taxRate);
        
        //Get delivery address
        String get_delivery_address_sql = "select sa.said, sa.first_name, sa.last_name, sa.line1, sa.line2, sa.zip_code, sa.mobile, sa.city, sa.stid, state.state "+
                                            "from customer_shipping_address sa, state where sa.said = ? and sa.stid = state.stid";
                                            
        ShippingAddress deliveryAddress = jdbcTemplateObject.queryForObject(get_delivery_address_sql, new Object[]{deliveryAddressId}, (rs, rowNum) -> {
            ShippingAddress sa = new ShippingAddress(rs.getString("line1"), rs.getString("city"), rs.getString("zip_code"), rs.getString("mobile"));
            sa.setId(String.valueOf(rs.getInt("said")));
            sa.setLine2(rs.getString("line2"));
            sa.setFirstName(rs.getString("first_name"));
            sa.setLastName(rs.getString("last_name"));
            sa.setState(rs.getString("state"));
            sa.setStateId(rs.getInt("stid"));
            Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getPreOrder(null, null, null)).withSelfRel();
            sa.add(selfLink);
            return sa;
        });
        Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getPreOrder(null, null, null)).withSelfRel();
        order.add(selfLink);
        order.setShippingAddress(deliveryAddress);
        order.setShippingAddressId(deliveryAddressId);
        return order;
    }

    private Customer assertAuthCustomerId(Principal principal, String customerId)
    {
        Customer customer = null;
        String get_customer_profile_query = "select c.cuid from customer c "+
                                    "where c.email = ? and c.cuid = ? and c.custatusid = (select custatusid from customer_status where name='Active')";
        try{
            customer = jdbcTemplateObject.queryForObject(get_customer_profile_query, new Object[]{principal.getName(), customerId}, (rs, rownum) -> {
                Customer c = new Customer();
                c.setId(String.valueOf(rs.getInt("cuid")));
                return c;
            });
        }
        catch(EmptyResultDataAccessException e)
        {
            log.error("Requested customer Id does not match with authenticated user or the customer is inactive");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You do not have permission to view details of the provided customer Id");
        }
        return customer;
    }
}
