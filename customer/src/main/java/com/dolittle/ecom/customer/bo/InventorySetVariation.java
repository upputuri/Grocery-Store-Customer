package com.dolittle.ecom.customer.bo;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import org.springframework.hateoas.RepresentationModel;

import lombok.Data;

public @Data class InventorySetVariation extends RepresentationModel<InventorySetVariation>{
    private String id;
    private String name;
    @JsonProperty(access = Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING) 
    private BigDecimal price;
    @JsonProperty(access = Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING) 
    private BigDecimal priceAfterDiscount;
    @JsonProperty(access = Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING) 
    private BigDecimal mrp;
    private String description;

    public InventorySetVariation(String id, String name, BigDecimal price, BigDecimal mrp)
    {
        this.id = id;
        this.name = name;
        this.price = price.setScale(2, RoundingMode.HALF_EVEN);
        this.mrp = mrp.setScale(2, RoundingMode.HALF_EVEN);
    }

    public void setPriceAfterDiscount(BigDecimal value)
    {
        this.priceAfterDiscount = value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
