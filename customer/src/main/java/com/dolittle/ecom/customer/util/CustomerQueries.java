package com.dolittle.ecom.customer.util;

public interface CustomerQueries {
    public String fetch_customer_sql = "select au.uid from auser au where au.user_id=? or (length(au.email) > 0 and au.email=?)";
    
}
