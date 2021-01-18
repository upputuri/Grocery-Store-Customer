package com.dolittle.ecom.app.security;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.dolittle.ecom.app.AppUser;
import com.dolittle.ecom.app.security.bo.OTPRequest;
import com.dolittle.ecom.app.util.CustomerRunnerUtil;
import com.dolittle.ecom.app.util.NotificationsService;
import com.dolittle.ecom.customer.CustomerDataService;
import com.dolittle.ecom.customer.bo.Customer;
import com.dolittle.ecom.customer.bo.LoginSession;
import com.dolittle.ecom.customer.bo.Membership;

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
public class CustomerAccountService {
    
    @Autowired
    private JdbcTemplate jdbcTemplateObject;
    
    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private NotificationsService notifications;

    @Autowired
    private CustomerDataService customerInfo;

    @Value("${spring.mail.username}")
    private String emailFromAddress;
    
    @GetMapping(value="/customers/me", produces = "application/hal+json")
    public LoginSession getLoginSession(Authentication auth)
    {
        if (auth == null)
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error during authentication. Please try again or contact support!");

        Customer customer = CustomerRunnerUtil.fetchAuthCustomer(auth);
        AppUser user = (AppUser)auth.getPrincipal();
        
        String get_cart_count_sql = "select count(*) from cart_item where cartid=(select cartid from cart where cuid=?) "+
        "and cartisid=(select cartisid from cart_item_status where name='Active')";
        int cartItemCount = jdbcTemplateObject.queryForObject(get_cart_count_sql, new Object[]{customer.getId()}, Integer.TYPE);
        Membership membership = customerInfo.getCustomerMembership(customer.getId(), auth);
        customer.setMembershipId(membership.getMembershipId());
        // TODO: Implement token/sessionid to avoid sending password each time.  
        // //A customer object is available
        // String update_old_sessions_sql = "update auser_session set ussid=2 where uid=?";
        // jdbcTemplateObject.update(update_old_sessions_sql, new Object[]{customer.getUid()}); 
        
        // //Generate a session id
        // String newSessionId = UUID.randomUUID().toString();
        // String clientIp = "";
        // String create_new_session_sql = "insert auser_session (uid, sid, ipaddress, ussid) values (?,?,?,1)";
        // jdbcTemplateObject.update(create_new_session_sql, new Object[]{customer.getUid(), newSessionId, clientIp});
        log.info("User with username "+user.getUsername()+" authenticated successfully. Returning user profile and session data.");
        
        LoginSession loginSession = new LoginSession(null, customer, cartItemCount);
        return loginSession;
    }

    @PostMapping(value = "/customers/me/otptokens")
    public void sendPasswordResetOTP(@RequestBody OTPRequest otpRequest, Authentication auth)
    {
        log.info("Processing OTP request for password reset");
        
        Customer customer = CustomerRunnerUtil.fetchAuthCustomer(auth);
		if (otpRequest.getType().equals("mobile"))
		{
			Pattern PHONE_NUMBER_REGEX = Pattern.compile("^\\d{10}$");
			if (otpRequest.getTarget() != null) {
				Matcher phoneMatcher = PHONE_NUMBER_REGEX.matcher(otpRequest.getTarget());
				if (!phoneMatcher.find()) {
					log.error("Invalid mobile number supplied for password reset request");
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Phone number");
				}
			}else{
				log.error("Invalid mobile number supplied for password reset request");
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Phone number");
			}

			//Ensure account exists with this mobile
			int cuid = 0;
			try{
				cuid = jdbcTemplateObject.queryForObject("select cuid from customer where mobile=?", new Object[]{otpRequest.getTarget()}, Integer.TYPE);
			}catch(EmptyResultDataAccessException e){
				log.error("No account registered with this mobile number");
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No account registered with this mobile number");
            }

            String otp = CustomerRunnerUtil.generateOTP(6).toString();
            // notifications.sendNotification(request, customerId, auth);
        }
        if (otpRequest.getType().equals("email"))
        {
            //Send email OTP to customer
            SimpleMailMessage message = new SimpleMailMessage(); 
            message.setFrom(emailFromAddress);
            //TODO: hack, change it
            String toEmailId = customer.getEmail();
            if (toEmailId != null && toEmailId.length() > 0){
                message.setTo(customer.getEmail()); 
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
