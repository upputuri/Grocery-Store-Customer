package com.dolittle.ecom.customer.bo;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.hateoas.RepresentationModel;

import lombok.Data;

public @Data class Customer extends RepresentationModel<Customer>
{
    private String id;
    private String uid;
    private String fName;
    private String lName;
    private String email;
    private String altEmail;
    private String dob;
    private String mobile;
    private String altMobile;
    private String password;
    private String image;
    private List<ShippingAddress> shippingAddresses = new ArrayList<ShippingAddress>();

    // public Customer(String id, String uid)
    // {
    //     this.id = id;
    //     this.uid = uid;
    // }
}
