package com.dolittle.ecom.app;

import java.util.List;

import com.dolittle.ecom.app.bo.Cover;
import com.dolittle.ecom.app.bo.CoverCity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StoresService {
    @Autowired
    JdbcTemplate jdbcTemplateObject;

    @GetMapping(value= "/stores/covers", produces = "application/hal+json")
	public CollectionModel<Cover> getCovers()
	{
		String fetch_covers_sql = "select distinct co.coverid, co.city, co.shipping_cost, co.min_order_amount from checkpoint chk left join coverage co on (chk.coverid = co.coverid) "+
									"inner join inventory_set invs on (chk.chkid = invs.chkid) where chk.chksid=1 and co.coversid=(select coversid from coverage_status where name='Active')";
		List<Cover> covers = jdbcTemplateObject.query(fetch_covers_sql, (rs, rowNumber) -> {
			Cover cover = new Cover();
			cover.setCoverId(rs.getString("coverid"));
			cover.setCoverCity(rs.getString("city"));
			cover.setShippingCost(rs.getBigDecimal("shipping_cost"));
			cover.setMinOrderValue(rs.getBigDecimal("min_order_amount"));
			Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getCovers()).withSelfRel();
			cover.add(selfLink);
			return cover;
		});

		Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getCovers()).withSelfRel();
		CollectionModel<Cover> result = CollectionModel.of(covers, selfLink);
		return result;
	}
	
	@GetMapping(value= "/stores/covers/cities", produces = "application/hal+json")
	public CollectionModel<CoverCity> getCities(@RequestParam(value="stateid", required=false) String stateId)
	{
		String state_filter_sql = "";
		if (stateId != null){
			state_filter_sql += "and state.stid="+stateId;
		}
		String fetch_cities_sql = "select distinct c.city, c.stid, state.state as state, p.pincodes from state, coverage c "+
								"left join (select ps.name as city, GROUP_CONCAT(psp.pincode SEPARATOR ',') as pincodes from pincode_set_pincode psp, pincode_set ps "+
								"where ps.pinsid=psp.pinsid group by ps.name) as p on (c.city=p.city) where c.coversid=1 and c.stid=state.stid "+state_filter_sql;
		List<CoverCity> cities = jdbcTemplateObject.query(fetch_cities_sql, (rs, rowNumber) -> {
			CoverCity city = new CoverCity();
			city.setName(rs.getString("city"));
			city.setStateId(rs.getString("stid"));
			city.setState(rs.getString("state"));
			city.setPinCodes(rs.getString("pincodes") != null ? rs.getString("pincodes").split(",") : new String[0]);
			Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getCities(stateId)).withSelfRel();
			city.add(selfLink);
			return city;
		});

		Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getCities(stateId)).withSelfRel();
		CollectionModel<CoverCity> result = CollectionModel.of(cities);
		result.add(selfLink);
		return result;
	}

    // @GetMapping(value= "/stores/covers/{city}/pincodes", produces = "application/hal+json")
	// public CollectionModel<String> getCityPinCodes(@PathVariable String city)
	// {
	// 	String fetch_pincodes_sql = "select pincode from pincode_set_pincode psp, pincode_set ps where ps.name=? and ps.pinsid=psp.pinsid";
	// 	List<String> pinCodes = jdbcTemplateObject.query(fetch_pincodes_sql, new Object[]{city}, (rs, rowNumber) -> {
	// 		return rs.getString("pincode");
	// 	});

	// 	Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getCityPinCodes(city)).withSelfRel();
	// 	CollectionModel<String> result = CollectionModel.of(pinCodes);
	// 	result.add(selfLink);
	// 	return result;
	// }
}
