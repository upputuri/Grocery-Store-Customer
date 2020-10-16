package com.dolittle.ecom.customer.bo;

import lombok.Data;

public @Data class LoginCredentials {
    private final String loginId;
    private final String password;

    public LoginCredentials(String loginId, String password)
    {
        this.loginId = loginId;
        this.password = password;
    }
}
