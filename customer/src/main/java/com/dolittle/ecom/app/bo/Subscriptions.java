package com.dolittle.ecom.app.bo;

import org.springframework.hateoas.RepresentationModel;

import lombok.Data;

public @Data class Subscriptions extends RepresentationModel<Subscriptions>{
    private boolean newsletter = false;
}
