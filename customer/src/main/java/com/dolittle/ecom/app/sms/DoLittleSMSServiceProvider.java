package com.dolittle.ecom.app.sms;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component(value = "dolittle")
public class DoLittleSMSServiceProvider implements SMSServiceProvider {

    @Value("${dolittlesmsapikey}")
    String key;

    @Value("${dolittlesmsapiurl}")
    String apiURL;

    @Value("${dolittlesmsapiotproute:4}")
    String otpRoute;
    
    @Value("${dolittlesmsapinotificationroute:4}")
    String notificationRoute;

    @Value("${dolittlesmsapisender}")
    String senderId;

    @Override
    public int sendOTP(String numbers, String message) {
        return sendMessage(numbers, otpRoute, message);
    }
    
    @Override
    public int sendNotification(String numbers, String message) {
        return sendMessage(numbers, notificationRoute, message);
    }

    private int sendMessage(String numbers, String route, String message) {
        try {
            String url = apiURL + "?key=" + key + "&route=" + route + "&sender=" + senderId + "&number=" + numbers
                    + "&sms=" + URLEncoder.encode(message, StandardCharsets.UTF_8.toString());
            RestTemplate restClient = new RestTemplate();
            log.info("Sending SMS with url: " + url);
            ResponseEntity<String> response = restClient.getForEntity(url, String.class);
            if (response.getStatusCode().equals(HttpStatus.OK)) {
                int responseCode = Integer.parseInt(response.getBody());
                if (responseCode > 101 && responseCode < 109) {
                    log.info("SMS api responded with error response. Response code is :" + responseCode);
                    return 1;
                } else {
                    log.info("SMS has been sent and the response code is: " + response.getBody());
                    return 0;
                }
            } else {
                return 1;
            }
        } catch (Exception e) {
            log.error("Unable to send OTP sms. Failed with exception", e);
        }
        return 1;
    }

    
    
}
