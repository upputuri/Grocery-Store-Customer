package com.dolittle.ecom.customer.bo;

import org.springframework.hateoas.RepresentationModel;

import lombok.Data;

public @Data class MPlanCategory extends RepresentationModel<MPlanCategory>{
    private String categoryName;
    private String categoryId;
}
