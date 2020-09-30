package com.dolittle.ecom.customer;

import java.util.List;

import com.dolittle.ecom.customer.bo.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@SpringBootApplication
@RestController

public class CustomerApplication implements CommandLineRunner{

	@Autowired
	JdbcTemplate jdbcTemplate;	
	
	public static void main(final String[] args) {
		SpringApplication.run(CustomerApplication.class, args);
	}

	@Override
	public void run(final String... args) throws Exception {
		final List<User> users = jdbcTemplate.query(
			"select * from auser", (rs, rowNumber) -> {
				User u = new User(rs.getInt("uid"), rs.getString("name"), rs.getString("user_id"));
				u.setPassword(rs.getString("password"));
				return new User(rs.getInt("uid"), rs.getString("name"), rs.getString("user_id"));
			}
		);
		users.forEach(u -> System.out.println(u));
	}

	@RequestMapping("/application/users")
	public List<User> getAllUsers()
	{
		final List<User> users = jdbcTemplate.query(
			"select * from auser", (rs, rowNumber) -> {
				User u = new User(rs.getInt("uid"), rs.getString("name"), rs.getString("user_id"));
				u.setPassword(rs.getString("password"));
				return new User(rs.getInt("uid"), rs.getString("name"), rs.getString("user_id"));
			}
		);
		return users;
	}
}
