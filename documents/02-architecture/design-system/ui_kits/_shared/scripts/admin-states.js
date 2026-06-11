/* =============================================================
   admin-states.js — shared per-screen state + dark-mode wiring
   for the kitehub-admin K-12 kit (GAP-364b cross-screen polish).

   Provides three previously-missing cross-screen affordances
   without per-screen markup duplication (DRY):

     1. Dark-mode toggle  — wires the `.dark` token layer that
        already exists in `_shared/colors_and_type.css`. Persists
        in localStorage + honours `prefers-color-scheme` on first
        load. Because every screen colour is `hsl(var(--token))`,
        flipping `.dark` on <html> recolours all 12 screens.

     2. Per-screen loading skeleton  — `?state=loading` injects a
        shimmer skeleton shaped per `body[data-skeleton]`
        (kpi-table | form | matrix | calendar | cards | wizard |
        generic) so every dense screen demonstrates its loading
        state, not just the dashboard.

     3. Per-screen empty state  — `?state=empty` injects an
        in-context empty block from `body[data-empty-*]` so each
        screen shows its OWN empty copy instead of relying on the
        shared empty-states.html gallery.

   Opt-out: `body[data-no-states]` (e.g. login) keeps dark-mode
   only and skips the loading/empty state tabs.
   ============================================================= */
