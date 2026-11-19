# Panduan Operasional Observability (OpenTelemetry, Grafana & GlitchTip)

Dokumen ini merupakan panduan teknis dan operasional (*runbook*) resmi untuk mengonfigurasi, mengoperasikan, dan melakukan *troubleshooting* pada sistem monitoring terdistribusi dan pelaporan error di **PROMS API (Spring Boot)**.

---

## 1. Arsitektur & Topologi Aliran Data

Sistem observability PROMS API menggunakan pendekatan terpusat (*unified observability*) berbasis standar industri cloud-native (CNCF):

```
┌────────────────────────────────────────────────────────────────────────┐
│                        PROMS API (Spring Boot)                         │
│                                                                        │
│   ┌────────────────────────────────┐   ┌───────────────────────────┐   │
│   │   Micrometer Tracing (OTel)    │   │        Logback MDC        │   │
│   │   W3C Context (traceparent)    │──▶│   [traceId] [spanId]      │   │
│   └───────────────┬────────────────┘   └─────────────┬─────────────┘   │
└───────────────────┼──────────────────────────────────┼─────────────────┘
                    │                                  │
           OTLP/HTTP POST                      Sentry Logback Appender
          (TLS Port 443)                      (TLS Port 443 + PII Filter)
                    │                                  │
                    ▼                                  ▼
      ┌───────────────────────────┐      ┌───────────────────────────┐
      │   otel.hyperdata.co.id    │      │ glitchtip.hyperdata.co.id │
      │   /v1/traces              │      │ /api/<project_id>/store/  │
      └─────────────┬─────────────┘      └─────────────┬─────────────┘
                    │                                  │
             Distributed Traces                 Error Incidents
             & Latency Waterfall                [Tag: trace_id]
                    │                                  │
                    ▼                                  │
      ┌───────────────────────────┐                    │
      │  grafana.hyperdata.co.id  │◀───────────────────┘
      │  (Tempo Trace Viewer)     │   Cari trace_id GlitchTip di Tempo
      └───────────────────────────┘
```

### Komponen Sistem:
1. **Backend Aplikasi**: Menghasilkan jejak span W3C (`traceparent`) otomatis untuk HTTP controller, database query, dan method service internal.
2. **OpenTelemetry Collector / Tempo (`https://otel.hyperdata.co.id`)**: Menerima data jejak (*traces*) via protokol OTLP/HTTP standar HTTPS tanpa port tambahan.
3. **Grafana Tempo (`https://grafana.hyperdata.co.id`)**: Menyediakan visualisasi grafik jejak, *flame graph*, dan analisa durasi bottleneck request.
4. **GlitchTip (`https://glitchtip.hyperdata.co.id`)**: Platform error tracking open-source yang 100% kompatibel dengan API Sentry, menangkap uncaught exception lengkap dengan stack trace, konteks server, dan tag korelasi `trace_id`.

---

## 2. Matriks Konfigurasi Environment Variables (`.env`)

Aplikasi PROMS API mengadopsi prinsip *12-Factor App*. Konfigurasi diatur sepenuhnya melalui environment variables tanpa perlu mengubah atau mengompilasi ulang kode sumber.

### A. Tabel Parameter

