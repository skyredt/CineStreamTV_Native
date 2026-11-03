(function () {
  if (window.cinedbkPlayer) return;
  const state = { failed: false, resumed: false, started: false };
  let cursor = null, hideTimer;
  const style = document.createElement('style');
  style.textContent = `
    html,body {margin:0!important;padding:0!important;width:100%!important;height:100%!important;background:#000!important;overflow:hidden!important}
    .jwplayer {position:fixed!important;inset:0!important;width:100vw!important;height:100vh!important;max-width:none!important;max-height:none!important;margin:0!important;border-radius:0!important}
    .jwplayer .jw-aspect {display:none!important}
    .jwplayer .jw-wrapper {height:100%!important}
    .jwplayer video {object-fit:contain!important}
    .cinedbk-navigating .jw-controls,.cinedbk-navigating .jw-controlbar {opacity:1!important;visibility:visible!important;transform:none!important}
    .cinedbk-selected {outline:3px solid #00e5ff!important;outline-offset:-3px!important;background-color:rgba(0,229,255,.18)!important}
  `;
  document.head.appendChild(style);
  function fit() {
    const width = Math.max(innerWidth, 1), height = Math.max(innerHeight, 1);
    [document.documentElement, document.body].filter(Boolean).forEach(el => el.style.setProperty('height', height+'px', 'important'));
    document.querySelectorAll('.jwplayer').forEach(el => {
      el.style.setProperty('width', width+'px', 'important');
      el.style.setProperty('height', height+'px', 'important');
    });
  }
  window.addEventListener('resize', fit);
  new MutationObserver(fit).observe(document.documentElement, {childList:true,subtree:true});
  fit(); requestAnimationFrame(fit);
  window.cinedbkFit = fit;
  function player() { try { return window.jwplayer && window.jwplayer(); } catch (_) { return null; } }
  function show() {
    document.documentElement.classList.add('cinedbk-navigating');
    const root = document.querySelector('.jwplayer');
    if (root) root.dispatchEvent(new MouseEvent('mousemove', { bubbles: true }));
    clearTimeout(hideTimer);
    hideTimer = setTimeout(() => {
      if (cursor) cursor.classList.remove('cinedbk-selected');
      document.documentElement.classList.remove('cinedbk-navigating');
    }, 8000);
  }
  function visible(el) {
    const r = el.getBoundingClientRect(), s = getComputedStyle(el);
    if (r.width < 2 || r.height < 2 || s.display === 'none' || s.visibility === 'hidden' || +s.opacity === 0) return false;
    const menu = el.closest('.jw-settings-menu');
    if (menu && (+getComputedStyle(menu).opacity === 0 || getComputedStyle(menu).visibility === 'hidden')) return false;
    return r.right > 0 && r.bottom > 0 && r.left < innerWidth && r.top < innerHeight;
  }
  function controls() {
    return [...document.querySelectorAll('.jw-controlbar [tabindex], .jw-controlbar [role=button], .jw-controlbar [role=slider], .jw-settings-menu [tabindex], .jw-settings-menu [role=menuitemradio], .jw-settings-menu button, .jw-display-icon-container [role=button], video[controls]')].filter(el => !['menu','tablist','group','tabpanel','listbox'].includes(el.getAttribute('role'))).filter(visible);
  }
  function activate(el) {
    const r = el.getBoundingClientRect();
    const options = {bubbles:true,button:0,clientX:r.left+r.width/2,clientY:r.top+r.height/2};
    ['mousedown','mouseup','click'].forEach(type => el.dispatchEvent(new MouseEvent(type, options)));
  }
  function focus(el) {
    if (!el) return false;
    if (cursor) cursor.classList.remove('cinedbk-selected');
    cursor = el;
    cursor.classList.add('cinedbk-selected');
    if (!cursor.hasAttribute('tabindex')) cursor.tabIndex = 0;
    cursor.focus({ preventScroll: true });
    return true;
  }
  function initial(list) {
    return list.find(el => el.matches('.jw-icon-playback')) || list[0];
  }
  window.cinedbkTvMove = function (code) {
    show();
    let list = controls();
    if (!list.length) return false;
    if (!cursor || !list.includes(cursor)) return focus(initial(list));
    const activeMenu = cursor.closest('.jw-settings-menu');
    if (activeMenu && (code === 19 || code === 20)) {
      const menuItems = list.filter(el => activeMenu.contains(el));
      if (menuItems.length) list = menuItems;
    }
    // Only the web player's focused seek bar receives seek keys. App navigation never seeks or changes volume.
    if (cursor.matches('[role=slider],input[type=range]') && (code === 21 || code === 22)) {
      const key = code === 21 ? 'ArrowLeft' : 'ArrowRight', number = code === 21 ? 37 : 39;
      cursor.dispatchEvent(new KeyboardEvent('keydown', { key, code:key, keyCode:number, which:number, bubbles:true }));
      cursor.dispatchEvent(new KeyboardEvent('keyup', { key, code:key, keyCode:number, which:number, bubbles:true }));
      return true;
    }
    const a = cursor.getBoundingClientRect(), ax = a.left+a.width/2, ay = a.top+a.height/2;
    const horizontal = code === 21 || code === 22, positive = code === 20 || code === 22;
    let ranked = list.filter(el => el !== cursor).map(el => {
      const b = el.getBoundingClientRect(), dx = b.left+b.width/2-ax, dy = b.top+b.height/2-ay;
      const along = horizontal ? dx : dy, cross = horizontal ? Math.abs(dy) : Math.abs(dx);
      return { el, cross, along: positive ? along : -along, score: Math.abs(along)+cross*3 };
    }).filter(x => x.along > 2).sort((a,b) => a.score-b.score);
    // Keep horizontal navigation in the button row; Up explicitly enters the timeline.
    if (horizontal && !cursor.matches("[role=slider],input[type=range]")) {
      const sameRow = ranked.filter(x => x.cross < Math.max(a.height / 2, 12));
      if (sameRow.length) ranked = sameRow;
    }
    return ranked.length ? focus(ranked[0].el) : true;
  };
  window.cinedbkTvPress = function () {
    show();
    const list = controls();
    if (!cursor || !list.includes(cursor)) focus(initial(list));
    if (!cursor) return false;
    if (!cursor.matches('[role=slider],input[type=range]')) activate(cursor);
    return true;
  };
  window.cinedbkTvBack = function () {
    const toggle = [...document.querySelectorAll('.jw-controlbar [aria-expanded=true]')].find(visible);
    if (!toggle) return false;
    activate(toggle); cursor = toggle; return true;
  };
  window.cinedbkPlayer = function () {
    const p = player();
    if (!p || !p.getState) return { failed:state.failed, position:0, duration:0, playing:false };
    if (!p.__cinedbkBound && p.on) { p.__cinedbkBound = true; p.on('error', () => { state.failed = true; }); p.on('setupError', () => { state.failed = true; }); }
    const duration = Number(p.getDuration()) || 0, position = Number(p.getPosition()) || 0;
    const status = p.getState();
    if (!state.started) {
      if (['playing','buffering','paused'].includes(status)) state.started = true;
      else if (duration > 0 && status === 'idle') { state.started = true; p.play(true); }
    }
    if (!state.resumed && duration > 0) {
      state.resumed = true;
      if (window.cinedbkResume > 0) p.seek(window.cinedbkResume);
    }
    return { failed:state.failed || status === 'error', position, duration, playing:status === 'playing' };
  };
  window.cinedbkPause = function () { const p = player(); if (p && p.pause) p.pause(true); };
})();
