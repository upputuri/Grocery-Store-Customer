package com.dolittle.ecom.customer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.dolittle.ecom.app.util.CustomerRunnerUtil;
import com.dolittle.ecom.customer.bo.Cart;
import com.dolittle.ecom.customer.bo.CartItem;
import com.dolittle.ecom.customer.bo.Customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class CustomerCartService
{
    @Autowired
    JdbcTemplate jdbcTemplateObject;

    @Transactional
    @GetMapping(value="/customers/{id}/cart/items", produces="application/hal+json")
    public CollectionModel<CartItem> getCartItems(@PathVariable(value="id", required = true) String customerId, Authentication auth)
    {
        log.info("Processing request to get cart items of customer Id: "+customerId);
        Customer customer = CustomerRunnerUtil.validateAndGetAuthCustomer(auth, customerId);
        try{
            String sql = "select c.cartid, ci.cartiid, ci.iid, ci.isvid, ci.cartisid, ci.quantity, ii.name as item_name, iip.image, ii.price as item_price, ii.item_discount, ins.price as variant_price, insv.name as variant_name, "+
            "offers.discount as offer_discount, offers.amount as offer_amount "+
            "from cart as c, cart_item as ci, cart_item_status as cis, item_item as ii left join (select oi.iid, discount, amount from offer_item oi, offer where offer.offid=oi.offid and offer.offsid= "+
            "(select offsid from offer_status where name='Active')) as offers on (ii.iid=offers.iid), "+
            "inventory_set as ins, inventory_set_variations as insv, "+
            "(select iid, title, image from item_item_photo group by iid) as iip "+
            "where c.cuid = ? and c.cartid = ci.cartid and iip.iid = ii.iid and ci.iid = ii.iid and ci.cartisid = cis.cartisid and ins.isvid = ci.isvid and insv.isvid = ci.isvid "+
            "and cis.name = 'Active'";

            List<CartItem> cartItems = jdbcTemplateObject.query( sql, new Object[]{customer.getId()} , (rs, rowNumber) -> {
                CartItem ci = new CartItem(String.valueOf(rs.getInt("iid")), String.valueOf(rs.getInt("isvid")));
                    BigDecimal offerDiscount = rs.getBigDecimal("offer_discount");
                    BigDecimal variationPrice = rs.getBigDecimal("variant_price");
                    BigDecimal variationPriceAfterDiscount = variationPrice;
                    if (offerDiscount != null) {
                        variationPriceAfterDiscount = variationPrice.subtract(variationPrice.multiply(offerDiscount.divide(new BigDecimal(100))));
                    }
                    ci.setCartItemId(String.valueOf(rs.getInt("cartiid")));
                    ci.setDiscount(offerDiscount);
                    ci.setUnitLabel(rs.getString("variant_name"));
                    ci.setProductName(rs.getString("item_name"));
                    ci.setImage(rs.getString("image"));
                    int qty = rs.getInt("quantity");
                    ci.setQty(qty);
                    //BigDecimal item_price = rs.getBigDecimal("item_price");
                    // BigDecimal variant_price = rs.getBigDecimal("variant_price");
                    //BigDecimal chosen_price = item_price.compareTo(BigDecimal.ZERO)>0 ? item_price : variant_price;
                    //BigDecimal discount = rs.getBigDecimal("item_discount");
                    
                    //BigDecimal final_price = variant_price.multiply(BigDecimal.ONE.subtract(discount)).setScale(2, RoundingMode.HALF_EVEN);
                    
                    ci.setUnitPrice(variationPrice);
                    ci.setUnitPriceAfterDiscount(variationPriceAfterDiscount);
                    ci.setTotalPrice(variationPrice.multiply(new BigDecimal(qty)));
                    ci.setTotalPriceAfterDiscount(variationPriceAfterDiscount.multiply(new BigDecimal(qty)));
                    Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getCartItem(customerId, rs.getString("cartiid"))).withSelfRel();
                    ci.add(selfLink);
                    //BigDecimal price = final_price.             
                    return ci;
            }
            );
            Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getCartItems(customerId, null)).withSelfRel();
            return CollectionModel.of(cartItems, selfLink);
        }
        catch(DataAccessException e)
        {
            log.error("An SQL exception occurred", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred!, pls retry after some time or pls call support");
        }        
    }
    
    @GetMapping(value="/customers/{id}/cart", produces="application/hal+json")
    public Cart getCart(@PathVariable(value="id", required = true) String customerId, Authentication auth)
    {
        log.info("Processing request to get cart");
        Customer customer = CustomerRunnerUtil.validateAndGetAuthCustomer(auth, customerId);
        Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getCart(customer.getId(), null)).withSelfRel();
        Cart cart = new Cart();
        cart.setCartItems(new ArrayList<CartItem>(getCartItems(customerId, auth).getContent()));
        cart.add(selfLink);
        return cart;
    }
    
    //This can very well be made a void function. Returning a cartitem may not be necessary.
    @Transactional
    @PostMapping(value = "/customers/{customerId}/cart" , produces = "application/hal+json")
    public CartItem addOrUpdateItemToCart(@PathVariable(value="customerId", required = true) String customerId, @RequestBody CartItem cartItem, Authentication auth)
    {
        // If user doesn't have a cart yet, create a cart?
        // Receives - productId, is_variation, qty
        log.info("Processing request to add/update item to cart for customer Id: "+customerId);
        CustomerRunnerUtil.validateAndGetAuthCustomer(auth, customerId);
        try{
            String check_cartitem_exists_sql = "select cartiid from cart_item where cartid = (select cartid from cart where cuid = ?) and iid= ? and isvid = ? "+
                                                "and cartisid=(select cartisid from cart_item_status where name='Active') limit 0,1";

            try{
                Integer cartiid = jdbcTemplateObject.queryForObject(check_cartitem_exists_sql, 
                            new Object[]{customerId, cartItem.getProductId(), cartItem.getVariationId()}, Integer.TYPE);
                //Update cart item
                String update_cart_item_sql = "update cart_item set cartisid = CASE WHEN quantity+?<=0 THEN (select cartisid from cart_item_status where name='Delete') "+
                                                "else cartisid END, quantity= quantity+?, updated_ts= current_timestamp where cartiid=?";
                int affected_row_count = jdbcTemplateObject.update(update_cart_item_sql, cartItem.getQty(), cartItem.getQty(), cartiid);
                if (affected_row_count < 1)
                {
                    //TODO: Exception. Internal server error.
                }
                cartItem.setCartItemId(cartiid.toString());
                return cartItem;                            
            }
            catch(EmptyResultDataAccessException e)
            {
                //add new cart item
                if (cartItem.getQty() > 0)
                {
                    String add_cart_item_sql = "insert into cart_item (cartid, iid, isvid, quantity, cartisid) values ((select cartid from cart where cuid = ?), ?, ?, ?, (select cartisid from cart_item_status where name = 'Active'))";
                    KeyHolder keyHolder = new GeneratedKeyHolder();
                    int affected_row_count = jdbcTemplateObject.update(connection -> {
                        PreparedStatement ps = connection.prepareStatement(add_cart_item_sql, Statement.RETURN_GENERATED_KEYS);
                        ps.setInt(1, Integer.parseInt(customerId));
                        ps.setInt(2, Integer.parseInt(cartItem.getProductId()));
                        ps.setInt(3, Integer.parseInt(cartItem.getVariationId()));
                        ps.setInt(4, cartItem.getQty());
                        return ps;
                    }, keyHolder);


                    if (affected_row_count < 1)
                    {
                        //TODO: Exception. Internal server error.
                    }
                    cartItem.setCartItemId(keyHolder.getKey().toString());
                }
                return cartItem;
            }

        }
        catch(DataAccessException e)
        {
            log.error("An SQL exception occurred", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred!, pls retry after some time or pls call support");
        }
    }

    // @PutMapping("/customers/{customerId}/cart/items/{cartItemId}")
    // public void updateCartItem(@PathVariable(value="id", required = true) String customerId)
    // {
      
    // }

    @GetMapping(value = "/customers/{customerId}/cart/{cartItemId}", produces = "application/hal+json")
    public CartItem getCartItem(@PathVariable(value="customerId", required = true) String customerId, 
                                @PathVariable(value="cartItemId", required = true) String cartItemId)
    {
        return null;
    }

    public static void main(String st[])
    {
        BigDecimal b = new BigDecimal("34.556");
        b = b.setScale(1, RoundingMode.HALF_EVEN);
        System.out.println(b);
    }

}