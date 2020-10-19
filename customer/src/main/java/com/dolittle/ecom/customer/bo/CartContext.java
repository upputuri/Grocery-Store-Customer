package com.dolittle.ecom.customer.bo;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

public @Data class CartContext {
    private String cartId;
    private List<String> promoCodes = new ArrayList<String>();
    private String deliveryAddressId;
}
