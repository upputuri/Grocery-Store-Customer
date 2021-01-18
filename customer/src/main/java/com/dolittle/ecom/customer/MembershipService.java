package com.dolittle.ecom.customer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.websocket.server.PathParam;

import com.dolittle.ecom.app.CustomerConfig;
import com.dolittle.ecom.app.util.CustomerRunnerUtil;
import com.dolittle.ecom.customer.bo.MPlan;
import com.dolittle.ecom.customer.bo.MPlanCategory;
import com.dolittle.ecom.customer.bo.Member;
import com.dolittle.ecom.customer.bo.Membership;
import com.dolittle.ecom.customer.bo.Nominee;
import com.dolittle.ecom.customer.bo.Relationship;
import com.dolittle.ecom.customer.bo.Transaction;
import com.dolittle.ecom.customer.payments.PGIService;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public @Data class MembershipService{

    @Autowired
    JdbcTemplate jdbcTemplateObject;

    @Autowired
    CustomerDataService customerInfo;
    
    @Autowired
    CustomerConfig config;

    @Autowired
    ApplicationContext appContext;

    @GetMapping(value = "/membership/plans/categories", produces = "application/hal+json")
    public CollectionModel<MPlanCategory> getMembershipCategories(@PathParam(value="id") String customerId, Authentication auth) {
        try{
            log.info("Processing request to get membership plan categories");
            String fetch_membership = "select wapacatid, name from wallet_pack_category where wapacatsid=1";

            List<MPlanCategory> mPlanCategories = jdbcTemplateObject.query(fetch_membership, new Object[]{}, (rs, rowNum) -> {
                MPlanCategory cat = new MPlanCategory();
                cat.setCategoryId(String.valueOf(rs.getInt("wapacatid")));
                cat.setCategoryName(rs.getString("name"));

                Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getMembershipCategories(customerId, auth)).withSelfRel();
                cat.add(selfLink);
                return cat;
            });
            
            Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getMembershipCategories(customerId, auth)).withSelfRel();
            CollectionModel<MPlanCategory> categories = CollectionModel.of(mPlanCategories, selfLink);
            return categories;
        }
        catch(DataAccessException e){
            log.error("An exception occurred while getting membership plan categories", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred!, pls retry after some time or pls call support");
        }
    }
    
    @GetMapping(value = "/membership/plans", produces = "application/hal+json")
    public CollectionModel<MPlan> getPlansInCategory(@RequestParam(value="category", required=true) String categoryId) {
        try{
            log.info("Processing request to get membership plans in category Id "+categoryId);
            String fetch_membership = "select wp.wapaid, wp.name from wallet_pack wp, wallet_pack_category wpc where wp.wapacatid = wpc.wapacatid and wpc.wapacatsid = 1 and wp.wapasid = 1 and wpc.wapacatid = ?";

            List<MPlan> mPlanCategories = jdbcTemplateObject.query(fetch_membership, new Object[]{categoryId}, (rs, rowNum) -> {
                MPlan plan = new MPlan();
                plan.setPlanId(String.valueOf(rs.getInt("wapaid")));
                plan.setPlanName(rs.getString("name"));

                Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getPlan(String.valueOf(rs.getInt("wapaid")))).withSelfRel();
                plan.add(selfLink);
                return plan;
            });
            
            Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getPlansInCategory(categoryId)).withSelfRel();
            CollectionModel<MPlan> categories = CollectionModel.of(mPlanCategories, selfLink);
            return categories;
        }
        catch(DataAccessException e){
            log.error("An exception occurred while getting membership plan categories", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred!, pls retry after some time or pls call support");
        }
    }

    @GetMapping(value = "/membership/plans/{planId}", produces = "application/hal+json")
    public MPlan getPlan(@PathVariable String planId) {
        try{
            log.info("Processing request to get membership plan details of plan Id "+planId);
            String fetch_membership = "select wp.wapaid, wp.name, wp.description, wp.short_desc, wp.price, wp.one_time_disc_per, wp.validity, wp.min_purchase_permonth, wp.max_purchase_permonth, wpc.wapacatid, wpc.name as category_name  "+
                                    "from wallet_pack wp, wallet_pack_category wpc where wapasid = 1 and wpc.wapacatsid=1 and wpc.wapacatid=wp.wapacatid and wp.wapaid= ? ";

            MPlan mPlan = jdbcTemplateObject.queryForObject(fetch_membership, new Object[]{planId}, (rs, rowNum) -> {
                MPlan plan = new MPlan();
                plan.setPlanId(String.valueOf(rs.getInt("wapaid")));
                plan.setPlanName(rs.getString("name"));
                plan.setValidityInYears(rs.getInt("validity"));
                plan.setDescription(rs.getString("description"));
                plan.setShortDescription(rs.getString("short_desc"));
                plan.setCategoryId(String.valueOf(rs.getInt("wapaid")));
                plan.setMaxPurchaseAmount(rs.getBigDecimal("max_purchase_permonth"));
                plan.setMinPurchaseAmount(rs.getBigDecimal("min_purchase_permonth"));
                plan.setPlanPrice(rs.getBigDecimal("price"));
                plan.setOneTimeDiscountPercent(rs.getBigDecimal("one_time_disc_per"));
                plan.setCategoryId(String.valueOf(rs.getInt("wapacatid")));
                plan.setCategoryName(rs.getString("category_name"));

                return plan;
            });
            Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getPlan(planId)).withSelfRel();
            mPlan.add(selfLink);
            return mPlan;
        }
        catch(DataAccessException e){
            log.error("An exception occurred while getting membership for plan id"+planId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred!, pls retry after some time or pls call support");
        }
    }

    @GetMapping(value = "/membership/relationships", produces = "application/hal+json")
    public CollectionModel<Relationship> getRelationships() {
        try{
            log.info("Processing request to get membership relationships");
            String fetch_relationships = "select relationship_id, name from relationship where relationship_sid=1";

            List<Relationship> rels = jdbcTemplateObject.query(fetch_relationships, new Object[]{}, (rs, rowNum) -> {
                Relationship r = new Relationship();
                r.setRelationshipId(String.valueOf(rs.getInt("relationship_id")));
                r.setRelationshipName(rs.getString("name"));

                Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getRelationships()).withSelfRel();
                r.add(selfLink);
                return r;
            });
            
            Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getRelationships()).withSelfRel();
            CollectionModel<Relationship> relationships = CollectionModel.of(rels, selfLink);
            return relationships;
        }
        catch(DataAccessException e){
            log.error("An exception occurred while getting membership relationships", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred!, pls retry after some time or pls call support");
        }
    }

    @Transactional
    @PostMapping(value = "/members", produces = "application/hal+json")
    public Membership registerMember(@RequestBody Membership membershipRequest, Authentication auth) {

        // Check if customer already a member, if yes, throw error
        // Check if the membership object has a transaction embedded. If not, create a new transaction, ste relevant fields inncluding pgiprovider, append it to the membership object and return.
        // If membership object has an embedded transaction, validate the pgiresponse using the corresponding pgi service.
        // If the payment validation was successful, then add new entry in the membership registration table, finalize transaction table entry and return membership.
        try{
            log.info("Processing request to register member");
            CustomerRunnerUtil.validateAndGetAuthCustomer(auth, membershipRequest.getCustomerId());
            Membership existingMembership = customerInfo.getCustomerMembership(membershipRequest.getCustomerId(), auth);
            if (existingMembership.getMembershipId() != null) {
                // Customer is already a member
                log.error("Customer already a member - membership id :"+existingMembership.getMembershipId());
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer already a member");
            }
            
            Transaction paymentTransaction = membershipRequest.getTransaction();
            if (paymentTransaction != null && paymentTransaction.getClientResponse() != null) {
                // Payment was already initiated, so check if the payment is complete
                String tran_id = paymentTransaction.getId();
                String orderString = jdbcTemplateObject.queryForObject("select response from transaction where tid=?", new Object[]{tran_id}, String.class);
                String provider = config.getMembershipPaymentProvider();
                PGIService pgiService = BeanFactoryAnnotationUtils.qualifiedBeanOfType(appContext, PGIService.class, provider);  
  
                int code = pgiService.validatePaymentResponse(orderString, paymentTransaction.getClientResponse());
                
                if (code != 0) {
                    // Validation failed
                    Membership membership = new Membership();
                    membership.setMembershipId("-1"+code); // Payment transaction issue
                    return membership;
                }
                else{
                    // Validation successful, update the transaction record and membership record to finalize them.
                    String finalize_transaction = "update transaction set tsid = 1 where tid=?";
                    jdbcTemplateObject.update(finalize_transaction, paymentTransaction.getId());
                    String activate_membership = "update customer_wallet_pack set cuwapasid = 1 where tid=? and cuid=?";
                    jdbcTemplateObject.update(activate_membership, paymentTransaction.getId(), membershipRequest.getCustomerId());
                    return customerInfo.getCustomerMembership(membershipRequest.getCustomerId(), auth);
                }
            }
            else {
                //Create a fresh transaction and add necessary pgi payload.
                //First validate the input member and nominee and plan details and store them in db.
                validateMembershipRequest(membershipRequest);
 
                //Get the membership plan to pay for
                MPlan plan = getPlan(membershipRequest.getPlan().getPlanId());
                Member member = membershipRequest.getMember();
                Nominee nominee = membershipRequest.getNominee();

                // @Data class TaxProfile{
                //     private String taxproid;
                //     private String name;
                //     private String tax_type;
                //     private BigDecimal rate;
                // }

                // String query_tax_detail_sql = "select taxproid, name, rate, tax_type from tax_profile as t where t.default = 1";
                // // Map<String, BigDecimal> tax_profile_rs_map = new HashMap<String, BigDecimal>();
                // TaxProfile taxProfile = jdbcTemplateObject.queryForObject(query_tax_detail_sql, (rs, rowNum) ->{
                //     TaxProfile t = new TaxProfile();
                //     t.taxproid = rs.getString("taxproid");
                //     t.name = rs.getString("name");
                //     t.rate = rs.getBigDecimal("rate");
                //     t.tax_type = rs.getString("tax_type");
                //     return t;
                // });

                // BigDecimal taxRate = taxProfile.rate.setScale(2, RoundingMode.HALF_EVEN);
                // BigDecimal totalTaxValue = plan.getPlanPrice().multiply(taxRate.divide(new BigDecimal(100)).setScale(2, RoundingMode.HALF_EVEN)).setScale(2, RoundingMode.HALF_EVEN);
                // BigDecimal transactionAmount = plan.getPlanPrice().add(totalTaxValue).setScale(2, RoundingMode.HALF_EVEN);

                paymentTransaction = createPaymentTransaction(membershipRequest.getCustomerId(), plan.getPlanPrice().setScale(2, RoundingMode.HALF_EVEN));
                
                SimpleJdbcInsert jdbcInsert_transaction = new SimpleJdbcInsert(jdbcTemplateObject)
                                    .usingColumns("cuid", "wapaid", "tid", "start_date", "end_date", "price", "validity", "min_purchase_permonth", "max_purchase_permonth",
                                                    "cuwapasid", "mem_fname", "mem_mname", "mem_fullname", "mem_email", "mem_dob", "mem_gender", "mem_mob", "mem_alt_mob", "mem_pre_addr", "mem_pre_pincode", "mem_photo", "mem_aadhar_fph", "mem_aadhar_bph",
                                                    "nom_fname", "nom_mname", "nom_fullname", "nom_email", "nom_dob", "nom_gender", "nom_mob", "nom_alt_mob", "nom_photo", "nom_aadhar_fph", "nom_aadhar_bph")
                                    .withTableName("customer_wallet_pack");
                Map<String, Object> parameters_insert_transaction = new HashMap<String, Object>(1);
                parameters_insert_transaction.put("cuid", membershipRequest.getCustomerId());
                parameters_insert_transaction.put("wapaid", plan.getPlanId());
                parameters_insert_transaction.put("tid", paymentTransaction.getId());
                Calendar now = Calendar.getInstance();
                now.setTimeZone(TimeZone.getTimeZone("GMT+0530"));
                String startDateStr = now.get(Calendar.YEAR)+"-"+now.get(Calendar.MONTH)+"-"+now.get(Calendar.DAY_OF_MONTH);
                parameters_insert_transaction.put("start_date", startDateStr);
                now.add(Calendar.YEAR, plan.getValidityInYears());
                String endDateStr = now.get(Calendar.YEAR)+"-"+now.get(Calendar.MONTH)+"-"+now.get(Calendar.DAY_OF_MONTH);
                parameters_insert_transaction.put("end_date", endDateStr);
                
                parameters_insert_transaction.put("price", plan.getPlanPrice());
                parameters_insert_transaction.put("validity", plan.getValidityInYears());
                parameters_insert_transaction.put("min_purchase_permonth", plan.getMinPurchaseAmount());
                parameters_insert_transaction.put("max_purchase_permonth", plan.getMaxPurchaseAmount());
                parameters_insert_transaction.put("cuwapasid", 2); // Pending

                parameters_insert_transaction.put("mem_fname", member.getFName());
                parameters_insert_transaction.put("mem_mname", member.getLName());
                parameters_insert_transaction.put("mem_fullname", member.getFName()+ " " +member.getLName());
                parameters_insert_transaction.put("mem_dob", member.getDob());
                parameters_insert_transaction.put("mem_gender", member.getGender());
                parameters_insert_transaction.put("mem_email", member.getEmail());
                parameters_insert_transaction.put("mem_mob", member.getMobile());
                parameters_insert_transaction.put("mem_alt_mob", member.getAltMobile());
                parameters_insert_transaction.put("mem_pre_addr", member.getPresentAddress());
                parameters_insert_transaction.put("mem_pre_pincode", member.getPresentPinCode());
                parameters_insert_transaction.put("mem_photo", member.getPhotoImg());
                parameters_insert_transaction.put("mem_aadhar_fph", member.getAdhaarFrontImg());
                parameters_insert_transaction.put("mem_aadhar_bph", member.getAdhaarBackImg());

                parameters_insert_transaction.put("nom_fname", nominee.getFName());
                parameters_insert_transaction.put("nom_mname", nominee.getLName());
                parameters_insert_transaction.put("nom_fullname", nominee.getFName()+ " " +nominee.getLName());
                parameters_insert_transaction.put("nom_email", nominee.getEmail());
                parameters_insert_transaction.put("nom_dob", nominee.getDob());
                parameters_insert_transaction.put("nom_gender", nominee.getGender());
                parameters_insert_transaction.put("nom_mob", nominee.getMobile());
                parameters_insert_transaction.put("nom_alt_mob", nominee.getAltMobile());
                parameters_insert_transaction.put("nom_relation", nominee.getRelationshipId()); 
                parameters_insert_transaction.put("nom_photo", nominee.getPhotoImg());
                parameters_insert_transaction.put("nom_aadhar_fph", nominee.getAdhaarFrontImg());
                parameters_insert_transaction.put("nom_aadhar_bph", nominee.getAdhaarBackImg());

                jdbcInsert_transaction.execute(parameters_insert_transaction);
                
                Membership membership = new Membership();
                membership.setCustomerId(membershipRequest.getCustomerId());
                membership.setTransaction(paymentTransaction);
                Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).registerMember(membershipRequest, auth)).withSelfRel();
                membership.add(selfLink);
                return membership;
            }
            
        }
        catch(DataAccessException e){
            log.error("An exception occurred while getting membership relationships", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred!, pls retry after some time or pls call support");
        }
    }

    private Transaction createPaymentTransaction(String customerId, BigDecimal amount) throws DataAccessException{
        int tsid = jdbcTemplateObject.queryForObject("select tsid from transaction_status where name='Pending'", Integer.TYPE);
        // Create a new transaction in db
        String provider = config.getMembershipPaymentProvider();
        PGIService pgiService = BeanFactoryAnnotationUtils.qualifiedBeanOfType(appContext, PGIService.class, provider);
        SimpleJdbcInsert jdbcInsert_transaction = new SimpleJdbcInsert(jdbcTemplateObject)
                    .usingColumns("cuid", "cpoid", "tsid", "amount", "discount_amt")
                    .withTableName("transaction")
                    .usingGeneratedKeyColumns("tid");
        Map<String, Object> parameters_insert_transaction = new HashMap<String, Object>(1);
        parameters_insert_transaction.put("cuid", customerId);
        parameters_insert_transaction.put("tsid", tsid);
        parameters_insert_transaction.put("amount", amount);
        parameters_insert_transaction.put("discount_amt", 0);        
        
        Number tran_id = jdbcInsert_transaction.executeAndReturnKey(parameters_insert_transaction);
        Transaction transaction = new Transaction();
        transaction.setId(tran_id.toString());
        int transactionAmount = amount.multiply(new BigDecimal(100)).intValue();
        transaction.setAmount(transactionAmount);
        
        String providerData = pgiService.startTransaction(transactionAmount, tran_id.toString());
        // Inject a payment gateway provider and its data
        transaction.setProviderId(provider);
        transaction.setProviderData(providerData);

        // Add the pgi's payment order object to the transaction in db
        jdbcTemplateObject.update("update transaction set response=? where tid="+tran_id, providerData);

        return transaction;
    }

    private int validateMembershipRequest(Membership request) {
        try{
            assert (request.getCustomerId() != null && request.getMember() != null && request.getNominee() != null && request.getPlan() != null);
            Member member = request.getMember();
            Nominee nominee = request.getNominee();
            MPlan plan = request.getPlan();
            assert (plan.getPlanId() != null);
            assert (member.getFName() != null && member.getLName() != null && member.getDob() != null && member.getGender() != null &&
                    member.getEmail() != null && member.getMobile() != null && member.getPresentAddress() != null && member.getPresentPinCode() != null &&
                    member.getAdhaarFrontImg() != null && member.getAdhaarBackImg() != null && member.getPhotoImg() != null);
            assert (nominee.getFName() != null && nominee.getLName() != null && nominee.getDob() != null && nominee.getGender() != null &&
                    nominee.getEmail() != null && nominee.getMobile() != null && nominee.getRelationshipId() != null &&
                    nominee.getAdhaarFrontImg() != null && nominee.getAdhaarBackImg() != null && nominee.getPhotoImg() != null);
        }
        catch(AssertionError e) {
            String msg = "Invalid request. Some of the required fields in membership request are missing or invalid";
            log.error(msg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
        }
        return 0;
    }
}
