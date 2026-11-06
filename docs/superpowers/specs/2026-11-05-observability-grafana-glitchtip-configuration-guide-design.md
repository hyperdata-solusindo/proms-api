# Design Specification: Panduan Konfigurasi Observability (OpenTelemetry ke Grafana & Sentry ke GlitchTip)

- **Date**: 2026-11-05
- **Status**: Draft (Under Review)
- **Target Systems**: 
  - Backend App: `proms-api-java` (Spring Boot 3/4)
  - Telemetry Ingestion: `https://otel.hyperdata.co.id` (OpenTelemetry OTLP/HTTP)
  - Visualization: `https://grafana.hyperdata.co.id` (Grafana & Tempo)
  - Error Tracking: `https://glitchtip.hyperdata.co.id` (GlitchTip / Sentry-compatible)
- **Scope**: Production & Staging Operational Configuration Guide (No Application Code Changes)

---

## 1. Ringkasan & Tujuan (Overview)

Dokumen ini adalah panduan operasional dan konfigurasi (*runbook*) lengkap untuk menghubungkan aplikasi backend **PROMS API** ke ekosistem monitoring dan error tracking internal Hyperdata:
1. **OpenTelemetry (OTel) Tracing** diekspor secara aman via HTTPS ke `https://otel.hyperdata.co.id` dan divisualisasikan melalui Grafana Tempo di `https://grafana.hyperdata.co.id`.
2. **Error & Exception Tracking** dikirim via Sentry SDK ke instance GlitchTip di `https://glitchtip.hyperdata.co.id`.
3. **Cross-Correlation**: Menghubungkan insiden error di GlitchTip dengan jejak latency (*trace waterfall*) di Grafana menggunakan `trace_id` yang konsisten.

---

## 2. Topologi & Alur Data (Data Flow Architecture)

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
      │  (Tempo Trace Viewer)     │   Manual/Direct Search via trace_id
      └───────────────────────────┘
```

---

## 3. Spesifikasi Konfigurasi Lingkungan (`.env` & Runtime)

Aplikasi telah dirancang *12-Factor App compliant*, di mana seluruh parameter monitoring dipetakan melalui Environment Variables tanpa perlu *rebuild* kode:

### A. Parameter OpenTelemetry (`otel.hyperdata.co.id`)
| Environment Variable | Nilai Rekomendasi (Staging / Production) | Keterangan |
| :--- | :--- | :--- |
| `OTEL_TRACING_ENABLED` | `true` | Mengaktifkan Micrometer Tracing bridge OTel |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `https://otel.hyperdata.co.id/v1/traces` | Endpoint ingestion OTLP via HTTPS (port 443 standar) |
| `OTEL_SAMPLING_PROBABILITY` | `1.0` (Staging) / `0.1`–`0.2` (Production) | Rasio jejak request yang direkam (100% di staging, 10-20% di production untuk efisiensi resource) |
| `OTEL_TIMEOUT_SECONDS` | `2s` | Timeout batas pengiriman batch traces ke collector |
| `PROJECT_NAME` | `proms-api` | Dipetakan otomatis ke OTel attribute `service.name` |
| `APP_VERSION` | `1.0.0` (atau tag commit Git) | Dipetakan ke OTel attribute `service.version` |
| `SPRING_PROFILES_ACTIVE` | `staging` / `production` | Dipetakan ke OTel attribute `deployment.environment` |

### B. Parameter GlitchTip / Sentry (`glitchtip.hyperdata.co.id`)
| Environment Variable | Nilai Rekomendasi (Staging / Production) | Keterangan |
| :--- | :--- | :--- |
| `SENTRY_DSN` | `https://<public-key>@glitchtip.hyperdata.co.id/<project-id>` | DSN yang didapatkan dari project GlitchTip |
| `SENTRY_ENVIRONMENT` | `${SPRING_PROFILES_ACTIVE}` (`staging` / `production`) | Label environment untuk memisahkan error dev, staging, dan prod |
| `SENTRY_TRACES_SAMPLE_RATE` | `1.0` (Staging) / `0.2` (Production) | Sampling rate untuk tracing Sentry |
| `MASKING_ENABLED` | `true` | Menjamin sensor data sensitif (UU PDP No. 27/2022) |

---

## 4. Panduan Setup GlitchTip (`glitchtip.hyperdata.co.id`)

### Langkah 1: Registrasi Proyek
1. Login ke web dashboard: `https://glitchtip.hyperdata.co.id`.
2. Buka menu **Organizations** $\to$ Pilih organisasi Hyperdata.
3. Klik **Projects** $\to$ **New Project**:
   - **Platform**: Pilih `Java / Spring Boot`.
   - **Project Name**: Masukkan `proms-api` (atau `proms-api-staging`).
4. Klik **Create Project**.

### Langkah 2: Mengambil DSN (Client Key)
1. Pada halaman project settings, navigasi ke tab **Client Keys (DSN)**.
2. Salin nilai DSN yang memiliki format:
   ```text
   https://0123456789abcdef0123456789abcdef@glitchtip.hyperdata.co.id/1
   ```
3. Masukkan nilai tersebut ke variabel `SENTRY_DSN` pada file `.env` server target.

### Langkah 3: Pengaturan Notifikasi & Alert
1. Buka tab **Alerts** pada project GlitchTip.
2. Tambahkan rule alert baru:
   - **Condition**: *An event is first seen* atau *An issue changes state from resolved to unresolved*.
   - **Action**: Kirim notifikasi ke Webhook (Discord/Telegram/Slack) atau Email tim operasional.

