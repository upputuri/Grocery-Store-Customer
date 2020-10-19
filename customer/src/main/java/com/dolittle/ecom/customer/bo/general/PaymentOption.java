package com.dolittle.ecom.customer.bo.general;

import java.util.HashMap;
import java.util.Map;

import org.springframework.hateoas.RepresentationModel;

import lombok.Data;

public @Data class PaymentOption extends RepresentationModel<PaymentOption>{
    private String id;
    private Map<String, String> attributes = new HashMap<String, String>(1);
    private String name;
    private String description;
    private String typeId;
    private String typeName;
}
