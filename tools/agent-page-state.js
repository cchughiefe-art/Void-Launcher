(() => {
  const visible = e => !!e.getClientRects().length && getComputedStyle(e).visibility !== 'hidden';
  const text = (document.body?.innerText || '').trim();
  const main = document.querySelector('main,[role="main"]') || document.body;
  const mainText = (main?.innerText || '').trim();
  const articles = [...document.querySelectorAll('article')].filter(visible);
  const busy = [...(main?.querySelectorAll('[role="progressbar"],[aria-busy="true"]') || [])].some(visible);
  const blocked = /verify you are human|checking your browser|unusual activity|automated requests|account suspended|temporarily restricted|rate limit exceeded|too many requests/i.test(text);
  const x = /(^|\.)(x\.com|twitter\.com)$/i.test(location.hostname);
  const timeline = x && !/^\/(compose|i\/flow|login|settings)(\/|$)/.test(location.pathname);
  const empty = /No results for|No posts yet|hasn.t posted|You don.t have any notifications|Nothing to see here|You.re all caught up|Welcome to your timeline|This account doesn.t exist|This Post was deleted|Hmm\.\.\.this page doesn.t exist|Something went wrong[\s\S]{0,150}(Retry|Try again)/i.test(mainText);
  const loadingOnly = /^(loading(?:\s+\w+){0,3}[….! ]*|please wait[….! ]*)$/i.test(mainText);
  const meaningful = timeline ? (articles.length > 0 || empty) : (mainText.length > 30 && !loadingOnly && !busy);
  const signature = [location.href, articles.length, mainText.length, mainText.slice(-250)].join('|');
  return {url:location.href, ready:document.readyState, meaningful, busy, blocked, empty, articles:articles.length,
    textLength:mainText.length, signature, viewport:{width:window.innerWidth || 0,height:window.innerHeight || 0},
    reason:blocked ? 'Site restriction' : meaningful ? 'Content present' : timeline ? 'Timeline content still loading' : 'Page content still loading'};
})()
