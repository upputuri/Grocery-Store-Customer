package com.dolittle.ecom.app.util;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;

import com.dolittle.ecom.customer.bo.Customer;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomerRunnerUtil {

    public static String generateSHA256PasswordHash(String password)
    {
        if (password == null || password.trim().equals("") || password.length()<4)
            throw new IllegalArgumentException("Invalid password input given to hash function");
            
        String passwordHash = "";
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.reset();
            digest.update(password.getBytes("utf8"));
            passwordHash = String.format("%064x", new BigInteger(1, digest.digest()));
        }
        catch(UnsupportedEncodingException e)
        {
            log.error("An SQL exception occurred", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password contains invalid characters.");
        }
        catch(NoSuchAlgorithmException e)
        {
            log.error("The algorithm used for message digest seems to be unrecognized. Please check the code and fix as necessary");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred!, pls retry after some time or pls call support");
        }

        return passwordHash;
    }

    public static String generateBcryptPasswordHash(String password)
    {
        return new BCryptPasswordEncoder().encode(password);
    }

    public static Customer assertAuthCustomerId(JdbcTemplate jdbcTemplateObject, Principal principal, String customerId)
    {
        Customer customer = null;
        String get_customer_profile_query = "select c.cuid, c.uid, c.email, c.fname, c.lname from customer c, auser au "+
                                    "where au.uid = c.uid and au.user_id = ? and c.cuid = ? and c.custatusid = (select custatusid from customer_status where name='Active')";
        try{
            customer = jdbcTemplateObject.queryForObject(get_customer_profile_query, new Object[]{principal.getName(), customerId}, (rs, rownum) -> {
                Customer c = new Customer();
                c.setId(String.valueOf(rs.getInt("cuid")));
                c.setUid(String.valueOf(rs.getInt("uid")));
                c.setEmail(rs.getString("email"));
                c.setFName(rs.getString("fname"));
                c.setLName(rs.getString("lname"));
                return c;
            });
        }
        catch(EmptyResultDataAccessException e)
        {
            log.error("Requested customer Id does not match with authenticated user or the customer is inactive");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You do not have permission to view details of the provided customer Id");
        }
        return customer;
    }

    public static void main(String st[])
    {
        System.out.println(new BCryptPasswordEncoder().encode("Password123"));
    }
}
