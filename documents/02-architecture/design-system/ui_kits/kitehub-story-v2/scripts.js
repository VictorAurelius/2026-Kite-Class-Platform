/* kitehub-story v2 — vanilla JS for scroll-reveal, parallax timeline,
   before/after slider, and stars sprinkle.
   No frameworks. Honors prefers-reduced-motion. */

(function () {
  'use strict';

  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  /* ---------- Reveal-on-scroll ----------------------------------- */
  function setupReveal() {
    const els = document.querySelectorAll('.kh-reveal');
    if (!els.length) return;
    if (reduced || !('IntersectionObserver' in window)) {
      els.forEach((el) => el.classList.add('is-visible'));
      return;
    }
    const io = new IntersectionObserver(
      (entries) => {
        entries.forEach((e) => {
          if (e.isIntersecting) {
            e.target.classList.add('is-visible');
            io.unobserve(e.target);
          }
        });
      },
      { rootMargin: '0px 0px -10% 0px', threshold: 0.05 }
    );
    els.forEach((el) => io.observe(el));
  }

  /* ---------- Hero stars sprinkle -------------------------------- */
  function setupStars() {
    const layer = document.querySelector('.kh-hero__stars');
    if (!layer) return;
    const N = reduced ? 12 : 36;
    const frag = document.createDocumentFragment();
    for (let i = 0; i < N; i++) {
      const star = document.createElement('span');
      star.className = 'kh-hero__star';
      star.style.top = Math.random() * 100 + '%';
      star.style.left = Math.random() * 100 + '%';
      star.style.animationDelay = (Math.random() * 3).toFixed(2) + 's';
      frag.appendChild(star);
    }
    layer.appendChild(frag);
  }

  /* ---------- Day-in-life: scroll-driven active step ------------- */
  function setupDayTimeline() {
    const steps = document.querySelectorAll('.kh-day__step');
    const sceneTitle = document.querySelector('[data-scene-title]');
    const sceneMeta = document.querySelector('[data-scene-meta]');
    const sceneBody = document.querySelector('[data-scene-body]');
    if (!steps.length || !sceneTitle) return;

    function activate(idx) {
      steps.forEach((s, i) => s.classList.toggle('is-active', i === idx));
      const step = steps[idx];
      if (!step) return;
      sceneTitle.textContent = step.dataset.title || '';
      sceneMeta.textContent = step.dataset.meta || '';
      sceneBody.textContent = step.dataset.scene || step.querySelector('p')?.textContent || '';
    }

    activate(0);
    steps.forEach((s, i) => s.addEventListener('click', () => activate(i)));

    if (reduced || !('IntersectionObserver' in window)) return;
    const io = new IntersectionObserver(
      (entries) => {
        entries.forEach((e) => {
          if (e.isIntersecting) {
            const idx = Array.prototype.indexOf.call(steps, e.target);
            if (idx >= 0) activate(idx);
          }
        });
      },
      { rootMargin: '-40% 0px -40% 0px', threshold: 0 }
    );
    steps.forEach((s) => io.observe(s));
  }

  /* ---------- Before / After slider ------------------------------ */
  function setupBaSlider() {
    const ba = document.querySelector('.kh-ba');
    if (!ba) return;
    const handle = ba.querySelector('.kh-ba__handle');
    const after = ba.querySelector('.kh-ba__panel--after');
    if (!handle || !after) return;

    function setPct(pct) {
      const clamped = Math.max(0, Math.min(100, pct));
      handle.style.left = clamped + '%';
      after.style.clipPath = `inset(0 0 0 ${clamped}%)`;
    }

    let dragging = false;
    function onMove(clientX) {
      const r = ba.getBoundingClientRect();
      setPct(((clientX - r.left) / r.width) * 100);
    }
    handle.addEventListener('mousedown', (e) => { dragging = true; e.preventDefault(); });
    window.addEventListener('mouseup',   () => { dragging = false; });
    window.addEventListener('mousemove', (e) => { if (dragging) onMove(e.clientX); });

    handle.addEventListener('touchstart', (e) => { dragging = true; }, { passive: true });
    window.addEventListener('touchend',   () => { dragging = false; });
    window.addEventListener('touchmove',  (e) => { if (dragging && e.touches[0]) onMove(e.touches[0].clientX); }, { passive: true });

    /* Keyboard accessibility */
    handle.tabIndex = 0;
    handle.setAttribute('role', 'slider');
    handle.setAttribute('aria-valuemin', '0');
    handle.setAttribute('aria-valuemax', '100');
    handle.setAttribute('aria-valuenow', '50');
    handle.setAttribute('aria-label', 'So sánh trước và sau khi dùng KiteHub');
    handle.addEventListener('keydown', (e) => {
      let pct = parseFloat(handle.style.left || '50');
      if (e.key === 'ArrowLeft')  { pct -= 4; e.preventDefault(); }
      if (e.key === 'ArrowRight') { pct += 4; e.preventDefault(); }
      if (e.key === 'Home')       { pct = 0; }
      if (e.key === 'End')        { pct = 100; }
      setPct(pct);
      handle.setAttribute('aria-valuenow', String(Math.round(Math.max(0, Math.min(100, pct)))));
    });

    setPct(50);
  }

  /* ---------- Mock dashboard chart dots ------------------------- */
  function setupChartDots() {
    const svg = document.querySelector('.kh-dash__chart svg');
    if (!svg) return;
    const dots = svg.querySelectorAll('.dot');
    dots.forEach((d, i) => {
      d.style.animationDelay = (1.6 + i * 0.18).toFixed(2) + 's';
    });
  }

  /* ---------- KPI count-up (small, light) ------------------------ */
  function setupKpiCountUp() {
    const els = document.querySelectorAll('[data-countup]');
    if (!els.length || reduced) {
      els.forEach((el) => { el.textContent = el.dataset.countup; });
      return;
    }
    const io = new IntersectionObserver(
      (entries) => {
        entries.forEach((e) => {
          if (!e.isIntersecting) return;
          const el = e.target;
          io.unobserve(el);
          const target = parseFloat(el.dataset.countup);
          const suffix = el.dataset.countupSuffix || '';
          const dur = 1400;
          const t0 = performance.now();
          (function tick(now) {
            const p = Math.min(1, (now - t0) / dur);
            const val = (target * (1 - Math.pow(1 - p, 3)));
            el.textContent = (Number.isInteger(target) ? Math.round(val).toLocaleString('vi-VN') : val.toFixed(1)) + suffix;
            if (p < 1) requestAnimationFrame(tick);
            else el.textContent = (Number.isInteger(target) ? target.toLocaleString('vi-VN') : target.toFixed(1)) + suffix;
          })(t0);
        });
      },
      { threshold: 0.4 }
    );
    els.forEach((el) => io.observe(el));
  }

  /* ---------- Smooth scroll for nav links ------------------------ */
  function setupSmoothNav() {
    document.querySelectorAll('a[href^="#"]').forEach((a) => {
      a.addEventListener('click', (e) => {
        const id = a.getAttribute('href').slice(1);
        if (!id) return;
        const target = document.getElementById(id);
        if (!target) return;
        e.preventDefault();
        target.scrollIntoView({ behavior: reduced ? 'auto' : 'smooth', block: 'start' });
      });
    });
  }

  /* ---------- Boot ---------------------------------------------- */
  document.addEventListener('DOMContentLoaded', () => {
    setupStars();
    setupReveal();
    setupDayTimeline();
    setupBaSlider();
    setupChartDots();
    setupKpiCountUp();
    setupSmoothNav();
  });
})();
