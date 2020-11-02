package com.dolittle.ecom.app.security;

import com.dolittle.ecom.app.AppUser;
import com.dolittle.ecom.app.security.bo.OTPRequest;
import com.dolittle.ecom.customer.bo.Customer;
import com.dolittle.ecom.customer.bo.LoginSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
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
public class AccountService {
    
    @Autowired
    private JdbcTemplate jdbcTemplateObject;
    
    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String emailFromAddress;
    
    @GetMapping(value="/me", produces = "application/hal+json")
    public LoginSession getLoginSession(Authentication auth)
    {
        if (auth == null)
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error during authentication. Please try again or contact support!");

        AppUser user = (AppUser)auth.getPrincipal();
        Customer customer = null;
        try{
            String fetch_customer_sql = "select c.cuid, c.fname, c.lname, c.email, c.mobile, c.photo from customer c where c.uid=?";
            customer = jdbcTemplateObject.queryForObject(fetch_customer_sql, new Object[]{user.getUid()}, (rs, rowNum) -> {
                Customer c = new Customer();
                c.setId(String.valueOf(rs.getInt("cuid")));
                c.setFName(rs.getString("fname"));
                c.setLName(rs.getString("lname"));
                c.setEmail(rs.getString("email"));
                c.setImage(rs.getString("photo"));
                c.setMobile(rs.getString("mobile"));
                return c;
            });
        }
        catch(EmptyResultDataAccessException e)
        {
            log.error("Authentication failed with the supplied login credentials");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid login Id or Password");            
        }

        String get_cart_count_sql = "select COALESCE(sum(quantity),0) from cart_item where cartid=(select cartid from cart where cuid=?) "+
                                    "and cartisid=(select cartisid from cart_item_status where name='Active')";
        int cartItemCount = jdbcTemplateObject.queryForObject(get_cart_count_sql, new Object[]{customer.getId()}, Integer.TYPE);
        // TODO: Implement token/sessionid to avoid sending password each time.  
        // //A customer object is available
        // String update_old_sessions_sql = "update auser_session set ussid=2 where uid=?";
        // jdbcTemplateObject.update(update_old_sessions_sql, new Object[]{customer.getUid()}); 

        // //Generate a session id
        // String newSessionId = UUID.randomUUID().toString();
        // String clientIp = "";
        // String create_new_session_sql = "insert auser_session (uid, sid, ipaddress, ussid) values (?,?,?,1)";
        // jdbcTemplateObject.update(create_new_session_sql, new Object[]{customer.getUid(), newSessionId, clientIp});

        LoginSession loginSession = new LoginSession(null, customer, cartItemCount);
        return loginSession;
    }

    @PostMapping(value = "/me/otptokens")
    public void sendOTP(@RequestBody OTPRequest otpRequest, Authentication auth)
    {
        log.info("Processing send OTP request");
        
        AppUser appUser = (AppUser)auth.getPrincipal();

        if (otpRequest.getType().equals("email"))
        {
            //Send email OTP to customer
            SimpleMailMessage message = new SimpleMailMessage(); 
            message.setFrom(emailFromAddress);
            //TODO: hack, change it
            String toEmailId = appUser.getEmail();
            if (toEmailId != null && toEmailId.length() > 0){
                message.setTo(appUser.getEmail()); 
            }
            else {
                message.setTo("thevegitclub@gmail.com");
            }
            message.setSubject("OTP Confirmation"); 
            message.setText(otpRequest.getMessage().replace("{}", otpRequest.getOtp()));
            mailSender.send(message);
        }
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
