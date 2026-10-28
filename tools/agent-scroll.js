(direction => {
  const sign = direction === 'up' ? -1 : 1;
  const root = document.scrollingElement || document.documentElement;
  const width = Math.max(1, window.innerWidth || root.clientWidth || 1);
  const height = Math.max(1, window.innerHeight || root.clientHeight || 1);
  const center = document.elementFromPoint(width / 2, height * 0.55);
  const ancestors = new Set();
  for (let e = center; e; e = e.parentElement) ancestors.add(e);
  const candidates = [];
  const elements = [...new Set([root, ...document.querySelectorAll('body *')])].slice(0, 6000);
  for (const e of elements) {
    const r = e.getBoundingClientRect();
    const style = getComputedStyle(e);
    const visibleHeight = Math.max(0, Math.min(height, r.bottom) - Math.max(0, r.top));
    const visibleWidth = Math.max(0, Math.min(width, r.right) - Math.max(0, r.left));
    if (style.visibility === 'hidden' || style.display === 'none' || visibleHeight < 40 || visibleWidth < 60) continue;
    if (e.scrollHeight <= e.clientHeight + 2 || e.clientHeight < 40) continue;
    if (e !== root && !/(auto|scroll|overlay)/.test(style.overflowY)) continue;
    const inMain = !!e.closest('main,[role="main"],[data-testid="primaryColumn"]');
    const inDialog = !!e.closest('[role="dialog"]');
    const score = visibleHeight * visibleWidth / (width * height) + (ancestors.has(e) ? 4 : 0) + (inMain ? 2 : 0) + (inDialog ? 8 : 0);
    candidates.push({e, score});
  }
  candidates.sort((a, b) => b.score - a.score);
  let last = {moved:false, atBoundary:true, reason:'No scrollable area in this direction', candidates:candidates.length};
  for (const {e} of candidates) {
    const before = e.scrollTop;
    const max = Math.max(0, e.scrollHeight - e.clientHeight);
    if ((sign > 0 && before >= max - 1) || (sign < 0 && before <= 1)) continue;
    const step = Math.max(40, Math.round(Math.min(height, e.clientHeight) * 0.8));
    const target = Math.max(0, Math.min(max, before + sign * step));
    // Assign scrollTop on the actual scroll container, including fixed-layout timelines.
    if (typeof e.scrollTo === 'function') e.scrollTo({top:target,left:e.scrollLeft || 0,behavior:'instant'});
    else e.scrollTop = target;
    const after = e.scrollTop;
    last = {moved:Math.abs(after - before) > 1, before, after, atBoundary:sign > 0 ? after >= max - 1 : after <= 1,
      target:e === root ? 'document' : (e.getAttribute('role') || e.tagName.toLowerCase()), candidates:candidates.length};
    if (last.moved) return last;
  }
  return last;
})