---

## 5. Panduan Setup Grafana Tempo & Korelasi (`grafana.hyperdata.co.id`)

### Langkah 1: Verifikasi Datasource Tempo
1. Login ke `https://grafana.hyperdata.co.id` dengan akun Administrator / Editor.
2. Buka menu **Connections** $\to$ **Data sources**.
3. Pastikan datasource bertipe **Tempo** sudah aktif (misal dengan nama `Tempo`).
4. Pada konfigurasi Datasource Tempo, pastikan:
   - **Type**: `Tempo`
   - **URL**: Mengarah ke backend internal Tempo.

### Langkah 2: Konfigurasi Trace-to-Logs (Opsional jika Loki aktif)
1. Di halaman setting Datasource Tempo, scroll ke bagian **Trace to logs**:
   - **Data source**: Pilih datasource Loki.
   - **Tags**: `service.name`, `environment`.
   - **Filter by Trace ID**: Centang `Enable` (menggunakan query `traceId="${__trace.id}"`).

### Langkah 3: Cara Kerja Investigasi Insiden (Cross-Correlation)
Saat terjadi issue/error di aplikasi:
1. Developer membuka **GlitchTip** (`https://glitchtip.hyperdata.co.id`) dan melihat insiden error baru.
2. Di detail insiden, lihat bagian **Tags** $\to$ cari tag **`trace_id`** (contoh: `4bf92f3577b34da6a3ce929d0e0e4736`).
3. Buka **Grafana** (`https://grafana.hyperdata.co.id`) $\to$ **Explore** $\to$ pilih datasource **Tempo**.
4. Masukkan ID tersebut pada kolom **TraceQL** atau **Search by Trace ID**:
   ```text
   4bf92f3577b34da6a3ce929d0e0e4736
   ```
5. Grafana akan langsung menampilkan *waterfall diagram*, durasi query database Hibernate/Postgres, dan span yang memicu error.

---

## 6. Prosedur Uji Coba & Validasi Operasional (Verification Runbook)

### Skenario 1: Verifikasi Konektivitas OpenTelemetry
1. Jalankan aplikasi atau container PROMS API.
2. Lakukan request HTTP ke salah satu endpoint publik (misal healthcheck):
   ```bash
   curl -I https://<host-proms-api>/actuator/health
   ```
3. Periksa header respons HTTP: pastikan header `X-Trace-Id` hadir (misal: `X-Trace-Id: 65421a...`).
4. Buka Grafana Tempo $\to$ Masukkan `X-Trace-Id` tersebut ke kolom pencarian Tempo.
5. **Kriteria Lolos (PASS)**: Span `GET /actuator/health` muncul di Tempo dengan atribut `service.name: proms-api`.

### Skenario 2: Verifikasi Pelaporan Error ke GlitchTip
1. Pemicuan error simulasi via endpoint internal atau uji coba unhandled exception (misal request ke resource yang memicu `500 Internal Server Error`).
2. Periksa log konsol server:
   ```text
   2026-11-05 20:30:00 [proms-api] [traceId=a1b2c3d4e5f6g7h8] ERROR c.h.p.GlobalExceptionHandler - Unhandled exception
   ```
3. Buka GlitchTip dashboard (`https://glitchtip.hyperdata.co.id`):
   - Insiden baru muncul di daftar Issues.
   - Stack trace terlihat lengkap.
   - Tag `trace_id` bernilai sama (`a1b2c3d4e5f6g7h8`).
4. **Kriteria Lolos (PASS)**: Event tercatat di GlitchTip tanpa membocorkan data sensitif (PII/Password tersanitasi).

---

## 7. Troubleshooting & FAQ Operasional

1. **Traces tidak muncul di Grafana Tempo**:
   - Periksa konektivitas jaringan dari host aplikasi ke `otel.hyperdata.co.id`:
     ```bash
     curl -vk https://otel.hyperdata.co.id/v1/traces
     ```
   - Pastikan SSL/TLS certificate di `otel.hyperdata.co.id` valid dan trusted oleh Java truststore (JDK 21 default CA certificates).
   - Pastikan `OTEL_TRACING_ENABLED=true` dan `OTEL_SAMPLING_PROBABILITY` tidak bernilai `0.0`.

2. **Error tidak masuk ke GlitchTip**:
   - Periksa keabsahan DSN di `.env`: pastikan tidak ada karakter terpotong atau spasi tambahan.
   - Uji koneksi dari host aplikasi ke GlitchTip:
     ```bash
     curl -I https://glitchtip.hyperdata.co.id
     ```
   - Periksa log aplikasi saat startup apakah ada peringatan `Invalid DSN` dari Sentry client.

---

## 8. Checklist Acceptance Criteria

- [ ] Seluruh environment variable untuk OTel dan Sentry terdokumentasi jelas dengan nilai default dan contoh production.
- [ ] Panduan registrasi project dan pengambilan DSN di GlitchTip lengkap dan mudah diikuti.
- [ ] Mekanisme korelasi `trace_id` antara GlitchTip dan Grafana Tempo terdeskripsikan secara konkret.
- [ ] Prosedur uji coba end-to-end (skenario sukses dan skenario error) memiliki langkah verifikasi yang dapat diaudit.
