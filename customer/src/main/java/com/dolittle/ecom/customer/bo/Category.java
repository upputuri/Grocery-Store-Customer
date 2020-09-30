package com.dolittle.ecom.customer.bo;

import org.springframework.hateoas.RepresentationModel;

public class Category extends RepresentationModel<Category>{
    private String id;
    private String name;
    private String image;

    //Synthetic properties
    private String imageURL;
    private String productListURL;

    public Category(String id, String name)
    {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public String getProductListURL() {
        return productListURL;
    }

    public void setProductListURL(String productListURL) {
        this.productListURL = productListURL;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
    
}
