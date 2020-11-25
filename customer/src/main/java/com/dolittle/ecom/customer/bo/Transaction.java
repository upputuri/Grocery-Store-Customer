package com.dolittle.ecom.customer.bo;

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
    private Object providerResponse;
    private String statusId;
    private String paymentOptionId;
    private Date ts;

    public Transaction(String id)
    {
        super();
        this.id = id;
    }
}
