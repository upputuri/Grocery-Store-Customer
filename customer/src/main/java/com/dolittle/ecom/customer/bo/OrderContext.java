package com.dolittle.ecom.customer.bo;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

public @Data class OrderContext {
    private String customerId;
    private List<String> promoCodes = new ArrayList<String>();
    private String deliveryAddressId;
    private String paymentOptionId;
    private String transactionId;
    private String instructions;
}
