#User data
select * from customer_shipping_address;
desc customer_shipping_address;
select * from customer_shipping_address_status;
select * from customer;
select * from state;
select * from customer_status;



#profile - fname, lname, email, alt_email, dob, mobile, alt_mobile, sa.first_name, sa.last_name, sa.line1, sa.line2, sa.city, sa.zip_code, sa.mobile, state.state
select c.cuid, c.fname, c.lname, c.email, c.alt_email, c.dob, c.mobile, c.alt_mobile 
from customer c
where c.email = 'usrikanth@gmail.com' and custatusid = (select custatusid from customer_status where name = 'Active');
#Assert that cuid passed as param is same as rs.cuid.
select sa.said, sa.first_name, sa.last_name, sa.line1, sa.line2, sa.zip_code, sa.mobile, state.state
from customer c, customer_shipping_address sa, state
where sa.cuid = ? and sa.sasid = (select sasid from customer_shipping_address where name = 'Active');