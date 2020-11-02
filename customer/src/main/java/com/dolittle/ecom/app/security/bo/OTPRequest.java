package com.dolittle.ecom.app.security.bo;

import lombok.Data;

public @Data class OTPRequest {
    private String type;
    private String otp;
    private String message;
    private String target;
}
