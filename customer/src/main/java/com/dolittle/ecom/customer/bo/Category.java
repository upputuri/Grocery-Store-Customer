package com.dolittle.ecom.customer.bo;

import org.springframework.hateoas.RepresentationModel;

import lombok.Data;

public @Data class Category extends RepresentationModel<Category>{
    private String id;
    private int rank;
    private String name;
    private String title;
    private String keywords;
    private String description;
    private String metaDescription;
    private String image;

    public Category(String id, String name)
    {
        this.id = id;
        this.name = name;
    }

    
}
