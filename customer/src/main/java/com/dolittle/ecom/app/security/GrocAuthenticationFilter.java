package com.dolittle.ecom.app.security;

import java.io.IOException;
import java.util.Base64;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

public class GrocAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    private AuthenticationManager authenticationManager;

    public GrocAuthenticationFilter(AuthenticationManager manager) {
        this.authenticationManager = manager;
        setFilterProcessesUrl("/*");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        Authentication requestAuthentication;
        String token= request.getHeader("Authorization");
        if (token.startsWith("Basic")) {
            // Basic auth
            token = StringUtils.removeStart(token, "Basic").trim();
            String[] userDetails = decode(token);
            requestAuthentication = new UsernamePasswordAuthenticationToken(userDetails[0], userDetails[1]);
        }
        else if (token.startsWith("Bearer")) {
            token = StringUtils.removeStart(token, "Bearer").trim();
            // requestAuthentication = new 
        }
        // return getAuthenticationManager().authenticate(requestAuthentication);
        return null;
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
            Authentication authResult) throws IOException, ServletException {
        // TODO Auto-generated method stub
        super.successfulAuthentication(request, response, chain, authResult);
    }

    private static String[] decode(final String encoded) {
        final byte[] decodedBytes 
                = Base64.getDecoder().decode(encoded.getBytes());
        final String pair = new String(decodedBytes);
        final String[] userDetails = pair.split(":", 2);
        return userDetails;
    }
}
