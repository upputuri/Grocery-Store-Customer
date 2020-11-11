package com.dolittle.ecom.customer;

import java.security.Principal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dolittle.ecom.app.AppUser;
import com.dolittle.ecom.app.util.CustomerRunnerUtil;
import com.dolittle.ecom.customer.bo.Customer;
import com.dolittle.ecom.customer.bo.CustomerQuery;
import com.dolittle.ecom.customer.bo.ShippingAddress;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class CustomerDataService {

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JdbcTemplate jdbcTemplateObject;

    @Transactional
    @PostMapping(value="/customers", produces = "application/hal+json")
    public Customer registerUser(@RequestBody Customer customer)
    {
        if (customer.getMobile() == null || customer.getMobile().trim().isEmpty()
                || customer.getPassword() == null || customer.getPassword().trim().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing mandatory inputs in register request, check for email and password");
        //Check if customer exists
        try{
            String fetch_customer_sql = "select cu.cuid from customer cu where (length(cu.mobile) > 0 and cu.mobile=?) or (length(cu.email) > 0 and cu.email=?)";
            customer = jdbcTemplateObject.queryForObject(fetch_customer_sql, new Object[]{customer.getMobile(), customer.getEmail()}, (rs, rowNum) -> {
                // If control comes here, that means there's a customer record already in db with same email.
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An account with the email/phone already exists");
            });
            log.error("We are not supposed to reach here. Investigate!");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred!, pls retry after some time or pls call support");
        }
        catch(EmptyResultDataAccessException e)
        {
           //Good. We will now register this new user
        //    String passwordHash = CustomerRunnerUtil.generateBcryptPasswordHash(customer.getPassword());
            String passwordHash = passwordEncoder.encode(customer.getPassword());
        //    int ustatusid = jdbcTemplateObject.queryForObject("select ustatusid from auser_status where name = 'Active'", Integer.TYPE);
        //    SimpleJdbcInsert userJdbcInsert = new SimpleJdbcInsert(jdbcTemplateObject)
        //                                            .usingColumns("name", "user_id", "email", "password", "type_auser", "ustatusid")
        //                                            .withTableName("auser")
        //                                            .usingGeneratedKeyColumns("uid");
        //     Map<String, Object> userParams = new HashMap<String, Object>(1);
        //     userParams.put("name", customer.getFName()+ " "+customer.getLName());
        //     userParams.put("email", customer.getEmail());
        //     userParams.put("password", passwordHash);
        //     userParams.put("user_id", customer.getMobile());
        //     userParams.put("type_auser", 3);
        //     userParams.put("ustatusid", ustatusid);        
        //     Number uid = userJdbcInsert.executeAndReturnKey(userParams);

        //     //Insert role records
        //     String get_customer_role_id_sql = "select rid from arole where name='Customer'";
        //     int rid = jdbcTemplateObject.queryForObject(get_customer_role_id_sql, Integer.TYPE);
        //     String add_authority_sql = "insert into auser_role (uid, rid) value (?,?)";
        //     jdbcTemplateObject.update(add_authority_sql, uid.intValue(), rid);

            //Now insert a customer record
            int custatusid = jdbcTemplateObject.queryForObject("select custatusid from customer_status where name = 'Active'", Integer.TYPE);
            SimpleJdbcInsert customerJdbcInsert = new SimpleJdbcInsert(jdbcTemplateObject)
                                                    .usingColumns("uid", "fname", "lname", "email", "password", "mobile", "alt_mobile", "custatusid")
                                                    .withTableName("customer")
                                                    .usingGeneratedKeyColumns("cuid");
            Map<String, Object> customerParams = new HashMap<String, Object>(1);
            customerParams.put("uid", null);
            customerParams.put("fname", customer.getFName());
            customerParams.put("lname", customer.getLName());
            customerParams.put("email", customer.getEmail());
            customerParams.put("mobile", customer.getMobile()); 
            customerParams.put("alt_mobile", customer.getAltMobile());       
            customerParams.put("password", passwordHash);
            customerParams.put("custatusid", custatusid);

            Number cuid = customerJdbcInsert.executeAndReturnKey(customerParams);
            
            //Insert cart record
            SimpleJdbcInsert cartJdbcInsert = new SimpleJdbcInsert(jdbcTemplateObject)
                                                    .usingColumns("cuid")
                                                    .withTableName("cart")
                                                    .usingGeneratedKeyColumns("cartid");
            Map<String, Object> cartParams = new HashMap<String, Object>(1);
            cartParams.put("cuid", cuid);
            Number cartid = cartJdbcInsert.executeAndReturnKey(cartParams);
            log.info("Created cart "+cartid);
                                                    
            Customer c = new Customer();
            c.setId(cuid.toString());
            c.setFName(customer.getFName());
            c.setLName(customer.getLName());
            c.setEmail(customer.getEmail());
            c.setMobile(customer.getMobile());
            Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getCustomerProfile(String.valueOf(cuid), null)).withSelfRel();
            c.add(selfLink);
            log.info("Registered new customer with customer Id "+cuid);
            return c;
        }

    }

    @GetMapping(value = "/customers/{customerId}", produces = "application/hal+json")
    public Customer getCustomerProfile(@PathVariable String customerId, Authentication auth)
    {   
        log.info("Getting customer profile for customer id: "+customerId);
        CustomerRunnerUtil.validateAndGetAuthCustomer(auth, customerId);
        Customer customer = (Customer)((AppUser)auth.getPrincipal()).getQualifiedUser();

        // String get_customer_profile_query = "select c.cuid, c.fname, c.lname, c.email, c.alt_email, c.dob, c.mobile, c.alt_mobile from customer c "+
        //                                     "where c.cuid = ? and custatusid = (select custatusid from customer_status where name = 'Active')";

        // Customer customer = jdbcTemplateObject.queryForObject(get_customer_profile_query, new Object[]{customerId}, (rs, rownum) -> {
        //     Customer c = new Customer();
        //     c.setId(String.valueOf(rs.getInt("cuid")));
        //     c.setFName(rs.getString("fname"));
        //     c.setLName(rs.getString("lname"));
        //     c.setEmail(rs.getString("email"));
        //     c.setDob(rs.getDate("dob"));
        //     c.setAltEmail(rs.getString("alt_email"));
        //     c.setMobile(rs.getString("mobile"));
        //     c.setAltMobile(rs.getString("alt_mobile"));
        //     return c;
        // });

        // if (!customerId.equals(customer.getId()))
        // {
        //     log.error("Requested customer Id does not match with the authenticated user");
        //     log.debug("Requested customer Id: {}, authorized user email {}, authorized user customer Id {}", customerId, principal.getName(), customer.getId());
        //     throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You do not have permission to view details of the provided customer Id");
        // }

        List<ShippingAddress> addressList = new ArrayList<ShippingAddress>();
        String get_customer_addresses = "select sa.said, sa.first_name, sa.last_name, sa.line1, sa.line2, sa.zip_code, sa.mobile, sa.city, sa.stid, state.state "+
                                        "from customer_shipping_address sa, state "+
                                        "where sa.cuid = ? and sa.stid = state.stid and sa.sasid = (select sasid from customer_shipping_address_status where name = 'Active')";

        addressList = jdbcTemplateObject.query(get_customer_addresses, new Object[]{customer.getId()}, (rs, rowNumber) -> {
            ShippingAddress sa = new ShippingAddress(rs.getString("line1"), rs.getString("city"), rs.getString("zip_code"), rs.getString("mobile"));
            sa.setId(String.valueOf(rs.getInt("said")));
            sa.setLine2(rs.getString("line2"));
            sa.setFirstName(rs.getString("first_name"));
            sa.setLastName(rs.getString("last_name"));
            sa.setState(rs.getString("state"));
            sa.setStateId(rs.getInt("stid"));
            return sa;
        });

        customer.setShippingAddresses(addressList);
        log.info("Returning customer profile (customer object) for requested customer");
        log.debug("Retrieved customer object"+customer);
        return customer;

    }
    
    @Transactional
    @PutMapping(value = "/customers/{customerId}", produces = "application/hal+json" )
    public void editProfile(@PathVariable String customerId, @RequestBody Customer profile, Authentication auth)
    {
        log.info("Processing edit profile request for customer Id: "+customerId);
        Customer customer = CustomerRunnerUtil.fetchAuthCustomer(auth);

        if (!customer.getId().equals(customerId)){
            log.error("Requested customer Id does not match with authenticated user or the customer is inactive");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You do not have permission to view details of the provided customer Id");
        }

        if (profile.getEmail() != null && !profile.getEmail().equals(customer.getEmail()))
        {
            String check_unique_username_sql = "select count(*) from customer where email=?";
            int count = jdbcTemplateObject.queryForObject(check_unique_username_sql, new Object[]{profile.getEmail()}, Integer.TYPE);
            if (count > 0) {
                log.error("Invalid input. An account with the given email already exists.");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email change not accepted. This email is registered with another account");
            }
        }
        if (profile.getMobile() != null && !profile.getMobile().equals(customer.getMobile()))
        {
            String check_unique_username_sql = "select count(*) from customer where mobile=?";
            int count = jdbcTemplateObject.queryForObject(check_unique_username_sql, new Object[]{profile.getMobile()}, Integer.TYPE);
            if (count > 0) {
                log.error("Invalid input. An account with the given mobile# already exists.");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mobile No. change not accepted. This mobile no.is registered with another account");
            }
        }
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");  
        String strDate = profile.getDob() != null ? dateFormat.format(profile.getDob()) : null;  
        String passwordHash = profile.getPassword() != null ? passwordEncoder.encode(profile.getPassword()) : null;
        try{
            jdbcTemplateObject.update("update customer set fname=?, lname=?, email=?, alt_email=?, dob=?, mobile=?, alt_mobile=?, password=? where cuid=?", 
                            profile.getFName(), profile.getLName(), profile.getEmail(), profile.getAltEmail(), strDate, profile.getMobile(), profile.getAltMobile(), passwordHash, customerId);
        }
        catch(DataAccessException e)
        {
            log.error("An exception occurred while updating profile data", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred!, pls retry after some time or pls call support");
        }
        log.info("Customer profile update complete for customer Id: {}", customerId);
    }

    @GetMapping(value = "/customers/{id}/addresses", produces = "application/hal+json")
    public CollectionModel<ShippingAddress> getCustomerAddresses(@PathVariable(value = "id") String customerId, Authentication auth)
    {
        log.info("Processing request Get Customer Addressses for customer Id {}"+customerId);
        CustomerRunnerUtil.validateAndGetAuthCustomer(auth, customerId);
        try{
            List<ShippingAddress> addressList = new ArrayList<ShippingAddress>();
            String get_customer_addresses = "select sa.said, sa.first_name, sa.last_name, sa.line1, sa.line2, sa.zip_code, sa.mobile, sa.city, sa.stid, state.state "+
                                            "from customer_shipping_address sa, state "+
                                            "where sa.cuid = ? and sa.stid = state.stid and sa.sasid = (select sasid from customer_shipping_address_status where name = 'Active')";
    
            addressList = jdbcTemplateObject.query(get_customer_addresses, new Object[]{customerId}, (rs, rowNumber) -> {
                ShippingAddress sa = new ShippingAddress(rs.getString("line1"), rs.getString("city"), rs.getString("zip_code"), rs.getString("mobile"));
                sa.setId(String.valueOf(rs.getInt("said")));
                sa.setLine2(rs.getString("line2"));
                sa.setFirstName(rs.getString("first_name"));
                sa.setLastName(rs.getString("last_name"));
                sa.setState(rs.getString("state"));
                sa.setStateId(rs.getInt("stid"));
                Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getCustomerAddresses(customerId, null)).withSelfRel();
                sa.add(selfLink);
                return sa;
            });
            Customer customer = new Customer();
            customer.setShippingAddresses(addressList);
            Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getCustomerAddresses(customerId, null)).withSelfRel();
            CollectionModel<ShippingAddress> result = CollectionModel.of(addressList, selfLink);
            return result;
        }
        catch(DataAccessException e)
        {
            log.error("An exception occurred while getting Shipping Address list for customer {}",customerId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred!, pls retry after some time or pls call support");
        }
    }

    @PostMapping(value = "/customers/{id}/addresses", produces = "application/hal+json")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ShippingAddress addNewAddress(@PathVariable(value = "id") String customerId, @RequestBody ShippingAddress address, Authentication auth)
    {
        try{
            // String queryString =  "insert into customer_shipping_address (cuid, first_name, last_name, line1, line2, city, zip_code, sasid) "+
            //                 "values (?, ?, ?, ?, ?, ?, ?, (select sasid from customer_shipping_address_status "+
            //                 "where name like 'active'))";   
            log.info("Processing request to add new address to customer Id {}", customerId);
            CustomerRunnerUtil.validateAndGetAuthCustomer(auth, customerId);
            String sql = "select sasid from customer_shipping_address_status where name = 'Active'";
            int sasid = jdbcTemplateObject.queryForObject(sql, Integer.TYPE);

            SimpleJdbcInsert simpleInsert = new SimpleJdbcInsert(jdbcTemplateObject)
                                            .usingColumns("cuid","first_name","last_name","line1","line2","city","zip_code","mobile", "stid", "sasid")
                                            .withTableName("customer_shipping_address")
                                            .usingGeneratedKeyColumns("said");
            
            Map<String, Object> parameters = new HashMap<>(1);
            parameters.put("cuid", customerId);
            parameters.put("first_name", address.getFirstName());
            parameters.put("last_name", address.getLastName());
            parameters.put("line1", address.getLine1());
            parameters.put("line2", address.getLine2());
            parameters.put("city", address.getCity());
            parameters.put("stid", address.getStateId());
            parameters.put("zip_code", address.getZipcode());
            parameters.put("mobile", address.getPhoneNumber());
            parameters.put("sasid", sasid);

            Number id = simpleInsert.executeAndReturnKey(parameters);
            address.setId(String.valueOf(id));
            
            return address;
        }
        catch(DataAccessException e)
        {
            log.error("An exception occurred while inserting a new Shipping Address", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred!, pls retry after some time or pls call support");
        }
    }

    @PutMapping(value = "/customers/{customerId}/addresses/{addressId}", produces = "application/hal+json")
    public void updateAddress(@PathVariable(value = "customerId") String customerId, 
                                            @PathVariable(value = "addressId") String addressId, 
                                            @RequestBody ShippingAddress address, 
                                            Authentication auth)
    {
        try{  
            log.info("Processing request to update address of customer Id {} with addressId {}", customerId, address.getId());
            CustomerRunnerUtil.validateAndGetAuthCustomer(auth, customerId);
            String address_update_sql = "update customer_shipping_address set first_name=?, last_name=?, line1=?, line2=?, city=?, zip_code=?, mobile=?, stid=? "+
                                        "where said=?";
                            
            int rows = jdbcTemplateObject.update(address_update_sql, address.getFirstName(), address.getLastName(), address.getLine1(), address.getLine2(),
                                                            address.getCity(), address.getZipcode(), address.getPhoneNumber(), address.getStateId(), addressId);

            if (rows != 1)
            {
                log.error("Failed to find an address record to update with the given query");
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "The sql to update the address in db could not find a matching record with the given address Id: "+address.getId());
            }
        }
        catch(DataAccessException e)
        {
            log.error("An exception occurred while inserting a new Shipping Address", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred!, pls retry after some time or pls call support");
        }
    }

    @Transactional
    @PostMapping(value = "/customers/{customerId}/queries", produces = "application/hal+json")
    public void createQuery(@RequestBody CustomerQuery query, @PathVariable String customerId, Authentication auth)
    {
        log.info("Processing create query request for customer Id: "+customerId);
        Customer c = CustomerRunnerUtil.validateAndGetAuthCustomer(auth, customerId);
        try{

            SimpleJdbcInsert queryJdbcInsert = new SimpleJdbcInsert(jdbcTemplateObject)
                                                    .usingColumns("cqiid", "cuid", "name", "email", "subject", "cqsid")
                                                    .withTableName("customer_query")
                                                    .usingGeneratedKeyColumns("cqid");

            Map<String, Object> queryParams = new HashMap<String, Object>(1);
            queryParams.put("name", c.getFName()+ " "+c.getLName());
            queryParams.put("email", c.getEmail());
            queryParams.put("cqiid", 1);
            queryParams.put("cuid", customerId);
            queryParams.put("subject", query.getSubject());        
            queryParams.put("cqsid", 1);

            Number cqid = queryJdbcInsert.executeAndReturnKey(queryParams);

            SimpleJdbcInsert msgJdbcInsert = new SimpleJdbcInsert(jdbcTemplateObject)
                                                    .usingColumns("uid", "cqid", "message", "user_type")
                                                    .withTableName("customer_query_message");

            Map<String, Object> msgParams = new HashMap<String, Object>(1);
            msgParams.put("uid", c.getUid());
            msgParams.put("cqid", cqid);
            msgParams.put("user_type", 3);
            msgParams.put("message", query.getDetail());        
            msgJdbcInsert.execute(msgParams);
        }
        catch(DataAccessException e){
            log.error("An exception occurred while inserting a new Shipping Address", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred!, pls retry after some time or pls call support");            
        }
    }



}
