package com.dolittle.ecom.app.bo;

import lombok.Data;

public @Data class NotificationRequest {
    private String subject;
    private String message;
    private String type;
}
