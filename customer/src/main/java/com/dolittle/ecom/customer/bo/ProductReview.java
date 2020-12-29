package com.dolittle.ecom.customer.bo;

import org.springframework.hateoas.RepresentationModel;

import lombok.Data;

public @Data class ProductReview extends RepresentationModel<ProductReview>{
    private String productId;
    private int rating;
    private String reviewTitle;
    private String reviewDetail;
}
