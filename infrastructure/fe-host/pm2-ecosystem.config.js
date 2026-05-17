// =============================================================================
// Wave 82 Bucket B — PM2 ecosystem config cho 2 Next.js standalone apps
// =============================================================================
//
// Mục đích: PM2 process manager run 2 Next.js standalone instance đồng thời
// trên EC2 t3.small (2GB RAM, 2 vCPU). Cấu hình theo GAP-566 AC để tránh
// OOM-kill khi ISR regen + traffic burst.
//
// Topology:
//   kitehub-frontend  → port 4701, marketing/landing/beta-status (ISR)
//   kiteclass-frontend → port 4700, tenant app (login, dashboard)
//
// RAM budget trên t3.small:
//   Total system   = 2048 MB
//   Linux + dnf    = ~150 MB
//   nginx          = ~50 MB
//   PM2 daemon     = ~80 MB
//   CloudWatch agt = ~80 MB
//   ------------------------
//   Free cho 2 Next = ~1700 MB
//   Per app cap    = 1.2 GB (max_memory_restart)
//   Nominal cap    = 2.4 GB total → swapfile 2GB gánh overhead (GAP-566)
//
// Triển khai (Wave 89 Bucket B GAP-602 fix — monorepo nested standalone layout):
//   sudo mkdir -p /var/www/{kitehub,kiteclass}-frontend
//   # rsync FULL monorepo workspace tree từ build artifact (preserves
//   # workspace structure that Next.js standalone bake-output reproduces).
//   sudo cp infrastructure/fe-host/pm2-ecosystem.config.js /var/www/pm2-ecosystem.config.js
//   sudo -u ec2-user pm2 start /var/www/pm2-ecosystem.config.js
//   sudo -u ec2-user pm2 save             # persist process list
//   sudo pm2 startup systemd -u ec2-user  # auto-start on reboot (GAP-603 wired
//                                         # vào ec2-kc-app.tf user_data — re-runnable safe)
//
// GAP-602 fix (Wave 89 Bucket B 2026-05-17): cwd path đã sửa từ
// `/var/www/<app>/.next/standalone` (non-monorepo layout giả định) sang
// `/var/www/<app>/<workspace>/<app>` (monorepo nested standalone layout) để
// match thực tế `pnpm --filter <app> build` standalone output preserve
// workspace structure. Wave 88 manual deploy bypass đã verify path đúng.
//
// Graceful deploy (zero-downtime reload sau khi rsync artifact mới):
//   pm2 reload pm2-ecosystem.config.js --update-env
//
// Cross-link:
//   - GAP-566 §Bước 2 (max_memory_restart=1.2G, min_uptime=60s, max_restarts=10)
//   - ADR-031 §Decision (PM2 cluster mode cân nhắc — Phase 1 BETA dùng fork mode)
//   - infrastructure/fe-host/nginx-fe.conf (upstream blocks point tới các port)
//
// =============================================================================

