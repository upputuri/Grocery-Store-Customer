#Orders Service
select * from item_order;
select * from item_order_item;
select * from item_order_item_status;
select * from item_order_shipping_address;
select * from item_order_status;
select * from item_order_transaction;
select * from tax_profile;
select * from cart_item;
select * from cart;
select * from cart_item_status;

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