select * from customer;
desc customer;
select * from customer_status;
select * from auser;
desc auser;
select * from auser_role;
select * from arole_status;
select * from arole;
select * from auser_session;
select * from auser_session_status;
select * from auser_status;

#uid = 75, cuid = 618

INSERT INTO `grocdb`.`customer` (`cuid`, `unq_id`, `fname`, `lname`, `email`, `mobile`) VALUES ('618', 'SU04101447', 'Srikanth', 'Upputuri', 'usrikanth@gmail.com', '9845281139');

#there is session id in request, so check if sessionid is valid
select * from auser_session where sid=?;
	#session id is valid, we fetched the uid for this user, now fetch cart details etc using the cuid mapped to the uid.

#login request.
select u.uid, c.cuid, c.fname, c.lname, c.email, c.photo from customer c, auser u where u.email=? and u.password=? and u.uid = c.uid;
	#Check if theres a valid customer in db. Now deactive old session and create new session.
    update auser_session set ussid=2 where uid=?;
    insert auser_session (uid, sid, ipaddress, ussid) values (?,?,?,1);
    #Get usid from the above insert and return the customer fname, lname, cuid, email along with usid to client
#no session 

insert into auser (name, email, password, type_auser, ustatusid) values (?, ?, ?, 3, (select ustatusid from auser_status where name = 'Active'));

insert into customer (uid, fname, lname, email, password) values (?, ?, ?, ?, ?);

insert into auser_role (uid, rid) value (?, ?);

update customer set salutation=1, email='usrikanth@5roads.in' where cuid='618';

select au.uid, au.user_id, au.name, au.email, au.password, aus.name as user_status, ar.name as role_name, ars.name as role_status 
                                from auser au, auser_status as aus, arole as ar, arole_status ars, auser_role as aur
                                where au.ustatusid = aus.ustatusid and ars.rsid = ar.rsid and ar.rid=aur.rid and aur.uid = au.uid and au.user_id='usrikanth@gmail.com';
                                
select c.cuid, c.uid, c.email, c.fname, c.lname from customer c, auser au
                                    where au.uid= c.uid and au.user_id = 'usrikanth@gmail.com' and c.cuid = '618' and c.custatusid = (select custatusid from customer_status where name='Active');                                

select au.uid from auser au where (length(au.email)>0 and au.email='') or au.user_id=9845281139;

select au.uid, au.user_id, au.name, au.email, au.password, aus.name as user_status, ar.name as role_name, ars.name as role_status 
from auser au, auser_status as aus, arole as ar, arole_status ars, auser_role as aur  
where au.ustatusid = aus.ustatusid and ars.rsid = ar.rsid and ar.rid=aur.rid and aur.uid = au.uid and  au.user_id='9845281139';