| Variabel | Tipe / Format | Default | Staging / Prod | Fungsi |
| :--- | :--- | :--- | :--- | :--- |
| `OTEL_TRACING_ENABLED` | Boolean (`true`/`false`) | `true` | `true` | Mengaktifkan/menonaktifkan instrumentasi tracing. |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | URL | `https://otel.hyperdata.co.id` | `https://otel.hyperdata.co.id` | Endpoint OTLP/HTTP base root domain untuk pengiriman traces (Spring Boot otomatis menambahkan path `/v1/traces`). |
| `OTEL_SAMPLING_PROBABILITY` | Float (`0.0` - `1.0`) | `1.0` | `1.0` (Staging)<br>`0.1`–`0.2` (Prod) | Rasio tracing. Nilai `1.0` merekam 100% request. Untuk production dengan beban tinggi, disarankan `0.1` (10%) untuk menghemat storage Tempo. |
| `OTEL_TIMEOUT_SECONDS` | Durasi | `2s` | `2s` | Batas waktu tunggu pengiriman batch OTLP ke collector. |
| `OTEL_METRICS_EXPORT_ENABLED` | Boolean (`true`/`false`) | `true` | `true` | Mengaktifkan pengiriman OTLP metrics langsung ke Grafana LGTM. |
| `OTEL_EXPORTER_OTLP_METRICS_ENDPOINT` | URL | `https://otel.hyperdata.co.id/v1/metrics` | `https://otel.hyperdata.co.id/v1/metrics` | Endpoint OTLP/HTTP untuk pengiriman metrics ke Prometheus internal LGTM. |
| `OTEL_METRICS_STEP` | Durasi | `1m` | `1m` | Interval pengiriman agregasi batch metrik ke server. |
| `INSTANCE_NAME` | String | `localhost:8080` | `proms-api-staging` / `proms-api-01` | Label identitas instance yang digunakan oleh filter dashboard Grafana (misal: 4701 / 19004). |
| `SENTRY_DSN` | URL | *(kosong)* | `https://<key>@glitchtip.hyperdata.co.id/<id>` | Kunci akses proyek di GlitchTip. Jika kosong, pengiriman error otomatis dinonaktifkan tanpa menimbulkan error. |
| `SENTRY_ENVIRONMENT` | String | `local` | `staging` / `production` | Label pembeda lingkungan di dashboard GlitchTip. |
| `SENTRY_TRACES_SAMPLE_RATE` | Float (`0.0` - `1.0`) | `1.0` | `1.0` (Staging)<br>`0.2` (Prod) | Rasio sampling transaksi di sisi Sentry client. |
| `MASKING_ENABLED` | Boolean | `true` | `true` | Mengaktifkan sensor data sensitif (password, token, NIK, kartu kredit) sesuai UU PDP No. 27/2022. |

### B. Contoh File `.env` Siap Pakai

#### 1. Untuk Lingkungan Staging:
```env
# Staging Environment
SPRING_PROFILES_ACTIVE=staging
PROJECT_NAME=proms-api
APP_VERSION=1.0.0-rc1

# OpenTelemetry (Traces & Metrics ke Grafana LGTM)
OTEL_TRACING_ENABLED=true
OTEL_SAMPLING_PROBABILITY=1.0
OTEL_EXPORTER_OTLP_ENDPOINT=https://otel.hyperdata.co.id
OTEL_TIMEOUT_SECONDS=2s
OTEL_METRICS_EXPORT_ENABLED=true
OTEL_EXPORTER_OTLP_METRICS_ENDPOINT=https://otel.hyperdata.co.id/v1/metrics
OTEL_METRICS_STEP=1m

# GlitchTip / Sentry
SENTRY_DSN=https://0123456789abcdef0123456789abcdef@glitchtip.hyperdata.co.id/2
SENTRY_ENVIRONMENT=staging
SENTRY_TRACES_SAMPLE_RATE=1.0

# Privacy & Masking
MASKING_ENABLED=true
```

#### 2. Untuk Lingkungan Production:
```env
# Production Environment
SPRING_PROFILES_ACTIVE=production
PROJECT_NAME=proms-api
APP_VERSION=1.0.0

# OpenTelemetry (Traces 10% sampling & Metrics 1m ke Grafana LGTM)
OTEL_TRACING_ENABLED=true
OTEL_SAMPLING_PROBABILITY=0.1
OTEL_EXPORTER_OTLP_ENDPOINT=https://otel.hyperdata.co.id
OTEL_TIMEOUT_SECONDS=2s
OTEL_METRICS_EXPORT_ENABLED=true
OTEL_EXPORTER_OTLP_METRICS_ENDPOINT=https://otel.hyperdata.co.id/v1/metrics
OTEL_METRICS_STEP=1m

# GlitchTip / Sentry
SENTRY_DSN=https://0123456789abcdef0123456789abcdef@glitchtip.hyperdata.co.id/1
SENTRY_ENVIRONMENT=production
SENTRY_TRACES_SAMPLE_RATE=0.2

# Privacy & Masking
MASKING_ENABLED=true
```

---

## 3. Langkah Setup di GlitchTip (`glitchtip.hyperdata.co.id`)

