package com.dolittle.ecom.customer.bo;

import java.util.Calendar;

import org.springframework.hateoas.RepresentationModel;

import lombok.Data;

public @Data class Membership extends RepresentationModel<Membership>{
    private String membershipId;
    private Transaction transaction;
    private String customerId;
    private MPlan plan;
    private Member member;
    private Nominee nominee;
    private Calendar startDate = Calendar.getInstance();
    private Calendar endDate = Calendar.getInstance();
    private Calendar renewalDate = Calendar.getInstance();
}
