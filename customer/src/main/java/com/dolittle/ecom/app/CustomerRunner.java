package com.dolittle.ecom.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.dolittle.ecom.app.bo.Subscriptions;
import com.dolittle.ecom.app.bo.Variables;
import com.dolittle.ecom.app.security.GrocPasswordEncoder;
import com.dolittle.ecom.app.security.bo.OTPRequest;
import com.dolittle.ecom.app.sms.SMSServiceProvider;
import com.dolittle.ecom.app.util.CustomerRunnerUtil;
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
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

// @SpringBootApplication
@ComponentScan("com.dolittle.ecom.customer, com.dolittle.ecom.runner, com.dolittle.ecom.app")
@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
@RestController
@Configuration
@Slf4j
public class CustomerRunner implements CommandLineRunner{

	@Autowired
	JdbcTemplate jdbcTemplate;	
	
	@Autowired
	private JavaMailSender mailSender;

	@Autowired
	private SMSServiceProvider smsSender;
	
	@Autowired
	private PasswordEncoder passwordEncoder;

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

	// @Bean
	// public PasswordEncoder getPasswordEncoder() {
	// 	// return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	// 	return new GrocPasswordEncoder();
	// }

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
		List<PaymentOption> paymentOptions = jdbcTemplate.query("select cpo.cpoid, cpo.cptid, cpo.name, cpo.description from customer_payment_option cpo, customer_payment_option_status cpos "+
												"where cptid=? and cpo.cposid = cpos.cposid and cpos.name='Active'", new Object[]{type}, (rs, rowNumber) -> {
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
    public void sendOTP(@RequestBody OTPRequest otpRequest)
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
		else if (otpRequest.getType().equals("mobile"))
		{
			int code = smsSender.sendOTP(otpRequest.getTarget(), otpRequest.getMessage().replace("{}", otpRequest.getOtp()));
			if (code == 0){
				log.info("OTP dispatched to SMS service successfully!");
			}
			else{
				log.error("Dispatching OTP to SMS service failed. Please check SMS service logs for service specific error codes");
			}
		}
	}
	
	@Data class CoverImage extends RepresentationModel<CoverImage>{
		String image;
		int rank;
	}

	@GetMapping(value="/application/coverimages", produces = "application/hal+json")
	public CollectionModel<CoverImage> getCoverImages()
	{
		log.info("Processing get cover images");

		String cover_images_sql = "SELECT name, cover_image.rank FROM cover_image WHERE coisid = '1' and ((curdate()>= from_time and curdate() <= to_time) "+
							"OR (from_time is null and to_time is null) OR (curdate()>=from_time and to_time is null)) order by cover_image.rank";
		
		List<CoverImage> images = new ArrayList<CoverImage>();
		try{
			images = jdbcTemplate.query(cover_images_sql, (rs, rowNum) -> {
				CoverImage ci = new CoverImage();
				ci.image = rs.getString("name");
				ci.rank = rs.getInt("rank");
				Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getCoverImages()).withSelfRel();
				ci.add(selfLink);
				return ci;
			});
		}
		catch(DataAccessException e)
        {
			log.error("An SQL exception occurred", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred!, pls retry after some time or pls call support");
		}
		Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getCoverImages()).withSelfRel();

		return CollectionModel.of(images).add(selfLink);
	}

	@GetMapping(value="/application/subscriptions", produces="application/hal+json")
    public Subscriptions getSubscriptions(@RequestParam String email)
    {
		log.info("Processing get subscriptions for email: "+email);
		Subscriptions subs = new Subscriptions();
		try{
			String get_subscriptions_sql = "select count(*) from newsletter_email where email=? and nlesid=1";
			int count = jdbcTemplate.queryForObject(get_subscriptions_sql, new Object[]{email}, Integer.TYPE);
			subs.setNewsletter(count > 0 ? true: false);
		}
		catch(DataAccessException e)
        {
			log.error("An SQL exception occurred", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred!, pls retry after some time or pls call support");
		}
        Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getSubscriptions(email)).withSelfRel();
        return subs.add(selfLink);
    }

