#Orders Service
select * from item_order;
select * from item_order_item;
select * from item_order_item_status;
select * from item_order_shipping_address;
select * from item_order_status;
select * from item_order_transaction;
select * from tax_profile;
select * from cart_item;
select * from inventory_set_variations;
select * from cart;
select * from cart_item_status;
select * from state;

select * from promo_code;
select * from promo_code_type;
select * from promo_code_discount_type;
select * from promo_code_item;
select * from promo_code_status;

select * from customer_payment_option;
select * from customer_payment_option_type;
select * from customer_payment_option_status;
select * from customer_payment_option_attribute;


#
-- 1. Create an order by inserting order row in item_order
--    Insert order items in item_order_item table
--    insert shipping address in the i_o_shipping_address table
--    Update cart_item records to executed status
-- 2. After an order record is inserted, proceed to make online payment
-- 3. If payment succeeds insert record in item_order_transaction, otherwise convert it to cod order
-- 4. If payment confirmation is not received, update status accordingly but keep the order, wait for callback

#

#create order
select taxproid, rate from tax_profile as t where t.default = 1;

#cuid - path, said - customer shipping address table, taxproid - taxprofile table, tax_percent - tax profile table, tax_type - tax_profile table, 
#price - computed, discounted_price - computed.
insert into item_order (cuid, said, taxproid, tax_percent, tax_type, price, discounted_price)
values (?, ?, ? , ?, ?, ?, ?);

#all fields - request body
insert into item_order_shipping_address (oid, shipping_first_name, shipping_last_name, shipping_line1, shipping_line2, shipping_city, shipping_zip_code, shipping_mobile,
billing_first_name, billing_last_name, billing_line1, billing_line2, billing_city, billing_zip_code) 
values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

#all fields - read from cart tables
insert into item_order_item (oid, iid, isvid, isid, quantity, discounted_price, item_price, oisid)
values (?, ?, ?, ?, ?, ?, ?, ?);

update cart_item set cartisid = (select id from cart_item_status where name like 'executed') 
where cartid = (select cartid from cart where cuid=?)
and cartisid = (select cartisid from cart_item_status where name like 'active');

insert into item_order_item (oid, iid, isvid, quantity, discounted_price, item_price, oisid) values (398, 129, 139, 0, 100.00, 100.00, (select oisid from item_order_item_status where name like 'pending'));
select oisid from item_order_item_status where name like 'pending';

#Get shipping address
select sa.said, sa.first_name, sa.last_name, sa.line1, sa.line2, sa.zip_code, sa.mobile, sa.city, sa.stid, state.state 
from customer_shipping_address sa, state where sa.said = 41 and sa.stid = state.stid;

#Get payment options
select cpo.cpoid, cpo.cptid , cpo.name, cpo.description from customer_payment_option cpo, customer_payment_option_status cpos where cptid=2 and cpo.cposid = cpos.cposid;

#Get orders
select oid, cuid, said, shipping_cost, tax_percent, price, discounted_price, ios.name, item_order.created_ts
                                from item_order, item_order_status as ios where cuid=618 and item_order.osid = ios.osid;
                                

#Get order detail
select io.oid, io.cuid, io.said, io.shipping_cost, io.tax_percent, io.price, io.discounted_price, ios.name as status, io.created_ts,
                                sa.shipping_first_name, sa.shipping_last_name, sa.shipping_line1, sa.shipping_line2, sa.shipping_city, sa.shipping_zip_code, state.state, state.stid,
                                sa.shipping_mobile from item_order as io, item_order_status as ios, item_order_shipping_address as sa, state where io.oid=421 and sa.oid = io.oid 
                                and io.osid = ios.osid and sa.shipping_stid = state.stid;
                                
#Cancel order
update item_order set osid = (select osid from item_order_status where name='Cancel Request') where oid=432;

#Get promocode
select * from promo_code;
select pcid, code, quantity, discount, order_amount, now() between valid_from and valid_to as isactive from promo_code where code='JGBJ3638';
select pcid, valid_from, valid_to, quantity, discount, order_amount from promo_code where code='JGBJ3638';
                                
                                
                                
                                
                                