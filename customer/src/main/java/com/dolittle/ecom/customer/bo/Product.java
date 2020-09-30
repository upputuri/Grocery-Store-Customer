package com.dolittle.ecom.customer.bo;

import java.util.Properties;
import java.util.Set;

import org.springframework.hateoas.RepresentationModel;

public class Product extends RepresentationModel<Product>{
    private String id;
    private String name;
    private String categoryId;
    private String image;
    private String imageURL;
    private String status;
    private String qtyUnit;
    private double price;
    private double markdownPrice;
    private boolean isInStock;
    private String offerLabel;
    private Properties productAttributes;
    private Set<String> tags;

    public Product(String id, String name, String image)
    {
        this.id = id;
        this.name = name;
        this.image = image;
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

    public String getQtyUnit() {
        return qtyUnit;
    }

    public void setQtyUnit(String qtyUnit) {
        this.qtyUnit = qtyUnit;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getMarkdownPrice() {
        return markdownPrice;
    }

    public void setMarkdownPrice(double markdownPrice) {
        this.markdownPrice = markdownPrice;
    }

    public boolean isInStock() {
        return isInStock;
    }

    public void setInStock(boolean isInStock) {
        this.isInStock = isInStock;
    }

    public String getOfferLabel() {
        return offerLabel;
    }

    public void setOfferLabel(String offerLabel) {
        this.offerLabel = offerLabel;
    }

    public Properties getProps() {
        return productAttributes;
    }

    public void setProps(Properties props) {
        this.productAttributes = props;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    
}
