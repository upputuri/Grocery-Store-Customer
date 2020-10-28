#customer query
select * from customer_query;
select * from customer_query_comment;
select * from customer_query_issue;
select * from customer_query_issue_attribute;
select * from customer_query_issue_attribute_status;
select * from customer_query_issue_attribute_value;
select * from customer_query_message;
select * from customer_query_status;

select * from customer;

insert into customer_query set cqiid=1, cuid=618, name='Srikanth', email='usrik@gmail.com', subject='Hello', cqsid=1;
insert into customer_query_message set uid=?, cqid=?, message=?, user_type=3