package com.dolittle.ecom.customer.util;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomerAppUtil {
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

    public static void main(String st[])
    {
        System.out.println(new BCryptPasswordEncoder().encode("Password123"));
    }
}
