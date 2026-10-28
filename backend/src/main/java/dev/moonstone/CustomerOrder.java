package dev.moonstone;
import jakarta.persistence.*;
@Entity @Table(name="customer_orders")
public class CustomerOrder { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id; @ManyToOne(fetch=FetchType.LAZY,optional=false) public Player player; public String clientName; @ManyToOne(fetch=FetchType.LAZY,optional=false) public Recipe targetRecipe; public long rewardMoonstones; public String status; public CustomerOrder(){} }
