package com.dolittle.ecom.app.security;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.web.server.ResponseStatusException;

public class GrocAuthEntryPoint extends BasicAuthenticationEntryPoint {

    public GrocAuthEntryPoint() {
        super.setRealmName("customer-runner");
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
            if (authException instanceof AccountExpiredException)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account Expired. Please contact support!");
            else if (authException instanceof BadCredentialsException)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, authException.getMessage());
    }
    
}
