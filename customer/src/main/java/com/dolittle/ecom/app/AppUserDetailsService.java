package com.dolittle.ecom.app;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class AppUserDetailsService implements UserDetailsService {

    @Autowired
    JdbcTemplate jdbcTemplateObject;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Pattern EMAIL_ADDRESS_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
        //Pattern PHONE_NUMBER_REGEX = Pattern.compile("^\\d{10}$");
        Matcher emailMatcher = EMAIL_ADDRESS_REGEX.matcher(username);
        //Matcher phoneMatcher = PHONE_NUMBER_REGEX.matcher(username);

        String user_id_match_sql = " au.user_id=?";
        String email_match_sql = " au.email=?";
        String get_user_sql = "select au.uid, au.user_id, au.name, au.email, au.password, aus.name as user_status, ar.name as role_name, ars.name as role_status "+
                                "from auser au, auser_status as aus, arole as ar, arole_status ars, auser_role as aur "+
                                " where au.ustatusid = aus.ustatusid and ars.rsid = ar.rsid and ar.rid=aur.rid and aur.uid = au.uid and "+
                                (emailMatcher.find() ? email_match_sql : user_id_match_sql);
        
        AppUser appUser = jdbcTemplateObject.queryForObject(get_user_sql, new Object[]{username}, (rs, rowNum) -> {
            GrantedAuthority ga = new SimpleGrantedAuthority(rs.getString("role_name"));
            UserDetails user = User.withUsername(username)
                            .password(rs.getString("password"))
                            .accountExpired(rs.getString("user_status").equals("Active") ? false : true)
                            .accountLocked(false)
                            .credentialsExpired(false)
                            .authorities(ga).build();
            AppUser au = new AppUser(rs.getString("uid"), rs.getString("name"));
            au.setEmail(rs.getString("email"));
            au.setMobile(rs.getString("user_id"));
            au.setUser(user);
            return au;
        });
        return appUser;
    }
    
}
