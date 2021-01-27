package com.dolittle.ecom.customer.bo;

import org.springframework.hateoas.RepresentationModel;

import lombok.Data;

public @Data class WidgetData extends RepresentationModel<WidgetData>{
    int customersCount = 10000;
    int ordersCount = 100000;
    int productsCount = 100;
    int membersCount = 1000;
}
