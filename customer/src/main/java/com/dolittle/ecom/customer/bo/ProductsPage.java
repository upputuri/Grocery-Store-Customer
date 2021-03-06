package com.dolittle.ecom.customer.bo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.hateoas.RepresentationModel;

import lombok.Data;

public @Data class ProductsPage extends RepresentationModel<ProductsPage>{
    private int offset = 0;
    private int size = 0;
    private int totalCount = 0;
    private List<Product> products = new ArrayList<Product>();
    private Map<String, String[]> filterOptions = new HashMap<String, String[]>();
}
