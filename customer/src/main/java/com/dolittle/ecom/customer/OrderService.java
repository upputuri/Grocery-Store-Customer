package com.dolittle.ecom.customer;

import java.beans.beancontext.BeanContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dolittle.ecom.app.CustomerConfig;
import com.dolittle.ecom.app.CustomerRunner;
import com.dolittle.ecom.app.util.CustomerRunnerUtil;
import com.dolittle.ecom.customer.bo.CartItem;
import com.dolittle.ecom.customer.bo.Order;
import com.dolittle.ecom.customer.bo.OrderContext;
import com.dolittle.ecom.customer.bo.OrderItem;
import com.dolittle.ecom.customer.bo.OrderSummary;
import com.dolittle.ecom.customer.bo.ShippingAddress;
import com.dolittle.ecom.customer.bo.Transaction;
import com.dolittle.ecom.customer.bo.general.PromoCode;
import com.dolittle.ecom.customer.payments.PGIService;
import com.mysql.cj.util.StringUtils;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @Autowired
    CustomerRunner customerApp;

    @Autowired
    CustomerConfig config;

    @Autowired
    ApplicationContext appContext;

    @GetMapping(value = "/orders", produces = "application/hal+json")
    public CollectionModel<OrderSummary> getOrders(@RequestParam String cuid, Authentication auth)
    {
        log.info("Processing get orders for customer id: "+cuid);
        CustomerRunnerUtil.validateAndGetAuthCustomer(auth, cuid);

        List<OrderSummary> orders = new ArrayList<OrderSummary>();
        String get_orders_sql = "select oid, cuid, said, shipping_cost, tax_percent, price, discounted_price, ios.name, item_order.created_ts "+
                                "from item_order, item_order_status as ios where cuid=? and item_order.osid = ios.osid order by item_order.created_ts desc";
        try {
            orders = jdbcTemplateObject.query(get_orders_sql, new Object[]{cuid}, (rs, rowNum) -> {
                OrderSummary os = new OrderSummary();
                BigDecimal taxRate = rs.getBigDecimal("tax_percent");
                BigDecimal totalChargesValue = rs.getBigDecimal("shipping_cost");
                BigDecimal totalPriceAfterDiscounts = rs.getBigDecimal("discounted_price");
                BigDecimal totalPriceAfterCharges = totalPriceAfterDiscounts.add(totalChargesValue);
                BigDecimal totalTaxValue = totalPriceAfterCharges.multiply(taxRate.divide(new BigDecimal(100)));
                BigDecimal totalPriceAfterTaxes = totalPriceAfterCharges.add(totalTaxValue);
                os.setOrderId(String.valueOf(rs.getInt("oid")));
                os.setCustomerId(String.valueOf(rs.getInt("cuid")));
                os.setShippingAddressId(String.valueOf(rs.getInt("said")));
                os.setOrderTotal(rs.getBigDecimal("price"));
                os.setDiscountedTotal(totalPriceAfterDiscounts);
                os.setOrderStatus(rs.getString("name"));
                os.setTotalChargesValue(totalChargesValue);
                os.setTotalTaxValue(totalTaxValue);
                os.setFinalTotal(totalPriceAfterTaxes);
                Calendar order_ts = Calendar.getInstance();
                order_ts.setTimeInMillis(rs.getTimestamp("created_ts").getTime());
                os.setCreatedTS(order_ts);
                Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getOrders(cuid, auth)).withSelfRel();
                os.add(selfLink);
                return os;
            });
        }catch(DataAccessException e){
            log.error("An exception occurred while getting orders data for customerId: "+cuid, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred!, pls retry after some time or pls call support");
        }
        Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getOrders(cuid, auth)).withSelfRel();
        CollectionModel<OrderSummary> result = CollectionModel.of(orders, selfLink);
        return result;
    }

    @GetMapping(value="/orders/{orderId}")
    public Order getOrderDetail(@PathVariable String orderId, Authentication auth)
    {
        log.info("Processing request to get Order detail for orderId: "+orderId);
        String get_order_sql = "select io.oid, io.cuid, io.said, io.shipping_cost, io.tax_percent, io.price, io.discounted_price, ios.name as status, io.created_ts, "+
                                "sa.shipping_first_name, sa.shipping_last_name, sa.shipping_line1, sa.shipping_line2, sa.shipping_city, sa.shipping_zip_code, state.state, state.stid, "+
                                "sa.shipping_mobile from item_order as io, item_order_status as ios, item_order_shipping_address as sa, state where io.oid=? and sa.oid = io.oid "+
                                "and io.osid = ios.osid and sa.shipping_stid = state.stid";
        Order order = jdbcTemplateObject.queryForObject(get_order_sql, new Object[]{orderId}, (rs, rowNum) -> {
            Order o = new Order();
            BigDecimal discountedTotal = rs.getBigDecimal("discounted_price").setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal taxRate = rs.getBigDecimal("tax_percent").setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal totalChargesValue = rs.getBigDecimal("shipping_cost");
            BigDecimal totalPriceAfterCharges = discountedTotal.add(totalChargesValue);
            BigDecimal totalTaxValue = totalPriceAfterCharges.multiply(taxRate.divide(new BigDecimal(100)));
            totalTaxValue = totalTaxValue.setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal totalPriceAfterTaxes = totalPriceAfterCharges.add(totalTaxValue);
            totalPriceAfterTaxes = totalPriceAfterTaxes.setScale(2, RoundingMode.HALF_EVEN);
            o.setId(String.valueOf(rs.getInt("oid")));
            o.setCustomerId(String.valueOf(rs.getInt("cuid")));
            o.setShippingAddressId(String.valueOf(rs.getInt("said")));
            o.setDiscountedTotal(discountedTotal);
            o.setStatus(rs.getString("status"));
            o.setTotalChargesValue(totalChargesValue.setScale(2, RoundingMode.HALF_EVEN));
            o.setTotalTaxRate(taxRate);
            o.setTotalTaxValue(totalTaxValue);
            o.setOrderTotal(rs.getBigDecimal("price").setScale(2, RoundingMode.HALF_EVEN));
            o.setFinalTotal(totalPriceAfterTaxes);
            ShippingAddress sa = new ShippingAddress(rs.getString("shipping_line1"), rs.getString("shipping_city"), rs.getString("shipping_zip_code"), rs.getString("shipping_mobile"));
            sa.setId(String.valueOf(rs.getInt("said")));
            sa.setLine2(rs.getString("shipping_line2"));
            sa.setFirstName(rs.getString("shipping_first_name"));
            sa.setLastName(rs.getString("shipping_last_name"));
            sa.setState(rs.getString("state"));
            sa.setStateId(rs.getInt("stid"));
            o.setShippingAddress(sa);
            Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getOrderDetail(orderId, auth)).withSelfRel();
            o.add(selfLink);
            return o;
        });
        CustomerRunnerUtil.validateAndGetAuthCustomer(auth, order.getCustomerId());

        String get_order_items = "select ioi.oiid, ioi.oid, ioi.iid, ioi.isvid, ioi.quantity, ioi.discounted_price, ii.name as item_name, insv.name as variant_name "+
                                "from item_order_item ioi, inventory_set_variations as insv, item_item as ii "+
                                "where ii.iid=ioi.iid and insv.isvid = ioi.isvid and ioi.oid=?";
                                
        List<OrderItem> orderItems = jdbcTemplateObject.query(get_order_items, new Object[]{orderId}, (rs, rowNum) -> {
            OrderItem oi = new OrderItem(String.valueOf(rs.getInt("iid")), String.valueOf(rs.getInt("isvid")), rs.getInt("quantity"));
            oi.setName(rs.getString("item_name"));
            oi.setQtyUnit(rs.getString("variant_name"));
            oi.setPriceAfterDiscount(rs.getBigDecimal("discounted_price"));
            Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getOrderDetail(orderId, auth)).withSelfRel();
            oi.add(selfLink);
            return oi;
        });
        
        order.setOrderItems(orderItems);
        Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getOrderDetail(orderId, auth)).withSelfRel();
        order.add(selfLink);

        return order;
    }

    @PostMapping(value = "/orders", produces = "application/hal+json")
    @Transactional
    public Order createOrder(@RequestBody OrderContext orderContext, Authentication auth)
    {

        // Check if the transaction state permits creation of order
        String provider = config.getPgiProviders().get(orderContext.getPaymentOptionId());
        PGIService pgiService = BeanFactoryAnnotationUtils.qualifiedBeanOfType(appContext, PGIService.class, provider);
        String tran_id = orderContext.getTransactionId();
        String orderString = jdbcTemplateObject.queryForObject("select response from transaction where tid=?", new Object[]{tran_id}, String.class);
        int code = pgiService.validatePaymentResponse(orderString, orderContext.getPgiResponse());
        
        if (code != 0) {
            Order order = new Order();
            order.setId("-1"+code); // Payment transaction issue
            return order;
        }

        // Update the transaction status
        String newStatus = pgiService.getId().equalsIgnoreCase("cod") ? "'Pending'" : "'Success'";
        String transaction_finalize_sql = "update transaction set tsid = (select tsid from transaction_status where name="+
                                            newStatus+") where tid="+tran_id;
        jdbcTemplateObject.update(transaction_finalize_sql);   


        // 1. Get the tax rate from tax profile
        // 2. Create order in db
        // 3. Add shipping address and link it to order
        // 4. Get the cart items and generate order items from them.
        // 5. Add order items to db and link them to order
        // 6. Change status of cart items in db to 'executed'

        log.info("Beginning to create an order for customer - "+orderContext.getCustomerId());
        CustomerRunnerUtil.validateAndGetAuthCustomer(auth, orderContext.getCustomerId());
        Order preOrder = this.createPreOrder(orderContext, true, auth);

        int osid = jdbcTemplateObject.queryForObject("select osid from item_order_status where name='Initial'", Integer.TYPE);

        SimpleJdbcInsert jdbcInsert_Order = new SimpleJdbcInsert(jdbcTemplateObject)
                                            .usingColumns("cuid", "said", "taxproid", "tax_percent", "shipping_cost", "tax_type", "price", "discounted_price", "pcid", "osid")
                                            .withTableName("item_order")
                                            .usingGeneratedKeyColumns("oid");

        Map<String, Object> parameters_insert_order = new HashMap<String, Object>(1);
        parameters_insert_order.put("cuid", orderContext.getCustomerId());
        parameters_insert_order.put("said", preOrder.getShippingAddressId());
        parameters_insert_order.put("taxproid", preOrder.getTaxProfileId());
        parameters_insert_order.put("tax_percent", preOrder.getTotalTaxRate());
        parameters_insert_order.put("tax_type", preOrder.getTaxType());        
        parameters_insert_order.put("price", preOrder.getOrderTotal());
        parameters_insert_order.put("shipping_cost", preOrder.getTotalChargesValue());
        parameters_insert_order.put("discounted_price", preOrder.getDiscountedTotal());
        parameters_insert_order.put("pcid", preOrder.getAppliedPromoCodeIdList().size() > 0? preOrder.getAppliedPromoCodeIdList().get(0) : null);
        parameters_insert_order.put("osid", osid);

        Number oid = jdbcInsert_Order.executeAndReturnKey(parameters_insert_order);

        Order order = preOrder;
        order.setId(oid.toString());
        

        // Now add a transaction - order mapping record in db

        SimpleJdbcInsert mapTransactionJDBCInsert = new SimpleJdbcInsert(jdbcTemplateObject)
                .usingColumns("oid", "tid")
                .withTableName("item_order_transaction");
        
        Map<String, Object> parameters_insert_transaction_order = new HashMap<String, Object>(1);
        parameters_insert_transaction_order.put("oid", oid);
        parameters_insert_transaction_order.put("tid", tran_id);

        mapTransactionJDBCInsert.execute(parameters_insert_transaction_order);

        String decrement_promocode_sql = "update promo_code set quantity = CASE WHEN quantity>0 THEN quantity-1 ELSE quantity END where pcid = ?";

        // No need to validate promocode once again as the createPreOrder call performed from within current transaction will ensure that.
        if (order.getAppliedPromoCodeIdList().size()>0) {
            jdbcTemplateObject.update(decrement_promocode_sql, order.getAppliedPromoCodeIdList().get(0));
        }

        // String insert_order_shipping_address = "insert into item_order_shipping_address (oid, shipping_first_name, shipping_last_name, "+
        //                                         "shipping_line1, shipping_line2, shipping_city, shipping_zip_code, shipping_mobile, "+
        //                                         "billing_first_name, billing_last_name, billing_line1, billing_line2, billing_city, billing_zip_code) "+
        //                                         "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        SimpleJdbcInsert jdbcInsert_shipping_address = new SimpleJdbcInsert(jdbcTemplateObject)
                                                        .usingColumns("oid", "shipping_first_name", "shipping_last_name", "shipping_line1", "shipping_line2",
                                                                    "shipping_city", "shipping_stid", "shipping_zip_code", "shipping_mobile", "billing_first_name", "billing_last_name",
                                                                    "billing_line1", "billing_line2", "billing_city", "billing_stid", "billing_zip_code")
                                                        .withTableName("item_order_shipping_address");

        Map<String, Object> parameters_insert_shipping_address = new HashMap<String, Object>(1);
        parameters_insert_shipping_address.put("oid", oid);
        parameters_insert_shipping_address.put("shipping_first_name", order.getShippingAddress().getFirstName());
        parameters_insert_shipping_address.put("shipping_last_name", order.getShippingAddress().getLastName());
        parameters_insert_shipping_address.put("shipping_line1", order.getShippingAddress().getLine1());
        parameters_insert_shipping_address.put("shipping_line2", order.getShippingAddress().getLine2());
        parameters_insert_shipping_address.put("shipping_city", order.getShippingAddress().getCity());
        parameters_insert_shipping_address.put("shipping_stid", order.getShippingAddress().getStateId());
        parameters_insert_shipping_address.put("shipping_zip_code", order.getShippingAddress().getZipcode());
        parameters_insert_shipping_address.put("shipping_mobile", order.getShippingAddress().getPhoneNumber());
        parameters_insert_shipping_address.put("billing_first_name", order.getShippingAddress().getFirstName());
        parameters_insert_shipping_address.put("billing_last_name", order.getShippingAddress().getLastName());
        parameters_insert_shipping_address.put("billing_line1", order.getShippingAddress().getLine1());
        parameters_insert_shipping_address.put("billing_line2", order.getShippingAddress().getLine2());
        parameters_insert_shipping_address.put("billing_city", order.getShippingAddress().getCity());
        parameters_insert_shipping_address.put("billing_stid", order.getShippingAddress().getStateId());
        parameters_insert_shipping_address.put("billing_zip_code", order.getShippingAddress().getZipcode());

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

    @PostMapping(value = "/orders/preorders", produces = "application/hal+json")
    @Transactional
    public Order createPreOrder(@RequestBody OrderContext context , 
                @RequestParam(value="skiptransaction", required=false, defaultValue = "false") boolean skipTransaction, Authentication auth)
    {
        log.info("Beginning to create a preorder for customer - "+context.getCustomerId());
        CustomerRunnerUtil.validateAndGetAuthCustomer(auth, context.getCustomerId());
        Order order = new Order();
        order.setCustomerId(context.getCustomerId());
        CollectionModel<CartItem> cartItemsModel = cartService.getCartItems(context.getCustomerId(), auth);
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
        List<String> promoCodeIdList = new ArrayList<String>();
        context.getPromoCodes().forEach((code) -> {
            PromoCode promoCode = customerApp.getPromoCodeDetail(code);
            if (promoCode.isValid()){
                promoCodeIdList.add(promoCode.getId());
                if (order.getOrderTotal().subtract(promoCode.getOrderAmount()).doubleValue() > 0.0)
                    order.addDiscount("_code_"+code, promoCode.getDiscount(), promoCode.getDiscountType());
            }
            return;
        });
        order.setAppliedPromoCodeIdList(promoCodeIdList);

        @Data class OrderCharge{
            private BigDecimal cost;
            private String name;
            private BigDecimal minOrderAmount;
        }

        String fetch_shipping_charges_sql = "select shipping_cost, min_order_amount from country where ctid='1'";
        OrderCharge shippingCharge = jdbcTemplateObject.queryForObject(fetch_shipping_charges_sql, (rs, rowNum)->{
            OrderCharge oc = new OrderCharge();
            oc.cost = rs.getBigDecimal("shipping_cost");
            oc.name = "shipping charge";
            oc.minOrderAmount = rs.getBigDecimal("min_order_amount");
            return oc;
        });

        if (order.getOrderTotal().compareTo(shippingCharge.minOrderAmount) < 0){
            order.addCharge(shippingCharge.name, shippingCharge.cost, "currency");
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
        order.setTaxProfileId(taxProfile.taxproid);
        order.setTaxType(taxProfile.tax_type);

        BigDecimal taxRate = taxProfile.rate;
        String taxDisplayName = taxProfile.name+"("+taxRate+")";
        order.addTax(taxDisplayName, taxRate);
        
        //Get delivery address
        String get_delivery_address_sql = "select sa.said, sa.first_name, sa.last_name, sa.line1, sa.line2, sa.zip_code, sa.mobile, sa.city, sa.stid, state.state "+
                                            "from customer_shipping_address sa, state where sa.said = ? and sa.stid = state.stid";
                                            
        ShippingAddress deliveryAddress = jdbcTemplateObject.queryForObject(get_delivery_address_sql, new Object[]{context.getDeliveryAddressId()}, (rs, rowNum) -> {
            ShippingAddress sa = new ShippingAddress(rs.getString("line1"), rs.getString("city"), rs.getString("zip_code"), rs.getString("mobile"));
            sa.setId(String.valueOf(rs.getInt("said")));
            sa.setLine2(rs.getString("line2"));
            sa.setFirstName(rs.getString("first_name"));
            sa.setLastName(rs.getString("last_name"));
            sa.setState(rs.getString("state"));
            sa.setStateId(rs.getInt("stid"));
            Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).createPreOrder(context, false, auth)).withSelfRel();
            sa.add(selfLink);
            return sa;
        });
        Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).createPreOrder(context, false, auth)).withSelfRel();
        order.add(selfLink);
        order.setShippingAddress(deliveryAddress);
        order.setShippingAddressId(context.getDeliveryAddressId());

        //Start a payment transaction protocol if a paymentOptionId is available in OrderContext
        // if (StringUtils.isNullOrEmpty(context.getTransactionId()) && 
        if(!StringUtils.isNullOrEmpty(context.getPaymentOptionId()) && !skipTransaction) {
            // && context.getPgiResponse() == null) {
            // Add the payment gateway provider name mapped to this id
            int tsid = jdbcTemplateObject.queryForObject("select tsid from transaction_status where name='Approved'", Integer.TYPE);
            // Create a new transaction in db
            String provider = config.getPgiProviders().get(context.getPaymentOptionId());
            PGIService pgiService = BeanFactoryAnnotationUtils.qualifiedBeanOfType(appContext, PGIService.class, provider);
            int transactionAmount = order.getFinalTotal().multiply(new BigDecimal(100)).intValue();
            SimpleJdbcInsert jdbcInsert_transaction = new SimpleJdbcInsert(jdbcTemplateObject)
                        .usingColumns("cuid", "cpoid", "tsid", "amount", "discount_amt")
                        .withTableName("transaction")
                        .usingGeneratedKeyColumns("tid");
            Map<String, Object> parameters_insert_transaction = new HashMap<String, Object>(1);
            parameters_insert_transaction.put("cuid", context.getCustomerId());
            parameters_insert_transaction.put("cpoid", context.getPaymentOptionId());
            parameters_insert_transaction.put("tsid", tsid);
            parameters_insert_transaction.put("amount", order.getFinalTotal());
            parameters_insert_transaction.put("discount_amt", order.getTotalDiscountValue());        
            
            Number tran_id = jdbcInsert_transaction.executeAndReturnKey(parameters_insert_transaction);
            Transaction transaction = new Transaction(tran_id.toString());
            transaction.setAmount(transactionAmount);
            transaction.setPaymentOptionId(context.getPaymentOptionId());
            
            String providerData = pgiService.startTransaction(transactionAmount, tran_id.toString());
            // Inject a payment gateway provider and its data
            transaction.setProviderId(provider);
            transaction.setProviderData(providerData);
            // JSONObject orderJSON = new JSONObject(orderString);
            // transaction.setPaymentOrderId(orderJSON.getString("id"));
            order.setTransaction(transaction);

            // Add the pgi's payment order object to the transaction in db
            jdbcTemplateObject.update("update transaction set response=? where tid="+tran_id, providerData);
        }
        // else if (!StringUtils.isNullOrEmpty(context.getTransactionId())) {
        //     // We are already inside a transaction. Fetch the transaction details from db only (do not use those from request) and attach to order.
        //     String fetch_transaction_sql = "select tid, cpoid, tsid, amount, response from transaction where tid=?";
        //     Transaction transaction = jdbcTemplateObject.queryForObject(fetch_transaction_sql, new Object[]{context.getTransactionId()}, (rs, rowNum) -> {
        //         Transaction tran = new Transaction(String.valueOf(rs.getInt("tid")));
        //         tran.setPaymentOptionId(String.valueOf(rs.getInt("cpoid")));
        //         tran.setStatusId(String.valueOf(rs.getInt("tsid")));
        //         tran.setAmount(rs.getBigDecimal("amount").multiply(new BigDecimal(100)).intValue());
        //         tran.setProviderResponse(rs.getString("response"));
        //         return tran;
        //     });

        //     order.setTransaction(transaction);
        // }
        return order;
    }

    @DeleteMapping(value = "/orders/{orderId}", produces="application/hal+json")
    public void cancelOrder(@PathVariable String orderId, Authentication auth){
        try{
            log.info("Processing request to cancel order Id: "+orderId);
            Number customerId = jdbcTemplateObject.queryForObject("select cuid from item_order where oid=?", new Object[]{orderId}, Integer.TYPE);
            CustomerRunnerUtil.validateAndGetAuthCustomer(auth, customerId.toString());
            String currentStatus = jdbcTemplateObject.queryForObject("select ios.name from item_order io, item_order_status ios where oid=? and io.osid = ios.osid",
                                                                 new Object[]{orderId}, String.class);
            if (currentStatus.trim().equals("Initial") || currentStatus.equals("Executing")){
                jdbcTemplateObject.update("update item_order set osid = (select osid from item_order_status where name='Cancel Request') where oid=?", orderId);
            }
            else{
                log.error("Invalid State of order. Order can not be cancelled in this state. OrderId: "+orderId);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can not cancel order in its current state. Request processing aborted");
            }
        }
        catch(DataAccessException e){
            log.error("An exception occurred while cancelling order with Id: "+orderId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred!, pls retry after some time or pls call support");
        }
    }

    // private Customer assertAuthCustomerId(Principal principal, String customerId)
    // {
    //     Customer customer = null;
    //     String get_customer_profile_query = "select c.cuid from customer c "+
    //                                 "where c.email = ? and c.cuid = ? and c.custatusid = (select custatusid from customer_status where name='Active')";
    //     try{
    //         customer = jdbcTemplateObject.queryForObject(get_customer_profile_query, new Object[]{principal.getName(), customerId}, (rs, rownum) -> {
    //             Customer c = new Customer();
    //             c.setId(String.valueOf(rs.getInt("cuid")));
    //             return c;
    //         });
    //     }
    //     catch(EmptyResultDataAccessException e)
    //     {
    //         log.error("Requested customer Id does not match with authenticated user or the customer is inactive");
    //         throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You do not have permission to view details of the provided customer Id");
    //     }
    //     return customer;
    // }
}
