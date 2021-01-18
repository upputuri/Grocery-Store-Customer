package com.dolittle.ecom.customer.bo;

import org.springframework.hateoas.RepresentationModel;

import lombok.Data;

public @Data class Member extends RepresentationModel<Member> {
    private String fName;
    private String lName;
    private String gender;
    private String email;
    private String dob;
    private String mobile;
    private String altMobile;
    private String presentAddress;
    private String presentPinCode;
    private String adhaarFrontImg;
    private String adhaarBackImg;
    private String photoImg;
}
