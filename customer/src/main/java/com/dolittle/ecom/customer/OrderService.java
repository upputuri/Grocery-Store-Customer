package com.dolittle.ecom.customer;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import com.dolittle.ecom.customer.bo.CartItem;
import com.dolittle.ecom.customer.bo.Order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class OrderService {
    @Autowired
    JdbcTemplate jdbcTemplateObject;

    @Autowired
    CustomerCartService cartService;

    @PostMapping(value = "/orders")
    @Transactional
    public Order createOrder(@RequestBody Order order)
    {
        // 1. Get the tax rate from tax profile
        // 2. Create order in db
        // 3. Add shipping address and link it to order
        // 4. Get the cart items and generate order items from them.
        // 5. Add order items to db and link them to order
        // 6. Change status of cart items in db to 'executed'
        log.info("Beginning to create an order for customer - "+order.getCustomerId());
        CollectionModel<CartItem> cartItemsModel = cartService.getCartItems(order.getCustomerId());
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

        BigDecimal taxRate = taxProfile.rate;
        String taxDisplayName = taxProfile.name+"("+taxRate+")";
        order.addTax(taxDisplayName, taxRate);
        //order.addDiscount(...)
        //order.addCharge(...)

        // Integer taxProfileId = jdbcTemplateObject.queryForObject(query_tax_detail_sql, Integer.TYPE);

        // String insert_order_sql = "insert into item_order (cuid, said, taxproid, tax_percent, tax_type, price, discounted_price) "+
        //                             "values (?, ?, ? , ?, ?, ?, ?)";
        SimpleJdbcInsert jdbcInsert_Order = new SimpleJdbcInsert(jdbcTemplateObject)
                                            .usingColumns("cuid", "said", "taxproid", "tax_percent", "tax_type", "price", "discounted_price")
                                            .withTableName("item_order")
                                            .usingGeneratedKeyColumns("oid");

        Map<String, Object> parameters_insert_order = new HashMap<String, Object>(1);
        parameters_insert_order.put("cuid", order.getCustomerId());
        parameters_insert_order.put("said", order.getShippingAddressId());
        parameters_insert_order.put("taxproid", taxProfile.taxproid);
        parameters_insert_order.put("tax_percent", taxProfile.rate);
        parameters_insert_order.put("tax_type", taxProfile.tax_type);        
        parameters_insert_order.put("price", order.getOrderTotal());
        parameters_insert_order.put("discounted_price", order.getDiscountedTotal());

        Number oid = jdbcInsert_Order.executeAndReturnKey(parameters_insert_order);
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

        jdbcTemplateObject.update(update_cart_items_to_executed, order.getCustomerId());
        log.debug("New order created - {} ",order.toString());
        log.info("Order "+oid+" created for customer - "+order.getCustomerId());
        return order;
    }
}
