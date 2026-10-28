package dev.moonstone;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;
import java.time.Instant;
@Service @Transactional
public class GameService {
 public static final long COLLECTION_PRICE=60;
 private final EntityManager em;
 public GameService(EntityManager em){this.em=em;}
 private Player player(){return em.createQuery("select p from Player p where p.name=:n",Player.class).setParameter("n","Алхимик").getSingleResult();}
 private Player lock(){Player p=player(); em.lock(p,LockModeType.PESSIMISTIC_WRITE); em.refresh(p); return p;}
 public static String signature(Map<Long,Integer> m){return m.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e->e.getKey()+":"+e.getValue()).collect(Collectors.joining(";"));}
 private List<Inventory> inventory(Player p){return em.createQuery("select i from Inventory i join fetch i.ingredient where i.player=:p order by i.ingredient.id",Inventory.class).setParameter("p",p).getResultList();}
 private List<PotionStock> potions(Player p){return em.createQuery("select s from PotionStock s join fetch s.recipe where s.player=:p order by s.recipe.id",PotionStock.class).setParameter("p",p).getResultList();}
 private List<Discovery> discoveries(Player p){return em.createQuery("select d from Discovery d join fetch d.recipe where d.player=:p order by d.unlockOrder",Discovery.class).setParameter("p",p).getResultList();}
 private boolean discover(Player p,Recipe recipe,String source){
  if(discoveries(p).stream().anyMatch(d->d.recipe.id.equals(recipe.id)))return false;
  Discovery d=new Discovery(); d.player=p; d.recipe=recipe; d.unlockOrder=p.nextUnlockOrder++; d.source=source; d.discoveredAt=Instant.now(); em.persist(d);return true;
 }
 public String brew(List<Long> ids){
  if(ids==null||ids.isEmpty()||ids.size()>100||ids.stream().anyMatch(Objects::isNull))throw new GameException("Выберите от 1 до 100 ингредиентов");
  Player p=lock(); Map<Long,Integer> needed=new TreeMap<>();ids.forEach(id->needed.merge(id,1,Integer::sum));
  Map<Long,Inventory> stock=inventory(p).stream().collect(Collectors.toMap(i->i.ingredient.id,i->i));
  for(var e:needed.entrySet())if(!stock.containsKey(e.getKey())||stock.get(e.getKey()).quantity<e.getValue())throw new GameException("Недостаточно ингредиентов");
  needed.forEach((id,q)->stock.get(id).quantity-=q);
  List<Recipe> found=em.createQuery("select r from Recipe r where r.signature=:s",Recipe.class).setParameter("s",signature(needed)).getResultList();
  if(found.isEmpty())return "Получилась мутная жижа. Ингредиенты потрачены.";
  Recipe recipe=found.get(0);PotionStock s=potions(p).stream().filter(x->x.recipe.id.equals(recipe.id)).findFirst().orElse(null);
  if(s==null){s=new PotionStock();s.player=p;s.recipe=recipe;em.persist(s);}s.quantity=Math.addExact(s.quantity,1);
  return "Сварено: "+recipe.name+(discover(p,recipe,"BREWING")?". Новый рецепт в книге!":".");
 }
 public String collection(){
  Player p=lock(); if(p.moonstones<COLLECTION_PRICE)throw new GameException("Нужно 60 лунных камней");
  List<Recipe> available=em.createQuery("select r from Recipe r where r.secret=true and not exists(select d.id from Discovery d where d.player=:p and d.recipe=r) order by r.id",Recipe.class).setParameter("p",p).getResultList();
  if(available.size()<2)throw new GameException("Осталось меньше двух секретных рецептов. Камни не списаны.");
  Collections.shuffle(available);int n=2+new Random().nextInt(Math.min(5,available.size())-1);p.moonstones-=COLLECTION_PRICE;
  List<String> names=new ArrayList<>();for(Recipe recipe:available.subList(0,n)){discover(p,recipe,"COLLECTION");names.add(recipe.name);}return "Открыты: "+String.join(", ",names);
 }
 public String buy(long ingredientId,int quantity){
  if(quantity<1||quantity>1000)throw new GameException("Количество должно быть от 1 до 1000");
  Player p=lock();Ingredient i=em.find(Ingredient.class,ingredientId);if(i==null)throw new GameException("Ингредиент не найден");
  long cost=Math.multiplyExact(i.basePrice,(long)quantity);if(p.moonstones<cost)throw new GameException("Недостаточно лунных камней");
  Inventory row=inventory(p).stream().filter(x->x.ingredient.id.equals(i.id)).findFirst().orElse(null);
  if(row==null){row=new Inventory();row.player=p;row.ingredient=i;em.persist(row);}row.quantity=Math.addExact(row.quantity,quantity);p.moonstones-=cost;return "Куплено: "+i.name+" × "+quantity;
 }
 public String deliver(long id){
  Player p=lock();CustomerOrder o=em.find(CustomerOrder.class,id);
  if(o==null||!o.player.id.equals(p.id))throw new GameException("Заказ не найден");
  if(!"OPEN".equals(o.status))throw new GameException("Заказ уже выполнен");
  PotionStock s=em.createQuery("select s from PotionStock s where s.player=:p and s.recipe=:recipe and s.quantity>0",PotionStock.class)
   .setParameter("p",p).setParameter("recipe",o.targetRecipe).setMaxResults(1).getResultList().stream()
   .findFirst().orElseThrow(()->new GameException("Сначала сварите нужное зелье"));
  s.quantity--;o.status="COMPLETED";p.moonstones=Math.addExact(p.moonstones,o.rewardMoonstones);return "Заказ выполнен! +"+o.rewardMoonstones+" камней";
 }
 public String newOrder(){
  Player p=lock();long count=em.createQuery("select count(o) from CustomerOrder o where o.player=:p and o.status='OPEN'",Long.class).setParameter("p",p).getSingleResult();
  if(count>=5)throw new GameException("Уже есть 5 активных заказов");
  List<Recipe> all=em.createQuery("select r from Recipe r order by r.id",Recipe.class).getResultList();Recipe recipe=all.get(new Random().nextInt(all.size()));
  long cost=recipe.ingredients.stream().mapToLong(i->i.quantity*i.ingredient.basePrice).sum();order(p,recipe,"Таинственный путник",cost+35);return "Пришел новый клиент";
 }
 // Free gathering prevents an economic dead end; every call is one game turn.
 public String gather(){Player p=lock();inventory(p).stream().filter(i->!i.ingredient.exclusive).forEach(i->i.quantity=Math.addExact(i.quantity,1));return "Сбор завершен: +1 каждого обычного ингредиента";}
 public record IngredientView(long id,String name,String rarity,long price,boolean exclusive,int quantity){}
 public record ComponentView(long id,String name,int quantity){}
 public record RecipeView(long id,String name,String description,long unlockOrder,String source,List<ComponentView> ingredients){}
 public record PotionView(long recipeId,String name,int quantity){}
 public record OrderView(long id,String clientName,long recipeId,String potion,long reward,String status){}
 public record State(long moonstones,long collectionPrice,List<IngredientView> ingredients,List<RecipeView> book,List<PotionView> potions,List<OrderView> orders){}
 @Transactional(readOnly=true)
 public State state(){
  Player p=player();var inv=inventory(p);var book=discoveries(p);
  var orders=em.createQuery("select o from CustomerOrder o join fetch o.targetRecipe where o.player=:p order by o.id",CustomerOrder.class).setParameter("p",p).getResultList();
  return new State(p.moonstones,COLLECTION_PRICE,
   inv.stream().map(i->new IngredientView(i.ingredient.id,i.ingredient.name,i.ingredient.rarity,i.ingredient.basePrice,i.ingredient.exclusive,i.quantity)).toList(),
   book.stream().map(d->new RecipeView(d.recipe.id,d.recipe.name,d.recipe.description,d.unlockOrder,d.source,d.recipe.ingredients.stream().sorted(Comparator.comparing(i->i.ingredient.id)).map(i->new ComponentView(i.ingredient.id,i.ingredient.name,i.quantity)).toList())).toList(),
   potions(p).stream().map(s->new PotionView(s.recipe.id,s.recipe.name,s.quantity)).toList(),
   orders.stream().map(o->new OrderView(o.id,o.clientName,o.targetRecipe.id,o.targetRecipe.name,o.rewardMoonstones,o.status)).toList());
 }
 private void order(Player p,Recipe r,String client,long reward){CustomerOrder o=new CustomerOrder();o.player=p;o.targetRecipe=r;o.clientName=client;o.rewardMoonstones=reward;o.status="OPEN";em.persist(o);}
 public void initialize(){
  if(em.createQuery("select count(p) from Player p",Long.class).getSingleResult()>0)return;
  Player p=new Player();p.name="Алхимик";p.moonstones=150;em.persist(p);
  String[] names={"Лунная трава","Роса","Красный гриб","Светлячок","Пепел феникса","Звездная пыль"};long[] prices={3,2,5,8,18,25};List<Ingredient> items=new ArrayList<>();
  for(int k=0;k<names.length;k++){Ingredient i=new Ingredient();i.name=names[k];i.basePrice=prices[k];i.exclusive=k>=4;i.rarity=k>=4?"LEGENDARY":k>=2?"RARE":"COMMON";em.persist(i);items.add(i);Inventory s=new Inventory();s.player=p;s.ingredient=i;s.quantity=k>=4?2:12;em.persist(s);}
  String[] namesR={"Зелье исцеления","Ночное зрение","Эликсир бодрости","Невидимость","Огненная кожа","Звездная мудрость","Сон феи","Лунный щит"};int[][] mix={{0,1},{0,3},{1,2},{3,5},{2,4},{0,4,5},{1,1,3},{0,0,5}};
  for(int k=0;k<namesR.length;k++){Recipe recipe=new Recipe();recipe.name=namesR[k];recipe.description="Древняя алхимическая формула";recipe.secret=k>0;Map<Long,Integer> m=new TreeMap<>();for(int index:mix[k])m.merge(items.get(index).id,1,Integer::sum);recipe.signature=signature(m);
   for(var e:m.entrySet()){RecipeIngredient c=new RecipeIngredient();c.recipe=recipe;c.ingredient=items.stream().filter(i->i.id.equals(e.getKey())).findFirst().orElseThrow();c.quantity=e.getValue();recipe.ingredients.add(c);}em.persist(recipe);if(k==0)order(p,recipe,"Травница Мира",40);
  }
 }
}
