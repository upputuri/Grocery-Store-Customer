select * from tbl_products_ratings;
select * from tbl_products_review;

SELECT * FROM wallet_pack_category;
select * from wallet_pack;
SELECT * FROM wallet_pack where wapacatid = ? and wapasid = 1;
SELECT * FROM wallet_pack where wapaid = ? and wapasid = 1;

SELECT cwp.*,wp.wapaid as memplanid,wp.price as purprice,wp.amount as puramt,wp.one_time_amt as onetimeamt,wp.one_time_disc_per as onetimediscper,wp.validity as purvalidity,wp.duration as purduration,wp.min_purchase_permonth as minpurpermonth,wp.max_purchase_permonth as maxpurpermonth FROM customer_wallet_pack cwp INNER JOIN wallet_pack wp ON cwp.wapaid = wp.wapaid WHERE cwp.cuid = '{CUSTOMER_ID}' AND cwp.cuwapasid = 1 AND cwp.wapaid != 'NULL' ORDER BY cwp.created_ts DESC