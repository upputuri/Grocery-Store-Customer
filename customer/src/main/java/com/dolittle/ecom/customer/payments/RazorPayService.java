package com.dolittle.ecom.customer.payments;

import java.security.Signature;
import java.security.SignatureException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import com.dolittle.ecom.app.CustomerConfig;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component(value = "razorpay")
public @Data class RazorPayService implements PGIService{
    @Autowired
    CustomerConfig config;

    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
    
    private final String id = "razorpay";

    @Value("${razorpaykey}")
    private String key;

    @Value("${razorpaysecret}")
    private String secret;

    @Override
    public String startTransaction(int amount, String transactionId) {
        try {
            RazorpayClient razorpay = new RazorpayClient(key, secret);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amount); // amount in the smallest currency unit
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "receipt_"+transactionId);
            
            JSONObject responseObject = new JSONObject();
            Order order = razorpay.Orders.create(orderRequest);
            responseObject.put("key", key);
            responseObject.put("order", order.toJson());
            return responseObject.toString();
        } catch (RazorpayException e) {
        // Handle Exception
            log.error(e.getMessage());
            return null;
        }
    }

    // 0 - success, 1 - signature 
    @Override
    public int validatePaymentResponse(String order, String response) {
        JSONObject responseJSON = new JSONObject(response);
        JSONObject orderJSON = new JSONObject(order);
        String response_payment_id = responseJSON.getString("razorpay_payment_id");
        String order_id = orderJSON.getJSONObject("order").getString("id");
        String response_signature = responseJSON.getString("razorpay_signature");
        try{
            if(response_payment_id != null && response_signature != null) {
                String signature = calculateRFC2104HMAC(order_id+ "|" +response_payment_id, secret);
                if (signature.equals(response_signature)) {
                    return 0; // Payment successful
                }
                else {
                    return 1; // Invalid signature, possible tampering of payment data
                }
            }
            else {
                return 2; // Transaction failed during payment
            }
        }
        catch(SignatureException e) {
            return 3; // Internal error, signature could not be generated.
        }
    }

    public static String calculateRFC2104HMAC(String data, String secret)
    throws java.security.SignatureException
    {
        String result;
        try {

            // get an hmac_sha256 key from the raw secret bytes
            SecretKeySpec signingKey = new SecretKeySpec(secret.getBytes(), HMAC_SHA256_ALGORITHM);

            // get an hmac_sha256 Mac instance and initialize with the signing key
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            mac.init(signingKey);

            // compute the hmac on input data bytes
            byte[] rawHmac = mac.doFinal(data.getBytes());

            // base64-encode the hmac
            result = DatatypeConverter.printHexBinary(rawHmac).toLowerCase();

        } catch (Exception e) {
            throw new SignatureException("Failed to generate HMAC : " + e.getMessage());
        }
        return result;
    }
}
