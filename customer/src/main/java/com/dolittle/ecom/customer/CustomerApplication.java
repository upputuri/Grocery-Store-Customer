package com.dolittle.ecom.customer;

import java.util.List;

import com.dolittle.ecom.customer.bo.User;
import com.dolittle.ecom.customer.bo.general.PaymentOption;
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
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.extern.slf4j.Slf4j;

// @SpringBootApplication
@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
@RestController
@Configuration
@Slf4j
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

	@GetMapping(value= "/application/states", produces = "application/hal+json")
	public CollectionModel<State> getStates()
	{
		List<State> states = jdbcTemplate.query("select stid, state from state", (rs, rowNumber) -> {
			State state = new State();
			state.setName(rs.getString("state"));
			state.setStateId(String.valueOf(rs.getInt("stId")));
			Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getStates()).withSelfRel();
			state.add(selfLink);
			return state;
		});

		Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getStates()).withSelfRel();
		CollectionModel<State> result = CollectionModel.of(states, selfLink);
		return result;
	}

	@GetMapping(value= "/application/paymentoptions", produces = "application/hal+json")
	public CollectionModel<PaymentOption> getPaymentOptions(@RequestParam String type)
	{
		if (!type.equalsIgnoreCase("ondelivery"))
		{
			log.error("Service not implemented for type: "+type);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not implemented");
		}
		List<PaymentOption> paymentOptions = jdbcTemplate.query("select cpo.cpoid, cpo.cptid, cpo.name, cpo.description from customer_payment_option cpo, customer_payment_option_status cpos "+
												"where cptid=2 and cpo.cposid = cpos.cposid", new Object[]{}, (rs, rowNumber) -> {
			PaymentOption option = new PaymentOption();
			option.setId(String.valueOf(rs.getString("cpoid")));
			option.setTypeId(String.valueOf(rs.getString("cptid")));
			option.setName(rs.getString("name"));
			option.setDescription(rs.getString("description"));
			Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getPaymentOptions(null)).withSelfRel();
			option.add(selfLink);
			return option;
		});

		Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getPaymentOptions(null)).withSelfRel();
		CollectionModel<PaymentOption> result = CollectionModel.of(paymentOptions, selfLink);
		return result;
	}
	
}
