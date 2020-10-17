package com.dolittle.ecom.customer;

import java.util.List;

import com.dolittle.ecom.customer.bo.User;
import com.dolittle.ecom.customer.bo.general.State;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// @SpringBootApplication
@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
@RestController
@Configuration
public class CustomerApplication implements CommandLineRunner{

	@Autowired
	JdbcTemplate jdbcTemplate;	
	
	public static void main(final String[] args) {
		SpringApplication.run(CustomerApplication.class, args);
	}

	@Override
	public void run(final String... args) throws Exception {
		// final List<User> users = jdbcTemplate.query(
		// 	"select * from auser", (rs, rowNumber) -> {
		// 		User u = new User(rs.getInt("uid"), rs.getString("name"), rs.getString("user_id"));
		// 		u.setPassword(rs.getString("password"));
		// 		return new User(rs.getInt("uid"), rs.getString("name"), rs.getString("user_id"));
		// 	}
		// );
		// users.forEach(u -> System.out.println(u));
	}

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**").allowedOrigins("http://localhost:8100");
				registry.addMapping("/**").allowedOrigins("https://xenodochial-heisenberg-09911c.netlify.app");
				registry.addMapping("/**").allowedOrigins("*").allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH");
			}
		};
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

	@RequestMapping(value= "/application/states", produces = "application/hal+json")
	public CollectionModel<State> getStates()
	{
		List<State> states = jdbcTemplate.query("select stid, state from state", (rs, rowNumber) -> {
			State state = new State();
			state.setName(rs.getString("state"));
			state.setStateId(String.valueOf(rs.getInt("stId")));
			return state;
		});

		Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getStates()).withSelfRel();
		CollectionModel<State> result = CollectionModel.of(states, selfLink);
		return result;
	}
	
}
