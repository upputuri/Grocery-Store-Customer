package com.dolittle.ecom.customer.bo;

import org.springframework.hateoas.RepresentationModel;

import lombok.Data;

public @Data class Relationship extends RepresentationModel<Relationship>{
    private String relationshipId;
    private String relationshipName;
}
