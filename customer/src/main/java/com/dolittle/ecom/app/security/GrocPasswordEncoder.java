package com.dolittle.ecom.app.security;

import static org.apache.commons.codec.digest.MessageDigestAlgorithms.MD5;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_1;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class GrocPasswordEncoder implements PasswordEncoder {

    @Value("${passwordsalt}")
    private String PASSWORD_SALT;

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
