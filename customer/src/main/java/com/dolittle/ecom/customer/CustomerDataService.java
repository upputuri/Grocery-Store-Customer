package com.dolittle.ecom.customer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dolittle.ecom.app.AppUser;
import com.dolittle.ecom.app.util.CustomerRunnerUtil;
import com.dolittle.ecom.customer.bo.Customer;
import com.dolittle.ecom.customer.bo.CustomerQuery;
import com.dolittle.ecom.customer.bo.MPlan;
import com.dolittle.ecom.customer.bo.Member;
import com.dolittle.ecom.customer.bo.Membership;
import com.dolittle.ecom.customer.bo.Nominee;
import com.dolittle.ecom.customer.bo.ProductReview;
import com.dolittle.ecom.customer.bo.ShippingAddress;
import com.dolittle.ecom.customer.bo.Transaction;

import org.json.JSONObject;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class CustomerDataService {

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JdbcTemplate jdbcTemplateObject;

    @GetMapping(value="/customers/idpool", produces = "application/hal+json")
    public Customer checkIfUserExists(@RequestParam(value="email", required = false) String email, 
                                        @RequestParam(value="mobile", required=false) String mobile)
    {
        if ((mobile == null || mobile.trim().isEmpty())
                && (email == null || email.trim().isEmpty())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing mandatory inputs, pass either mobile or email");
        }
        
        try{
            String fetch_customer_sql = "select cu.cuid from customer cu where cu.custatusid != (select custatusid from customer_status where name = 'Deleted') and "+
                                    "((length(cu.mobile) > 0 and cu.mobile=?) or (length(cu.email) > 0 and cu.email=?)) ";
            jdbcTemplateObject.queryForObject(fetch_customer_sql, new Object[]{mobile, email}, (rs, rowNum) -> {
                // If control comes here, that means there's a customer record already in db with same email.
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An account with the email/phone already exists");
            });
            log.error("We are not supposed to reach here. Investigate!");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred!, pls retry after some time or pls call support");
        }
        catch(EmptyResultDataAccessException e)
        {
            // This is good. We found not existing user with the given principal. Can use it to register a new user.
            Customer c = new Customer();
            c.setEmail(email);
            c.setMobile(mobile);
            Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).checkIfUserExists(email, mobile)).withSelfRel();
            c.add(selfLink);
            return c;
        }
    }

    @Transactional
    @PostMapping(value="/customers", produces = "application/hal+json")
    public Customer registerUser(@RequestBody Customer customer)
    {
        if (customer.getMobile() == null || customer.getMobile().trim().isEmpty()
                || customer.getPassword() == null || customer.getPassword().trim().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing mandatory inputs in register request, check for email and password");
        //Check if customer exists
        try{
            String fetch_customer_sql = "select cu.cuid from customer cu where cu.custatusid != (select custatusid from customer_status where name = 'Deleted') and "+
                                    "((length(cu.mobile) > 0 and cu.mobile=?) or (length(cu.email) > 0 and cu.email=?)) ";
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
            int custatusid = jdbcTemplateObject.queryForObject("select custatusid from customer_status where name = 'Email Unverified'", Integer.TYPE);
            SimpleJdbcInsert customerJdbcInsert = new SimpleJdbcInsert(jdbcTemplateObject)
                                                    .usingColumns("uid", "fname", "lname", "genderid", "email", "password", "mobile", "alt_mobile", "custatusid")
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
            customerParams.put("genderid", customer.getGender() !=null ? (customer.getGender().equals("male") ? 1 : 2) : null);

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
        log.info("Received profile object is"+profile.toString());
        Customer customer = CustomerRunnerUtil.fetchAuthCustomer(auth);

        if (!customer.getId().equals(customerId)){
            log.error("Requested customer Id does not match with authenticated user or the customer is inactive");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You do not have permission to view details of the provided customer Id");
        }

        if (profile.getEmail() != null && !profile.getEmail().equals(customer.getEmail()))
        {
            String check_unique_username_sql = "select count(*) from customer where email=? and cuid != ? and custatusid != (select custatusid from customer_status where name = 'Deleted')";
            int count = jdbcTemplateObject.queryForObject(check_unique_username_sql, new Object[]{profile.getEmail(), customerId}, Integer.TYPE);
            if (count > 0) {
                log.error("Invalid input. An account with the given email already exists.");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email change not accepted. This email is registered with another account");
            }
        }
        if (profile.getMobile() != null && !profile.getMobile().equals(customer.getMobile()))
        {
            String check_unique_username_sql = "select count(*) from customer where mobile=? and cuid != ? and custatusid != (select custatusid from customer_status where name = 'Deleted')";
            int count = jdbcTemplateObject.queryForObject(check_unique_username_sql, new Object[]{profile.getMobile(), customerId}, Integer.TYPE);
            if (count > 0) {
                log.error("Invalid input. An account with the given mobile# already exists.");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mobile No. change not accepted. This mobile no.is registered with another account");
            }
        }
        else if (profile.getMobile() == null){
            log.error("Invalid input. A mobile number is required.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mobile number is a required input"); 
        }
        
        Integer genderId = null;
        if (profile.getGender() != null){
            genderId = profile.getGender().equals("male") ? 1 : 2;
        }
        // DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");  
        String strDate = profile.getDob() != null ? profile.getDob() : null;
        if (profile.getPassword() == null || profile.getPassword().length() < 6){
            log.error("Invalid input. Password must contain atleast 6 characters");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must contain atleast 6 characters"); 
        }  

        String passwordHash = profile.getPassword() != null ? passwordEncoder.encode(profile.getPassword()) : null;
        try{
            jdbcTemplateObject.update("update customer set fname=?, lname=?, genderid=?, email=?, alt_email=?, dob=?, mobile=?, alt_mobile=?, password=? where cuid=? and custatusid != (select custatusid from customer_status where name = 'Deleted')", 
                            profile.getFName(), profile.getLName(), genderId, profile.getEmail(), profile.getAltEmail(), strDate, profile.getMobile(), profile.getAltMobile(), passwordHash, customerId);
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
            String get_customer_addresses = "select sa.said, sa.default_address, sa.first_name, sa.last_name, sa.line1, sa.line2, sa.zip_code, sa.mobile, sa.city, sa.stid, state.ctid, country.name as country_name, atype.name as label, atype.addtid, state.state "+
                                            "from customer_shipping_address sa left join address_type atype on (atype.addtid = sa.addtid) inner join state on (sa.stid=state.stid) inner join country on (state.ctid=country.ctid) "+
                                            "where sa.cuid = ? and sa.sasid = (select sasid from customer_shipping_address_status where name = 'Active')";
    
            addressList = jdbcTemplateObject.query(get_customer_addresses, new Object[]{customerId}, (rs, rowNumber) -> {
                ShippingAddress sa = new ShippingAddress(rs.getString("line1"), rs.getString("city"), rs.getString("zip_code"), rs.getString("mobile"));
                sa.setId(String.valueOf(rs.getInt("said")));
                sa.setLine2(rs.getString("line2"));
                sa.setFirstName(rs.getString("first_name"));
                sa.setLastName(rs.getString("last_name"));
                sa.setState(rs.getString("state"));
                sa.setStateId(rs.getInt("stid"));
                sa.setCountry(rs.getString("country_name"));
                sa.setCountryId(rs.getInt("ctid"));
                sa.setDefault(rs.getInt("default_address") == 1);
                sa.setType(rs.getString("label"));
                sa.setTypeId(String.valueOf(rs.getInt("addtid")));
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
            int addressCount = getCustomerAddresses(customerId, auth).getContent().size();
            String sql = "select sasid from customer_shipping_address_status where name = 'Active'";
            int sasid = jdbcTemplateObject.queryForObject(sql, Integer.TYPE);

            SimpleJdbcInsert simpleInsert = new SimpleJdbcInsert(jdbcTemplateObject)
                                            .usingColumns("cuid","first_name","last_name","line1","line2","city","zip_code","mobile", "stid", "ctid", "sasid", "default_address")
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
            parameters.put("ctid", address.getCountryId());
            parameters.put("zip_code", address.getZipcode());
            parameters.put("mobile", address.getPhoneNumber());
            parameters.put("sasid", sasid);
            boolean isDefault = address.isDefault();
            if (!isDefault && addressCount <= 0){
                isDefault = true;
            }
            parameters.put("default_address", isDefault ? 1 : 0);

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

            if (address.isDefault()) {
                // Remove current default if any
                jdbcTemplateObject.update("update customer_shipping_address set default_address=0 where default_address=1");
            }

            String address_update_sql = "update customer_shipping_address set addtid=?, first_name=?, last_name=?, line1=?, line2=?, city=?, zip_code=?, mobile=?, stid=?, ctid=?, default_address=? "+
                                        "where said=?";
                            
            int rows = jdbcTemplateObject.update(address_update_sql, address.getTypeId(), address.getFirstName(), address.getLastName(), address.getLine1(), address.getLine2(),
                                                            address.getCity(), address.getZipcode(), address.getPhoneNumber(), address.getStateId(), address.getCountryId(), address.isDefault()?1:0, addressId);

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

    @DeleteMapping(value = "/customers/{customerId}/addresses/{addressId}", produces = "application/hal+json")
    public void removeAddress(@PathVariable(value = "customerId") String customerId, 
                                            @PathVariable(value = "addressId") String addressId, 
                                            Authentication auth)
    {
        try{  
            log.info("Processing request to remove address of customer Id {} with addressId {}", customerId, addressId);
            CustomerRunnerUtil.validateAndGetAuthCustomer(auth, customerId);
            String address_update_sql = "delete from customer_shipping_address "+
                                        "where said=?";
                            
            int rows = jdbcTemplateObject.update(address_update_sql, addressId);

            if (rows != 1)
            {
                log.error("Failed to find an address record to delete with the given query. Returning a success response anyway.");
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

    @Transactional
    @PostMapping(value = "/customers/{customerId}/productreviews", produces = "application/hal+json")
    public void addOrUpdateProductReview(@RequestBody ProductReview review, @PathVariable String customerId, Authentication auth)
    {
        log.info("Processing add or update prduct review request for customer Id: "+customerId);
        CustomerRunnerUtil.validateAndGetAuthCustomer(auth, customerId);
        try{
            if (review.getRating() > 0) {
                String rating_update_sql = "update tbl_products_ratings set ratings_score=?, ratings_status=1 where product_id = ? and user_id = ?";
                int rows = jdbcTemplateObject.update(rating_update_sql, new Object[]{review.getRating(), review.getProductId(), customerId});
                if (rows == 0){
                    String rating_insert_sql = "insert into tbl_products_ratings (product_id, user_id, ratings_score, ratings_status) values (?,?,?,1)";
                    jdbcTemplateObject.update(rating_insert_sql, new Object[]{review.getProductId(), customerId, review.getRating()});
                }
            }
            else {
                log.error("Invalid rating submitted");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid rating submitted");
            }

            if (StringUtils.isNotEmpty(review.getReviewTitle()) || StringUtils.isNotEmpty(review.getReviewDetail())){
                String review_update_sql = "update tbl_products_review set title=?, description=?, reviewsid=2 where product_id = ? and user_id = ?";
                int rows = jdbcTemplateObject.update(review_update_sql, new Object[]{review.getReviewTitle(), review.getReviewDetail(), review.getProductId(), customerId});
                if (rows == 0){
                    String review_insert_sql = "insert into tbl_products_review (product_id, user_id, title, description, reviewsid) values (?, ?, ?, ?, 2)";
                    jdbcTemplateObject.update(review_insert_sql, new Object[]{review.getProductId(), customerId, review.getReviewTitle(), review.getReviewDetail()});
                }
            }

        }
        catch(DataAccessException e){
            log.error("An exception occurred while inserting a new Shipping Address", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred!, pls retry after some time or pls call support");            
        }
    }

    @GetMapping(value = "/customers/{customerId}/productreviews", produces = "application/hal+json")
    public ProductReview getProductReview(@RequestParam (value="productid", required=false) String productId, @PathVariable String customerId, Authentication auth){
        log.info("Processing get a single product review request for customer Id: "+customerId);
        CustomerRunnerUtil.validateAndGetAuthCustomer(auth, customerId);
        try{
            String get_single_product_rating = "select ratings_score from tbl_products_ratings where product_id=? and user_id=? and ratings_status=1";
            ProductReview review = new ProductReview();
            jdbcTemplateObject.query(get_single_product_rating, new Object[]{productId, customerId}, (rs, rowNum) -> {
                review.setRating(rs.getInt("ratings_score"));
                return null;
            });
            
            String get_single_product_review = "select title, description from tbl_products_review where product_id=? and user_id=? and reviewsid != 3";
            jdbcTemplateObject.query(get_single_product_review, new Object[]{productId, customerId}, (rs, rowNum) -> {
                review.setReviewTitle(rs.getString("title"));
                review.setReviewDetail(rs.getString("description"));
                return null;
            });

            Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getProductReview(productId, customerId, auth)).withSelfRel();
            review.add(selfLink);
            return review;
        }
        catch(DataAccessException e){
            log.error("An exception occurred while getting product review", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred!, pls retry after some time or pls call support");            
        }
    }

    @GetMapping("/customers/{customerId}/membership")
    public Membership getCustomerMembership(@PathVariable String customerId, Authentication auth) {
        try{
            log.info("Processing request to get membership for customer Id"+customerId);
            CustomerRunnerUtil.validateAndGetAuthCustomer(auth, customerId.toString());
            String fetch_membership = "select cwp.cuwapaid, cwp.tid, tr.created_ts, cwp.start_date, cwp.end_date, cwp.validity, cwp.duration, cwp.min_purchase_permonth, cwp.max_purchase_permonth, wpc.name "+
                                    "as category_name, wp.wapaid, wp.name, wp.renewal_period, wp.renewal_dur, wp.description, wp.short_desc, "+
                                    "cwp.mem_fname, cwp.mem_lname, cwp.mem_email, cwp.mem_dob, cwp.mem_gender, cwp.mem_mob, cwp.mem_alt_mob, cwp.mem_pre_addr, cwp.mem_pre_pincode, cwp.mem_photo, cwp.mem_aadhar_fph, cwp.mem_aadhar_bph, "+
                                    "cwp.nom_fname, cwp.nom_lname, cwp.nom_email, cwp.nom_dob, cwp.nom_gender, cwp.nom_mob, cwp.nom_alt_mob, cwp.nom_relation as relation_id, rl.name as relation_name, cwp.nom_photo, cwp.nom_aadhar_fph, cwp.nom_aadhar_bph "+
                                    "from customer_wallet_pack cwp inner join wallet_pack wp on (cwp.wapaid = wp.wapaid and cwp.cuwapasid = 1 and cwp.cuid = ?) "+
                                    "inner join wallet_pack_category wpc on (wp.wapacatid = wpc.wapacatid) left join relationship rl on (cwp.nom_relation = rl.relationship_id) "+
                                    "inner join transaction tr on (cwp.tid = tr.tid)";

            Membership membership = jdbcTemplateObject.queryForObject(fetch_membership, new Object[]{customerId}, (rs, rowNum) -> {
                Membership mship = new Membership();
                mship.setMembershipId(String.valueOf(rs.getInt("cuwapaid")));
                mship.setCustomerId(customerId);
                Calendar start_date = Calendar.getInstance();
                start_date.setTime(rs.getDate("start_date"));
                Calendar end_date = Calendar.getInstance();
                end_date.setTime(rs.getDate("end_date"));
                mship.setStartDate(start_date);
                mship.setEndDate(end_date);
                int renewal_period = rs.getInt("renewal_period");
                int renewal_duration = rs.getInt("renewal_dur");
                Calendar renewal_date = Calendar.getInstance();
                renewal_date.setTimeInMillis(start_date.getTimeInMillis());
                if (renewal_period != 0){
                    if (renewal_duration == 1){ //years
                        renewal_date.add(Calendar.YEAR, renewal_period);
                    }
                    else if (renewal_duration == 2){
                        renewal_date.add(Calendar.MONTH, renewal_period);
                    }
                    else if (renewal_duration == 3){
                        renewal_date.add(Calendar.DAY_OF_MONTH, renewal_period);
                    }
                    mship.setRenewalDate(renewal_date);
                }
                else{
                    mship.setRenewalDate(null);
                }
                // mship.setRenewalDate(rs.getString("renewal_date"));
                MPlan plan = new MPlan();
                plan.setPlanId(String.valueOf(rs.getInt("wapaid")));
                plan.setPlanName(rs.getString("name"));
                plan.setCategoryName(rs.getString("category_name"));
                plan.setValidityInYears(rs.getInt("validity"));
                plan.setDescription(rs.getString("description"));
                plan.setShortDescription(rs.getString("short_desc"));
                plan.setCategoryId(String.valueOf(rs.getInt("wapaid")));
                plan.setMaxPurchaseAmount(rs.getBigDecimal("max_purchase_permonth"));
                plan.setMinPurchaseAmount(rs.getBigDecimal("min_purchase_permonth"));
                Member member = new Member();
                member.setFName(rs.getString("mem_fname"));
                member.setLName(rs.getString("mem_lname"));
                member.setEmail(rs.getString("mem_email"));
                member.setDob(rs.getString("mem_dob"));
                member.setGender(rs.getString("mem_gender"));
                member.setMobile(rs.getString("mem_mob"));
                member.setAltMobile(rs.getString("mem_alt_mob"));
                member.setPresentAddress(rs.getString("mem_pre_addr"));
                member.setPresentPinCode(rs.getString("mem_pre_pincode"));
                member.setPhotoImg(rs.getString("mem_photo"));
                member.setAdhaarFrontImg(rs.getString("mem_aadhar_fph"));
                member.setAdhaarBackImg(rs.getString("mem_aadhar_bph"));
                Nominee nominee = new Nominee();
                nominee.setFName(rs.getString("nom_fname"));
                nominee.setLName(rs.getString("nom_lname"));
                nominee.setEmail(rs.getString("nom_email"));
                nominee.setDob(rs.getString("nom_dob"));
                nominee.setGender(rs.getString("nom_gender"));
                nominee.setRelationshipId(rs.getString("relation_id"));
                nominee.setRelationshipName(rs.getString("relation_name"));
                nominee.setMobile(rs.getString("nom_mob"));
                nominee.setAltMobile(rs.getString("nom_alt_mob"));
                nominee.setPhotoImg(rs.getString("nom_photo"));
                nominee.setAdhaarFrontImg(rs.getString("nom_aadhar_fph"));
                nominee.setAdhaarBackImg(rs.getString("nom_aadhar_bph"));
                
                Transaction transaction = new Transaction();
                transaction.setId(rs.getString("tid"));
                Calendar tran_ts = Calendar.getInstance();
                tran_ts.setTime(rs.getDate("created_ts"));
                transaction.setCreatedTS(tran_ts);

                mship.setPlan(plan);
                mship.setMember(member);
                mship.setNominee(nominee);
                mship.setTransaction(transaction);
                return mship;
            });

            Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getCustomerMembership(customerId, auth)).withSelfRel();
            membership.add(selfLink);
            return membership;
        }
        catch(EmptyResultDataAccessException e){
            Membership membership = new Membership();
            membership.setCustomerId(customerId);
            Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getCustomerMembership(customerId, auth)).withSelfRel();
            membership.add(selfLink);
            return membership;
        }
        catch(DataAccessException e){
            log.error("An exception occurred while getting membership for customer id"+customerId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred!, pls retry after some time or pls call support");
        }
    }

}
