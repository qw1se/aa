package dev.moonstone;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.*;
@RestController @RequestMapping("/api")
public class GameController {
 private final GameService game;public GameController(GameService game){this.game=game;}
 public record BrewRequest(List<Long> ingredientIds){}
 public record BuyRequest(Long ingredientId,Integer quantity){}
 public record Message(String message){}
 @GetMapping("/state") public GameService.State state(){return game.state();}
 @PostMapping("/brew") public Message brew(@RequestBody BrewRequest request){return new Message(game.brew(request.ingredientIds()));}
 @PostMapping("/shop/collection") public Message collection(){return new Message(game.collection());}
 @PostMapping("/shop/ingredient") public Message buy(@RequestBody BuyRequest r){if(r.ingredientId()==null||r.quantity()==null)throw new GameException("Укажите ingredientId и quantity");return new Message(game.buy(r.ingredientId(),r.quantity()));}
 @PostMapping("/orders/{id}/deliver") public Message deliver(@PathVariable long id){return new Message(game.deliver(id));}
 @PostMapping("/orders") public Message order(){return new Message(game.newOrder());}
 @PostMapping("/gather") public Message gather(){return new Message(game.gather());}
 @ExceptionHandler({GameException.class,HttpMessageNotReadableException.class,ArithmeticException.class})
 public ResponseEntity<Message> error(Exception e){return ResponseEntity.badRequest().body(new Message(e instanceof GameException?e.getMessage():"Некорректный запрос или слишком большое число"));}
}
