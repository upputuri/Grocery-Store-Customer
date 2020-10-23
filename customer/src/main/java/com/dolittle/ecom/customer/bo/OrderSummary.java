package com.dolittle.ecom.customer.bo;

import java.math.BigDecimal;
import java.util.Calendar;

import org.springframework.hateoas.RepresentationModel;

import lombok.Data;

public @Data class OrderSummary extends RepresentationModel<OrderSummary>{
    private String orderId;
    private String customerId;
    private String shippingAddressId;
    private Calendar createdTS;
    private String orderStatus;
    private BigDecimal orderTotal;
    private BigDecimal discountedTotal;
    private BigDecimal totalTaxValue;
    private BigDecimal totalChargesValue;
    private BigDecimal finalTotal;
}
