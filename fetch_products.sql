select * from arole;
select * from auser;
select * from customer;

select * from item_item;
select * from item_item_status;
select * from category;
select * from item_gi_category;
select * from item_item_photo;

select * from item_gi_attribute;
select * from item_gi;
select * from item_attribute;
select * from item_attribute_value;

select * from inventory_set_variations;
select * from inventory_set;
	
select i.iid, i.created_ts as created_ts, i.name as item_name, i.description, i.price, i.item_discount, p.imagefiles, offers.discount 
as offer_discount, offers.amount as offer_amount 
from item_item as i 
left join (select oi.iid, discount, amount from offer_item oi, offer where offer.offid=oi.offid and offer.offsid= (select offsid from offer_status where name='Active')) as offers on (i.iid=offers.iid)
left join (select iid, title, GROUP_CONCAT(image separator ',') as imagefiles from item_item_photo group by iid) as p on (i.iid=p.iid),
item_item_status as s, item_gi_category as ic, category as c
where s.istatusid = i.istatusid and s.name = 'Active' and ic.giid = i.giid and c.catid = ic.catid  and c.catid = 2  group by i.iid 
order by  item_name asc limit 0, 15 ;

SELECT i.iid, lis.price, lis.mrp, offers.discount as offer_discount, lis.description, lis.isvid, lis.name, 
(CASE WHEN lis.quantity IS NULL THEN 0 ELSE lis.quantity END) as quantity, 
(CASE WHEN ois.ordered IS NULL THEN 0 ELSE ois.ordered END) as ordered 
FROM item_item i 
INNER JOIN ( SELECT * FROM ( SELECT *, SUM(quantity1) as quantity FROM ( SELECT iss.iid,iss.isvid,iss.price,iss.mrp,isv.name,isv.description,count(isi.isiid) as quantity1 FROM inventory_set iss LEFT JOIN (select * from inventory_set_item isi_a where isi_a.isisid='1') as isi ON (iss.isid = isi.isid) INNER JOIN inventory_set_variations isv ON (iss.isvid = isv.isvid) WHERE iss.issid = '1' AND iss.istid='1' AND isv.isvsid = '1' GROUP By iss.isid ORDER BY price DESC ) as x GROUP BY x.isvid ORDER BY x.price DESC ) as y ORDER BY y.price DESC ) as lis ON (i.iid = lis.iid) 
LEFT JOIN ( SELECT (CASE WHEN ioie.exe_o IS NULL THEN SUM(ioi.quantity) ELSE SUM(ioi.quantity) - SUM(ioie.exe_o) END) as ordered, ioi.isvid FROM item_order_item as ioi LEFT JOIN item_order io ON (ioi.oid = io.oid) LEFT JOIN ( SELECT count(exe_ioie.oiid) as exe_o, exe_ioie.oiid from item_order_item_execution exe_ioie GROUP BY exe_ioie.oiid ) as ioie ON ioie.oiid = ioi.oiid LEFT JOIN inventory_set_variations isv ON (isv.isvid = ioi.isvid) WHERE (io.osid = '1' OR io.osid = '7') AND ioi.oisid='1' AND isv.isvsid = '1' GROUP BY isv.isvid ) as ois ON (lis.isvid = ois.isvid) 
left join (select oi.iid, discount, amount from offer_item oi, offer where offer.offid=oi.offid and offer.offsid= (select offsid from offer_status where name='Active')) as offers on (i.iid=offers.iid) where i.iid in (118,117,119,120,125,123) order by i.iid;

select i.iid, i.name as item_name, i.description, i.price, i.item_discount, i.istatusid, 
imagefiles, attributes, offers.discount as offer_discount, offers.amount as offer_amount
from item_item as i 
left join (select oi.iid, discount, amount from offer_item oi, offer where offer.offid=oi.offid and offer.offsid= (select offsid from offer_status where name='Active')) as offers on (i.iid=offers.iid)
left join (select iip.iid, GROUP_CONCAT(distinct iip.image SEPARATOR ',') as imagefiles from item_item_photo iip group by iip.iid) as p on (i.iid=p.iid)
left join (select i.iid, 
GROUP_CONCAT(distinct concat_ws('#', iag.name, ia.name, iav.value) SEPARATOR ',') as attributes 
from item_gi igi inner join item_item i on (i.giid=igi.giid)
				left join item_gi_attribute igia on (igi.giid = igia.giid)
				inner join item_attribute ia on (igia.aid=ia.aid)
					left join item_attribute_group iag on (ia.agid=iag.agid)
                    inner join item_attribute_value iav on (iav.aid=ia.aid)
group by i.iid) as att on (att.iid=i.iid),
item_item_status as s
where s.name = 'Active' and s.istatusid = i.istatusid and i.iid=117 group by i.iid;

select i.iid, 
GROUP_CONCAT(distinct concat_ws('#', iag.name, ia.name, iav.value) SEPARATOR ',') as attributes 
from item_gi igi inner join item_item i on (i.giid=igi.giid)
				left join item_gi_attribute igia on (igi.giid = igia.giid)
				inner join item_attribute ia on (igia.aid=ia.aid)
					left join item_attribute_group iag on (ia.agid=iag.agid)
                    inner join item_attribute_value iav on (iav.aid=ia.aid)
group by i.iid;
						
						
select i.iid, i.name as item_name, i.description, i.price, i.item_discount, i.istatusid, 
offers.discount as offer_discount, offers.amount as offer_amount,
GROUP_CONCAT(p.image SEPARATOR ',') as imagefiles
from item_item as i 
left join item_item_photo as p on (p.iid=i.iid)
left join (select oi.iid, discount, amount from offer_item oi, offer where offer.offid=oi.offid and offer.offsid= (select offsid from offer_status where name='Active')) as offers on (i.iid=offers.iid),
item_item_status as s
where s.name = 'Active' and s.istatusid = i.istatusid and i.iid=123;
