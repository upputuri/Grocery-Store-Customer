select * from cart;
select * from cart_item;
select * from cart_item_status;
select * from item_item;
select * from inventory_set;
select * from inventory_set_variations;
select * from customer;
select * from auser;

#Cart Service
#cart id=47

#Get all items in a customer's cart
select c.cartid, ci.iid, ci.cartiid, ci.isvid, ci.cartisid, ci.quantity, ii.name as item_name, ii.price as item_price, ii.item_discount, ins.price as isv_price, insv.name as variant_name
from cart as c, cart_item as ci, cart_item_status as cis, item_item as ii, inventory_set as ins, inventory_set_variations as insv
where c.cuid = 618 and c.cartid = ci.cartid and ci.iid = ii.iid and ci.cartisid = cis.cartisid and ins.isvid = ci.isvid and insv.isvid = ci.isvid
and cis.name like 'active';

#Get user cart
select cartid from cart where cuid=618;

#Check if a cartitem exists for same item and variation
select cartiid from cart_item where cartid = (select cartid from cart where cuid = ?) and iid= ? and isvid = ? limit 0,1;

#Add item to cart if item doesn't exist
insert into cart_item (cartid, iid, isvid, quantity, cartisid) values ((select cartid from cart where cuid = ?), ?, ?, ?, (select cartisid from cart_item_status where name like 'active'));

#Update item to cart if item exists
update cart_item set quantity= ?, updated_ts= current_timestamp where cartiid=?;


select cartiid from cart_item where cartid = (select cartid from cart where cuid = 617) and iid= 130 and isvid = 137 limit 0,1;
delete from cart_item where cartiid=672;
insert into inventory_set_variations (iid, name, description, showing, isvsid) values (130, "250GM", "1pc guava", 1, 1);
select * from cart_item_status;

select COALESCE(sum(quantity),0) from cart_item 
where cartid=(select cartid from cart where cuid=630) and cartisid=(select cartisid from cart_item_status where name='Active');

UPDATE cart_item 
   SET cartisid = CASE WHEN quantity = 1 THEN (select cartisid from cart_item_status where name='Inactive') else cartisid END,
   quantity = quantity-1
   where cartiid = 698;

