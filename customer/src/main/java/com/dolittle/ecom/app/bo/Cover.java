package com.dolittle.ecom.app.bo;

import java.math.BigDecimal;

import org.springframework.hateoas.RepresentationModel;

import lombok.Data;

public @Data class Cover extends RepresentationModel<Cover>{
    private String coverId;
    private String coverCity;
    private BigDecimal shippingCost;
    private BigDecimal minOrderValue;
}