    @PostMapping(value="/application/subscriptions")
    public void updateSubscription(@RequestBody Subscriptions subs, @RequestParam String email)
    {
		log.info("Processing update subscription for email: "+email);
		try{
			Subscriptions s = getSubscriptions(email);
			if (!s.isNewsletter() && subs.isNewsletter()){
				String add_subscription_sql = "INSERT INTO newsletter_email (email , nlesid) VALUES(? , '1')";
				jdbcTemplate.update(add_subscription_sql, email);
			}else if (s.isNewsletter() && !subs.isNewsletter())
			{
				String remove_subscription_sql = "DELETE FROM newsletter_email where email=?";
				jdbcTemplate.update(remove_subscription_sql, email);
			}
		}
		catch(DataAccessException e)
        {
			log.error("An SQL exception occurred", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred!, pls retry after some time or pls call support");
		}
	}

	// Reset password request from a registered user. The mobile number must be validated as belonging to the user before calling this endpoint.
	// The implementation of this end point ensures the mobile number is verified to belong to a registered user before sending a new password to 
	// the verified mobile number.
	@PostMapping(value="/application/passwords")
	@Transactional
	public void resetPassword(@RequestBody OTPRequest otpRequest) {
		log.info("Processing reset Password request");
		
		//Generate a new password
		String newPassword = new String(CustomerRunnerUtil.generatePassword(6));

		if (otpRequest.getType().equals("mobile"))
		{
			Pattern PHONE_NUMBER_REGEX = Pattern.compile("^\\d{10}$");
			if (otpRequest.getTarget() != null) {
				Matcher phoneMatcher = PHONE_NUMBER_REGEX.matcher(otpRequest.getTarget());
				if (!phoneMatcher.find()) {
					log.error("Invalid mobile number supplied for password reset request");
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Phone number");
				}
			}else{
				log.error("Invalid mobile number supplied for password reset request");
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Phone number");
			}

			//Ensure account exists with this mobile
			int cuid = 0;
			try{
				cuid = jdbcTemplate.queryForObject("select cuid from customer where mobile=?", new Object[]{otpRequest.getTarget()}, Integer.TYPE);
			}catch(EmptyResultDataAccessException e){
				log.error("No account registered with this mobile number");
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No account registered with this mobile number");
			}

			//Update in db
			String passwordHash = passwordEncoder.encode(newPassword);
			try{
				jdbcTemplate.update("update customer set password=? where cuid=?", passwordHash, cuid);
			}
			catch(DataAccessException e)
			{
				log.error("An exception occurred while updating profile data", e);
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred!, pls retry after some time or pls call support");
			}
		
            //Send email OTP to customer
            SimpleMailMessage message = new SimpleMailMessage(); 
            message.setFrom(emailFromAddress);

			Pattern EMAIL_ADDRESS_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
			//Pattern PHONE_NUMBER_REGEX = Pattern.compile("^\\d{10}$");
            String toEmailId = "thevegitclub@gmail.com";// TODO: hack, use otpRequest.getTarget() instead;
			Matcher emailMatcher = EMAIL_ADDRESS_REGEX.matcher(toEmailId);
            if (toEmailId != null && toEmailId.length() > 0 && emailMatcher.find()){
                message.setTo(toEmailId); 
            }
            else {
				log.error("Bad request. Invalid email Id supplied: "+otpRequest.getTarget());
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email Id supplied");
            }
            message.setSubject("Vegit Password Reset"); 
            message.setText(otpRequest.getMessage().replace("{}", newPassword));
			mailSender.send(message);
			log.info("New Password sent to requested target");
        }
	}

	@GetMapping(value="/application/variables", produces="application/hal+json")
	public Variables getVariables(@RequestParam(value="keys", required=true) String keyCsv) {
		log.info("Processing request to get values of variables: "+keyCsv);
		keyCsv = Arrays.stream(keyCsv.split(",")).map(key -> "'"+key+"'").collect(Collectors.joining(","));
		String fetch_variables_query = "select vid, value from variable where vid in ("+keyCsv+")";
		Map<String, Object> vars = new HashMap<String, Object>(1);
		jdbcTemplate.query(fetch_variables_query, new Object[]{}, (rs, rowNum) -> {
			String vid = rs.getString("vid");
			String value = rs.getString("value");
			vars.put(vid, value);
			return null;
		});
		
		Variables variables = new Variables();
		variables.setVariables(vars);
		Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getVariables(null)).withSelfRel();
		variables.add(selfLink);
		return variables;
	}

	@GetMapping(value="/application/socialhandles", produces="application/hal+json")
	public Variables getVariables() {
		log.info("Processing request to get values of social media handles: ");
		String fetch_variables_query = "select swl.name, swd.url from social_widget_list swl, social_widget_list_status swls, social_widget_details swd, social_widget_details_status swds "+
										"where swl.swlid = swd.swlid and swds.swdsid = swd.swdsid and swls.name='Active' and swds.name='Active' ORDER BY swdid ASC";
		Map<String, Object> vars = new HashMap<String, Object>(1);
		jdbcTemplate.query(fetch_variables_query, new Object[]{}, (rs, rowNum) -> {
			String name = rs.getString("name");
			String url = rs.getString("url");
			vars.put(name, url);
			return null;
		});
		
		Variables variables = new Variables();
		variables.setVariables(vars);
		Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getVariables(null)).withSelfRel();
		variables.add(selfLink);
		return variables;
	}	
}
