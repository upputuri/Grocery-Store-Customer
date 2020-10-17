select * from customer;
desc customer;
select * from customer_status;
select * from auser;
desc auser;
select * from auser_role;
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

update customer set salutation=1, email='usrikanth@5roads.in' where cuid='618';



