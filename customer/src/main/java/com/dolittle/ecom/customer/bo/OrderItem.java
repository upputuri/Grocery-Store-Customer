package com.dolittle.ecom.customer.bo;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.fasterxml.jackson.annotation.JsonFormat;

import org.springframework.hateoas.RepresentationModel;

import lombok.Data;

public @Data class OrderItem extends RepresentationModel<OrderItem>{
    private String id;
    private String name;
    private String productId;
    private String insvid;
    private String orderItemStatus;
    private String qtyUnit;
    private int qty;
    @JsonFormat(shape = JsonFormat.Shape.STRING)    
    private BigDecimal originalPrice = BigDecimal.ZERO;
    @JsonFormat(shape = JsonFormat.Shape.STRING)    
    private BigDecimal priceAfterDiscount = BigDecimal.ZERO;
    @JsonFormat(shape = JsonFormat.Shape.STRING) 
    private BigDecimal totalPriceAfterDiscount = BigDecimal.ZERO;

    public OrderItem(String productId, String insvid, int qty)
    {
        super();
        this.productId = productId;
        this.insvid = insvid;
        this.qty = qty;
        this.orderItemStatus = "pending";
    }

    public void setOriginalPrice(BigDecimal price){
        this.originalPrice = price.setScale(2, RoundingMode.HALF_EVEN);
        computeTotals();
    }

    public void setPriceAfterDiscount(BigDecimal price){
        this.priceAfterDiscount = price.setScale(2, RoundingMode.HALF_EVEN);
        computeTotals();
    }

    public void setQty(int qty){
        this.qty = qty;
        computeTotals();
    }

    private void computeTotals(){
        this.totalPriceAfterDiscount = this.priceAfterDiscount.multiply(new BigDecimal(this.qty));
        this.totalPriceAfterDiscount = this.totalPriceAfterDiscount.setScale(2, RoundingMode.HALF_EVEN);
    }
}
