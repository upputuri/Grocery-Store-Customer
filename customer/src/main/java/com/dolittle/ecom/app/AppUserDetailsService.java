package com.dolittle.ecom.app;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.dolittle.ecom.customer.bo.Customer;

@Component
public class AppUserDetailsService implements UserDetailsService {

    @Autowired
    JdbcTemplate jdbcTemplateObject;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Pattern EMAIL_ADDRESS_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
        // Matcher emailMatcher = EMAIL_ADDRESS_REGEX.matcher(username);
        //Pattern PHONE_NUMBER_REGEX = Pattern.compile("^\\d{10}$");
        //Matcher phoneMatcher = PHONE_NUMBER_REGEX.matcher(username);

        // String user_id_match_sql = " au.user_id=?";
        // String email_match_sql = " au.email=?";
        // String get_user_sql = "select au.uid, au.user_id, au.name, au.email, au.password, aus.name as user_status, ar.name as role_name, ars.name as role_status "+
        //                         "from auser au, auser_status as aus, arole as ar, arole_status ars, auser_role as aur "+
        //                         " where au.ustatusid = aus.ustatusid and ars.rsid = ar.rsid and ar.rid=aur.rid and aur.uid = au.uid and "+
        //                         (emailMatcher.find() ? email_match_sql : user_id_match_sql);
        
        // String email_match_sql = " cu.email=?";
        // String mobile_match_sql = " cu.mobile=?";
        // String provider = "S";
        // String userType = "C";
        // if (username.startsWith("("))
        // {
        //     String loginType = username.substring(0, 4);
        //     provider = loginType.substring(1, 2);
        //     userType = loginType.substring(2, 3);
        // }

        // if (provider.equals("G")) // Google
        // {
        //     // Validate token
        // }
        String get_customer_profile_query = "select c.uid, c.cuid, c.email, c.password, c.fname, c.lname, c.dob, c.mobile, c.alt_email, c.alt_mobile, cs.name as user_status from customer c, "+
                                    "customer_status cs where (c.mobile = ? or c.email = ?) and cs.custatusid=c.custatusid";
        
        AppUser appUser;
        try{
            appUser = jdbcTemplateObject.queryForObject(get_customer_profile_query, new Object[]{username, username}, (rs, rowNum) -> {
                GrantedAuthority ga = new SimpleGrantedAuthority("customer");
                UserDetails user = User.withUsername(username)
                                .password(rs.getString("password"))
                                .accountExpired(rs.getString("user_status").equals("Active") || rs.getString("user_status").equals("Email Unverified") ? false : true)
                                .accountLocked(false)
                                .credentialsExpired(false)
                                .authorities(ga).build();
                AppUser au = new AppUser();
                Customer c = new Customer();
                c.setUid(String.valueOf(rs.getInt("uid")));
                c.setId(String.valueOf(rs.getInt("cuid")));
                c.setFName(rs.getString("fname"));
                c.setLName(rs.getString("lname"));
                c.setEmail(rs.getString("email"));
                c.setDob(rs.getString("dob"));
                c.setAltEmail(rs.getString("alt_email"));
                c.setMobile(rs.getString("mobile"));
                c.setAltMobile(rs.getString("alt_mobile"));
                au.setQualifiedUser(c);
                au.setUser(user);
                return au;
            });
        }
        catch(EmptyResultDataAccessException e) {
            throw new BadCredentialsException("Invalid Username/Password");
        }
        return appUser;
    }
    
}
