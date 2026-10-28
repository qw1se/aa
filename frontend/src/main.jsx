import React, {useEffect, useState} from 'react';
import {createRoot} from 'react-dom/client';
import './style.css';
async function api(path, body) {
 const options=body===undefined?{}:{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)};
 const res=await fetch('/api'+path,options);
 const data=await res.json().catch(()=>({message:'Сервер вернул неожиданный ответ'}));
 if(!res.ok) throw new Error(data.message||'Ошибка сервера'); return data;
}
function App(){
 const [state,setState]=useState(null),[selected,setSelected]=useState({}),[tab,setTab]=useState('lab');
 const [message,setMessage]=useState('Добро пожаловать! Лунная трава + Роса = исцеление.');
 const [busy,setBusy]=useState(false),[failed,setFailed]=useState(false);
 const refresh=async()=>setState(await api('/state'));
 useEffect(()=>{refresh().catch(e=>{setFailed(true);setMessage('Не удалось подключиться к бэкенду: '+e.message);});},[]);
 async function act(path,body={},clear=false){
  setBusy(true);setFailed(false);
  try{const result=await api(path,body);if(clear)setSelected({});setMessage(result.message);await refresh();}
  catch(e){setFailed(true);setMessage(e.message);}finally{setBusy(false);}
 }
 function change(i,delta){setSelected(old=>({...old,[i.id]:Math.max(0,Math.min(i.quantity,(old[i.id]||0)+delta))}));}
 const total=Object.values(selected).reduce((a,b)=>a+b,0);
 const brew=()=>act('/brew',{ingredientIds:Object.entries(selected).flatMap(([id,q])=>Array(q).fill(Number(id)))},true);
 return <div className="app">
  <header><div><span className="eyebrow">АЛХИМИЧЕСКАЯ МАСТЕРСКАЯ</span><h1>☽ Лунная алхимия</h1><p>Экспериментируй. Открывай. Превращай тайны в зелья.</p></div><div className="wallet">✦ {state?.moonstones??'…'}<small>лунных камней</small></div></header>
  <div className={'notice '+(failed?'error':'')} role="status">{message}</div>
  {!state?<button disabled={busy} onClick={async()=>{setBusy(true);try{await refresh();setFailed(false);setMessage('Подключено');}catch(e){setMessage(e.message);}finally{setBusy(false);}}}>Повторить подключение</button>:<>
   <nav>{[['lab','⚗ Лаборатория'],['book','✧ Книга открытий'],['shop','◇ Магазин'],['orders','✉ Заказы']].map(([key,label])=><button key={key} className={tab===key?'active':''} onClick={()=>setTab(key)}>{label}</button>)}</nav>
   {tab==='lab'&&<div className="columns"><section><h2>Кладовая</h2><p>Добавляй ингредиенты в котел кнопкой +. Повторения важны.</p><div className="grid">{state.ingredients.map(i=><article key={i.id}><span className={'badge '+i.rarity}>{i.exclusive?'Эксклюзив':i.rarity==='RARE'?'Редкий':'Обычный'}</span><h3>{i.name}</h3><p>В наличии: {i.quantity}</p><div className="counter"><button disabled={busy||!selected[i.id]} onClick={()=>change(i,-1)}>−</button><span>{selected[i.id]||0}</span><button disabled={busy||(selected[i.id]||0)>=i.quantity||total>=100} onClick={()=>change(i,1)}>+</button></div></article>)}</div></section><aside><div className="cauldron">⚗</div><h2>Алхимический котел</h2>{state.ingredients.filter(i=>selected[i.id]).map(i=><p key={i.id}>{i.name} × {selected[i.id]}</p>)}{!total&&<p>Выбери ингредиенты слева</p>}<button className="primary" disabled={busy||!total} onClick={brew}>Сварить зелье ({total})</button><button disabled={busy||!total} onClick={()=>setSelected({})}>Очистить</button><hr/><h3>Готовые зелья</h3>{state.potions.filter(p=>p.quantity>0).map(p=><p key={p.recipeId}>{p.name} × {p.quantity}</p>)}<button disabled={busy} onClick={()=>act('/gather')}>Собрать базовые ингредиенты</button><small>Один сбор — один игровой ход. +1 каждого неэксклюзивного ингредиента.</small></aside></div>}
   {tab==='book'&&<section><h2>Книга открытий <span className="muted">{state.book.length}</span></h2><p>Записи остаются в порядке открытия. Рецепты из коллекций тоже видны здесь.</p>{!state.book.length&&<p>Книга пуста. Попробуй смешать Лунную траву и Росу.</p>}<div className="grid">{state.book.map(r=><article key={r.id}><span className="badge">№ {r.unlockOrder} · {r.source==='BREWING'?'Эксперимент':'Коллекция'}</span><h3>{r.name}</h3>{r.ingredients.map(i=><p key={i.id}>{i.name} × {i.quantity}</p>)}<button disabled={busy} onClick={()=>{setSelected(Object.fromEntries(r.ingredients.map(i=>[i.id,Math.min(i.quantity,state.ingredients.find(x=>x.id===i.id)?.quantity||0)])));setTab('lab');setMessage('Состав перенесен в котел в пределах запасов. Проверь количества перед варкой.');}}>Подготовить состав</button></article>)}</div></section>}
   {tab==='shop'&&<section><h2>Лавка редкостей</h2><article className="collection"><div><span className="badge">Древний манускрипт</span><h3>Коллекция секретных рецептов</h3><p>2–5 неизвестных формул. Дубликатов нет. Если осталось меньше двух — покупка отменяется.</p></div><button disabled={busy||state.moonstones<state.collectionPrice} className="primary" onClick={()=>act('/shop/collection')}>Купить · {state.collectionPrice} ✦</button></article><div className="grid">{state.ingredients.map(i=><article key={i.id}><span className="badge">{i.exclusive?'Только в магазине':'Ингредиент'}</span><h3>{i.name}</h3><p>В запасе: {i.quantity}</p><button disabled={busy||state.moonstones<i.price} onClick={()=>act('/shop/ingredient',{ingredientId:i.id,quantity:1})}>Купить 1 · {i.price} ✦</button></article>)}</div></section>}
   {tab==='orders'&&<section><div className="section-title"><h2>Клиенты мастерской</h2><button disabled={busy} onClick={()=>act('/orders')}>Пригласить клиента</button></div><div className="grid">{state.orders.map(o=><article key={o.id}><span className="badge">{o.status==='OPEN'?'Активный заказ':'Выполнен'}</span><h3>{o.clientName}</h3><p>Нужно: {o.potion}</p><p className="reward">Награда: {o.reward} ✦</p><button disabled={busy||o.status!=='OPEN'||!state.potions.some(p=>p.recipeId===o.recipeId&&p.quantity>0)} onClick={()=>act('/orders/'+o.id+'/deliver')}>{o.status==='OPEN'?'Отдать зелье':'Выполнен ✓'}</button></article>)}</div></section>}
  </>}
 </div>;
}
createRoot(document.getElementById('root')).render(<App/>);
