const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('src/com/android/launcher3/agent/AgentCapture.java', 'utf8');
const script = JSON.parse(source.match(/SCRIPT = (.*);/)[1]);
function capture({path='/public', body='Useful public post', password=false}={}) {
  const input = {getClientRects:()=>[{}]};
  const context = {JSON,Date,location:{href:'https://x.com'+path,pathname:path},getComputedStyle:()=>({visibility:'visible'}),document:{title:'Page',body:{innerText:body},images:[],querySelectorAll:selector=>selector.startsWith('input[')&&password?[input]:[],querySelector:()=>null}};
  return JSON.parse(vm.runInNewContext(script, context));
}
assert.equal(capture().url,'https://x.com/public');
assert.equal(capture().text,'Useful public post');
assert.match(capture({path:'/login'}).error,/withheld/);
assert.equal(capture({password:true}).text,undefined);
assert.equal(capture({body:'Verify you are human'}).blocked,true);
console.log('Agent capture privacy and restriction checks passed');
