package com.dolittle.ecom.app;

import com.dolittle.ecom.app.security.GrocAuthEntryPoint;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    PasswordEncoder passwordEncoder;
     
    @Autowired
    public void configAuthentication(AuthenticationManagerBuilder auth) throws Exception {
        // auth.jdbcAuthentication().passwordEncoder(new BCryptPasswordEncoder())
        //     .dataSource(dataSource)
        //     .usersByUsernameQuery("select email, password, true from customer where email=?")
        //     .authoritiesByUsernameQuery("select email, 'CUSTOMER' from customer where email=?")
        // ;
        //auth.authenticationProvider(GrocGoogleAuthProvider)
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder);
        // PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        // auth.inMemoryAuthentication()
        //   .withUser("usrikanth@gmail.com")
        //   .password(encoder.encode("Password123"))
        //   .roles("USER");
    }
 
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        AuthenticationEntryPoint authenticationEntryPoint = new GrocAuthEntryPoint();
        http.csrf().disable().cors().and()
                                .authorizeRequests().antMatchers("/me").authenticated()
                                .antMatchers("/customers/*/cart/items").authenticated()
                                .antMatchers("/customers/*/cart").authenticated()
                                .antMatchers("/customers/*/addresses", "/customers/*/addresses/*").authenticated()
                                .antMatchers("/orders/*").authenticated()
                                .antMatchers("runners/**").authenticated()
                                .antMatchers("/**").permitAll()
                                .and()
                                .httpBasic();//.authenticationEntryPoint(authenticationEntryPoint);
        // http.csrf().disable().cors().and().authorizeRequests().anyRequest().permitAll();
    }
}