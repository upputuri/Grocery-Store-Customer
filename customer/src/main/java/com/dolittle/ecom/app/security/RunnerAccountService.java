package com.dolittle.ecom.app.security;

import com.dolittle.ecom.app.security.bo.OTPRequest;
import com.dolittle.ecom.app.util.CustomerRunnerUtil;
import com.dolittle.ecom.customer.bo.Customer;
import com.dolittle.ecom.runner.Runner;
import com.dolittle.ecom.runner.RunnerLoginSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class RunnerAccountService {
    
    @Autowired
    private JdbcTemplate jdbcTemplateObject;
    
    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String emailFromAddress;
    
    @GetMapping(value="/runners/me", produces = "application/hal+json")
    public RunnerLoginSession getLoginSession(Authentication auth)
    {
        if (auth == null)
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error during authentication. Please try again or contact support!");

        Runner runner = CustomerRunnerUtil.fetchAuthRunner(auth);
        // TODO: Implement token/sessionid to avoid sending password each time.  

        RunnerLoginSession loginSession = new RunnerLoginSession(null, runner);
        return loginSession;
    }

    @PostMapping(value = "/runners/me/otptokens")
    public void sendOTP(@RequestBody OTPRequest otpRequest, Authentication auth)
    {
        // log.info("Processing send OTP request");
        
        // Runner runner = CustomerRunnerUtil.fetchAuthRunner(auth);

        // if (otpRequest.getType().equals("email"))
        // {
        //     //Send email OTP to customer
        //     SimpleMailMessage message = new SimpleMailMessage(); 
        //     message.setFrom(emailFromAddress);
        //     //TODO: hack, change it
        //     String toEmailId = customer.getEmail();
        //     if (toEmailId != null && toEmailId.length() > 0){
        //         message.setTo(customer.getEmail()); 
        //     }
        //     else {
        //         message.setTo("thevegitclub@gmail.com");
        //     }
        //     message.setSubject("OTP Confirmation"); 
        //     message.setText(otpRequest.getMessage().replace("{}", otpRequest.getOtp()));
        //     mailSender.send(message);
        // }
    }


    // public static void main(String st[]) throws Exception
    // {
    //     MessageDigest digest = MessageDigest.getInstance("SHA-256");
    //     digest.reset();
    //     digest.update("Password123".getBytes("utf8"));
    //     String toReturn = String.format("%064x", new BigInteger(1, digest.digest()));
    //     System.out.println(toReturn);
    // }
}
