package com.dolittle.ecom.customer.bo.general;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.hateoas.RepresentationModel;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

public @Data class PromoCode extends RepresentationModel<PromoCode>{
    private String id;
    private String codeValue;
    private boolean isValid;
    private String reason;
    private String discountType;//'percentage' or 'currency'
    @JsonProperty(access = Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal discount = BigDecimal.ZERO;
    @JsonProperty(access = Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal orderAmount = BigDecimal.ZERO;

    public void setDiscount(BigDecimal discount) {
        this.discount = discount.setScale(2, RoundingMode.HALF_EVEN);
    }

    public void  setOrderAmount(BigDecimal amount) {
        this.orderAmount = amount.setScale(2, RoundingMode.HALF_EVEN);
    }
}
