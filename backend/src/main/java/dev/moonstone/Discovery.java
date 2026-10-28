package dev.moonstone;
import jakarta.persistence.*;
@Entity @Table(name="discoveries",uniqueConstraints={@UniqueConstraint(columnNames={"player_id","recipe_id"}),@UniqueConstraint(columnNames={"player_id","unlock_order"})})
public class Discovery { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id; @ManyToOne(fetch=FetchType.LAZY,optional=false) public Player player; @ManyToOne(fetch=FetchType.LAZY,optional=false) public Recipe recipe; @Column(name="unlock_order",nullable=false) public long unlockOrder; public String source; public java.time.Instant discoveredAt; public Discovery(){} }
