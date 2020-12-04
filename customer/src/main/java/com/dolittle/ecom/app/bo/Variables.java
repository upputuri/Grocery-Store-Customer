package com.dolittle.ecom.app.bo;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.MapSerializer;

import org.springframework.hateoas.RepresentationModel;

import lombok.Data;

public @Data class Variables extends RepresentationModel<Variables>{
    @JsonSerialize(keyUsing = MapSerializer.class)
    private Map<String, Object> variables = new HashMap<String, Object>();
}
