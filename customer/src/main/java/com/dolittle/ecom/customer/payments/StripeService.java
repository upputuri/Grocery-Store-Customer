package com.dolittle.ecom.customer.payments;

import javax.annotation.PostConstruct;

import com.google.gson.JsonSyntaxException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.stripe.model.StripeObject;
import com.stripe.net.ApiResource;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;

import com.google.gson.JsonSyntaxException;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component(value = "stripe")
@RestController
public @Data class StripeService implements PGIService {

    private final String id = "stripe";

    @Value("${stripekey}")
    private String key; 

    @Value("${stripesecret}")
    private String secret;

    @PostConstruct
    public void init() {
      Stripe.apiKey = secret;
    }

    @Override
    public String startTransaction(int amount, String transactionId) {
        try {
            SessionCreateParams params =
            SessionCreateParams.builder()
              .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
              .setMode(SessionCreateParams.Mode.PAYMENT)
              .setSuccessUrl("http://localhost:8100/support")
              .setCancelUrl("http://localhost:8100/login")
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

    @PostMapping(value = "/stripeevents", produces = "application/hal+json")
    public void handleStripeWebHookEvents(@RequestBody String payload)
    {
        Event event = null;

        try {
            event = ApiResource.GSON.fromJson(payload, Event.class);
        } catch (JsonSyntaxException e) {
          // Invalid payload
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
      
        // Deserialize the nested object inside the event
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = null;
        if (dataObjectDeserializer.getObject().isPresent()) {
          stripeObject = dataObjectDeserializer.getObject().get();
        } else {
          // Deserialization failed, probably due to an API version mismatch.
          // Refer to the Javadoc documentation on `EventDataObjectDeserializer` for
          // instructions on how to handle this case, or return an error here.
        }
      
        // Handle the event
        switch (event.getType()) {
          case "checkout.session.completed":
            Session paymentSession = (Session) stripeObject;
            System.out.println("Payment session complete!");
            break;
          case "payment_intent.succeeded":
            PaymentIntent paymentIntent = (PaymentIntent) stripeObject;
            System.out.println("PaymentIntent was successful!");
            break;
          case "payment_method.attached":
            PaymentMethod paymentMethod = (PaymentMethod) stripeObject;
            System.out.println("PaymentMethod was attached to a Customer!");
            break;
          // ... handle other event types
          default:
            System.out.println("Unhandled event type: " + event.getType());
        }
    }
    
}
