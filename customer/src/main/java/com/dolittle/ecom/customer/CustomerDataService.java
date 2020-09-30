package com.dolittle.ecom.customer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dolittle.ecom.customer.bo.ShippingAddress;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class CustomerDataService {

    @Autowired
    JdbcTemplate jdbcTemplateObject;

    @GetMapping("/customers/{id}/addresses")
    public List<ShippingAddress> getCustomerAddresses(@PathVariable(value = "id") String customerId)
    {
        try{
            // #Get all addresses of a customer. Args - 78
            // select first_name, last_name, line1, line2, city, csa.cuid, csas.name
            // from customer_shipping_address as csa, customer_shipping_address_status as csas
            // where csa.cuid = 78 and csas.name like "Active" and csa.sasid = csas.sasid;         
            String queryString =  "select first_name, last_name, line1, line2, city, zip_code, mobile "+
                            "from customer_shipping_address as csa, customer_shipping_address_status as csas "+
                            "where csa.cuid = ? and csas.name like 'Active' and csa.sasid = csas.sasid";   

            List<ShippingAddress> addressList = jdbcTemplateObject.query(queryString, new Object[]{customerId}, (rs, rowNum) -> {
                ShippingAddress a = new ShippingAddress(rs.getString("line1"), rs.getString("city"), rs.getString("zip_code"), rs.getString("mobile"));
                a.setFirstName(rs.getString("first_name"));
                a.setLastName(rs.getString("last_name"));
                a.setLine2(rs.getString("line2"));
                return a;
            });
            return addressList;
        }
        catch(DataAccessException e)
        {
            log.error("An exception occurred while inserting a new Shipping Address", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong! Check logs");
        }
    }

    @PostMapping("/customers/{id}/addresses")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ShippingAddress addNewAddress(@PathVariable(value = "id") String customerId, @RequestBody ShippingAddress address)
    {
        try{
            // String queryString =  "insert into customer_shipping_address (cuid, first_name, last_name, line1, line2, city, zip_code, sasid) "+
            //                 "values (?, ?, ?, ?, ?, ?, ?, (select sasid from customer_shipping_address_status "+
            //                 "where name like 'active'))";   

            String sql = "select sasid from customer_shipping_address_status where name like 'active'";
            int sasid = jdbcTemplateObject.queryForObject(sql, Integer.TYPE);

            SimpleJdbcInsert simpleInsert = new SimpleJdbcInsert(jdbcTemplateObject)
                                            .usingColumns("cuid","first_name","last_name","line1","line2","city","zip_code","sasid")
                                            .withTableName("customer_shipping_address")
                                            .usingGeneratedKeyColumns("said");
            
            Map<String, Object> parameters = new HashMap<>(1);
            parameters.put("cuid", customerId);
            parameters.put("first_name", address.getFirstName());
            parameters.put("last_name", address.getLastName());
            parameters.put("line1", address.getLine1());
            parameters.put("line2", address.getLine2());
            parameters.put("city", address.getCity());
            parameters.put("zip_code", address.getZipcode());
            parameters.put("sasid", sasid);

            Number id = simpleInsert.executeAndReturnKey(parameters);
            address.setId(String.valueOf(id));
            
            return address;
        }
        catch(DataAccessException e)
        {
            log.error("An exception occurred while inserting a new Shipping Address", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong! Check logs");
        }
    }

}
