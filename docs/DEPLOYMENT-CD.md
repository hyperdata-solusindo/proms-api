# Panduan Setup CD (Continuous Deployment)

Dokumen ini menjelaskan cara mengonfigurasi dan menggunakan pipeline CD (Continuous Deployment) pada repositori `hyperdata-solusindo/proms-api-java`.

Pipeline didefinisikan pada [`.github/workflows/cd.yml`](../.github/workflows/cd.yml).

---

## 1. Konsep Deployment

Pipeline CD ini menggunakan **Pola Label-Based (Branch-Agnostic)** untuk lingkungan Staging dan **Tag-Based** untuk Production:

```
[Developer PR: feature/xyz]
       │
   Beri Label 'deploy:staging-1'
       ▼
[Build & Push ke ghcr.io]
       ▼
[SSH Rollout ke VPS Staging-1]
       ▼
[Actuator Health Check /health/liveness]
       ▼
[Bot Comment di PR: Status UP & Info Image]
```

### Keuntungan:
1. **Branch-Agnostic**: Fitur pada branch apa pun bisa langsung diuji di server Staging tanpa perlu di-merge ke branch `dev`.
2. **Multi-Staging Ready**: Mendukung banyak slot staging (`staging-1`, `staging-2`, dst.) hanya dengan mengganti label PR.
3. **Audit & Safety**: Production diisolasi menggunakan Git Tag release (`v*.*.*`) atau approval manual.

---

## 2. Setup GitHub Environments & Secrets

Agar pipeline dapat terhubung ke VPS masing-masing environment, buat **GitHub Environments** di repository:
Buka **GitHub Repository** > **Settings** > **Environments**.

### A. Environment `staging-1`
Tambahkan Environment bernama `staging-1` dengan Secrets & Variables berikut:

#### Secrets:
- `VPS_HOST`: IP Address atau domain VPS Staging 1 (contoh: `103.x.x.x` atau `staging1.yourdomain.com`)
- `VPS_USER`: Username SSH di VPS (contoh: `ubuntu` atau `deployer`)
- `VPS_SSH_KEY`: Private Key SSH (OpenSSH format) untuk login ke VPS
- `VPS_SSH_PORT`: (Opsional, default: `22`) Port SSH VPS

#### Variables:
- `VPS_DEPLOY_DIR`: Direktori aplikasi di VPS (contoh: `/opt/proms/api-staging-1`)
- `VPS_HOST_PORT`: Port host yang di-mapping ke container (contoh: `8080`)

### B. Environment `staging-2` (Jika ada VPS kedua)
Lakukan hal yang sama untuk environment `staging-2` dengan IP / Port masing-masing.

### C. Environment `production`
Tambahkan Environment bernama `production` dengan IP VPS Production, serta aktifkan opsi **"Required reviewers"** di GitHub Environment Settings agar setiap deployment ke Production memerlukan persetujuan dari Tech Lead/Admin.

---

## 3. Persiapan di Sisi Server (VPS)

Di server VPS target, siapkan direktori aplikasi dan file `.env`:

```bash
# 1. Buat folder aplikasi
sudo mkdir -p /opt/proms/api-staging-1
cd /opt/proms/api-staging-1

# 2. Buat file .env untuk kredensial database dan runtime
cat << 'EOF' > .env
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=staging
DB_HOST=postgres
DB_PORT=5432
DB_NAME=proms_staging
DB_USER=postgres
DB_PASSWORD=your_secure_password
LIQUIBASE_ENABLED=true
EOF

# 3. Buat docker network jika belum ada
docker network create proms-network || true
```

---

## 4. Cara Penggunaan Sehari-hari

### Menguji Branch Fitur di Staging
1. Buka Pull Request di GitHub.
2. Tambahkan label: **`deploy:staging-1`** (atau `deploy:staging-2`).
3. GitHub Actions otomatis membangun image ke `ghcr.io`, mendeploy ke VPS, dan memvalidasi `curl /actuator/health/liveness`.
4. Bot GitHub akan memposting status deployment di PR.
5. Jika ada commit baru (`git push`), Staging otomatis ter-update selama label masih menempel.
6. Setelah pengujian selesai, **copot label** untuk menandai slot siap dipakai tim lain.

### Deploy ke Production
Buat release tag pada git:
```bash
git checkout main
git pull origin main
git tag v1.0.0
git push origin v1.0.0
```
Pipeline otomatis mem-build image release dan melakukan deployment ke Production setelah disetujui (jika approval diaktifkan).
