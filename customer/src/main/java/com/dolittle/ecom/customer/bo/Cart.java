package com.dolittle.ecom.customer.bo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import org.springframework.hateoas.RepresentationModel;

import lombok.Data;

public  @Data class Cart extends RepresentationModel<Cart>{
    private List<CartItem> cartItems = new ArrayList<CartItem>();
    @JsonProperty(access = Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal cartTotal = BigDecimal.ZERO;

    public void setCartItems(List<CartItem> cartItems){
        if (cartItems != null)
        {
            this.cartItems = cartItems;
            for (CartItem ci : cartItems)
            {
                this.cartTotal = this.cartTotal.add(ci.getTotalPriceAfterDiscount());
            }
        }
    }
}
