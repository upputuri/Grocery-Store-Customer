package com.dolittle.ecom.app.bo;

import java.util.List;

import org.springframework.hateoas.RepresentationModel;

import lombok.Data;

public @Data class CoverCity extends RepresentationModel<CoverCity>{
    private String name;
    private String stateId;
    private String state;
    private String[] pinCodes;
}