Ikuti langkah-langkah berikut untuk mengonfigurasi proyek baru di GlitchTip:

### Langkah 1: Membuat Proyek Baru
1. Buka browser dan login ke **`https://glitchtip.hyperdata.co.id`**.
2. Masuk ke halaman **Organizations** $\to$ pilih organisasi yang relevan (misal: `Hyperdata`).
3. Pada navigasi kiri, klik **Projects** $\to$ klik tombol **New Project**.
4. Isi data proyek:
   - **Platform**: Pilih `Java` atau `Spring Boot`.
   - **Project Name**: Masukkan nama proyek, misalnya `proms-api-production` atau `proms-api-staging`.
5. Klik **Create Project**.

### Langkah 2: Mengambil DSN (Client Key)
1. Setelah proyek dibuat, buka menu **Settings** pada proyek tersebut.
2. Pilih tab **Client Keys (DSN)**.
3. Anda akan melihat URL DSN dengan format:
   ```text
   https://4a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p@glitchtip.hyperdata.co.id/3
   ```
4. Salin DSN tersebut ke variabel `SENTRY_DSN` di file `.env` server target.

### Langkah 3: Mengonfigurasi Notifikasi Alert (Opsional namun Disarankan)
1. Pada menu proyek GlitchTip, buka tab **Alerts**.
2. Buat rule baru untuk memberi tahu tim ketika terjadi exception:
   - **Condition**: *An event is first seen* (kejadian error baru pertama kali muncul).
   - **Action**: Pilih integrasi notifikasi (Email tim, Webhook Slack, Discord, atau Telegram).
3. Simpan rule alert.

---

## 4. Konfigurasi Grafana LGTM (`grafana.hyperdata.co.id`)

Infrastruktur monitoring Anda menggunakan container all-in-one resmi **`grafana/otel-lgtm`** (Loki, Grafana, Tempo, Metrics/Prometheus) dengan OTel Collector bawaan pada port `4317` (gRPC) dan `4318` (HTTP):

```yaml
services:
  otel-lgtm:
    image: docker.io/grafana/otel-lgtm:latest
    container_name: grafana-lgtm
    restart: unless-stopped
    ports:
      - "3002:3000"   # Web UI Grafana Dashboard
      - "4317:4317"   # OpenTelemetry OTLP gRPC Receiver
      - "4318:4318"   # OpenTelemetry OTLP HTTP Receiver
    volumes:
      - ./lgtm-data:/data
```

### Keunggulan Arsitektur OTel-LGTM (Zero Config di Server):
1. **Tidak Perlu Setup Scraper**: OTel Collector bawaan di port `4318` otomatis mendistribusikan sinyal ke engine yang tepat:
   - Request ke `https://otel.hyperdata.co.id/v1/traces` $\to$ otomatis masuk ke **Tempo**.
   - Request ke `https://otel.hyperdata.co.id/v1/metrics` $\to$ otomatis masuk ke **Prometheus internal LGTM**.
2. **Datasource Pre-Configured**: Datasource `Tempo` dan `Prometheus` di Grafana Anda (`grafana.hyperdata.co.id`) sudah otomatis terdaftar sejak kontainer pertama kali dijalankan.

### Langkah 1: Melihat Metrik di Grafana Explore
1. Buka browser ke **`https://grafana.hyperdata.co.id`** (atau port `:3002`).
2. Masuk ke menu **Explore** (ikon kompas pada navigasi kiri).
3. Pilih datasource **`Prometheus`**.
4. Pada kolom kueri (Metrics Browser), ketik salah satu metrik berikut:
   - `jvm_memory_used_bytes` (Penggunaan memory heap/non-heap JVM)
   - `http_server_requests_seconds_count` (Throughput total request API)
   - `hikaricp_connections_active` (Jumlah koneksi database aktif)
5. Klik **Run query** untuk melihat grafik metrik secara real-time.

