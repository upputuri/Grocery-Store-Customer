package com.dolittle.ecom.customer.bo;

import java.util.Date;

import org.springframework.hateoas.RepresentationModel;

import lombok.Data;

public @Data class Transaction extends RepresentationModel<Transaction>{
    private String id;
    private String orderId;
    private Date ts;
    private String type;
    private String paymentOption;

    public Transaction(String id, String orderId)
    {
        super();
        this.id = id;
        this.orderId = orderId;
    }
}
