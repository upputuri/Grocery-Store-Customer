package com.dolittle.ecom.app.bo;

import java.util.HashMap;
import java.util.Map;

import org.springframework.hateoas.RepresentationModel;

import lombok.Data;

public @Data class AddressTypes extends RepresentationModel<AddressTypes>{
    Map<String, String> types = new HashMap<String, String>();
}