### Langkah 2: Mengimpor Dashboard Grafana Siap Pakai
Untuk memantau performa aplikasi secara visual tanpa perlu membuat dashboard manual:
1. Buka Grafana $\to$ **Dashboards** $\to$ klik tombol **New** $\to$ pilih **Import**.
2. Masukkan ID dashboard resmi komunitas:
   - **`4701`** (JVM Micrometer: CPU, Memory Heap/Non-Heap, Garbage Collection, Threads).
   - **`12856`** (Spring Boot 3/4 System Statistics: HTTP Latency p95/p99, Error rate, Connection pool).
3. Pilih datasource target: **`Prometheus`**.
4. Klik **Import**. Dashboard visual interaktif langsung aktif seketika.

### Langkah 3: Konfigurasi Trace-to-Logs (Korelasi Tempo & Loki)
Di dalam `otel-lgtm`, Anda dapat menghubungkan trace dengan log di Loki:
1. Buka menu **Administration** $\to$ **Data sources** $\to$ klik datasource **Tempo**.
2. Scroll ke bagian **Trace to logs**:
   - **Data source**: Pilih `Loki`.
   - **Filter by Trace ID**: Aktifkan (`true`).
   - **Query / Tags**: Petakan tag `service.name` dan `environment`.
3. Klik **Save & test**.

---

## 5. Alur Investigasi Insiden (Korelasi GlitchTip $\leftrightarrow$ Grafana)

Ketika sebuah insiden atau error dilaporkan oleh pengguna:

```
┌──────────────────────────────────────────────┐
│  1. Buka GlitchTip                           │
│     Lihat Issue -> Detail -> Tags: trace_id  │
│     Contoh: "e7c24f6a1d8b9e0f2c4a6e8b0d1a3c5e"│
└──────────────────────┬───────────────────────┘
                       │ Salin (Copy) trace_id
                       ▼
┌──────────────────────────────────────────────┐
│  2. Buka Grafana Explore                     │
│     Pilih Datasource: "Tempo"                │
│     Query type: "Search by Trace ID"         │
└──────────────────────┬───────────────────────┘
                       │ Tempel (Paste) trace_id
                       ▼
┌──────────────────────────────────────────────┐
│  3. Analisis Jejak (Waterfall)               │
│     - Lihat rentang waktu per method/span    │
│     - Periksa query database yang lambat     │
│     - Identifikasi titik kegagalan transaksi │
└──────────────────────────────────────────────┘
```

1. **Buka GlitchTip**: Buka daftar **Issues** di `https://glitchtip.hyperdata.co.id`.
2. **Ambil `trace_id`**: Klik insiden yang terjadi, scroll ke bagian **Tags**. Cari tag bernama `trace_id`.
3. **Buka Grafana Explore**: Buka `https://grafana.hyperdata.co.id/explore` dan pilih datasource **Tempo**.
4. **Pencarian Jejak**: Masukkan nilai `trace_id` tersebut ke kolom input pencarian.
5. **Analisis Akar Masalah**: Grafana akan langsung menampilkan *waterfall view* yang memetakan seluruh alur eksekusi dari pintu gerbang controller hingga query database PostgreSQL/HikariCP.

---

## 6. Prosedur Uji Coba & Verifikasi Operasional (Verification Runbook)

Sebelum merilis konfigurasi ke staging atau production, lakukan pengujian berikut:

### Skenario 1: Uji Konektivitas Jaringan dari Server Host
Jalankan perintah ini di dalam server atau kontainer tempat PROMS API berjalan:

```bash
# 1. Uji koneksi ke endpoint OTLP
curl -I -s --connect-timeout 5 https://otel.hyperdata.co.id/v1/traces | head -n 1

# 2. Uji koneksi ke GlitchTip
curl -I -s --connect-timeout 5 https://glitchtip.hyperdata.co.id | head -n 1
```
*Kriteria Lolos*: Kedua perintah merespons dengan HTTP status code (misal `200`, `405`, atau `400`), menandakan DNS teresolusi dan TLS handshake berhasil.

### Skenario 2: Uji Tracing Normal & Header `X-Trace-Id`
1. Lakukan request ke salah satu endpoint API PROMS:
   ```bash
   curl -i https://<host-proms-api>/actuator/health
   ```
2. Periksa header respons HTTP:
   ```text
   HTTP/1.1 200 OK
   Content-Type: application/vnd.spring-boot.actuator.v3+json
   X-Trace-Id: 4bf92f3577b34da6a3ce929d0e0e4736
   ```
