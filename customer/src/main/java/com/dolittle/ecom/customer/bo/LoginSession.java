package com.dolittle.ecom.customer.bo;

import lombok.Data;

public @Data class LoginSession {
    private final String sessionId;
    private final Customer customer;
    private final int cartItemCount;

    public LoginSession(String sessionId, Customer customer, int cartCount)
    {
        this.sessionId = sessionId;
        this.customer = customer;
        this.cartItemCount = cartCount;
    }

}
