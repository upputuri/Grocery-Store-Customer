package com.dolittle.ecom.customer.payments;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component(value = "stripe")
public @Data class StripeService implements PGIService {

    private final String id = "stripe";

    // @Value("${stripekey}")
    private String key;

    // @Value("${stripesecret}")
    private String secret;

    @Override
    public String startTransaction(int amount, String transactionId) {
        Stripe.apiKey = secret;
        try {
            SessionCreateParams params =
            SessionCreateParams.builder()
              .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
              .setMode(SessionCreateParams.Mode.PAYMENT)
              .setSuccessUrl("https://example.com/success")
              .setCancelUrl("https://example.com/cancel")
              .addLineItem(
              SessionCreateParams.LineItem.builder()
                .setQuantity(1L)
                .setPriceData(
                  SessionCreateParams.LineItem.PriceData.builder()
                    .setCurrency("INR")
                    .setUnitAmount(100L)
                    .setProductData(
                      SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName("T-shirt")
                        .build())
                    .build())
                .build())
              .build();
    
          Session session = Session.create(params);
          JSONObject responseObject = new JSONObject();
          responseObject.put("session", session.toJson());
          responseObject.put("key", key);
          return responseObject.toString();
        } catch (StripeException e) {
        // Handle Exception
            log.error(e.getMessage());
            return null;
        }
    }

    @Override
    public int validatePaymentResponse(String order, String response) {
        return 0;
    }
    
}