3. Salin nilai `X-Trace-Id`.
4. Buka Grafana Tempo $\to$ Masukkan ID tersebut di kolom pencarian.
5. *Kriteria Lolos*: Span `GET /actuator/health` terlihat di Grafana Tempo.

### Skenario 3: Uji Simulasi Error & Pelaporan ke GlitchTip
1. Lakukan request yang memicu error (atau buat pemanggilan endpoint yang tidak valid):
   ```bash
   curl -i https://<host-proms-api>/api/v1/non-existent-trigger-500
   ```
2. Buka dashboard GlitchTip:
   - Pastikan issue baru muncul di daftar.
   - Pastikan stack trace Java tertangkap secara utuh.
   - Pastikan tag `trace_id` tersedia.
3. *Kriteria Lolos*: Error tercatat di GlitchTip dalam waktu kurang dari 5 detik.

### Skenario 4: Uji Endpoint Metrik Prometheus (`/actuator/prometheus`)
1. Lakukan request langsung ke endpoint Prometheus:
   ```bash
   curl -s https://<host-proms-api>/actuator/prometheus | head -n 30
   ```
2. Periksa output respons:
   ```text
   # HELP jvm_memory_used_bytes The amount of used memory
   # TYPE jvm_memory_used_bytes gauge
   jvm_memory_used_bytes{area="heap",id="G1 Survivor Space",} ...
   # HELP http_server_requests_seconds Duration of HTTP server requests
   # TYPE http_server_requests_seconds summary
   http_server_requests_seconds_count{error="none",exception="none",method="GET",outcome="SUCCESS",status="200",uri="/actuator/health",} 1.0
   ```
3. *Kriteria Lolos*: Endpoint merespons dengan HTTP `200 OK` dan konten teks standar OpenMetrics/Prometheus memuat metrik `jvm_*`, `http_server_requests_*`, dan `hikaricp_*`.

---

## 7. Troubleshooting & FAQ Operasional

### 1. Traces tidak muncul di Grafana Tempo
- **Penyebab A: Sampling Rate bernilai 0**.
  - *Solusi*: Periksa apakah `OTEL_SAMPLING_PROBABILITY` diatur ke `0.0`. Ubah menjadi `1.0` untuk pengujian.
- **Penyebab B: Kegagalan TLS / Sertifikat SSL**.
  - *Solusi*: Pastikan domain `otel.hyperdata.co.id` menggunakan sertifikat SSL yang valid (Let's Encrypt / CA resmi) agar tidak ditolak oleh Java Truststore default (`cacerts`).
- **Penyebab C: Format Endpoint Salah**.
  - *Solusi*: Pastikan endpoint bernilai `https://otel.hyperdata.co.id/v1/traces` (lengkap dengan `/v1/traces` untuk OTLP HTTP receiver).

### 2. Error tidak terkirim ke GlitchTip
- **Penyebab A: DSN Kosong atau Format Rusak**.
  - *Solusi*: Pastikan variabel `SENTRY_DSN` terisi dengan benar tanpa spasi atau kutip berlebih.
- **Penyebab B: Level Log di bawah ERROR**.
  - *Solusi*: Sentry Logback Appender hanya menangkap log berlevel `ERROR` dan `WARN`. Exception yang ditangkap oleh try-catch lokal tanpa logging `log.error(...)` tidak akan dikirim.
- **Penyebab C: Blocked oleh Egress Firewall**.
  - *Solusi*: Pastikan server aplikasi diizinkan membuka koneksi keluar (*outbound*) port 443 ke IP `glitchtip.hyperdata.co.id`.

### 3. Jaminan Perlindungan Data Pribadi (UU PDP No. 27/2022)
Aplikasi PROMS API memiliki filter sanitasi otomatis bawaan:
- Header `Authorization`, `Cookie`, dan `Set-Cookie` otomatis dihapus sebelum payload dikirim ke GlitchTip.
- Nilai sensitif seperti `password`, `token`, `secret`, `nik`, dan `credit_card` otomatis digantikan dengan label `[MASKED]`.
- Pengaturan `sentry.send-default-pii=false` dipertahankan aktif di level kode.
