package com.dolittle.ecom.app.security.bo;

import org.springframework.hateoas.RepresentationModel;

import lombok.Data;

public @Data class PasswordOverwrite extends RepresentationModel<PasswordOverwrite>{
    String mobile;
    String otp;
    String password;
}
