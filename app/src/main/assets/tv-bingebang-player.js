(function () {
  if (window.cinedbkPlayer) return;
  let cursor = null, resumed = false, hideTimer;
  const style = document.createElement('style');
  style.textContent = `
    .cinedbk-navigating .pl-chrome,.cinedbk-navigating .pl-top,.cinedbk-navigating .pl-bottom {opacity:1!important;visibility:visible!important;transform:none!important}
    .cinedbk-selected {outline:3px solid #00e5ff!important;outline-offset:-3px!important;background-color:rgba(0,229,255,.18)!important}
  `;
  document.head.appendChild(style);
  function visible(el) {
    if (!el || el.disabled || el.closest('[hidden]')) return false;
    const r = el.getBoundingClientRect();
    if (r.width < 2 || r.height < 2 || r.right <= 0 || r.bottom <= 0 || r.left >= innerWidth || r.top >= innerHeight) return false;
    for (let node = el; node && node.nodeType === 1; node = node.parentElement) {
      const s = getComputedStyle(node);
      if (s.display === 'none' || s.visibility === 'hidden' || +s.opacity === 0) return false;
    }
    return true;
  }
  function show() {
    window.BB_PLAYER?.showChrome();
    document.documentElement.classList.add('cinedbk-navigating');
    clearTimeout(hideTimer);
    hideTimer = setTimeout(() => {
      cursor?.classList.remove('cinedbk-selected');
      document.documentElement.classList.remove('cinedbk-navigating');
    }, 8000);
  }
  function controls() {
    const panel = [...document.querySelectorAll('[data-ep-gate], [data-gate]')].find(visible);
    const root = panel || document.querySelector('[data-player]');
    return root ? [...root.querySelectorAll(panel ? 'button,[role=button]' : '.pl-top button,.pl-bottom button,[data-err] button')].filter(visible) : [];
  }
  function focus(el) {
    if (!el) return false;
    cursor?.classList.remove('cinedbk-selected');
    cursor = el; cursor.classList.add('cinedbk-selected');
    cursor.focus({ preventScroll: true });
    cursor.scrollIntoView({ block: 'nearest', inline: 'nearest' });
    return true;
  }
  function initial(list) { return list.find(el => el.matches('[data-toggle]')) || list[0]; }
  window.cinedbkTvMove = function (code) {
    show();
    const list = controls();
    if (!cursor || !list.includes(cursor)) return focus(initial(list));
    const a = cursor.getBoundingClientRect(), ax = a.left + a.width / 2, ay = a.top + a.height / 2;
    const horizontal = code === 21 || code === 22, positive = code === 20 || code === 22;
    let ranked = list.filter(el => el !== cursor).map(el => {
      const b = el.getBoundingClientRect(), dx = b.left + b.width / 2 - ax, dy = b.top + b.height / 2 - ay;
      const along = horizontal ? dx : dy, cross = horizontal ? Math.abs(dy) : Math.abs(dx);
      return { el, cross, along: positive ? along : -along, score: Math.abs(along) + cross * 3 };
    }).filter(x => x.along > 2).sort((a, b) => a.score - b.score);
    if (horizontal) {
      const row = ranked.filter(x => x.cross < Math.max(a.height / 2, 12));
      if (row.length) ranked = row;
    }
    return ranked.length ? focus(ranked[0].el) : true;
  };
  window.cinedbkTvPress = function () {
    show();
    const list = controls();
    if (!cursor || !list.includes(cursor)) focus(initial(list));
    if (!cursor) return false;
    cursor.click();
    setTimeout(() => { const next = controls(); if (!next.includes(cursor)) focus(initial(next)); }, 100);
    return true;
  };
  window.cinedbkTvBack = function () {
    const panel = [...document.querySelectorAll('[data-ep-gate], [data-gate]')].find(visible);
    const close = panel?.querySelector('button[data-ep-close],button[data-gate-close]');
    if (!close) return false;
    close.click(); cursor = null; show(); return true;
  };
  window.cinedbkPlayer = function () {
    const engine = window.BB_PLAYER?.engine, video = engine?.video;
    if (!video) return { position: 0, duration: 0, playing: false, failed: visible(document.querySelector('[data-err]')) };
    if (!resumed && video.readyState >= 1) {
      resumed = true;
      if (window.cinedbkResume > 0) engine.seek(window.cinedbkResume);
    }
    return { position: video.currentTime || 0, duration: Number.isFinite(video.duration) ? video.duration : 0,
      playing: !video.paused && !video.ended && video.readyState >= 2, failed: visible(document.querySelector('[data-err]')) };
  };
  window.cinedbkPause = function () { window.BB_PLAYER?.engine.pause(); };
})();
