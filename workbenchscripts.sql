#Get all categories
select * from category;
select * from category_status;
select * from item_item;
select * from item_gi;
select * from item_item_photo;
select * from category;
select * from item_item_status;
select * from item_gi_category;
select * from inventory_set_variations;
select * from inventory_set;
select * from item_item;
select * from item_attribute;
select * from item_attribute_group;
select * from item_gi_attribute;
select * from item_attribute_value;

#Get products in a category as a page. Args 77, 10, 10
select i.iid, i.name as item_name, i.description, i.price, i.item_discount, variations.price variation_price, variations.mrp as variation_mrp, variations.description as variation_desc,
i.istatusid, p.imagefiles, p.title, s.name as status, s.description, min(variations.name) as variation_name, variations.isvid as variation_id
from item_item as i, item_item_status as s, item_gi_category as ic, category as c, 
(select insv.isvid, insv.iid, insv.name, insv.description, price, mrp from inventory_set_variations as insv, inventory_set ins where ins.isvid = insv.isvid order by insv.name) as variations,
(select iid, title, GROUP_CONCAT(image separator ',') as imagefiles from item_item_photo group by iid) as p
where p.iid = i.iid and s.istatusid = i.istatusid and ic.giid = i.iid and c.catid = ic.catid and c.catid = 77 and variations.iid = i.iid
group by i.iid 
limit 0, 1000 ;

#Get all unique iids in photo table, grouping them by iid and capturing all images as a comma separated list.
select iid, title, GROUP_CONCAT(image separator ',') as imagefiles from item_item_photo group by iid;

#Get product detail. Args - 129
select i.iid, i.name as item_name, i.description, i.price, i.item_discount, i.istatusid, p.imagefiles, p.title, s.name as status, s.description 
from item_item as i, item_item_status as s, 
(select iid, title, GROUP_CONCAT(image separator ',') as imagefiles from item_item_photo where iid=?) as p
where p.iid = i.iid and s.istatusid = i.istatusid;

select i.iid, i.name as item_name, i.description, i.price, i.item_discount, i.istatusid, p.imagefiles, p.title,
-- concat_ws('#', v.name,v.price,v.mrp),
GROUP_CONCAT(distinct concat_ws('#', v.isvid, v.name, v.price,v.mrp, v.description) SEPARATOR ',') as variations,
-- concat_ws('#', ia.name, iag.name)
GROUP_CONCAT(distinct concat_ws('#', iag.name, ia.name, iav.value) SEPARATOR ',') as attributes 
from item_item as i, item_item_status as s, 
item_gi as igi, item_gi_attribute as igia, item_attribute as ia, item_attribute_group as iag, item_attribute_value as iav,
(select insv.isvid, insv.iid, insv.name, insv.description, ins.price, ins.mrp from inventory_set_variations insv, inventory_set ins 
	where ins.isvid = insv.isvid order by insv.name) as v , 
(select iid, title, GROUP_CONCAT(image separator ',') as imagefiles from item_item_photo group by iid) as p 
where s.name = 'Active' and p.iid = i.iid and s.istatusid = i.istatusid and v.iid = i.iid and i.iid=129 
and i.giid = igi.giid and igia.giid = igi.giid and igia.aid = ia.aid and ia.agid = iag.agid and ia.atid = 1 and ia.aid = iav.aid;

select insv.iid, insv.name, ins.price, ins.mrp
from inventory_set_variations insv, inventory_set ins where ins.isvid = insv.isvid and insv.iid=129;

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




select * from customer_shipping_address where cuid=618;
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