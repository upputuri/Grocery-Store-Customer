package com.dolittle.ecom.customer.bo;

import java.math.BigDecimal;

import org.springframework.hateoas.RepresentationModel;

import lombok.Data;

public @Data class InventorySetVariation extends RepresentationModel<InventorySetVariation>{
    private String id;
    private String name;
    private BigDecimal price;
    private BigDecimal mrp;
    private String description;

    public InventorySetVariation(String id, String name, BigDecimal price, BigDecimal mrp)
    {
        this.id = id;
        this.name = name;
        this.price = price;
        this.mrp = mrp;
    }
}
