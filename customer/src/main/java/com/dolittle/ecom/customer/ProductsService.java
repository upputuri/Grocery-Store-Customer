package com.dolittle.ecom.customer;

import java.util.ArrayList;
import java.util.List;

import com.dolittle.ecom.customer.bo.Category;
import com.dolittle.ecom.customer.bo.Product;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductsService {
    
	@Autowired
    JdbcTemplate jdbcTemplate;

    @GetMapping("/products/categories")
    public List<Category> getProductCategories()
    {
        List<Category> cats = new ArrayList<Category>();
        try{
             cats = jdbcTemplate.query(
                "select * from category", (rs, rowNumber) -> {
                    Category c = new Category(String.valueOf(rs.getInt("catid")), rs.getString("name"));
                    c.setImage(rs.getString("image"));
                    return c;
                }
            );
        }
        catch(DataAccessException e)
        {
            throw e;
            //e.printStackTrace();
        }
		return cats;
    }

    @GetMapping("/products/categories/{id}/products")
    public List<Product> getProductsOfCategory(@PathVariable(value = "id") String id, 
                                                @RequestParam(value = "offset") int pageOffset, 
                                                @RequestParam(value = "size") int pageSize)
    {
        List<Product> prods = new ArrayList<Product>();
        try{
            prods = jdbcTemplate.query(
               "select i.iid, i.name as item_name, i.description, i.price, i.item_discount, i.istatusid, p.image, p.title, p.width, p.height, s.name as status, s.description "+
               "from item_item as i, item_item_photo as p, item_item_status as s "+
               "where p.iid = i.iid and s.istatusid = i.istatusid "+
               "limit ?, ? ;", new Object[]{pageOffset, pageSize}, (rs, rowNumber) -> {
                   Product p = new Product(String.valueOf(rs.getInt("iid")), rs.getString("item_name"), rs.getString("image"));
                   p.setStatus(rs.getString("status"));
                   return p;
               }
           );
       }
       catch(DataAccessException e)
       {
           throw e;
           //e.printStackTrace();
       }
       return prods;
    }
    
    @GetMapping("/products/{id}")
    public List<Product> getProductDetail(@PathVariable(value = "id") String id)
    {
        System.out.println(id);
        return jdbcTemplate.query(
               "select i.iid, i.name as item_name, i.description, i.price, i.item_discount, i.istatusid, p.image, p.title, p.width, p.height, s.name as status, s.description "+
               "from item_item as i, item_item_photo as p, item_item_status as s "+
               "where p.iid = i.iid and s.istatusid = i.istatusid and i.iid = ?", new Object[]{id}, (rs, rowNumber) -> {
                   Product p = new Product(String.valueOf(rs.getInt("iid")), rs.getString("item_name"), rs.getString("image"));
                   p.setStatus(rs.getString("status"));
                   return p;
               }
           );
    }

}
