package dev.moonstone;
import jakarta.persistence.*;
@Entity @Table(name="ingredients")
public class Ingredient { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id; @Column(nullable=false,unique=true) public String name; public String rarity; public long basePrice; public boolean exclusive; public Ingredient(){} }
