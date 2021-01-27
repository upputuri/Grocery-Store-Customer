package com.dolittle.ecom.app.security;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.dolittle.ecom.app.AppUser;
import com.dolittle.ecom.app.CustomerRunner;
import com.dolittle.ecom.app.security.bo.OTPRequest;
import com.dolittle.ecom.app.security.bo.PasswordOverwrite;
import com.dolittle.ecom.app.util.CustomerRunnerUtil;
import com.dolittle.ecom.app.util.NotificationsService;
import com.dolittle.ecom.customer.CustomerDataService;
import com.dolittle.ecom.customer.bo.Customer;
import com.dolittle.ecom.customer.bo.LoginSession;
import com.dolittle.ecom.customer.bo.Membership;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    @Autowired
    private CustomerRunner application;

    @Autowired
    PasswordEncoder passwordEncoder;

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

    @DeleteMapping(value = "/customers/me")
    public void deactivateAccount(Authentication auth){
        log.info("Processing account delete request from user");
        Customer customer = CustomerRunnerUtil.fetchAuthCustomer(auth);
        try{
            String delete_account_sql = "update customer set custatusid= (select custatusid from customer_status where name='Inactive') where cuid=?";
            jdbcTemplateObject.update(delete_account_sql, new Object[]{customer.getId()});
        }catch(DataAccessException e){
            log.error("An error occurred", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred!, pls retry after some time or pls call support");
        }
    }

    @PostMapping(value = "/customers/me")
    public void resetPasswordWithOTP(@RequestBody PasswordOverwrite overwrite){
        log.info("Processing password reset using an OTP token");
        String mobile = overwrite.getMobile();
        String otp = overwrite.getOtp();
        String password = overwrite.getPassword();
        String passwordHash = passwordEncoder.encode(password);
        if (otp != null && otp.length() > 0 && password != null && password.length() > 0) {
            String password_reset_sql = "update customer set data=null, password=? where mobile = ? and data = ? and custatusid in "+
                "(select custatusid from customer_status where (name = 'Active' or name = 'Email UnVerified'))";
            int rows = jdbcTemplateObject.update(password_reset_sql, new Object[] {passwordHash, mobile, otp});
            if (rows == 0){
                String msg = "Either the OTP is invalid or no account is in a state to accept OTP based password reset request";
                log.error(msg);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
            }
        }
        else {
            log.error("Invalid data. Either otp or password or both are missing in request");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input data received. Please check the request");
        }
    }

    @PostMapping(value = "/customers/me/otptokens")
    public void getPasswordResetOTP(@RequestBody OTPRequest otpRequest)
    {
        log.info("Processing OTP request for password reset");
        
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
            String otp;
			try{
                String get_customer_id = "select c.cuid from customer c, "+
                    "customer_status cs where c.mobile = ? and cs.custatusid=c.custatusid and (cs.name = 'Active' or cs.name = 'Email UnVerified')";
				cuid = jdbcTemplateObject.queryForObject(get_customer_id, new Object[]{otpRequest.getTarget()}, Integer.TYPE);
                otp = new String(CustomerRunnerUtil.generateOTP(6));
                jdbcTemplateObject.update("update customer set data = ? where cuid = ?", new Object[]{otp, cuid});
			}catch(EmptyResultDataAccessException e){
                log.error("Attempt to reset password for a non-existent or in mobile number");
                return;
            }catch(DataAccessException e){
                log.error("An error occurred", e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred!, pls retry after some time or pls call support");
            }

            otpRequest.setOtp(otp);
            application.sendOTP(otpRequest);
            // notifications.sendNotification(request, customerId, auth);
        }
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
