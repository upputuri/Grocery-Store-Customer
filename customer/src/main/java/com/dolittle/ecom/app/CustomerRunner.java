package com.dolittle.ecom.app;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.dolittle.ecom.app.security.bo.OTPRequest;
import com.dolittle.ecom.customer.bo.general.PaymentOption;
import com.dolittle.ecom.customer.bo.general.PromoCode;
import com.dolittle.ecom.customer.bo.general.State;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.extern.slf4j.Slf4j;

// @SpringBootApplication
@ComponentScan("com.dolittle.ecom.customer, com.dolittle.ecom.app")
@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
@RestController
@Configuration
@Slf4j
public class CustomerRunner implements CommandLineRunner{

	@Autowired
	JdbcTemplate jdbcTemplate;	
	
	@Autowired
	private JavaMailSender mailSender;
	
	@Value("${spring.mail.username}")
	private String emailFromAddress;
	
	public static void main(final String[] args) {
		SpringApplication.run(CustomerRunner.class, args);
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

	// @RequestMapping("/application/users")
	// public List<User> getAllUsers()
	// {
	// 	final List<User> users = jdbcTemplate.query(
	// 		"select * from auser", (rs, rowNumber) -> {
	// 			User u = new User(rs.getInt("uid"), rs.getString("name"), rs.getString("user_id"));
	// 			u.setPassword(rs.getString("password"));
	// 			return new User(rs.getInt("uid"), rs.getString("name"), rs.getString("user_id"));
	// 		}
	// 	);
	// 	return users;
	// }

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

	@GetMapping(value = "/application/promocodes", produces = "application/hal+json")
	public PromoCode getPromoCodeDetail(@RequestParam String code)
	{
		log.info("Processing request to check validity of promocode: "+code);
		PromoCode promoCode = null;
		try{
			String get_promocode_sql = "select pc.pcid, pc.code, pc.quantity, pc.discount, pcdt.title, pc.order_amount, now() between pc.valid_from and pc.valid_to as isactive "+
			" from promo_code as pc, promo_code_status as pcs, promo_code_discount_type as pcdt where pc.code=? and pc.pcdtid=pcdt.pcdtid and pc.pcsid = pcs.pcsid "+
			"and pcs.name='Active'";
			promoCode = jdbcTemplate.queryForObject(get_promocode_sql, new Object[]{code}, (rs, rowNum) -> {
				PromoCode pc = new PromoCode();
				int qty = rs.getInt("quantity");
				int isActive = rs.getInt("isactive");
				pc.setCodeValue(rs.getString("code"));
				pc.setId(String.valueOf(rs.getString("pcid")));
				pc.setDiscount(rs.getBigDecimal("discount"));
				String discountType = rs.getString("title");
				pc.setDiscountType(discountType.equals("INR")?"currency":"percentage");
				pc.setOrderAmount(rs.getBigDecimal("order_amount"));
				pc.setValid(qty>0 && isActive == 1);
				if (isActive == 0) {
					pc.setReason("expired");
				}
				else if (qty <= 0) {
					pc.setReason("consumed");
				}
				else {
					pc.setReason("active");
				}
				return pc;
			});
			
		}
		catch(EmptyResultDataAccessException e)
		{
			log.error("Invalid promocode: "+code);
			promoCode = new PromoCode();
			promoCode.setCodeValue(code);
			promoCode.setValid(false);
			promoCode.setReason("invalid");
			Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getPromoCodeDetail(null)).withSelfRel();
			promoCode.add(selfLink);
			return promoCode;
		}
		catch(DataAccessException e)
        {
			log.error("An SQL exception occurred", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred!, pls retry after some time or pls call support");
		}
		Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getPromoCodeDetail(null)).withSelfRel();
		promoCode.add(selfLink);
		return promoCode;
	}
	
	@PostMapping(value = "/application/otptokens")
    public void sendOTP(@RequestBody OTPRequest otpRequest, Authentication auth)
    {
        log.info("Processing send OTP request");
        
        if (otpRequest.getType().equals("email"))
        {
            //Send email OTP to customer
            SimpleMailMessage message = new SimpleMailMessage(); 
            message.setFrom(emailFromAddress);

			Pattern EMAIL_ADDRESS_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
			//Pattern PHONE_NUMBER_REGEX = Pattern.compile("^\\d{10}$");
            String toEmailId = otpRequest.getTarget();
			Matcher emailMatcher = EMAIL_ADDRESS_REGEX.matcher(toEmailId);
            if (toEmailId != null && toEmailId.length() > 0 && emailMatcher.find()){
                message.setTo(toEmailId); 
            }
            else {
				log.error("Bad request. Invalid email Id supplied: "+otpRequest.getTarget());
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email Id supplied");
            }
            message.setSubject("OTP Confirmation"); 
            message.setText(otpRequest.getMessage().replace("{}", otpRequest.getOtp()));
			mailSender.send(message);
			log.info("OTP sent to requested target");
        }
    }
}
