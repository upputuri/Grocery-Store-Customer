package com.dolittle.ecom.customer.bo;

import java.math.BigDecimal;

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
    private BigDecimal originalPrice;
    private BigDecimal priceAfterDiscount;

    public OrderItem(String productId, String insvid, int qty)
    {
        super();
        this.productId = productId;
        this.insvid = insvid;
        this.qty = qty;
        this.orderItemStatus = "pending";
    }
}
