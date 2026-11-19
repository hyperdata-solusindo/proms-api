# Rule 15: CI/CD, Container Hardening & Deployment Standards

## 1. Staging vs Production Deployment Standards
Deployment aplikasi dipisahkan secara tegas antara lingkungan Staging dan Production:

### A. Staging Environment (Fast Branch-Agnostic Slots)
- **Mekanisme**: Menggunakan sistem slot dinamis berbasis label Pull Request (`deploy:staging-1` atau `deploy:staging-2`).
- **Toleransi**: Jeda restart singkat (5–10 detik) diperbolehkan.
- **Workflow**: Developer tidak perlu me-merge branch fitur ke `dev` untuk menguji aplikasi di server Staging.

### B. Production Environment (Near-Zero Downtime Nginx Blue/Green)
- **Mekanisme**: Rilis berbasis Git Tag semantic versioning (`v*.*.*`) dengan environment protection (memerlukan persetujuan review di GitHub).
- **Zero Downtime**: Menggunakan Nginx reverse proxy dengan pola **Blue/Green switching**:
  - Slot Blue: Port `8081`
  - Slot Green: Port `8082`
- **Alur Orkestrasi**:
  1. Kontainer versi baru dinyalakan pada slot yang sedang idle.
  2. Sistem memverifikasi kesehatan aplikasi via Actuator probe (`/actuator/health/liveness`).
  3. Nginx upstream dialihkan ke slot baru dan di-reload secara graceful (`nginx -s reload`).
  4. Kontainer pada slot lama dimatikan setelah jeda *in-flight traffic* (10 detik).

---

## 2. Dynamic Scaling & Database Migration Separation
Untuk mendukung *horizontal dynamic scaling* (multi-replica container):
- **Container Server**: Environment variable `LIQUIBASE_ENABLED` pada container aplikasi utama **wajib `false`** untuk mencegah race condition atau lock table saat banyak container booting bersamaan.
- **Dedicated Runner**: Migrasi Liquibase (`db:migrate`) wajib dijalankan sebagai **Dedicated One-Shot Job/Container** yang terisolasi sebelum container aplikasi versi baru di-deploy.

---

## 3. Container Security & Vulnerability Gates (Trivy in CD)
Sistem keamanan Trivy dijalankan **secara eksklusif pada pipeline CD ([`.github/workflows/cd.yml`](../../.github/workflows/cd.yml)) saat proses deployment dipicu**:
- **Trigger**: Hanya berjalan ketika ada event deployment (misal: label PR `deploy:staging-*` atau rilis tag production).
- **Inspeksi Container**: Memindai Docker image sebelum di-push ke GitHub Container Registry (`ghcr.io`).
- **Blocking Gate**: Memblokir deployment jika ditemukan kerentanan dengan tingkat keparahan **`CRITICAL`** yang sudah memiliki patch resmi dari vendor (`ignore-unfixed: true`).
- **CI Isolation**: Pipeline CI harian (`ci.yml`) tetap fokus pada kecepatan eksekusi Java verification dan AI review tanpa overhead container scanning.

---

## 4. Container Hardening Standards (`docker/Dockerfile`)
Setiap image kontainer aplikasi wajib memenuhi kriteria berikut:
1. **Multi-Stage Build**: Memisahkan stage kompilasi (JDK 21) dari stage runtime (JRE 21) untuk meminimalkan ukuran image.
2. **Non-Root Execution**: Aplikasi wajib dijalankan menggunakan user dan group non-root (`appuser:appgroup`). Dilarang menjalankan aplikasi Java sebagai user `root`.
3. **Container Healthcheck**: Dockerfile wajib mendeklarasikan directive `HEALTHCHECK` yang memantau Spring Boot Actuator probe (`curl -f http://localhost:8080/actuator/health/liveness`).
4. **JVM Memory Tuning**: JVM options wajib menyertakan flag container cgroup awareness (misal: `-XX:MaxRAMPercentage=75.0` dan `-XX:+ExitOnOutOfMemoryError`).

---

## 5. Pipeline Triggers & Job Guards Architecture
- **CI Pipeline (`ci.yml`)**:
  - Dibatasi secara eksklusif pada event `pull_request` (target branch `dev` dan `main`). Trigger `push to main` ditiadakan agar tidak terjadi eksekusi build redundan setelah merge PR.
  - Memiliki 2 job utama: `verify` (Java checks + ArchUnit + Redis service container) dan `ai-review` (evaluasi otomatis The 16 Golden Rules via Gemini).
- **CD Pipeline (`cd.yml`)**:
  - Event `pull_request` dibatasi secara ketat hanya pada `types: [labeled]`. Event `synchronize` ditiadakan agar push commit reguler ke PR tidak memicu workflow deployment kosong.
  - Job `resolve-intent` diproteksi guard `startsWith(github.event.label.name, 'deploy:staging-')`, sehingga penambahan label non-deploy (seperti `bug`, `wip`, `enhancement`) langsung diabaikan tanpa menyalakan runner virtual machine.
