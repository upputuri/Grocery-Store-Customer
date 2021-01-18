package com.dolittle.ecom.customer.bo;

import java.math.BigDecimal;

import org.springframework.hateoas.RepresentationModel;

import lombok.Data;

public @Data class MPlan extends RepresentationModel<MPlan>{
    private String planId;
    private String planName;
    private String categoryId;
    private String categoryName;
    private BigDecimal minPurchaseAmount = BigDecimal.ZERO;
    private BigDecimal maxPurchaseAmount = BigDecimal.ZERO;
    private int validityInYears;
    private String shortDescription;
    private String description;
    private BigDecimal planPrice;
    private BigDecimal oneTimeDiscountPercent;
}
