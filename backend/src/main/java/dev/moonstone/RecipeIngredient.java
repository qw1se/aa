package dev.moonstone;
import jakarta.persistence.*;
@Entity @Table(name="recipe_ingredients",uniqueConstraints={@UniqueConstraint(columnNames={"recipe_id","ingredient_id"})})
public class RecipeIngredient { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id; @ManyToOne(fetch=FetchType.LAZY,optional=false) public Recipe recipe; @ManyToOne(fetch=FetchType.LAZY,optional=false) public Ingredient ingredient; public int quantity; public RecipeIngredient(){} }