(function () {
  'use strict';
  var root = document.documentElement;
  var body = document.body;
  var THEME_KEY = 'kite-admin-theme';

  /* ---------- 1. Dark mode ---------- */
  var stored = null;
  try { stored = localStorage.getItem(THEME_KEY); } catch (e) {}
  var prefersDark = window.matchMedia &&
    window.matchMedia('(prefers-color-scheme: dark)').matches;
  if (stored === 'dark' || (!stored && prefersDark)) {
    root.classList.add('dark');
  }

  function isDark() { return root.classList.contains('dark'); }

  function persist() {
    try { localStorage.setItem(THEME_KEY, isDark() ? 'dark' : 'light'); } catch (e) {}
  }

  function makeThemeToggle() {
    var btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'theme-toggle';
    btn.setAttribute('aria-label', 'Bật/tắt chế độ tối');
    btn.setAttribute('title', 'Chế độ sáng/tối');
    btn.style.cssText =
      'display:inline-flex;align-items:center;justify-content:center;' +
      'width:28px;height:24px;border:0;background:transparent;cursor:pointer;' +
      'color:hsl(var(--muted-foreground));border-radius:var(--radius-full);';
    function paint() {
      btn.innerHTML = isDark()
        ? '<i data-lucide="sun" style="width:15px;height:15px"></i>'
        : '<i data-lucide="moon" style="width:15px;height:15px"></i>';
      btn.setAttribute('aria-pressed', isDark() ? 'true' : 'false');
      if (window.lucide) window.lucide.createIcons();
    }
    btn.addEventListener('click', function () {
      root.classList.toggle('dark');
      persist();
      paint();
    });
    paint();
    return btn;
  }

  /* ---------- 2 + 3. State views ---------- */
  var params = new URLSearchParams(window.location.search);
  var state = params.get('state'); // 'loading' | 'empty' | null
  var allowStates = !body.hasAttribute('data-no-states');
  var main = document.querySelector('.shell__main');

  function sk(w, h, extra) {
    return '<span class="skeleton" style="height:' + h + ';width:' + w +
      ';' + (extra || '') + '"></span>';
  }

  function skeletonMarkup(shape) {
    var kpiRow =
      '<div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));gap:0.75rem;margin-bottom:1.25rem">' +
      Array(6).join('0').split('0').map(function () {
        return '<div class="card-base" style="padding:1rem 1.125rem">' +
          sk('60%', '0.7rem', 'margin-bottom:0.6rem') +
          sk('45%', '1.6rem', 'margin-bottom:0.5rem') +
          sk('70%', '0.7rem') + '</div>';
      }).join('') + '</div>';
    var tableRows = '';
    for (var i = 0; i < 7; i++) {
      tableRows += '<div style="display:flex;gap:1rem;align-items:center;padding:0.65rem 0.9rem;border-bottom:1px solid hsl(var(--border))">' +
        sk('28px', '28px', 'border-radius:var(--radius-full);flex:0 0 auto') +
        sk('22%', '0.85rem') + sk('14%', '0.85rem') + sk('14%', '0.85rem') +
        sk('10%', '0.85rem') + '</div>';
    }
    var tableBlock = '<div class="dt-wrap" style="max-height:none">' + tableRows + '</div>';
    var formBlock = '<div class="card-base" style="padding:1.5rem">' +
      Array(7).join('0').split('0').map(function () {
        return '<div style="margin-bottom:1.1rem">' + sk('30%', '0.75rem', 'margin-bottom:0.45rem') +
          sk('100%', '2.25rem') + '</div>';
      }).join('') + '</div>';
    var matrixBlock = '<div class="card-base" style="padding:1rem;display:grid;grid-template-columns:repeat(6,1fr);gap:1px;background:hsl(var(--border))">' +
      Array(31).join('0').split('0').map(function () {
        return '<div style="background:hsl(var(--card));padding:0.6rem">' + sk('80%', '0.7rem') + '</div>';
      }).join('') + '</div>';
    var calendarBlock = '<div class="cal-grid">' +
      Array(36).join('0').split('0').map(function () {
        return '<div class="cal-cell">' + sk('20%', '0.7rem', 'margin-bottom:0.4rem') + sk('70%', '0.6rem') + '</div>';
      }).join('') + '</div>';
    var cardsBlock = Array(5).join('0').split('0').map(function () {
      return '<div class="esc-card" style="margin-bottom:0.75rem">' +
        sk('40px', '40px', 'border-radius:var(--radius-full);flex:0 0 auto') +
        '<div style="flex:1">' + sk('55%', '0.9rem', 'margin-bottom:0.5rem') +
        sk('80%', '0.75rem', 'margin-bottom:0.5rem') + sk('30%', '0.7rem') + '</div>' +
        sk('72px', '2rem', 'flex:0 0 auto') + '</div>';
    }).join('');
    var wizardBlock = '<div class="steps" style="margin-bottom:1rem">' +
      Array(6).join('0').split('0').map(function () { return sk('90px', '1.6rem'); }).join('') +
      '</div>' + '<div class="dropzone">' + sk('48px', '48px', 'border-radius:var(--radius-full);margin:0 auto 0.75rem') +
      sk('40%', '1rem', 'margin:0 auto 0.5rem') + sk('25%', '0.8rem', 'margin:0 auto') + '</div>';

    switch (shape) {
      case 'form': return formBlock;
      case 'matrix': return matrixBlock;
      case 'calendar': return calendarBlock;
      case 'cards': return cardsBlock;
      case 'wizard': return wizardBlock;
      case 'kpi-table': return kpiRow + tableBlock;
      default: return kpiRow + tableBlock;
    }
  }

  function injectState() {
    if (!main || !state) return;
    // Hide real content blocks (keep the sticky header for context).
    var kids = main.children;
    for (var i = 0; i < kids.length; i++) {
      var el = kids[i];
      if (el.tagName === 'HEADER') continue;
      el.setAttribute('hidden', '');
    }
    var holder = document.createElement('div');
    holder.setAttribute('data-state-view', state);
    holder.className = 'stack-4';
    holder.style.marginTop = '1rem';

    if (state === 'loading') {
      holder.setAttribute('aria-busy', 'true');
      holder.setAttribute('aria-label', 'Đang tải dữ liệu');
      holder.innerHTML =
        '<div class="section-title" role="status">' +
        '<i data-lucide="loader" class="w-3 h-3 inline"></i> Đang tải dữ liệu…</div>' +
        skeletonMarkup(body.getAttribute('data-skeleton') || 'kpi-table');
    } else if (state === 'empty') {
      var icon = body.getAttribute('data-empty-icon') || 'inbox';
      var title = body.getAttribute('data-empty-title') || 'Chưa có dữ liệu';
      var msg = body.getAttribute('data-empty-msg') ||
        'Khi có dữ liệu, nội dung sẽ hiển thị tại đây.';
      var cta = body.getAttribute('data-empty-cta');
      holder.innerHTML =
        '<div class="empty-state" role="status">' +
        '<span class="empty-state__icon"><i data-lucide="' + icon + '" style="width:30px;height:30px"></i></span>' +
        '<div class="h3">' + title + '</div>' +
        '<p class="body-sm muted" style="max-width:30rem">' + msg + '</p>' +
        (cta ? '<button class="gradient-button" type="button" style="margin-top:0.5rem">' +
          '<i data-lucide="plus" class="w-4 h-4"></i>' + cta + '</button>' : '') +
        '</div>';
    }
    main.appendChild(holder);
  }

  /* ---------- Wire state-tabs ---------- */
  function decorateStateTabs() {
    var tabs = document.querySelector('.state-tabs');
    if (!tabs) return;
    var here = window.location.pathname.split('/').pop() || 'index.html';

    if (allowStates) {
      var loadingLink = document.createElement('a');
      loadingLink.href = here + '?state=loading';
      loadingLink.textContent = 'Tải';
      if (state === 'loading') loadingLink.setAttribute('data-active', 'true');

      var emptyLink = document.createElement('a');
      emptyLink.href = here + '?state=empty';
      emptyLink.textContent = 'Trống';
      if (state === 'empty') emptyLink.setAttribute('data-active', 'true');

      // Default state-tab should only be "active" when no ?state set.
      var defaultTab = tabs.querySelector('a[data-active="true"]');
      if (defaultTab && state) defaultTab.removeAttribute('data-active');

      tabs.appendChild(loadingLink);
      tabs.appendChild(emptyLink);
    }
    tabs.appendChild(makeThemeToggle());
  }

  decorateStateTabs();
  injectState();
  if (window.lucide) window.lucide.createIcons();
})();
