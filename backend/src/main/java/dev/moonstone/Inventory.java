package dev.moonstone;
import jakarta.persistence.*;
@Entity @Table(name="inventory",uniqueConstraints={@UniqueConstraint(columnNames={"player_id","ingredient_id"})})
public class Inventory { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id; @ManyToOne(fetch=FetchType.LAZY,optional=false) public Player player; @ManyToOne(fetch=FetchType.LAZY,optional=false) public Ingredient ingredient; public int quantity; public Inventory(){} }
