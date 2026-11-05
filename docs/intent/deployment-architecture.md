# Statement of Intent: Arsitektur Deployment & CI/CD Enterprise

Dokumen ini merekam hasil wawancara terstruktur (*interview-me*) mengenai arsitektur deployment jangka panjang untuk proyek enterprise `hyperdata-solusindo/proms-api-java`.

---

## 1. Desired Outcome
Membangun sistem CI/CD dan deployment hybrid yang membedakan perlakuan antara:
- **Staging**: Lingkungan cepat uji berbasis branch-agnostic slot menggunakan label Pull Request (`deploy:staging-1`, `deploy:staging-2`) dengan toleransi jeda restart singkat (5-10 detik).
- **Production**: Lingkungan stabil berstandar *near-zero downtime* menggunakan orkestrasi **Nginx Blue/Green deployment**.

---

## 2. Pilar Arsitektur Inti

### A. Pemisahan Migrasi Database (Separation of Concerns for Liquibase)
- Aplikasi dirancang untuk mendukung *horizontal/dynamic scaling* (multi-replica container).
- **Aturan**: Container backend utama dilarang menjalankan migrasi database saat booting (`LIQUIBASE_ENABLED=false`).
- Migrasi skema database dijalankan oleh **One-Shot Migration Runner/Job** khusus sebelum container aplikasi versi baru dinyalakan.

### B. Traffic Switching di Production (Nginx Blue/Green)
- Nginx bertindak sebagai front-facing reverse proxy di VPS Production.
- Terdapat dua slot port backend:
  - **Slot Blue**: Container berjalan di port `8081`.
  - **Slot Green**: Container berjalan di port `8082`.
- Saat rilis baru diajukan:
  1. Skrip deployment mendeteksi slot mana yang sedang idle (misal `green`).
  2. Container versi baru dinyalakan pada slot `green`.
  3. Skrip memvalidasi Actuator health check (`/actuator/health/liveness`) pada slot `green`.
  4. Jika sehat, Nginx upstream di-switch ke slot `green` dan di-reload (`nginx -s reload`) tanpa memutus koneksi aktif.
  5. Container pada slot lama (`blue`) dimatikan dengan aman (*graceful stop*).

### C. Quality Gates & Shift-Left Security
- **JaCoCo**: Menegakkan *code coverage threshold* pada level build Java (`./gradlew check`). File non-logika (DTO, Entity, Model, Config) dikecualikan.
- **Trivy (Ring 1 - Parallel Fast Scan)**: Memindai repository dari kebocoran secret atau password secara instan pada setiap PR/push.
- **Trivy (Ring 2 - Container Gate)**: Memindai Docker image sebelum di-push ke `ghcr.io` di pipeline CD. Memblokir rilis jika terdapat CVE berstatus `CRITICAL` dengan fix tersedia.

---

## 3. Batasan & Non-Goals (Out of Scope)
- Migrasi ke Kubernetes cluster (saat ini tetap fokus pada VPS Linux + Docker + Nginx).
- Replikasi multi-region geo-distributed database.
