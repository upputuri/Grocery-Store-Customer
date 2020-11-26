package com.dolittle.ecom.app.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class GrocGoogleAuthProvider implements AuthenticationProvider {

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String name = authentication.getName();
        String password = authentication.getCredentials().toString();
        
 
            // use the credentials
            // and authenticate against the third-party system
            // return new UsernamePasswordAuthenticationToken(
            //   name, password, new ArrayList<>());
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return false;
    }
    
}