module.exports = {
  apps: [
    // -------------------------------------------------------------------------
    // App 1: kitehub-frontend — marketing + landing + beta-status
    // -------------------------------------------------------------------------
    {
      name: 'kitehub-frontend',

      // cwd: thư mục chứa server.js standalone Next.js generate.
      // GAP-602 fix (Wave 89 Bucket B 2026-05-17): monorepo nested layout.
      // `pnpm --filter kitehub-frontend build` với `output: 'standalone'`
      // preserve workspace tree → server.js ở `<root>/kitehub/kitehub-frontend/server.js`
      // (KHÔNG phải `<root>/.next/standalone/server.js` non-monorepo giả định).
      // Trước khi PM2 start phải rsync .next/static + public vào folder này
      // (Next standalone KHÔNG tự copy 2 folder này — bug-by-design).
      cwd: '/var/www/kitehub-frontend/kitehub/kitehub-frontend',

      // script: file Node.js entrypoint. Next standalone generate `server.js`
      // tại root của standalone folder. PM2 chạy `node server.js`.
      script: 'server.js',

      // Mode fork (mặc định) — 1 instance per app. Phase 1 BETA cohort nhỏ
      // không cần cluster (multiple workers cùng port). Cluster mode tiết
      // kiệm RAM kém hơn (mỗi worker = ~600MB heap).
      // Trade-off Phase 2: cân nhắc cluster khi >10 concurrent users sustained.
      exec_mode: 'fork',
      instances: 1,

      // Env vars: PORT bind loopback 127.0.0.1, nginx proxy_pass tới đây.
      // HOSTNAME: '127.0.0.1' đảm bảo Next chỉ listen loopback, KHÔNG public.
      // SG rule chặn port 4701 external nhưng defense-in-depth giữ binding hẹp.
      env: {
        NODE_ENV: 'production',
        PORT: 4701,
        HOSTNAME: '127.0.0.1',

        // NEXT_TELEMETRY_DISABLED: tắt opt-in telemetry gửi về Vercel.
        // Self-host = không cần dữ liệu Vercel side; tiết kiệm 1 outbound call.
        NEXT_TELEMETRY_DISABLED: '1',

        // NODE_OPTIONS: --max-old-space-size cap V8 heap để PM2 max_memory_restart
        // có thể intercept TRƯỚC khi Linux OOM-killer terminate. 1024MB heap
        // + native overhead ~200MB → tổng ~1200MB = max_memory_restart trigger.
        NODE_OPTIONS: '--max-old-space-size=1024',
      },

      // ---- GAP-566 §Bước 2 mandatory keys ----

      // max_memory_restart: PM2 sẽ gracefully restart process khi RSS vượt
      // ngưỡng. 1.2G = 1228.8 MB. PM2 check mỗi 30s mặc định.
      // Phối hợp với NODE_OPTIONS --max-old-space-size=1024 ở trên:
      // V8 hit heap limit TRƯỚC → throw OOM JS error → PM2 restart sạch hơn
      // là Linux OOM-killer SIGKILL.
      max_memory_restart: '1.2G',

      // min_uptime: process phải sống ≥60s mới count "stable". Restart trong
      // 60s đầu = count vào max_restarts → tránh restart loop khi config sai.
      min_uptime: '60s',

      // max_restarts: tối đa 10 lần restart liên tiếp trong 60s window.
      // Vượt → PM2 mark process "errored" + alert CloudWatch (qua PM2 logrotate
      // hook hoặc systemd journal).
      max_restarts: 10,

      // restart_delay: đợi 4s giữa mỗi restart attempt. Cho phép Linux
      // reclaim RAM (free page cache, OS file descriptor) trước khi try lại.
      restart_delay: 4000,

      // autorestart: PM2 tự restart khi process exit (signal hoặc crash).
      autorestart: true,

      // ---- Logging ----

      // Log path: PM2 default ~/.pm2/logs/. Đổi sang /var/log/pm2/ để
      // logrotate system-wide manage (PM2 có module logrotate riêng nhưng
      // tốt hơn để OS quản lý cho audit consistency).
      out_file: '/var/log/pm2/kitehub-frontend-out.log',
      error_file: '/var/log/pm2/kitehub-frontend-error.log',

      // log_date_format: timestamp UTC để correlate với CloudWatch
      // (CW dùng UTC; local TZ Asia/Bangkok sẽ off ±7h khi cross-check).
      log_date_format: 'YYYY-MM-DD HH:mm:ss Z',

      // merge_logs: gom log từ multiple instances (cluster mode) — fork chỉ
      // 1 instance nhưng giữ flag để chuyển cluster sau dễ.
      merge_logs: true,

      // ---- Watch ----
      // KHÔNG watch file change trên production. Deploy = rsync + pm2 reload
      // explicit thay vì auto-restart trên file change (file race condition
      // khi rsync chưa xong sẽ restart Next với artifact incomplete).
      watch: false,

      // ---- Kill timeout ----
      // PM2 gửi SIGINT → đợi N ms → SIGKILL nếu chưa exit.
      // Next standalone xử lý SIGINT gracefully (drain pending request).
      // 10s đủ cho most request hoàn thành (timeout proxy nginx = 60s
      // nhưng PM2 reload chỉ trigger khi deploy, traffic đã shift sang
      // instance khác qua nginx reload).
      kill_timeout: 10000,

      // ---- Wait ready ----
      // PM2 đợi 'ready' signal từ Node app trước khi mark "online".
      // Next.js KHÔNG emit 'process.send("ready")' mặc định — disable wait_ready.
      // (Bucket C có thể wrap server.js để emit ready sau khi listen() resolve.)
      wait_ready: false,
      listen_timeout: 10000,
    },

    // -------------------------------------------------------------------------
    // App 2: kiteclass-frontend — tenant app (login, dashboard, attendance, payment)
    // -------------------------------------------------------------------------
    // Config giống kitehub-frontend, khác:
    //   - cwd → kiteclass-frontend folder
    //   - PORT → 4700 (per package.json dev script)
    //   - log file naming
    {
      name: 'kiteclass-frontend',

      // GAP-602 fix (Wave 89 Bucket B): monorepo nested layout — see comment trên kitehub-frontend
      cwd: '/var/www/kiteclass-frontend/kiteclass/kiteclass-frontend',
      script: 'server.js',

      exec_mode: 'fork',
      instances: 1,

      env: {
        NODE_ENV: 'production',
        PORT: 4700,
        HOSTNAME: '127.0.0.1',
        NEXT_TELEMETRY_DISABLED: '1',
        NODE_OPTIONS: '--max-old-space-size=1024',
      },

      // Cùng RAM cap như kitehub — symmetric. Nếu tenant app traffic nhiều
      // hơn marketing, có thể bump lên 1.4G cho kiteclass và giảm 1.0G cho
      // kitehub. Phase 1 BETA giữ symmetric, đo qua CloudWatch trước khi tune.
      max_memory_restart: '1.2G',

      min_uptime: '60s',
      max_restarts: 10,
      restart_delay: 4000,
      autorestart: true,

      out_file: '/var/log/pm2/kiteclass-frontend-out.log',
      error_file: '/var/log/pm2/kiteclass-frontend-error.log',
      log_date_format: 'YYYY-MM-DD HH:mm:ss Z',
      merge_logs: true,

      watch: false,

      kill_timeout: 10000,

      wait_ready: false,
      listen_timeout: 10000,
    },
  ],

  // ---------------------------------------------------------------------------
  // Deploy hook section — graceful reload sau rsync artifact mới
  // ---------------------------------------------------------------------------
  // PM2 deploy framework (pm2 deploy production) optional. Phase 1 BETA dùng
  // SSM SendCommand triggered từ GitHub Actions (kế thừa pattern Wave 65).
  //
  // Manual reload workflow:
  //   1. CI build .next/standalone artifact qua `pnpm build` trong runner
  //   2. CI upload artifact tar.gz → S3 hoặc EC2 SSM via SCP
  //   3. SSM command trên EC2:
  //        cd /var/www/kitehub-frontend && tar -xzf /tmp/build.tar.gz
  //        # rsync .next/static + public (standalone không tự copy)
  //        rsync -a /tmp/extracted/.next/static/ .next/standalone/.next/static/
  //        rsync -a /tmp/extracted/public/ .next/standalone/public/
  //        pm2 reload pm2-ecosystem.config.js --update-env --only kitehub-frontend
  //   4. Verify: pm2 logs kitehub-frontend --lines 50 + curl healthz
  //
  // --update-env: PM2 re-read env vars từ ecosystem.config.js (vd khi đổi
  // NODE_OPTIONS heap size). Không có flag này thì env stays cached.
  //
  // --only <name>: reload chỉ 1 app, app còn lại tiếp tục serve traffic.
  // Zero-downtime cho user trên domain kia.

  deploy: {
    production: {
      // PM2 deploy framework placeholder. Phase 1 BETA KHÔNG dùng (SSM-based).
      // Giữ skeleton để Phase 1.5 evaluate khi tenant cohort lớn dần.
      user: 'ec2-user',
      host: 'kc-app.internal',  // SSM tunnel hoặc bastion sau Phase 2 K8s
      ref: 'origin/main',
      repo: 'git@github.com:VictorAurelius/2026-Kite-Class-Platform.git',
      path: '/var/www',

      // post-deploy: chạy sau khi git pull thành công
      'post-deploy':
        'pnpm install --frozen-lockfile && ' +
        'pnpm -F kitehub-frontend build && ' +
        'pnpm -F kiteclass-frontend build && ' +
        'pm2 reload pm2-ecosystem.config.js --update-env',
    },
  },
};
