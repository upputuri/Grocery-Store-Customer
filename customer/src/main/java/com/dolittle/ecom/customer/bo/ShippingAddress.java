package com.dolittle.ecom.customer.bo;

import org.springframework.hateoas.RepresentationModel;

import lombok.Data;

public @Data class ShippingAddress extends RepresentationModel<ShippingAddress>{
    private String id;
    private boolean isDefault;
    private boolean label;
    private String firstName;
    private String lastName;
    private final String line1;
    private String line2;
    private final String city;
    private String state;
    private int stateId;
    private String country;
    private final String zipcode;
    private final String phoneNumber;

    public ShippingAddress(String line1, String city, String zipcode, String phone)
    {
        super();
        this.line1 = line1;
        this.city = city;
        this.zipcode = zipcode;
        this.phoneNumber = phone;
    }

    // public String getFirstName() {
    //     return firstName;
    // }

    // public void setFirstName(String firstName) {
    //     this.firstName = firstName;
    // }

    // public String getLastName() {
    //     return lastName;
    // }

    // public void setLastName(String lastName) {
    //     this.lastName = lastName;
    // }

    // public String getLine1() {
    //     return line1;
    // }

    // public String getLine2() {
    //     return line2;
    // }

    // public void setLine2(String line2) {
    //     this.line2 = line2;
    // }

    // public String getCity() {
    //     return city;
    // }

    // public String getState() {
    //     return state;
    // }

    // public void setState(String state) {
    //     this.state = state;
    // }

    // public String getCountry() {
    //     return country;
    // }

    // public void setCountry(String country) {
    //     this.country = country;
    // }

    // public String getZipcode() {
    //     return zipcode;
    // }

    // public String getId() {
    //     return id;
    // }

    // public void setId(String id) {
    //     this.id = id;
    // }
}
