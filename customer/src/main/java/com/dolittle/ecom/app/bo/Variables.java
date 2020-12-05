package com.dolittle.ecom.app.bo;

import java.util.HashMap;
import java.util.Map;

import org.springframework.hateoas.RepresentationModel;

import lombok.Data;

public @Data class Variables extends RepresentationModel<Variables>{
    private Map<String, Object> variables = new HashMap<String, Object>();
}
