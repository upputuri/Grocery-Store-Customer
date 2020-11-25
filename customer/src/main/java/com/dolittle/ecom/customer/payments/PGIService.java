package com.dolittle.ecom.customer.payments;

public interface PGIService {
    public String getId();
    public String startTransaction(int amount, String receiptId);
    public int validatePaymentResponse(String order, String response);
}
