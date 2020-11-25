package com.dolittle.ecom.app.util;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.dolittle.ecom.app.AppUser;
import com.dolittle.ecom.app.security.GrocPasswordEncoder;
import com.dolittle.ecom.customer.bo.Customer;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    public static Customer validateAndGetAuthCustomer(Authentication auth, String customerId)
    {
        try{
            Customer c = (Customer)((AppUser)auth.getPrincipal()).getQualifiedUser();
            if (!customerId.equals(c.getId())) {
                throw new Exception();
            }
            return c;
        }
        catch(Exception e)
        {
            log.error("Requested customer Id does not match with authenticated user or the customer is inactive");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You do not have permission to view details of the provided customer Id");
        }
    }

    public static Customer fetchAuthCustomer(Authentication auth)
    {
        try{
            Customer c = (Customer)((AppUser)auth.getPrincipal()).getQualifiedUser();
            return c;
        }
        catch(Exception e)
        {
            log.error("Unknown user");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown user");
        }
    }

    public static char[] generatePassword(int length) {
        String capitalCaseLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCaseLetters = "abcdefghijklmnopqrstuvwxyz";
        String specialCharacters = "!@#$";
        String numbers = "1234567890";
        String combinedChars = capitalCaseLetters + lowerCaseLetters + specialCharacters + numbers;
        Random random = new Random();
        char[] password = new char[length];
  
        password[0] = lowerCaseLetters.charAt(random.nextInt(lowerCaseLetters.length()));
        password[1] = capitalCaseLetters.charAt(random.nextInt(capitalCaseLetters.length()));
        password[2] = specialCharacters.charAt(random.nextInt(specialCharacters.length()));
        password[3] = numbers.charAt(random.nextInt(numbers.length()));
     
        for(int i = 4; i< length ; i++) {
           password[i] = combinedChars.charAt(random.nextInt(combinedChars.length()));
        }
        return password;
     }
     
    public static void main(String st[])
    {
        // PasswordEncoder encoder = new GrocPasswordEncoder();
        // System.out.println(encoder.encode("Abc123"));
        // Pattern csv_regex = Pattern.compile("(?:^|,)\s*(?:(?:(?=\")\"([^\"].*?)\")|(?:(?!\")(.*?)))(?=,|$)", Pattern.CASE_INSENSITIVE);
        // Matcher csv_matcher = csv_regex.matcher("abc,def,aldkj,slfkjsf");
       
        //Pattern PHONE_NUMBER_REGEX = Pattern.compile("^\\d{10}$");
        //Matcher phoneMatcher = PHONE_NUMBER_REGEX.matcher(username);

        String date = "2020-11-25T11:26:22+05:30";
        ZonedDateTime zdt = ZonedDateTime.parse(date);
        System.out.println(zdt.getYear()+"-"+zdt.getMonthValue()+"-"+zdt.getDayOfMonth());
    }
}
