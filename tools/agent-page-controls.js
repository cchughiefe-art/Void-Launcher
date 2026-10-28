(function(request){
 const visible=e=>!!e.getClientRects().length&&getComputedStyle(e).visibility!=='hidden';
 const blocked=()=>/verify you are human|checking your browser|unusual activity|automated requests|account suspended|temporarily restricted|rate limit exceeded|too many requests/i.test(document.body?.innerText||'');
 const sensitive=e=>e.type==='password'||/password|passwd|one.?time|otp|cc-number|credit.?card|cvv|cvc|private.?key|seed.?phrase/i.test([e.autocomplete,e.name,e.id,e.getAttribute('aria-label')].join(' '));
 const sensitivePage=()=>/\/(login|signin|sign-in|auth|oauth|checkout|payment)(\/|$)/i.test(location.pathname)||[...document.querySelectorAll('input[type=password],input[autocomplete=one-time-code]')].some(visible);
 const caption=e=>(e.getAttribute('aria-label')||e.labels?.[0]?.innerText||e.getAttribute('placeholder')||((e.tagName==='BUTTON'||e.tagName==='A'||e.getAttribute('role')==='button')?e.innerText:'')||e.getAttribute('title')||e.name||e.tagName).trim().slice(0,200);
 const contextInfo=e=>{const article=e.closest?.('article');if(article){const time=article.querySelector?.('time');const url=time?.closest?.('a')?.href||'';return {url,text:(article.innerText||'').trim().slice(0,180),key:url||(article.innerText||'').trim().slice(0,180)};}const dialog=e.closest?.('[role=dialog]');return {url:'',text:dialog?(dialog.innerText||'').trim().slice(0,180):'',key:dialog?(dialog.innerText||'').trim().slice(0,180):''};};
 const signature=e=>[e.tagName,e.type||'',e.getAttribute('role')||'',caption(e)].join('|');
 if(blocked())return {blocked:true,error:'Site restriction encountered; session stopped.'};
 if(request.action==='inspect'){
  if(sensitivePage()){delete globalThis.__voidPageControls;return {controls:[],manual:true};}
  const token=Date.now().toString(36)+'-'+Math.random().toString(36).slice(2);const elements=new Map();const controls=[];
  for(const e of document.querySelectorAll('button,a[href],input,textarea,select,[contenteditable=true],[role=button],[role=textbox]')){
   if(controls.length>=120)break;if(!visible(e)||e.disabled||e.getAttribute('aria-disabled')==='true'||e.type==='hidden'||sensitive(e))continue;
   const id='e'+(controls.length+1);const context=contextInfo(e);elements.set(id,{element:e,signature:signature(e),contextKey:context.key});
   const item={id,tag:e.tagName.toLowerCase(),type:e.type||'',role:e.getAttribute('role')||'',label:caption(e),contextText:context.text,contextUrl:context.url};
   if(e.tagName==='A'&&e.href?.startsWith('https://'))item.url=e.href;
   if(e.tagName==='SELECT')item.options=[...e.options].slice(0,50).map(o=>({value:o.value,label:o.text}));
   controls.push(item);
  }
  globalThis.__voidPageControls={token,url:location.href,elements,consumed:false};return {snapshotId:token,pageUrl:location.href,controls};
 }
 const state=globalThis.__voidPageControls;
 if(!state||state.consumed||state.token!==request.snapshotId||state.url!==request.pageUrl||location.href!==request.pageUrl)return {error:'Page controls changed. Capture again; do not retry a publication blindly.'};
 const record=state.elements.get(request.targetId),e=record?.element;
 if(!e||!e.isConnected||!visible(e)||e.disabled||e.getAttribute('aria-disabled')==='true'||signature(e)!==record.signature)return {error:'Target changed. Capture again.'};
 if(contextInfo(e).key!==record.contextKey)return {error:'Target context changed. Capture again.'};
 if(sensitivePage()||sensitive(e))return {error:'Complete credentials, verification codes and sensitive forms manually.'};
 if(request.action==='fill'){
  if(typeof request.text!=='string'||request.text.length>12000)return {error:'Invalid field text'};
  if(e.tagName==='SELECT'){if(![...e.options].some(o=>o.value===request.text))return {error:'Option not present'};e.value=request.text;}
  else if(e.isContentEditable){e.focus();const range=document.createRange();range.selectNodeContents(e);const selection=getSelection();selection.removeAllRanges();selection.addRange(range);if(!document.execCommand('insertText',false,request.text))e.textContent=request.text;}
  else if(e.tagName==='TEXTAREA'||(e.tagName==='INPUT'&&['text','search','email','url','tel','number','date','datetime-local','time'].includes(e.type))){const setter=Object.getOwnPropertyDescriptor(Object.getPrototypeOf(e),'value')?.set;if(!setter)return {error:'Field is not editable'};setter.call(e,request.text);}
  else return {error:'Target is not an editable field'};
  e.dispatchEvent(new Event('input',{bubbles:true}));e.dispatchEvent(new Event('change',{bubbles:true}));state.consumed=true;
  return {status:'field_updated',characters:request.text.length,outcome:'verify_before_submitting'};
 }
 if(request.action==='click'||request.action==='submit'){
  if(request.action==='submit'&&!(e.tagName==='BUTTON'||e.type==='submit'||e.getAttribute('role')==='button'))return {error:'Select the observed submit/publish button'};
  state.consumed=true;setTimeout(()=>e.click(),0);return {status:'action_dispatched',outcome:'needs_verification',pageUrl:location.href,targetLabel:caption(e)};
 }
 return {error:'Unsupported page control action'};
})
