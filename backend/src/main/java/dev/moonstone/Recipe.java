package dev.moonstone;
import jakarta.persistence.*;
@Entity @Table(name="recipes")
public class Recipe { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id; @Column(nullable=false,unique=true) public String name; public String description; public boolean secret; @Column(nullable=false,unique=true,length=2000) public String signature; @OneToMany(mappedBy="recipe",cascade=CascadeType.ALL,orphanRemoval=true) public java.util.List<RecipeIngredient> ingredients=new java.util.ArrayList<>(); public Recipe(){} }
