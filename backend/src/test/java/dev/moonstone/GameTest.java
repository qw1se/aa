package dev.moonstone;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:game;DB_CLOSE_DELAY=-1","spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GameTest {
 @Autowired GameService game;
 @Autowired MockMvc mvc;
 long id(String name){return game.state().ingredients().stream().filter(i->i.name().equals(name)).findFirst().orElseThrow().id();}
 List<Long> heal(){return List.of(id("Лунная трава"),id("Роса"));}
 @Test void brewingUnlocksOnceAndStoresPotions(){game.brew(heal());game.brew(heal());var s=game.state();assertEquals(1,s.book().size());assertEquals(1,s.book().get(0).unlockOrder());assertEquals(2,s.potions().get(0).quantity());}
 @Test void exactMultiplicitiesAndFailedExperiment(){long dew=id("Роса"),light=id("Светлячок");assertTrue(game.brew(List.of(dew,light,dew)).contains("Сон феи"));int before=game.state().ingredients().stream().filter(i->i.id()==dew).findFirst().orElseThrow().quantity();assertTrue(game.brew(List.of(dew,light)).contains("мутная жижа"));assertEquals(before-1,game.state().ingredients().stream().filter(i->i.id()==dew).findFirst().orElseThrow().quantity());assertEquals(1,game.state().book().size());}
 @Test void collectionCostsMoneyAndHasOrderedUniqueDiscoveries(){long before=game.state().moonstones();game.collection();var s=game.state();assertEquals(before-60,s.moonstones());assertTrue(s.book().size()>=2&&s.book().size()<=5);assertEquals(s.book().size(),s.book().stream().map(GameService.RecipeView::id).distinct().count());for(int i=0;i<s.book().size();i++)assertEquals(i+1,s.book().get(i).unlockOrder());}
 @Test void orderConsumesPotionAndPaysOnlyOnce(){game.brew(heal());long order=game.state().orders().get(0).id();long before=game.state().moonstones();game.deliver(order);assertEquals(before+40,game.state().moonstones());assertEquals(0,game.state().potions().get(0).quantity());assertThrows(GameException.class,()->game.deliver(order));assertEquals(before+40,game.state().moonstones());}
 @Test void deliveryEndpointConsumesPotionAndReturnsSuccess() throws Exception {game.brew(heal());long order=game.state().orders().get(0).id();mvc.perform(post("/api/orders/"+order+"/deliver")).andExpect(status().isOk()).andExpect(jsonPath("$.message").value("Заказ выполнен! +40 камней"));assertEquals("COMPLETED",game.state().orders().get(0).status());assertEquals(0,game.state().potions().get(0).quantity());}
 @Test void failedPurchaseAndInvalidBrewDoNotChangeState(){var before=game.state();assertThrows(GameException.class,()->game.buy(id("Роса"),1000));assertEquals(before,game.state());assertThrows(GameException.class,()->game.brew(List.of(id("Роса"),999999L)));assertEquals(before,game.state());}
 @Test void gatherAddsOnlyNonexclusiveIngredients(){var before=game.state();game.gather();var after=game.state();for(int i=0;i<before.ingredients().size();i++){var b=before.ingredients().get(i);assertEquals(b.quantity()+(b.exclusive()?0:1),after.ingredients().get(i).quantity());}}
 @Test void restReturnsStateAndValidationError() throws Exception {mvc.perform(get("/api/state")).andExpect(status().isOk()).andExpect(jsonPath("$.moonstones").value(150));mvc.perform(post("/api/brew").contentType("application/json").content("{\"ingredientIds\":[]}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());}
}
