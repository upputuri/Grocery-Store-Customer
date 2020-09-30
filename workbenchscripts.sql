#Get all categories
select * from category;

#Get products in a category as a page. Args 77, 10, 10
select i.iid, i.name as item_name, i.description, i.price, i.item_discount, i.istatusid, p.imagefiles, p.title, s.name as status, s.description
from item_item as i, item_item_status as s, item_gi_category as ic, category as c,
(select iid, title, GROUP_CONCAT(image separator ',') as imagefiles from item_item_photo group by iid) as p
where p.iid = i.iid and s.istatusid = i.istatusid and ic.giid = i.iid and c.catid = ic.catid and c.catid = 77
limit 0, 1000 ;

#Get all unique iids in photo table, grouping them by iid and capturing all images as a comma separated list.
select iid, title, GROUP_CONCAT(image separator ',') as imagefiles from item_item_photo group by iid;

#Get product detail. Args - 129
select i.iid, i.name as item_name, i.description, i.price, i.item_discount, i.istatusid, p.imagefiles, p.title, s.name as status, s.description 
from item_item as i, item_item_status as s, 
(select iid, title, GROUP_CONCAT(image separator ',') as imagefiles from item_item_photo where iid=?) as p
where p.iid = i.iid and s.istatusid = i.istatusid;


#Get all shipping addresses of a customer. Args - 78
select first_name, last_name, line1, line2, city, csa.cuid, csas.name
from customer_shipping_address as csa, customer_shipping_address_status as csas
where csa.cuid = 78 and csas.name like "Active" and csa.sasid = csas.sasid;

#Add a new shipping address for a customer
insert into customer_shipping_address (cuid, first_name, last_name, line1, line2, city, zip_code, sasid)
values (130, "Srikanth", "Upputuri", "line one address", "line two address", "hyd", "500001", (select sasid from customer_shipping_address_status
where name like 'active'));

#Get all shipping addresses of a customer
select sasid from customer_shipping_address_status where name like 'active';




select * from customer_shipping_address where cuid=131;
select * from customer_shipping_address_status;
select iid, GROUP_CONCAT(image separator ',') from item_item_photo group by iid;

select * from item_item_photo;
select * from cart_item_status;
select * from cart_item;
select * from cart;
insert into cart (cuid) values (129); 
select * from item_item;
select * from item_gi;
select * from item_gi_category;
select * from item_item where iid="145"