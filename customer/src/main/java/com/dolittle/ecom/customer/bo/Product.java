package com.dolittle.ecom.customer.bo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.hateoas.RepresentationModel;

import lombok.Data;

public @Data class Product extends RepresentationModel<Product>{
    private final String id;
    private final String name;
    private String categoryId;
    private String[] images; 
    private final BigDecimal price;
    private BigDecimal discount;
    private String description;
    private boolean isInStock;
    private List<InventorySetVariation> variations = new ArrayList<InventorySetVariation>();
    private Map<String, Properties> attributes = new HashMap<String, Properties>();

    public Product(String id, String name, BigDecimal price)
    {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    
}
