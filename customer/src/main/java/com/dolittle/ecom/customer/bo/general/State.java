package com.dolittle.ecom.customer.bo.general;

import org.springframework.hateoas.RepresentationModel;

import lombok.Data;

public @Data class State extends RepresentationModel<State>{
    private String stateId;
    private String name;
}
