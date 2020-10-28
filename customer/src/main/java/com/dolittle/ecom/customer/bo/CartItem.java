package com.dolittle.ecom.customer.bo;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import org.springframework.hateoas.RepresentationModel;

import lombok.Data;

public @Data class CartItem extends RepresentationModel<CartItem>{
    private String cartItemId;
    private final String productId;
    private final String variationId;
    private String productName;
    private String image;
    private String unitLabel;
    private int qty;
    @JsonProperty(access = Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal unitPrice = BigDecimal.ZERO;
    @JsonProperty(access = Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal unitPriceAfterDiscount = BigDecimal.ZERO;
    @JsonProperty(access = Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal totalPrice = BigDecimal.ZERO;
    @JsonProperty(access = Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal totalPriceAfterDiscount = BigDecimal.ZERO;
    @JsonProperty(access = Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal discount = BigDecimal.ZERO;
    private String productURL;

    public CartItem(String productId, String variationId)
    {
        super();
        if (productId == null || variationId == null)
            throw new IllegalArgumentException("Exception when creating CartItem Object - Either productId or variationId passed to constructor is null"+productId+variationId);
        this.productId = productId;
        this.variationId = variationId;
        this.qty = 0;
    }

    public OrderItem getOrderItem()
    {
        OrderItem oi = new OrderItem(this.productId, this.variationId, this.qty);
        oi.setName(this.productName);
        oi.setQtyUnit(this.unitLabel);
        oi.setOriginalPrice(this.unitPrice);
        oi.setPriceAfterDiscount(this.unitPriceAfterDiscount);
        return oi;
    }

    // public BigDecimal getDiscountedPrice()
    // {
    //     return unitPrice.multiply(BigDecimal.ONE.subtract(discount)).setScale(2, RoundingMode.HALF_EVEN);
    // }

    // public String getProductId() {
    //     return productId;
    // }

    // public String getProductName() {
    //     return productName;
    // }

    // public String getImageURL() {
    //     return imageURL;
    // }

    // public void setImageURL(String imageURL) {
    //     this.imageURL = imageURL;
    // }

    // public String getQtyUnit() {
    //     return qtyUnit;
    // }

    // public void setQtyUnit(String qtyUnit) {
    //     this.qtyUnit = qtyUnit;
    // }

    // public int getQty() {
    //     return qty;
    // }

    // public void setQty(int qty) {
    //     this.qty = qty;
    // }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice.setScale(2, RoundingMode.HALF_EVEN);
    }

    public void setUnitPriceAfterDiscount(BigDecimal unitPriceAfterDiscount) {
        this.unitPriceAfterDiscount = unitPriceAfterDiscount.setScale(2, RoundingMode.HALF_EVEN);
    }
    
    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice =  totalPrice.setScale(2, RoundingMode.HALF_EVEN);;
    }

    public void setTotalPriceAfterDiscount(BigDecimal totalPriceAfterDiscount) {
        this.totalPriceAfterDiscount = totalPriceAfterDiscount.setScale(2, RoundingMode.HALF_EVEN);
    }

    // public String getProductURL() {
    //     return productURL;
    // }

    // public void setProductURL(String productURL) {
    //     this.productURL = productURL;
    // }


}
