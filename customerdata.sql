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
select sa.said, sa.first_name, sa.last_name, sa.line1, sa.line2, sa.zip_code, sa.mobile, sa.city, sa.stid, state.state
from customer_shipping_address sa, state
where sa.cuid = 618 and sa.stid = state.stid and sa.sasid = (select sasid from customer_shipping_address_status where name = 'Active');

#Update address
update customer_shipping_address set first_name=?, last_name=?, line1=?, line2=?, city=?, zip_code=?, mobile=?, stid=? where said = ?;

#Add a new shipping address for a customer
insert into customer_shipping_address (cuid, first_name, last_name, line1, line2, city, zip_code, sasid)
values (130, "Srikanth", "Upputuri", "line one address", "line two address", "hyd", "500001", (select sasid from customer_shipping_address_status
where name like 'active'));

#Get all shipping addresses of a customer
select sasid from customer_shipping_address_status where name like 'active';