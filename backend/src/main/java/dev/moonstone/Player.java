package dev.moonstone;
import jakarta.persistence.*;
@Entity @Table(name="players")
public class Player { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id; @Column(unique=true,nullable=false) public String name; public long moonstones; public long nextUnlockOrder=1; @Version public long version; public Player(){} }
