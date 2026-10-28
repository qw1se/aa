package dev.moonstone;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
@SpringBootApplication
public class Application {
 public static void main(String[] args){SpringApplication.run(Application.class,args);}
 @Bean CommandLineRunner seed(GameService game){return args->game.initialize();}
}
