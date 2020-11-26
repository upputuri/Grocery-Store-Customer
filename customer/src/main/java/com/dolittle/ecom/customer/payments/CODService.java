package com.dolittle.ecom.customer.payments;

import org.springframework.stereotype.Component;

@Component(value="cod")
public class CODService implements PGIService{

    @Override
    public String startTransaction(int amount, String receiptId) {
        return "{\"id\": \"cod\"}";
    }

    @Override
    public int validatePaymentResponse(String order, String response) {
        return 0;
    }

    @Override
    public String getId() {
        return "cod";
    }
    
}
