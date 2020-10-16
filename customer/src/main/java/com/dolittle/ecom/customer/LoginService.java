package com.dolittle.ecom.customer;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.Principal;

import com.dolittle.ecom.customer.bo.Customer;
import com.dolittle.ecom.customer.bo.LoginCredentials;
import com.dolittle.ecom.customer.bo.LoginSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class LoginService {
    
    @Autowired
    JdbcTemplate jdbcTemplateObject;
    
    @GetMapping(value="/me", produces = "application/hal+json")
    public LoginSession getLoginSession(Principal principal)
    {
        if (principal == null)
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error during authentication. Please try again or contact support!");

        Customer customer = null;
        try{
            String fetch_customer_sql = "select c.cuid, c.fname, c.lname, c.email, c.photo from customer c where c.email=?";
            customer = jdbcTemplateObject.queryForObject(fetch_customer_sql, new Object[]{principal.getName()}, (rs, rowNum) -> {
                Customer c = new Customer();
                c.setId(String.valueOf(rs.getInt("cuid")));
                c.setFName(rs.getString("fname"));
                c.setLName(rs.getString("lname"));
                c.setEmail(rs.getString("email"));
                c.setImage(rs.getString("photo"));
                return c;
            });
        }
        catch(EmptyResultDataAccessException e)
        {
            log.error("Authentication failed with the supplied login credentials");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid login Id or Password");            
        }

        String get_cart_count_sql = "select COALESCE(sum(quantity),0) from cart_item where cartid=(select cartid from cart where cuid=?)";
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


    public static void main(String st[]) throws Exception
    {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.reset();
        digest.update("Password123".getBytes("utf8"));
        String toReturn = String.format("%064x", new BigInteger(1, digest.digest()));
        System.out.println(toReturn);
    }
}
