package com.dolittle.ecom.customer;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
 
    @Autowired
    private DataSource dataSource;
     
    @Autowired
    public void configAuthentication(AuthenticationManagerBuilder auth) throws Exception {
        auth.jdbcAuthentication().passwordEncoder(new BCryptPasswordEncoder())
            .dataSource(dataSource)
            .usersByUsernameQuery("select email, password, true from customer where email=?")
            .authoritiesByUsernameQuery("select email, 'USER' from customer where email=?")
        ;
        // PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        // auth.inMemoryAuthentication()
        //   .withUser("usrikanth@gmail.com")
        //   .password(encoder.encode("Password123"))
        //   .roles("USER");
    }
 
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable().cors().and()
                                .authorizeRequests().antMatchers("/me").authenticated()
                                // .antMatchers("/customers/*/cart/items").authenticated()
                                // .antMatchers("/customers/*/cart").authenticated()
                                .antMatchers("/customers/*").authenticated()
                                .antMatchers("/**").permitAll()
                                .and()
                                .httpBasic();
        // http.csrf().disable().cors().and().authorizeRequests().anyRequest().permitAll();
    }
}