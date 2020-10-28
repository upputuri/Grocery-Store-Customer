package com.dolittle.ecom.customer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.dolittle.ecom.customer.bo.Category;
import com.dolittle.ecom.customer.bo.InventorySetVariation;
import com.dolittle.ecom.customer.bo.Product;
import com.dolittle.ecom.customer.bo.ProductsPage;

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
                "select * from category where catsid = (select catsid from category_status where name = 'Active')", (rs, rowNumber) -> {
                    Category c = new Category(String.valueOf(rs.getInt("catid")), rs.getString("name"));
                    c.setImage(rs.getString("image"));
                    c.setName(rs.getString("name"));
                    c.setRank(rs.getInt("category_rank"));
                    c.setMetaDescription(rs.getString("meta_description"));
                    c.setTitle(rs.getString("title"));
                    c.setDescription(rs.getString("description"));
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
        log.info("Retrieved categories, returning result.");
		return result;
    }

    @GetMapping(value = "/products", produces = "application/hal+json")
    public ProductsPage getProducts(@RequestParam(value = "category", required=false, defaultValue = "") String categoryId, 
                                                @RequestParam(value = "keywords", required=false, defaultValue = "") String keywords,
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
        int totalCount = 0;
        try{
            String limit_sql = " limit ?, ? ";
            String score_col_sql = keywords.length() > 0 ? ", match (i.name, i.description, i.keywords) against (? IN BOOLEAN MODE) as score " : "";
            String category_filter_sql = categoryId.length() >0 ? " and c.catid = ? ": "";
            String keyword_match_sql = keywords.length() > 0 ? " and match (i.name, i.description, i.keywords) against (? IN BOOLEAN MODE) ":"";
            String orderby_sql = keywords.length() > 0 ? " order by score desc ":"";
            String products_fetch_sql = "select i.iid, i.name as item_name, i.description, i.price, i.item_discount, variations.price variation_price, variations.mrp as variation_mrp, variations.description as variation_desc,"+
            "i.istatusid, p.imagefiles, p.title, min(variations.name) as variation_name, variations.isvid as variation_id, "+
            "offers.discount as offer_discount, offers.amount as offer_amount "+score_col_sql+
            "from item_item as i left join (select oi.iid, discount, amount from offer_item oi, offer where offer.offid=oi.offid and offer.offsid= "+
            "(select offsid from offer_status where name='Active')) as offers on (i.iid=offers.iid), "+
            "item_item_status as s, item_gi_category as ic, category as c, "+
            "(select insv.isvid, insv.iid, insv.name, insv.description, price, mrp from inventory_set_variations as insv, inventory_set ins where ins.isvid = insv.isvid order by insv.name) as variations, "+
            "(select iid, title, GROUP_CONCAT(image separator ',') as imagefiles from item_item_photo group by iid) as p "+
            "where p.iid = i.iid and s.istatusid = i.istatusid and s.name = 'Active' and ic.giid = i.iid and c.catid = ic.catid and variations.iid = i.iid "+
            category_filter_sql+keyword_match_sql+" group by i.iid "+orderby_sql+limit_sql
            ;

            List<Object> params = new ArrayList<Object>(1);
            if (keywords.length() > 0) {
                params.add(keywords);
            }

            if (categoryId.length() > 0) {
                params.add(categoryId);
            }

            if (keywords.length() > 0) {
                params.add(keywords);
            }

            params.add(pageOffset);
            params.add(pageSize);

            prods = jdbcTemplateObject.query(
               products_fetch_sql, params.toArray(), (rs, rowNumber) -> {
                    Product p = new Product(String.valueOf(rs.getInt("iid")), rs.getString("item_name"), rs.getBigDecimal("price"));
                    BigDecimal offerDiscount = rs.getBigDecimal("offer_discount");
                    BigDecimal variationPrice = rs.getBigDecimal("variation_price");
                    BigDecimal variationPriceAfterDiscount = variationPrice;
                    if (offerDiscount != null) {
                        variationPriceAfterDiscount = variationPrice.subtract(variationPrice.multiply(offerDiscount.divide(new BigDecimal(100))));
                    }
                    p.setDiscount(offerDiscount);
                    p.setCategoryId(categoryId);
                    p.setImages(rs.getString("imagefiles").split(","));
                    p.setInStock(true);
                    p.setDescription(rs.getString("description"));
                    InventorySetVariation variation = new InventorySetVariation(String.valueOf(rs.getInt("variation_id")), 
                                                                rs.getString("variation_name"), variationPrice, rs.getBigDecimal("variation_mrp"));
                    variation.setDescription(rs.getString("variation_desc"));
                    variation.setPriceAfterDiscount(variationPriceAfterDiscount);
                    List<InventorySetVariation> v = new ArrayList<InventorySetVariation>();
                    v.add(variation);
                    p.setVariations(v);
                    Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getProductDetail(String.valueOf(rs.getInt("iid")))).withSelfRel();    
                    p.add(selfLink);               
                    return p;
               }               
            );
            //Removing the limit, offset params
            params.remove(params.size()-1);
            params.remove(params.size()-1);
            String total_count_query = "select count(*) from ("+products_fetch_sql.substring(0, products_fetch_sql.indexOf(limit_sql))+") as products";
            totalCount = jdbcTemplateObject.queryForObject(total_count_query, params.toArray(), Integer.TYPE);

       }
       catch(DataAccessException e)
       {
            log.error("An SQL exception occurred", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred!, pls retry after some time or pls call support");
       }
       ProductsPage page = new ProductsPage();
       page.setOffset(pageOffset);
       page.setSize(prods.size());
       page.setTotalCount(totalCount);
       page.setProducts(prods);
       Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getProducts(categoryId,"", 0, 1000000)).withSelfRel();
       page.add(selfLink);
       return page;
    }
    
    @GetMapping(value = "/products/{id}", produces = "application/hal+json")
    public Product getProductDetail(@PathVariable(value = "id") String productId)
    {
        // #Get product detail
        // select i.iid, i.name as item_name, i.description, i.price, i.item_discount, i.istatusid, p.imagefiles, p.title, s.name as status, s.description 
        // from item_item as i, item_item_status as s, 
        // (select iid, title, GROUP_CONCAT(image separator ',') as imagefiles from item_item_photo where iid=?) as p
        // where p.iid = i.iid and s.istatusid = i.istatusid;        
        // System.out.println(id);
        try{
            return jdbcTemplateObject.queryForObject(
                "select i.iid, i.name as item_name, i.description, i.price, i.item_discount, i.istatusid, p.imagefiles, p.title, "+
                "GROUP_CONCAT(distinct concat_ws('#', v.isvid, v.name,v.price,v.mrp, v.description) SEPARATOR ',') as variations, "+
                "GROUP_CONCAT(distinct concat_ws('#', iag.name, ia.name, iav.value) SEPARATOR ',') as attributes, "+
                "offers.discount as offer_discount, offers.amount as offer_amount "+
                "from item_item as i left join (select oi.iid, discount, amount from offer_item oi, offer where offer.offid=oi.offid and offer.offsid= "+
                "(select offsid from offer_status where name='Active')) as offers on (i.iid=offers.iid), "+
                "item_item_status as s, "+
                "item_gi as igi, item_gi_attribute as igia, item_attribute as ia, item_attribute_group as iag, item_attribute_value as iav, "+
                "(select insv.isvid, insv.iid, insv.name, insv.description, ins.price, ins.mrp from inventory_set_variations insv, inventory_set ins  "+
                "	where ins.isvid = insv.isvid order by insv.name) as v , "+
                "(select iid, title, GROUP_CONCAT(image separator ',') as imagefiles from item_item_photo group by iid) as p "+
                "where s.name = 'Active' and p.iid = i.iid and s.istatusid = i.istatusid and v.iid = i.iid and i.iid=? " +
                "and i.giid = igi.giid and igia.giid = igi.giid and igia.aid = ia.aid and ia.agid = iag.agid and ia.atid = 1 and ia.aid = iav.aid",
                new Object[]{productId}, (rs, rowNumber) -> {
                    Product p = new Product(String.valueOf(rs.getInt("iid")), rs.getString("item_name"), rs.getBigDecimal("price"));
                    p.setDiscount(rs.getBigDecimal("offer_discount"));
                    p.setDescription(rs.getString("description"));
                    p.setImages((rs.getString("imagefiles") != null ) ? rs.getString("imagefiles").split(","): null);
                    p.setInStock(true);

                    BigDecimal offerDiscount = rs.getBigDecimal("offer_discount");
                    //Parse variations
                    String[] v_str = rs.getString("variations") != null ? rs.getString("variations").split(","): new String[0];
                    List<InventorySetVariation> variations = new ArrayList<InventorySetVariation>();
                    Arrays.stream(v_str).forEach((str) -> {
                        String[] parts = str.split("#");
                        BigDecimal variationPrice = new BigDecimal(parts[2]);
                        BigDecimal variationPriceAfterDiscount = variationPrice;
                        if (offerDiscount != null) {
                            variationPriceAfterDiscount = variationPrice.subtract(variationPrice.multiply(offerDiscount.divide(new BigDecimal(100))));
                        }                        
                        InventorySetVariation v = new InventorySetVariation(parts[0], parts[1], variationPrice, new BigDecimal(parts[3]));
                        v.setDescription(parts[4]);
                        v.setPriceAfterDiscount(variationPriceAfterDiscount);
                        variations.add(v);
                    });
                    p.setVariations(variations);

                    //Parse attributes
                    String[] a_str = rs.getString("attributes") != null ? rs.getString("attributes").split(","): new String[0];
                    Map<String, Properties> attributes = new HashMap<String, Properties>();
                    Arrays.stream(a_str).forEach((str) -> {
                        String[] parts = str.split("#");
                        if (attributes.get(parts[0]) == null)
                        {
                            Properties props = new Properties();
                            attributes.put(parts[0], props);
                        }
                        
                        attributes.get(parts[0]).setProperty(parts[1], parts[2]);
                    });
                    p.setAttributes(attributes);
                    
                    Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getProductDetail(String.valueOf(rs.getInt("iid")))).withSelfRel();    
                    p.add(selfLink);               
                    return p;
                }
            );
        }
        catch(DataAccessException e)
        {
            log.error("An SQL exception occurred", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred!, pls retry after some time or pls call support");
        }
            
    }

}
