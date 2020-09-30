package com.dolittle.ecom.customer.bo;

import org.springframework.hateoas.RepresentationModel;

import lombok.Data;

public @Data class Invoice extends RepresentationModel<Invoice>{
    private final String id;
    private final String orderId;
    private String downloadLink;

    public Invoice(String id, String orderId)
    {
        super();
        this.id = id;
        this.orderId = orderId;
    }
}
