package dev.moonstone;
import jakarta.persistence.*;
@Entity @Table(name="potion_stock",uniqueConstraints={@UniqueConstraint(columnNames={"player_id","recipe_id"})})
public class PotionStock { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id; @ManyToOne(fetch=FetchType.LAZY,optional=false) public Player player; @ManyToOne(fetch=FetchType.LAZY,optional=false) public Recipe recipe; public int quantity; public PotionStock(){} }
