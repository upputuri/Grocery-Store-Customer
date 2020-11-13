package com.dolittle.ecom.app.security;

import static org.apache.commons.codec.digest.MessageDigestAlgorithms.MD5;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_1;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class GrocPasswordEncoder implements PasswordEncoder {

    private String PASSWORD_SALT = "K<47`5n9~8H5`*^Ks.>ie5&";

    @Override
    public String encode(CharSequence rawPassword) {
            String hashedSalt = new DigestUtils(MD5).digestAsHex(PASSWORD_SALT);
            String hashedPassword = new DigestUtils(SHA_1).digestAsHex(hashedSalt+rawPassword.toString());
            return hashedPassword;
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        String hashedPassword = encode(rawPassword);
        return CharSequence.compare(encodedPassword, hashedPassword) == 0;
    }
    
}
