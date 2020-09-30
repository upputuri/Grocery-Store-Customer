package com.dolittle.ecom.customer;

import java.util.ArrayList;
import java.util.List;

import com.dolittle.ecom.customer.bo.Category;
import com.dolittle.ecom.customer.bo.Product;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class ProductsService{
    
	@Autowired
    JdbcTemplate jdbcTemplateObject;

    @GetMapping(value = "/products/categories/{id}", produces = "application/hal+json")
    public Category getCategoryDetail()
    {
        return null;
    }

    @GetMapping(value = "/products/categories", produces = "application/hal+json")
    public CollectionModel<Category> getProductCategories()
    {
        List<Category> cats = new ArrayList<Category>();
        try{
             cats = jdbcTemplateObject.query(
                "select * from category", (rs, rowNumber) -> {
                    Category c = new Category(String.valueOf(rs.getInt("catid")), rs.getString("name"));
                    c.setImage(rs.getString("image"));
                    Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getCategoryDetail()).withSelfRel();
                    c.add(selfLink);
                    return c;
                }
            );
        }
        catch(DataAccessException e)
        {
            log.error("An SQL exception occurred", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred!, pls retry after some time or pls call support");
        }
        Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getCategoryDetail()).withSelfRel();
        CollectionModel<Category> result = CollectionModel.of(cats, selfLink);
		return result;
    }

    @GetMapping(value = "/products/categories/{id}/products", produces = "application/hal+json")
    public List<Product> getProductsOfCategory(@PathVariable(value = "id") String id, 
                                                @RequestParam(value = "offset", required=false, defaultValue = "0") int pageOffset, 
                                                @RequestParam(value = "size", required=false, defaultValue = "1000000") int pageSize)
    {
            /*
                #Get products in a category as a page. Args 129, 77, 10, 10

                select i.iid, i.name as item_name, i.description, i.price, i.item_discount, i.istatusid, p.imagefiles, p.title, s.name as status, s.description
                from item_item as i, item_item_status as s, item_gi_category as ic, category as c,
                (select iid, title, GROUP_CONCAT(image separator ',') as imagefiles from item_item_photo group by iid) as p
                where p.iid = i.iid and s.istatusid = i.istatusid and ic.giid = i.iid and c.catid = ic.catid and c.catid = ?
                limit ?, ? ;
            */
        List<Product> prods = new ArrayList<Product>();
        try{
            prods = jdbcTemplateObject.query(
               "select i.iid, i.name as item_name, i.description, i.price, i.item_discount, i.istatusid, p.imagefiles, p.title, s.name as status, s.description "+
               "from item_item as i, item_item_status as s, item_gi_category as ic, category as c, "+
               "(select iid, title, GROUP_CONCAT(image separator ',') as imagefiles from item_item_photo group by iid) as p "+
               "where p.iid = i.iid and s.istatusid = i.istatusid and ic.giid = i.iid and c.catid = ic.catid and c.catid = ? "+
               "limit ?, ? ", new Object[]{id, pageOffset, pageSize}, (rs, rowNumber) -> {
                   Product p = new Product(String.valueOf(rs.getInt("iid")), rs.getString("item_name"), rs.getString("imagefiles"));
                   p.setStatus(rs.getString("status"));
                   return p;
               }
           );
       }
       catch(DataAccessException e)
       {
            log.error("An SQL exception occurred", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "A internal error occurred!, pls retry after some time or pls call support");
       }
       return prods;
    }
    
    @GetMapping(value = "/products/{id}", produces = "application/hal+json")
    public Product getProductDetail(@PathVariable(value = "id") String id)
    {
        // #Get product detail
        // select i.iid, i.name as item_name, i.description, i.price, i.item_discount, i.istatusid, p.imagefiles, p.title, s.name as status, s.description 
        // from item_item as i, item_item_status as s, 
        // (select iid, title, GROUP_CONCAT(image separator ',') as imagefiles from item_item_photo where iid=?) as p
        // where p.iid = i.iid and s.istatusid = i.istatusid;        
        // System.out.println(id);
        try{
            return jdbcTemplateObject.queryForObject(
                "select i.iid, i.name as item_name, i.description, i.price, i.item_discount, i.istatusid, p.imagefiles, p.title, s.name as status, s.description "+
                "from item_item as i, item_item_status as s, "+
                "(select iid, title, GROUP_CONCAT(image separator ',') as imagefiles from item_item_photo where iid=?) as p "+
                "where p.iid = i.iid and s.istatusid = i.istatusid", new Object[]{id}, (rs, rowNumber) -> {
                    Product p = new Product(String.valueOf(rs.getInt("iid")), rs.getString("item_name"), rs.getString("imagefiles"));
                    p.setStatus(rs.getString("status"));
                    Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getProductDetail(id)).withSelfRel();
                    p.add(selfLink);
                    return p;
                }
            );
        }
        catch(DataAccessException e)
        {
            log.error("An SQL exception occurred", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "A internal error occurred!, pls retry after some time or pls call support");
        }
            
    }

}
