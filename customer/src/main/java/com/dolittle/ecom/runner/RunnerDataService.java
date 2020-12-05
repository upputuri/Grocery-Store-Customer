package com.dolittle.ecom.runner;

import com.dolittle.ecom.app.util.CustomerRunnerUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class RunnerDataService {
    @Autowired
    PasswordEncoder passwordEncoder;
    
    @Autowired
    JdbcTemplate jdbcTemplateObject;
    
    @Transactional
    @PutMapping(value = "/runners/{runnerId}", produces = "application/hal+json" )
    public void editProfile(@PathVariable String runnerId, @RequestBody Runner profile, Authentication auth)
    {
        log.info("Processing edit profile request for runner Id: "+runnerId);
        log.info("Received profile object is"+profile.toString());
        Runner runner = CustomerRunnerUtil.fetchAuthRunner(auth);

        if (!runner.getId().equals(runnerId)){
            log.error("Requested runner Id does not match with authenticated user or the runner is inactive");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You do not have permission to view details of the provided runner Id");
        }

        if (profile.getEmail() != null && !profile.getEmail().equals(runner.getEmail()))
        {
            String check_unique_username_sql = "select count(*) from auser where email=?";
            int count = jdbcTemplateObject.queryForObject(check_unique_username_sql, new Object[]{profile.getEmail()}, Integer.TYPE);
            if (count > 0) {
                log.error("Invalid input. An account with the given email already exists.");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email change not accepted. This email is registered with another account");
            }
        }
        if (profile.getMobile() != null && !profile.getMobile().equals(runner.getMobile()))
        {
            String check_unique_username_sql = "select count(*) from auser where mobile=?";
            int count = jdbcTemplateObject.queryForObject(check_unique_username_sql, new Object[]{profile.getMobile()}, Integer.TYPE);
            if (count > 0) {
                log.error("Invalid input. An account with the given mobile# already exists.");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mobile No. change not accepted. This mobile no.is registered with another account");
            }
        }

        String passwordHash = profile.getPassword() != null ? passwordEncoder.encode(profile.getPassword()) : null;
        try{
            jdbcTemplateObject.update("update auser set name=?, email=?, mobile=?, password=? where uid=?", 
                            profile.getName(), profile.getEmail(), profile.getMobile(), passwordHash, runnerId);
        }
        catch(DataAccessException e)
        {
            log.error("An exception occurred while updating profile data", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred!, pls retry after some time or pls call support");
        }
        log.info("Customer profile update complete for customer Id: {}", runnerId);
    }
}
