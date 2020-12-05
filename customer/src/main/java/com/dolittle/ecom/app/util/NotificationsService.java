package com.dolittle.ecom.app.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.dolittle.ecom.app.bo.NotificationRequest;
import com.dolittle.ecom.app.sms.SMSServiceProvider;
import com.dolittle.ecom.customer.bo.Customer;
import com.mysql.cj.util.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class NotificationsService {
    
    @Value("${spring.mail.username}")
    private String emailFrom;
    
    @Autowired
	private JavaMailSender mailSender;

	@Autowired
    private SMSServiceProvider smsSender;
    
    @PostMapping(value = "/customers/{customerId}/notifications", produces = "application/hal+json")
    public void sendNotification(@RequestBody NotificationRequest request, @PathVariable String customerId, Authentication auth) {
        Customer customer = CustomerRunnerUtil.fetchAuthCustomer(auth);

        if (request.getType().equalsIgnoreCase("email") && !StringUtils.isNullOrEmpty(customer.getEmail())) {
            sendEmailNotification(customer.getEmail(), request.getSubject(), request.getMessage());
        }
        else if (request.getType().equalsIgnoreCase("mobile")) {
            sendSMSNotification(customer.getMobile(), request.getMessage());
        }
    }

    private void sendEmailNotification(String emailTo, String subject, String message){
        try{
            SimpleMailMessage eMailMessage = new SimpleMailMessage(); 
            eMailMessage.setFrom(emailFrom);

            Pattern EMAIL_ADDRESS_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

            Matcher emailMatcher = EMAIL_ADDRESS_REGEX.matcher(emailTo);
            if (emailTo != null && emailTo.length() > 0 && emailMatcher.find()){
                eMailMessage.setTo(emailTo); 
            }
            else {
                log.error("Bad request. Invalid target email Id supplied: "+emailTo);
            }
            eMailMessage.setSubject(subject); 
            eMailMessage.setText(message);
            mailSender.send(eMailMessage);
            log.info("Notification sent to requested target email");
        }
        catch(Exception e) {
            log.error("An error occured while sending email notification", e);
        }
    }

    private void sendSMSNotification(String numbers, String message){
        int code = 0;
        try{
            code = smsSender.sendNotification(numbers, message);
            if (code == 0){
                log.info("OTP dispatched to SMS service successfully!");
            }
            else{
                log.error("Dispatching OTP to SMS service failed. Please check SMS service logs for service specific error codes");
            }
        }
        catch(Exception e){
            log.error("An error occured while sending SMS notification", e);
        }
    }
}
