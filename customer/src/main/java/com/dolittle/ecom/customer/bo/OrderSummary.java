package com.dolittle.ecom.customer.bo;

import java.math.BigDecimal;
import java.util.Calendar;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import org.springframework.hateoas.RepresentationModel;

import lombok.Data;

public @Data class OrderSummary extends RepresentationModel<OrderSummary>{
    private String orderId;
    private String customerId;
    private String shippingAddressId;
    private Calendar createdTS;
    private String orderStatus;
    @JsonProperty(access = Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal orderTotal = BigDecimal.ZERO;
    @JsonProperty(access = Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal discountedTotal = BigDecimal.ZERO;
    @JsonProperty(access = Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal totalTaxValue = BigDecimal.ZERO;
    @JsonProperty(access = Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal totalChargesValue = BigDecimal.ZERO;
    @JsonProperty(access = Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal finalTotal = BigDecimal.ZERO;

}
