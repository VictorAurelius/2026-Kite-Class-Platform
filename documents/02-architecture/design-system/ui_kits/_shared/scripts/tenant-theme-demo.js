/* ============================================================================
 * tenant-theme-demo.js — per-tenant theme DEMO switcher (GAP-1230 phần b)
 * ----------------------------------------------------------------------------
 * Dùng chung 4 KC dashboard kits: kiteclass-{student,teacher,parent,pro-v2}.
 * Inject floating switcher 3-GV demo-trio (Cô Hà / Thầy Nhì / Cô Khánh).
 * Click → set class kc-demo-{ha|nhi|khanh} trên <html> + <body> (KHÔNG xoá
 * class kit có sẵn) → token --primary/--accent/--ring đổi runtime THẬT (CSS
 * trong tenant-theme-demo.css), brand identity trong widget cập nhật theo.
 *
 * Per design-source-implementation-parity.md §3.2 — affordance click có hiệu
 * ứng runtime thật, không inert.
 *
 * Production note: theme thật đến từ `branding` package per ADR-009 (build-time
 * per-tenant). Switcher này CHỈ là demo affordance trong design kit.
 * ==========================================================================*/
(function () {
  'use strict';

  var TENANTS = [
    { key: 'ha',    cls: 'kc-demo-ha',    mark: 'H', name: 'Lớp Toán cô Hà',   tag: 'Gia sư Toán tiểu học', label: 'Cô Hà · Toán' },
    { key: 'nhi',   cls: 'kc-demo-nhi',   mark: 'N', name: 'Lớp Hóa thầy Nhì',  tag: 'Luyện Hóa THPT',       label: 'Thầy Nhì · Hóa' },
    { key: 'khanh', cls: 'kc-demo-khanh', mark: 'K', name: 'Anh ngữ cô Khánh',  tag: 'Luyện thi IELTS',      label: 'Cô Khánh · Anh' }
  ];
  var ALL_CLS = TENANTS.map(function (t) { return t.cls; });

  function build() {
    if (document.querySelector('.kc-theme-demo')) { return; }

    var root = document.documentElement;
    var body = document.body;

    var panel = document.createElement('div');
    panel.className = 'kc-theme-demo';
    panel.setAttribute('role', 'group');
    panel.setAttribute('aria-label', 'Chủ đề theo giáo viên (demo per-tenant)');

    var html = '' +
      '<div class="kc-theme-demo__title">Chủ đề theo giáo viên</div>' +
      '<div class="kc-theme-demo__brand">' +
        '<span class="kc-theme-demo__mark" data-kc-mark></span>' +
        '<span class="kc-theme-demo__name"><span data-kc-name></span><small data-kc-tag></small></span>' +
      '</div>' +
      '<div class="kc-theme-demo__row">';
    TENANTS.forEach(function (t) {
      html += '<button type="button" class="kc-theme-demo__btn kc-theme-demo__btn--' + t.key +
        '" data-kc-theme="' + t.key + '" aria-pressed="false">' + t.label + '</button>';
    });
    html += '</div>' +
      '<div class="kc-theme-demo__note">Demo per-tenant · production: theme từ <code>branding</code> per ADR-009</div>';
    panel.innerHTML = html;
    body.appendChild(panel);

    var markEl = panel.querySelector('[data-kc-mark]');
    var nameEl = panel.querySelector('[data-kc-name]');
    var tagEl  = panel.querySelector('[data-kc-tag]');
    var btns   = Array.prototype.slice.call(panel.querySelectorAll('[data-kc-theme]'));

    function apply(key) {
      var t = TENANTS.filter(function (x) { return x.key === key; })[0] || TENANTS[0];
      // toggle theme class on both html + body (keep existing kit classes)
      root.classList.remove.apply(root.classList, ALL_CLS);
      body.classList.remove.apply(body.classList, ALL_CLS);
      root.classList.add(t.cls);
      body.classList.add(t.cls);
      // update brand identity in widget (runtime-real)
      markEl.textContent = t.mark;
      nameEl.textContent = t.name;
      tagEl.textContent = t.tag;
      btns.forEach(function (b) {
        b.setAttribute('aria-pressed', b.getAttribute('data-kc-theme') === key ? 'true' : 'false');
      });
    }

    btns.forEach(function (b) {
      b.addEventListener('click', function () { apply(b.getAttribute('data-kc-theme')); });
    });

    apply('ha'); // default tenant
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', build);
  } else {
    build();
  }
})();
