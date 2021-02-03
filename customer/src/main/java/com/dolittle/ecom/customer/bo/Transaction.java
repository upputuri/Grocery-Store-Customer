package com.dolittle.ecom.customer.bo;

import java.util.Calendar;
import java.util.Date;

import org.springframework.hateoas.RepresentationModel;

import lombok.Data;

public @Data class Transaction extends RepresentationModel<Transaction>{
    private String id;
    private String providerId;
    private String providerData;
    private String paymentOrderId;
    //Amount in INR in paisa
    private int amount = 0;
    private String clientResponse;
    private String statusId;
    private String paymentOptionId;
    private Calendar createdTS;
}
