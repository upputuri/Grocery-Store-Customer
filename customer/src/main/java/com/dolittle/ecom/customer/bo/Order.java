package com.dolittle.ecom.customer.bo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.hateoas.RepresentationModel;

import lombok.Data;

public @Data class Order extends RepresentationModel<Order>{
    @JsonProperty(access = Access.READ_ONLY)   
    private String id;
    private String customerId;
    private String taxProfileId;
    private String taxType;
    private List<OrderItem> orderItems = new ArrayList<OrderItem>();
    private String shippingAddressId;
    private ShippingAddress shippingAddress;
    @JsonProperty(access = Access.READ_ONLY)    
    private String status = "preparing";
    @JsonProperty(access = Access.READ_ONLY)    
    private Transaction transaction;
    @JsonProperty(access = Access.READ_ONLY)    
    private Invoice invoice;
    @JsonProperty(access = Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal orderTotal = BigDecimal.ZERO;
    @JsonProperty(access = Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal discountedTotal = BigDecimal.ZERO;
    @JsonProperty(access = Access.READ_ONLY)
    private Map<String, BigDecimal> taxes = new HashMap<String, BigDecimal>();
    @JsonProperty(access = Access.READ_ONLY)    
    private Map<String, BigDecimal> discounts = new HashMap<String, BigDecimal>();
    @JsonProperty(access = Access.READ_ONLY)    
    private Map<String, BigDecimal> charges = new HashMap<String, BigDecimal>();
    @JsonProperty(access = Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING)    
    private BigDecimal totalTaxRate = BigDecimal.ZERO;
    @JsonProperty(access = Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING)    
    private BigDecimal totalTaxValue = BigDecimal.ZERO;
    @JsonProperty(access = Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING)        
    private BigDecimal totalDiscountValue = BigDecimal.ZERO;
    @JsonProperty(access = Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING)    
    private BigDecimal totalChargesValue = BigDecimal.ZERO;
    @JsonProperty(access = Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING)    
    private BigDecimal finalTotal = BigDecimal.ZERO;

    public Order()
    {
        super();
    }

    public void addOrderItem(OrderItem item)
    {
        if (item == null)
            throw new NullPointerException("Attempt to add null OrderItem to Order");
        this.orderItems.add(item);
        this.orderTotal = this.orderTotal.add(item.getTotalPriceAfterDiscount());
        this.computeTotals();
    }

    public void addTax(String name, BigDecimal taxRate)
    {
        taxes.put(name, taxRate);
        computeTotals();
    }

    public void addDiscount(String name, BigDecimal discountRate)
    {
        discounts.put(name, discountRate);
        computeTotals();
    }

    public void addCharge(String name, BigDecimal chargeRate)
    {
        charges.put(name, chargeRate);
        computeTotals();
    }

    private void computeTotals()
    {
        double totalDiscountRate = 0.0;
        for (Map.Entry<String, BigDecimal> entry: this.discounts.entrySet())
        {
            totalDiscountRate += entry.getValue().doubleValue();
        }

        this.totalDiscountValue = this.orderTotal.multiply(new BigDecimal(totalDiscountRate/100));
        this.totalDiscountValue = this.totalDiscountValue.setScale(2, RoundingMode.HALF_EVEN);
        this.discountedTotal = this.orderTotal.subtract(this.totalDiscountValue).setScale(2, RoundingMode.HALF_EVEN);

        double totalChargesRate = 0.0;
        for (Map.Entry<String, BigDecimal> entry: this.charges.entrySet())
        {
            totalChargesRate += entry.getValue().doubleValue();
        }

        this.totalChargesValue = this.discountedTotal.multiply(new BigDecimal(totalChargesRate/100));
        this.totalChargesValue = this.totalChargesValue.setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal grossTotalAfterCharges = this.discountedTotal.add(this.totalChargesValue).setScale(2, RoundingMode.HALF_EVEN);

        double totalTaxRate = 0.0;
        for (Map.Entry<String, BigDecimal> entry: this.taxes.entrySet())
        {
            totalTaxRate += entry.getValue().doubleValue();
        }
        this.totalTaxRate = new BigDecimal(totalTaxRate).setScale(2, RoundingMode.HALF_EVEN);
        this.totalTaxValue = grossTotalAfterCharges.multiply(new BigDecimal(totalTaxRate/100).setScale(2, RoundingMode.HALF_EVEN)).setScale(2, RoundingMode.HALF_EVEN);
        this.finalTotal = grossTotalAfterCharges.add(totalTaxValue).setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal getTotalTaxValue(BigDecimal inputValue)
    {
        computeTotals();
        return this.totalTaxValue;
    }

    public BigDecimal getTotalDiscountValue(BigDecimal inputValue)
    {
        computeTotals();
        return this.totalDiscountValue;
    }

    public BigDecimal getTotalChargesValue(BigDecimal inputValue)
    {
        computeTotals();
        return this.totalChargesValue;
    }

    public BigDecimal getFinalTotal()
    {
        computeTotals();
        return this.finalTotal;
    }

    public static void main(String st[]) throws Exception
    {
        Order order = new Order();
        ShippingAddress sa = new ShippingAddress("line one ", "Hyderabad", "500019", "9877776677");
        order.setShippingAddress(sa);
        //Creating the ObjectMapper object
      ObjectMapper mapper = new ObjectMapper();
      //Converting the Object to JSONString
      String jsonString = mapper.writeValueAsString(order);
      System.out.println(jsonString);
    }
}
