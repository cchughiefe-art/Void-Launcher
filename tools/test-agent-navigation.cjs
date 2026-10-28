const {test}=require('node:test');
const assert=require('node:assert/strict');
const vm=require('node:vm');
const fs=require('node:fs');
const scroll=fs.readFileSync('tools/agent-scroll.js','utf8');
const state=fs.readFileSync('tools/agent-page-state.js','utf8');
function element({height=600,total=2400,top=0,overflow='auto',main=false,hidden=false}={}){
  return {tagName:'DIV',clientWidth:360,clientHeight:height,scrollHeight:total,scrollTop:top,parentElement:null,
    style:{visibility:hidden?'hidden':'visible',display:'block',overflowY:overflow},
    getBoundingClientRect:()=>({top:0,left:0,right:360,bottom:height}),
    closest:selector=>selector.includes('main')&&main?{}:null,getAttribute:()=>null};
}
function move(root,others=[],center=root,direction='down'){
  return vm.runInNewContext(scroll,{document:{scrollingElement:root,documentElement:root,elementFromPoint:()=>center,querySelectorAll:()=>others},window:{innerWidth:360,innerHeight:640},getComputedStyle:e=>e.style})(direction);
}
test('scrolls a nested timeline when the document cannot scroll',()=>{
  const root=element({height:640,total:640});const timeline=element({main:true});timeline.parentElement=root;
  const result=move(root,[timeline],timeline);assert.equal(result.moved,true);assert.equal(timeline.scrollTop,480);assert.equal(root.scrollTop,0);
});
test('uses document scrolling and supports returning upward',()=>{
  const root=element({height:640,total:3000});assert.equal(move(root).moved,true);const before=root.scrollTop;
  assert.equal(move(root,[],root,'up').moved,true);assert(root.scrollTop<before);
});
test('reports the boundary instead of pretending a scroll succeeded',()=>{
  const root=element({height:640,total:640});const result=move(root);assert.equal(result.moved,false);assert.equal(result.atBoundary,true);
});
test('ignores hidden scroll regions and favors the central main timeline',()=>{
  const root=element({height:640,total:640}),hidden=element({hidden:true}),sidebar=element(),timeline=element({main:true});
  const result=move(root,[hidden,sidebar,timeline],timeline);assert.equal(result.moved,true);assert.equal(hidden.scrollTop,0);assert.equal(sidebar.scrollTop,0);assert(timeline.scrollTop>0);
});
test('does not report movement when a site refuses the requested position',()=>{
  const root=element({height:640,total:640}),fixed=element({main:true});Object.defineProperty(fixed,'scrollTop',{get:()=>0,set:()=>{}});
  assert.equal(move(root,[fixed],fixed).moved,false);
});
function page({host='x.com',path='/TeaonaX',body='TEAONA Profile Posts Replies Media',mainText=body,count=0,busy=false}={}){
  const main={innerText:mainText,querySelectorAll:()=>busy?[{getClientRects:()=>[{}]}]:[]};
  return vm.runInNewContext(state,{document:{readyState:'complete',body:{innerText:body},querySelector:()=>main,querySelectorAll:()=>Array.from({length:count},()=>({getClientRects:()=>[{}]}))},window:{innerWidth:360,innerHeight:640},location:{href:'https://'+host+path,hostname:host,pathname:path},getComputedStyle:()=>({visibility:'visible'})});
}
test('a profile header and search navigation are not readable timelines',()=>{
  assert.equal(page().meaningful,false);assert.equal(page({path:'/search',body:'Top Latest People Media Lists Search timeline'}).meaningful,false);
});
test('a rendered post and an explicit empty result are readable',()=>{
  assert.equal(page({count:1}).meaningful,true);assert.equal(page({path:'/search',mainText:'No results for this query'}).meaningful,true);
});
test('a generic loading shell is not captured as useful content',()=>{
  assert.equal(page({host:'rarelist.io',path:'/projects/agent-dojo',mainText:'Loading project…'}).meaningful,false);
  assert.equal(page({host:'example.org',mainText:'A complete useful article with enough information to read.'}).meaningful,true);
});
test('restrictions remain explicit and are not treated as loading',()=>{
  assert.equal(page({body:'Verify you are human'}).blocked,true);
});
test('the Android app embeds the exact tested scripts',()=>{
  const java=fs.readFileSync('src/com/android/launcher3/agent/AgentPageRuntime.java','utf8');
  const embeddedState=JSON.parse(java.match(/String STATE = (.*);/)[1]);
  const embeddedScroll=JSON.parse(java.match(/String SCROLL = (.*);/)[1]);
  assert.equal(embeddedState,'JSON.stringify('+state.trim()+')');assert.equal(embeddedScroll,scroll.trim());
});
