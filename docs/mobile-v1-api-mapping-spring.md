# PROMS Mobile v1: Pemetaan Kontrak API untuk Backend Java Spring

- **Tanggal:** 28 Okt 26
- **Status:** Draft untuk review (tim mobile, tim backend Spring, PO)
- **Pembaca:** developer backend Spring (implementasi) dan developer mobile React Native (konsumen)
- **Sumber:** `docs/standards/proms-api.yaml` (kontrak, sumber kebenaran), `routes/api.php` + `app/Domain/*` (implementasi Laravel),
  `database/migrations/*` (skema), brief `docs/superpowers/specs/2026-10-27-mobile-app-v1-design-brief-design.md` (§4 layar)

---

## 1. Konteks & asumsi

```
 App mobile (RN/Expo) ──HTTPS──▶  Backend Spring  ─┐
                                    (/api/v1)      │
 Web (Vue/Blade) ───────HTTPS──▶  Backend Laravel ─┼──▶ PostgreSQL 16 yang SAMA
                                    (/api/v1)      │    (schema proms + platform, RLS)
                                    Reverb (WS) ◀──┘    + worker outbox:dispatch (Laravel)
```

**Arah produk:** Java Spring akan menjadi **satu-satunya backend PROMS**. Mobile dikerjakan duluan; web dipindah ke Java
belakangan. Selama masa transisi, Laravel (web) dan Spring (mobile) berjalan bersama di database yang sama.

Konsekuensinya, Spring **bukan BFF khusus mobile**. Spring adalah backend utama yang sedang dibangun bertahap, dan endpoint
mobile hanyalah irisan pertama. Setiap modul (Task, Inbox, Board, …) ditulis lengkap sesuai kontrak dan siap dipakai web nanti.

Aturan masa transisi (fase 1, lihat §11 untuk fase berikutnya):

1. Spring melayani app mobile. Web tetap memakai Laravel sampai modulnya dipindah.
2. **Laravel tetap pemilik skema selama transisi.** Perubahan tabel dilakukan lewat migrasi Laravel, bukan Flyway/Liquibase/Hibernate
   di Spring. Kepemilikan skema pindah ke Java di fase 3 (§11).
3. **Worker outbox sementara tetap di Laravel.** Spring cukup menulis baris ke `proms.outbox_events`, lalu listener Laravel
   (notifikasi, poin, push) memprosesnya. Writer outbox di Spring dibuat sedemikian rupa sehingga dispatcher Java bisa menggantikan
   worker Laravel tanpa mengubah kode use case.
4. Spring mengikuti **kontrak `proms-api.yaml`**, bukan perilaku Laravel. Header kontrak menyatakan ini:
   *"satu kontrak untuk backend Java dan Laravel"*. Implementasi Laravel masih punya banyak penyimpangan dari kontrak
   (snake_case, path berbeda; lihat §7). Spring tidak boleh meniru penyimpangan itu. Ini makin penting karena frontend web
   nantinya juga akan berbicara dengan Spring.

---

## 2. Aturan wajib Spring di database bersama

Ini bagian terpenting. Kalau ada satu yang dilanggar, data bisa bocor antar tenant atau web dan mobile tidak konsisten.

### 2.1 Koneksi & role database

| DataSource Spring | User login | Role grup | Dipakai untuk |
|---|---|---|---|
| `promsDataSource` | `proms_login` | `proms_app` | Semua tabel `proms.*`. Di `platform.*` hanya bisa baca view directory (`platform.v_directory_members`) dan insert audit log |
| `platformDataSource` | `platform_login` | `platform_app` | Validasi token, resolve tenant, keanggotaan (`platform.tenants`, `platform.tenant_members`, `platform.users`, `platform.personal_access_tokens`) |

- **Jangan pernah** memakai `proms_owner` (role migrasi) dari aplikasi.
- `spring.jpa.hibernate.ddl-auto=none` (atau `validate`). Flyway/Liquibase **nonaktif**.
- Nama tabel selalu lengkap dengan schema: `proms.tasks`, `platform.users`.

### 2.2 Konteks tenant (RLS, pola P-01)

RLS di setiap tabel ber-`tenant_id` membaca setting transaksi:

```sql
-- policy: using (tenant_id = platform.current_tenant())
select set_config('app.tenant_id',      :tenantId, true),
       set_config('app.user_id',        :userId,   true),
       set_config('app.workspace_role', :role,     true);
```

- Jalankan statement di atas **sebagai statement pertama di setiap transaksi** (parameter ke-3 `true` = berlaku lokal di
  transaksi, otomatis hilang saat commit/rollback, sehingga aman untuk HikariCP).
  Implementasi yang disarankan: aspect/`TransactionSynchronization` yang memasang konteks dari `TenantContext` request.
- Query di luar transaksi bertenant akan melihat **0 baris**. Ini disengaja (gagal aman). Jangan "memperbaikinya" dengan
  menambah `where tenant_id = ?` sebagai pengganti RLS. Filter manual hanya boleh sebagai tambahan.
- `platform.tenant_members` juga ber-RLS. Saat resolve tenant, pasang konteks sementara dengan role `member`, baru baca
  keanggotaan (lihat `app/Http/Middleware/ResolveTenant.php`).
- **Satu use case = satu transaksi** (`@Transactional` di service). Jangan memanggil HTTP, kirim email, atau push di dalam transaksi.

### 2.3 Autentikasi (token)

Token sekarang diterbitkan Laravel Sanctum dan disimpan di `platform.personal_access_tokens`. Agar user yang sama bisa
memakai web dan mobile, Spring memvalidasi token dengan format yang sama:

1. Header `Authorization: Bearer <id>|<plain>`.
2. Ambil baris `id`; cocokkan `token = sha256_hex(plain)` dengan perbandingan *constant-time*.
3. Pastikan `tokenable_type = 'App\Models\User'`, `expires_at` null atau di masa depan, dan `platform.users.is_active = true`.
4. Perbarui `last_used_at` (boleh di-*debounce*, mis. sekali per menit).
5. Gagal → `401 UNAUTHENTICATED`.

> **Perhatian:** `POST /auth/login` di Laravel (`LoginUser`) **hanya aktif di environment `local`/`testing`**
> (di luar itu `404 ENDPOINT_NOT_FOUND`). Kontrak merencanakan Keycloak OIDC. **Login produksi belum ada**, jadi ini
> keputusan terbuka #1 di §9. Token Sanctum juga tidak pernah kedaluwarsa (`sanctum.expiration = null`).
>
> Karena Java akan menjadi backend tunggal, validasi token Sanctum di atas sebaiknya hanya **jembatan sementara**.
> Login mobile disarankan langsung memakai Keycloak OIDC (sesuai kontrak). Kalau Keycloak belum siap, Spring boleh menerbitkan
> token format Sanctum, asalkan modul auth-nya terisolasi sehingga mudah diganti ke OIDC tanpa menyentuh modul lain.

### 2.4 Header tenant

`X-Tenant: <slug|uuid>` wajib di semua endpoint terautentikasi (Laravel juga menerima `X-Tenant-ID`). Urutan cek:

| Kondisi | Respons |
|---|---|
| Header tidak ada | `400 TENANT_REQUIRED` |
| Tenant tidak ada | `404 TENANT_NOT_FOUND` |
| Bukan anggota aktif | `403 TENANT_ACCESS_REVOKED` |
| Tenant `read_only`/`suspended` dan method bukan GET/HEAD | `403 TENANT_READ_ONLY` |

Setelah lolos: `TenantContext(tenantId, userId, workspaceRole, tenantTimezone)`.

### 2.5 Hak akses board (P-02)

Satu query, sama dengan `BoardPermission::of()`:

```sql
select bm.id, bm.role, bm.is_owner, b.status
from proms.board_members bm
join proms.boards b on b.id = bm.board_id and b.deleted_at is null
where bm.board_id = :boardId and bm.user_id = :userId
```

- Tidak ada baris → **`404 BOARD_NOT_FOUND`** (bukan 403, supaya keberadaan board tidak bocor).
- `b.status = 'archived'` dan request menulis → `409 BOARD_ARCHIVED`.
- Role project: `admin`, `pm`, `member`, `viewer`, `guest`. Ringkasan hak yang relevan untuk mobile:

| Aksi | admin/pm | owner (`is_owner`) | member | viewer/guest |
|---|---|---|---|---|---|
| Baca task | ✓ | ✓ | ✓ | ✓ (guest hanya `shared_with_client`) |
| Ubah field task / pindah kolom | ✓ | ✓ | ✓ | ✗ `403 FORBIDDEN_TASK_EDIT` / `FORBIDDEN_TASK_MOVE` |
| Buat task | ✓ | ✓ | ✗ `403 FORBIDDEN_TASK_CREATE` | ✗ |
| Pindah ke kolom Done | ✓ | sesuai `WorkflowGuard` | ✗ `403 BR-WF-07` | ✗ |

> Catatan: pada kode Laravel saat ini, **member tidak boleh membuat task** (`canCreateTask`). Quick add (layar 11) untuk
> member akan selalu ditolak. Ini perlu dikonfirmasi PO sebelum layar 11 dirilis (keputusan #5).

### 2.6 Versi baris & If-Match (P-08)

- `version` = `updated_at` dalam format UTC ISO-8601 dengan mikrodetik: `2026-10-27T03:15:22.123456Z`.
- `PATCH`/`DELETE` wajib header `If-Match: <version>`. Tidak ada header → `428 IF_MATCH_REQUIRED`.
- Update selalu dengan syarat versi:
  ```sql
  update proms.tasks set ... where id = :id and deleted_at is null and updated_at = :version::timestamptz
  ```
  0 baris → `409 STALE_VERSION` dengan baris terbaru di `error.current`.
- `updated_at` diisi **trigger** `<tabel>_touch`. Jangan set `updated_at` dari Java.

### 2.7 Idempotensi (P-07)

`POST` yang membuat data wajib header `Idempotency-Key` (8–128 karakter). Gunakan tabel yang sama
`proms.idempotency_keys` agar perilakunya identik dengan Laravel:

1. Transaksi A: hapus key milik user yang sudah `expires_at < now()`, lalu
   `insert ... (tenant_id, user_id, key, method, path, request_hash) on conflict do nothing`.
   `request_hash = sha256(method + ' ' + path + ' ' + rawBody)`.
2. Kalau insert gagal (key sudah ada):
   - hash beda → `422 IDEMPOTENCY_KEY_REUSED`
   - `status_code` masih null → `409 IDEMPOTENCY_IN_PROGRESS`
   - selain itu → kirim ulang `response_body` + `status_code` yang tersimpan, dengan header `Idempotent-Replayed: true`.
3. Jalankan use case (transaksi B).
4. Transaksi C: respons 2xx/4xx → simpan `status_code`, `response_body`. Respons 5xx → hapus key supaya bisa diulang.

Karena tabelnya sama, key yang dibuat dari Laravel dan Spring tidak akan bertabrakan selama `path` sama.

### 2.8 Hal yang dikerjakan database (jangan diulang di Java)

| Trigger / fungsi | Efek | Konsekuensi untuk Spring |
|---|---|---|
| `tasks_number` → `proms.assign_task_number()` | Mengisi `tasks.number` dari `boards.task_seq` | Insert **tanpa** kolom `number`, lalu `returning number` |
| `tasks_completed_at` → `proms.sync_completed_at()` | Mengisi/mengosongkan `completed_at` saat pindah ke/dari kolom `done` (BR-WF-14) | Jangan set `completed_at` manual |
| `<tabel>_touch` | `updated_at = now()` setiap update | Versi baru dibaca ulang setelah update |
| `platform.user_memberships(uuid[])` | Daftar workspace user | Dipakai untuk `GET /me` → `otherTenants` (layar 02) |

### 2.9 Data lain yang wajib dijaga

- **ID:** UUID v7 (`com.github.f4b6a3:uuid-creator` → `UuidCreator.getTimeOrderedEpoch()`), dibuat di aplikasi.
- **Soft delete manual:** `deleted_at = now(), deleted_by = :userId`. Semua query data aktif `where deleted_at is null`.
- **Jejak task (P-05):** setiap perubahan task → insert `proms.task_activities` **satu baris per field**
  (`action`, `field`, `old_value` jsonb, `new_value` jsonb). Ikuti format kolom yang sudah ditulis Laravel
  (`UpdateTask`, `MoveTask`, `StoreAttachment`). Layar komentar (10) menampilkan baris-baris ini.
- **Waktu:** simpan UTC. "Hari ini", "terlambat", dan batas standup memakai **zona waktu tenant** (`platform.tenants.timezone`).
  Respons memakai ISO UTC (`date-time`) atau `YYYY-MM-DD` (`date`). Format tampilan `27 Okt 26` (BR-UI-01) dikerjakan app.
- **Rahasia:** jangan me-*log* token, email, atau isi komentar/chat.

### 2.10 Outbox: efek samping lewat Laravel (P-04/P-06)

Spring menulis event ke `proms.outbox_events` **di transaksi yang sama** dengan perubahan data:

```sql
insert into proms.outbox_events (tenant_id, event_type, aggregate_id, payload, actor_id)
values (:tenantId, :eventType, :aggregateId, :payload::jsonb, :userId)
```

Scheduler Laravel `outbox:dispatch` (tiap 5 detik, `for update skip locked`) mengambil baris `dispatched_at is null`, lalu
menjalankan listener di `config/proms.php → events`. **Nama event dan bentuk payload harus persis sama** dengan yang ditulis Laravel:

| event_type | Payload (camelCase) | Listener Laravel |
|---|---|---|
| `TaskCreated` | `taskId, boardId, assigneeIds[]` | `NotifyAssignees` |
| `TaskUpdated` | `taskId, boardId, addedAssigneeIds[]` | `NotifyAssignees` |
| `TaskMoved` | `taskId, boardId, fromColumnId, toColumnId` | `NotifyStatusChange` |
| `TaskCompleted` | `taskId, boardId, completedAt, dueDate, assigneeIds[]` | `AwardTaskPoints` (BR-PT-05) |
| `TaskReopened` | `taskId, boardId, reopenedAt` | `ReversePointsOnReopen` (BR-PT-18) |
| `CommentAdded` | `commentId, taskId, boardId, boardKey, taskNumber, taskTitle, taskCreatedBy, commentText, …` (lihat `CreateComment.php` baris 133) | `NotifyMentions`, `NotifyCommentParticipants` |

Dengan pola ini, notifikasi Inbox, poin, dan (nanti) push native **otomatis jalan** untuk aksi dari mobile tanpa ditulis ulang di Spring.

### 2.11 Realtime (Reverb)

Broadcast WebSocket saat ini dipicu dari kode PHP (event `ChatMessageCreated`, `TaskCommentCreated`, dst.).
Penulisan lewat Spring **tidak akan terlihat live di web** kecuali Spring ikut mem-*publish*. Pilihan:

- **A (disarankan):** Spring memanggil Reverb lewat **Pusher HTTP API** (library `pusher-http-java`, app key/secret yang sama),
  dengan channel dan nama event yang sama, mis. `private-tenant.{tenantId}.chat.{channelId}`.
  Panggil **setelah commit** (`TransactionSynchronization.afterCommit`).
  Untuk jangka panjang, Reverb (server PHP) bisa diganti server WebSocket lain yang juga berprotokol Pusher (mis. Soketi).
  Klien web dan mobile tidak perlu diubah, dan Spring tetap memakai kode publish yang sama.
- **B:** biarkan web memuat ulang saat fokus/pull. Ini lebih murah, tetapi chat web tidak lagi realtime untuk pesan dari HP.

Notifikasi Inbox tetap realtime karena dibuat oleh listener Laravel.
App mobile subscribe ke Reverb untuk `private-tenant.{tenantId}.user.{userId}` (badge Inbox) dan channel chat yang sedang dibuka.
Endpoint otorisasi channel `POST /broadcasting/auth` perlu disediakan Spring, atau app diarahkan ke Laravel (keputusan #4).

### 2.12 Penyimpanan file

Lampiran task disimpan Laravel di **disk lokal** server (`Storage::disk('local')`, kolom `attachments.storage_key`), dan file
chat di disk `public`. Spring di server lain **tidak bisa membaca file ini**. Sebelum lampiran dan chat file di mobile berjalan,
penyimpanan harus dipindah ke object storage bersama (S3/MinIO) untuk kedua backend (keputusan #3).

---

## 3. Konvensi HTTP

| Hal | Aturan |
|---|---|
| Base URL | `https://{tenant}.proms.id/api/v1` (atau host khusus mobile, mis. `m-api`; keputusan #2) |
| Header wajib | `Authorization: Bearer …`, `X-Tenant: <slug>`, `Accept: application/json` |
| Header kondisional | `If-Match` (PATCH/DELETE), `Idempotency-Key` (POST pembuat data) |
| Header disarankan dari app | `X-Client: proms-mobile/<versi> (<ios|android>)` untuk log & dukungan |
| JSON | `camelCase`, UUID string, enum huruf kecil seperti di kontrak |
| Format respons | **Envelope standar** untuk semua respons, sukses maupun error (§3.2). `Content-Type: application/json` |
| Paging | `?limit=50&cursor=<opaque>` → daftar di `data`, cursor di `meta.nextCursor` (`null` = halaman terakhir). `limit` maks 200 |

### 3.2 Envelope respons standar

Semua endpoint (kecuali API-45, lihat catatannya) mengembalikan bentuk yang sama:

```json
{
  "statusCode": 200,
  "message": "Berhasil login",
  "data": { "token": "12|Xk9…", "user": { "id": "0192…", "fullName": "Fariz Keren" } }
}
```

| Field | Tipe | Aturan |
|---|---|---|
| `statusCode` | integer | **Selalu sama** dengan HTTP status. Server tidak pernah mengirim HTTP 200 untuk error |
| `message` | string | Kalimat singkat bahasa Indonesia, aman ditampilkan ke user (toast/sheet) |
| `data` | object / array / `null` | Isi respons. Objek tunggal = objek, daftar = array, tanpa isi = `null`. Skema isi = skema di §5 dan `proms-api.yaml` |
| `meta` | object | Hanya untuk daftar: `nextCursor`, ditambah angka ringkasan bila ada (mis. `unreadCount` di inbox, `summary` di boards) |
| `error` | object | Hanya untuk error (`statusCode` ≥ 400). Untuk sukses, field ini tidak dikirim |

Daftar (paging):

```json
{
  "statusCode": 200,
  "message": "Berhasil",
  "data": [ { "id": "…", "key": "NB-12", "title": "Integrasi sensor suhu" } ],
  "meta": { "nextCursor": "eyJvIjo1MH0" }
}
```

Error:

```json
{
  "statusCode": 422,
  "message": "Alasan wajib diisi saat memindah task ke Blocked",
  "data": null,
  "error": {
    "code": "BR-WF-09",
    "detail": "Isi alasan Blocked lalu coba lagi.",
    "fields": [ { "field": "blockedReason", "message": "Wajib diisi" } ]
  }
}
```

| Field `error` | Isi |
|---|---|
| `code` | Kode BR (`BR-WF-08`) atau kode teknis (`STALE_VERSION`). **App membuat keputusan berdasarkan `code`, bukan `message`** |
| `detail` | Penjelasan tambahan (opsional) |
| `fields` | Daftar kesalahan per field untuk `422 VALIDATION` (opsional) |
| `current` | Data terbaru untuk `409 STALE_VERSION` (opsional) |

Aturan tambahan:

- Respons tanpa isi (dulu `204`) menjadi `200` dengan `"data": null`, supaya app selalu bisa membaca envelope.
- Replay idempotensi (`Idempotent-Replayed: true`) mengembalikan envelope yang sama persis dengan respons pertama.
- Error yang muncul sebelum controller (token salah, tenant salah, 404 route) juga memakai envelope. Implementasinya lewat
  `@RestControllerAdvice` dan `AuthenticationEntryPoint` di Spring.
- **Pengecualian:** `POST /broadcasting/auth` (API-45) mengembalikan JSON mentah `{"auth": "…"}`, karena library Pusher/Reverb
  di klien membaca format itu langsung.
- ⚠ Kontrak `proms-api.yaml` saat ini **belum memakai envelope** (objek langsung dan `problem+json`). Kontrak perlu diperbarui
  (MOB-S03) supaya Spring, mobile, dan web nanti memakai format yang sama.

### 3.1 Kode error yang wajib ditangani app

Kolom `code` di bawah dibaca dari `error.code`.

| Status | `code` | Tampilan di app (brief §5) |
|---|---|---|
| 401 | `UNAUTHENTICATED` | Kembali ke login |
| 403 | `TENANT_ACCESS_REVOKED` | Layar pilih workspace |
| 403 | `TENANT_READ_ONLY` | Banner "Workspace dalam mode baca saja", tombol tulis nonaktif |
| 403 | `FORBIDDEN_*`, `BR-WF-06`, `BR-WF-07` | Pesan `message` dari server di sheet/toast |
| 404 | `BOARD_NOT_FOUND`, `TASK_NOT_FOUND`, … | "Board tidak ditemukan" (jangan tulis "tidak punya akses") |
| 409 | `STALE_VERSION` | Toast "Data sudah berubah, dimuat ulang" lalu muat ulang dari `error.current` |
| 409 | `BOARD_ARCHIVED` | Toast "Board diarsipkan, tidak bisa diubah" |
| 409 | `BR-WF-08` | Kolom penuh (WIP), pesan dari server |
| 422 | `BR-WF-09` | Sheet meminta alasan Blocked |
| 422 | `VALIDATION` | Pesan per field dari `error.fields` |
| 428 | `IF_MATCH_REQUIRED` | Bug di app; tidak ditampilkan ke user |

---

## 4. Pemetaan layar → endpoint

Kolom **Status**:
- ✅ ada di kontrak dan Laravel
- 🟡 ada di kontrak, Laravel menyimpang (Spring ikut kontrak)
- 🟠 ada di Laravel, **belum di kontrak** (perlu ditulis ke kontrak dulu)
- 🔴 belum ada di mana pun

Kolom **Respons sukses** hanya berisi status HTTP. Contoh body lengkap (envelope standar §3.2) ada di §4.1 sesuai urutan ID.
Respons error selalu berbentuk envelope error di §3.2.

| Layar | ID | Method & path (yang dipakai Spring) | Respons sukses | Status | Laravel saat ini |
|---|---|---|---|---|---|
| 01 Login | API-01 | `POST /auth/login` | `200 OK` | 🔴 produksi / 🟠 dev | `POST /auth/login` (dev saja) |
| 01 Logout | API-02 | `POST /auth/logout` | `200 OK` | 🟠 | sama |
| 02 Workspace, 15 Profil | API-03 | `GET /me` | `200 OK` | ✅ | sama (`GET\|POST`) |
| 03, 04, 14a | API-04 | `GET /me/tasks` | `200 OK` | 🟡 | `GET /my-tasks` (snake_case, filter berbeda) |
| 03, 05 | API-05 | `GET /inbox` | `200 OK` | 🟡 | tanpa paging, tanpa `unreadCount`, snake_case |
| 05 | API-06 | `PATCH /inbox/{notificationId}` | `200 OK` | 🟡 | dipecah: `POST /inbox/{id}/read`, `/unread`, `/snooze`, `/unsnooze`, `DELETE /inbox/{id}` |
| 05 | API-07 | `POST /inbox/read-all` | `200 OK` | ✅ | respons beda (`updated_count`) |
| 05, 05a | API-08 | `POST /inbox/{notificationId}/decision` | `200 OK` | 🟠 | sama |
| 03, 13 | API-09 | `GET /me/standup/today` | `200 OK` | ✅ | sama |
| 13 | API-10 | `POST /me/standups` | `201 Created` | ✅ | sama |
| 13 | API-11 | `PATCH /me/standups/{standupId}` | `200 OK` | ✅ | sama |
| 07 | API-12 | `GET /boards?tab=mine\|shared\|archived` | `200 OK` | ✅ | sama |
| 08, 09b | API-13 | `GET /boards/{boardId}` (kolom, workstream, member) | `200 OK` | ✅ | sama |
| 08 | API-14 | `GET /boards/{boardId}/tasks` | `200 OK` | ✅ | sama |
| 09 | API-15 | `GET /tasks/{taskId}` | `200 OK` | ✅ | sama |
| 09 (deep link `NB-12`) | API-16 | `GET /task-keys/{taskKey}` | `200 OK` | 🟡 | **belum ada** di Laravel |
| 09, 09b, 09c, 04 swipe kiri | API-17 | `PATCH /tasks/{taskId}` | `200 OK` | 🟡 | input snake_case |
| 09a, 04 swipe kanan | API-18 | `PATCH /tasks/{taskId}/move` | `200 OK` | ✅ | sama |
| 11 | API-19 | `POST /boards/{boardId}/tasks` | `201 Created` | ✅ | sama |
| 10 | API-20 | `GET /tasks/{taskId}/comments` | `200 OK` | ✅ | sama |
| 10 | API-21 | `POST /tasks/{taskId}/comments` | `201 Created` | ✅ | sama |
| 10 | API-22 | `GET /tasks/{taskId}/activities` | `200 OK` | ✅ | sama |
| 10 (`@`) | API-23 | `GET /boards/{boardId}` → `members` | `200 OK` | ✅ | sama |
| 09d | API-24 | `GET /tasks/{taskId}/attachments` | `200 OK` | 🟠 | sama |
| 09d | API-25 | `POST /tasks/{taskId}/attachments` (multipart) | `201 Created` | 🟠 | sama |
| 09d | API-26 | `GET /attachments/{attachmentId}/download` | `200 OK` | 🟠 | sama |
| 12 | API-27 | `GET /search?q=&types=` | `200 OK` | ✅ | sama |
| 14 | API-28 | `GET /focus-sessions?from=&to=` | `200 OK` | ✅ (+`task` 🔴) | sama |
| 14 | API-29 | `POST /focus-sessions` (+`taskId` 🔴) | `201 Created` | ✅ | sama |
| 15 | API-30 | `GET /me/notification-settings` | `200 OK` | ✅ | sama |
| 15 | API-31 | `PATCH /me/notification-settings` | `200 OK` | ✅ | sama |
| 15a | API-32 | `POST /me/devices` | `201 Created` | 🔴 | ada Web Push (`push/subscriptions`), bukan FCM/APNs |
| Logout | API-33 | `DELETE /me/devices/{deviceId}` | `200 OK` | 🔴 | – |
| 06 | API-34 | `GET /chat/channels` | `200 OK` | 🟠 | sama |
| 06b | API-35 | `GET /chat/channels/{channelId}/messages` | `200 OK` | 🟠 | sama |
| 06b | API-36 | `POST /chat/channels/{channelId}/messages` | `201 Created` | 🟠 | sama |
| 06b | API-37 | `POST /chat/channels/{channelId}/read` | `200 OK` | 🟠 | sama |
| 06b | API-38 | `PATCH /chat/messages/{messageId}` | `200 OK` | 🟠 | `PUT\|PATCH` |
| 06b | API-39 | `DELETE /chat/messages/{messageId}` | `200 OK` | 🟠 | sama |
| 06b | API-40 | `POST /chat/messages/{messageId}/pin` | `200 OK` | 🟠 | sama |
| 06b | API-41 | `POST /chat/upload` (multipart) | `200 OK` | 🟠 | sama |
| 06c | API-42 | `POST /chat/direct` | `200 OK` | 🟠 | sama |
| 06c | API-43 | `GET /directory/members` | `200 OK` | 🟠 | sama |
| 06 (channel board) | API-44 | `GET /boards/{boardId}/chat/channels` + messages | `200 OK` | 🟠 | sama |
| Realtime | API-45 | `POST /broadcasting/auth` | `200 OK` (tanpa envelope) | 🟠 | sama (Laravel bawaan) |

**Hitungan:** 45 endpoint. 20 sudah sesuai kontrak (✅), 5 menyimpang di Laravel (🟡), 17 belum ada di kontrak (🟠), 2 belum ada di mana pun (🔴, device push). Ditambah 1 login produksi (🔴) dan perluasan `taskId` di focus (🔴).

### 4.1 Contoh respons
Isi dipersingkat dengan `…` (field lain sesuai skema di §5 dan `proms-api.yaml`). ID dan versi ditulis `"…"`.

#### Auth & profil

**API-01** · `POST /auth/login` · layar 01 Login

```json
{
  "statusCode": 200,
  "message": "Berhasil login",
  "data": {
    "token": "12|Xk9…",
    "tokenType": "Bearer",
    "user": {
      "id": "0192…",
      "fullName": "Fariz Keren",
      "email": "fariz@hyperdata.co.id",
      "locale": "id",
      "timezone": "Asia/Jakarta"
    },
    "tenant": { "id": "0191…", "slug": "hyperdata", "name": "Hyperdata" },
    "workspaceRole": "member"
  }
}
```

**API-02** · `POST /auth/logout` · layar 01 Logout

```json
{ "statusCode": 200, "message": "Berhasil logout", "data": null }
```

**API-03** · `GET /me` · layar 02 Workspace, 15 Profil

```json
{
  "statusCode": 200,
  "message": "Berhasil",
  "data": {
    "user": { "id": "0192…", "fullName": "Fariz Keren", "email": "fariz@…" },
    "tenant": {
      "id": "0191…",
      "slug": "hyperdata",
      "name": "Hyperdata",
      "status": "active",
      "timezone": "Asia/Jakarta"
    },
    "workspaceRole": "member",
    "features": { "points": true, "standup": true },
    "otherTenants": [{ "slug": "telemedis", "name": "Telemedis" }]
  }
}
```

#### My Tasks & Inbox

**API-04** · `GET /me/tasks` · layar 03, 04, 14a

```json
{
  "statusCode": 200,
  "message": "Berhasil",
  "data": [
    {
      "id": "0193…",
      "key": "NB-12",
      "boardId": "…",
      "boardName": "Nusa Bioscience",
      "category": "in_progress",
      "title": "Integrasi sensor suhu",
      "priority": "high",
      "dueDate": "2026-10-28",
      "dueState": "soon",
      "assignees": [
        { "id": "…", "fullName": "Fariz Keren", "initials": "FK" }
      ],
      "isBlocked": false,
      "subtaskDone": 1,
      "subtaskTotal": 3,
      "commentCount": 4,
      "version": "2026-10-27T03:15:22.123456Z"
    }
  ],
  "meta": { "nextCursor": "eyJvIjo1MH0" }
}
```

**API-05** · `GET /inbox` · layar 03, 05

```json
{
  "statusCode": 200,
  "message": "Berhasil",
  "data": [
    {
      "id": "…",
      "type": "mention",
      "actor": { "id": "…", "fullName": "Rina A" },
      "board": { "id": "…", "key": "NB", "name": "Nusa Bioscience" },
      "task": { "id": "…", "key": "NB-12", "title": "Integrasi sensor suhu" },
      "text": "mentioned you in",
      "snippet": "@Fariz tolong cek…",
      "groupCount": 1,
      "read": false,
      "snoozedUntil": null,
      "approval": null,
      "createdAt": "2026-10-28T02:10:00Z"
    }
  ],
  "meta": { "nextCursor": null, "unreadCount": 7 }
}
```

**API-06** · `PATCH /inbox/{notificationId}` · layar 05

```json
{
  "statusCode": 200,
  "message": "Notifikasi diperbarui",
  "data": {
    "id": "…",
    "type": "mention",
    "read": true,
    "snoozedUntil": "2026-10-29T01:00:00Z",
    …
  }
}
```

**API-07** · `POST /inbox/read-all` · layar 05

```json
{
  "statusCode": 200,
  "message": "Semua notifikasi ditandai dibaca",
  "data": { "updatedCount": 7 }
}
```

**API-08** · `POST /inbox/{notificationId}/decision` · layar 05, 05a

```json
{
  "statusCode": 200,
  "message": "Permintaan ditolak",
  "data": {
    "id": "…",
    "type": "approval",
    "approval": {
      "entityType": "expense",
      "entityId": "…",
      "canDecide": false,
      "decision": "rejected",
      "decisionReason": "Nota belum dilampirkan"
    },
    …
  }
}
```

#### Standup

**API-09** · `GET /me/standup/today` · layar 03, 13

```json
{
  "statusCode": 200,
  "message": "Berhasil",
  "data": {
    "reportDate": "2026-10-28",
    "required": true,
    "reason": null,
    "report": null,
    "deadlineTime": "10:00",
    "blockingBanner": false
  }
}
```

**API-10** · `POST /me/standups` · layar 13

```json
{
  "statusCode": 201,
  "message": "Standup terkirim",
  "data": {
    "id": "…",
    "reportDate": "2026-10-28",
    "plan": "Lanjut NB-12",
    "done": "Selesai NB-9",
    "blockers": null,
    "submittedAt": "2026-10-28T02:41:00Z",
    "isLate": false,
    "version": "…"
  }
}
```

**API-11** · `PATCH /me/standups/{standupId}` · layar 13

```json
{
  "statusCode": 200,
  "message": "Standup diperbarui",
  "data": {
    "id": "…",
    "plan": "Lanjut NB-12 & NB-14",
    "done": "Selesai NB-9",
    "blockers": null,
    "isLate": false,
    "version": "…"
  }
}
```

#### Boards & task

**API-12** · `GET /boards?tab=mine\|shared\|archived` · layar 07

```json
{
  "statusCode": 200,
  "message": "Berhasil",
  "data": [
    {
      "id": "…",
      "key": "NB",
      "name": "Nusa Bioscience",
      "status": "active",
      "health": "at_risk",
      "healthReason": "2 task overdue",
      "progressPct": 64,
      "dueDate": "2026-12-15",
      "pm": { "id": "…", "fullName": "Dewi S" },
      "memberCount": 8,
      "myRole": "member",
      "archivedAt": null,
      "version": "…"
    }
  ],
  "meta": { "nextCursor": null, "summary": { … } }
}
```

**API-13** · `GET /boards/{boardId}` (kolom, workstream, member) · layar 08, 09b

```json
{
  "statusCode": 200,
  "message": "Berhasil",
  "data": {
    "id": "…",
    "key": "NB",
    "name": "Nusa Bioscience",
    "workstreams": [{ "id": "…", "name": "Hardware" }],
    "columns": [
      {
        "id": "…",
        "name": "In progress",
        "category": "in_progress",
        "wipLimit": 5,
        "position": "a1",
        "taskCount": 3
      }
    ],
    "members": [{ "id": "…", "fullName": "Fariz Keren" }],
    "permissions": { "canManageStructure": false },
    …
  }
}
```

**API-14** · `GET /boards/{boardId}/tasks` · layar 08

```json
{
  "statusCode": 200,
  "message": "Berhasil",
  "data": [
    {
      "id": "…",
      "key": "NB-12",
      "title": "Integrasi sensor suhu",
      "columnId": "…",
      …
    }
  ],
  "meta": { "nextCursor": null }
}
```

**API-15** · `GET /tasks/{taskId}` · layar 09

```json
{
  "statusCode": 200,
  "message": "Berhasil",
  "data": {
    "id": "…",
    "key": "NB-12",
    "title": "Integrasi sensor suhu",
    "subtasks": […],
    "checklist": [{ "id": "…", "title": "Kalibrasi", "isDone": true }],
    "attachments": […],
    "permissions": {
      "canEdit": true,
      "canMoveToDone": false,
      "canEditDueDate": true
    },
    "version": "…",
    …
  }
}
```

**API-16** · `GET /task-keys/{taskKey}` · layar 09 (deep link `NB-12`)

```json
{
  "statusCode": 200,
  "message": "Berhasil",
  "data": {
    "id": "0193…",
    "key": "NB-12",
    "title": "Integrasi sensor suhu",
    "columnId": "…",
    "category": "in_progress",
    "priority": "high",
    "dueDate": "2026-10-28",
    "version": "…",
    …
  }
}
```

**API-17** · `PATCH /tasks/{taskId}` · layar 09, 09b, 09c, 04 swipe kiri

```json
{
  "statusCode": 200,
  "message": "Task diperbarui",
  "data": {
    "id": "0193…",
    "key": "NB-12",
    "title": "Integrasi sensor suhu",
    "columnId": "…",
    "category": "in_progress",
    "priority": "high",
    "dueDate": "2026-10-28",
    "version": "…",
    …
  }
}
```

**API-18** · `PATCH /tasks/{taskId}/move` · layar 09a, 04 swipe kanan

```json
{
  "statusCode": 200,
  "message": "Status task diubah",
  "data": {
    "id": "…",
    "key": "NB-12",
    "columnId": "…",
    "category": "in_progress",
    "version": "…",
    …
  }
}
```

**API-19** · `POST /boards/{boardId}/tasks` · layar 11

```json
{
  "statusCode": 201,
  "message": "NB-23 dibuat",
  "data": {
    "id": "…",
    "key": "NB-23",
    "title": "Ganti kabel sensor",
    "version": "…",
    …
  }
}
```

**API-20** · `GET /tasks/{taskId}/comments` · layar 10

```json
{
  "statusCode": 200,
  "message": "Berhasil",
  "data": [
    {
      "id": "…",
      "taskId": "…",
      "author": { "id": "…", "fullName": "Rina A" },
      "body": { … },
      "bodyText": "@Fariz tolong cek",
      "visibility": "internal",
      "mentions": [{ "id": "…", "fullName": "Fariz Keren" }],
      "editedAt": null,
      "createdAt": "…",
      "version": "…"
    }
  ],
  "meta": { "nextCursor": null }
}
```

**API-21** · `POST /tasks/{taskId}/comments` · layar 10

```json
{
  "statusCode": 201,
  "message": "Komentar terkirim",
  "data": {
    "id": "…",
    "taskId": "…",
    "author": { … },
    "bodyText": "Siap, aku cek sore ini",
    "createdAt": "…",
    "version": "…",
    …
  }
}
```

**API-22** · `GET /tasks/{taskId}/activities` · layar 10

```json
{
  "statusCode": 200,
  "message": "Berhasil",
  "data": [
    {
      "id": "…",
      "actor": { "fullName": "Dewi S" },
      "action": "updated",
      "field": "column",
      "oldValue": { "name": "To do" },
      "newValue": { "name": "In progress" },
      "createdAt": "…"
    }
  ],
  "meta": { "nextCursor": null }
}
```

**API-23** · `GET /boards/{boardId}` → `members` · layar 10 (`@`)

Lihat API-13, field `data.members`.

#### Lampiran

**API-24** · `GET /tasks/{taskId}/attachments` · layar 09d

```json
{
  "statusCode": 200,
  "message": "Berhasil",
  "data": [
    {
      "id": "…",
      "fileName": "foto-panel.jpg",
      "mimeType": "image/jpeg",
      "sizeBytes": 482113,
      "downloadUrl": "https://…",
      "uploadedBy": { "fullName": "Fariz Keren" },
      "createdAt": "…"
    }
  ]
}
```

**API-25** · `POST /tasks/{taskId}/attachments` (multipart) · layar 09d

```json
{
  "statusCode": 201,
  "message": "Lampiran diunggah",
  "data": {
    "id": "…",
    "fileName": "foto-panel.jpg",
    "mimeType": "image/jpeg",
    "sizeBytes": 482113,
    …
  }
}
```

**API-26** · `GET /attachments/{attachmentId}/download` · layar 09d

```json
{
  "statusCode": 200,
  "message": "Berhasil",
  "data": {
    "url": "https://storage…?X-Amz-Signature=…",
    "expiresAt": "2026-10-28T03:05:00Z"
  }
}
```

#### Search & focus

**API-27** · `GET /search?q=&types=` · layar 12

```json
{
  "statusCode": 200,
  "message": "Berhasil",
  "data": {
    "exactTask": null,
    "results": [
      {
        "type": "task",
        "id": "…",
        "title": "Integrasi sensor suhu",
        "subtitle": "NB-12 · Nusa Bioscience"
      },
      { "type": "person", "id": "…", "title": "Rina A" }
    ]
  }
}
```

**API-28** · `GET /focus-sessions?from=&to=` · layar 14

```json
{
  "statusCode": 200,
  "message": "Berhasil",
  "data": [
    {
      "id": "…",
      "startedAt": "2026-10-28T01:00:00Z",
      "endedAt": "2026-10-28T01:50:00Z",
      "durationSec": 3000,
      "durationMin": 50,
      "date": "2026-10-28",
      "task": { "id": "…", "key": "NB-12", "title": "…" }
    }
  ],
  "meta": {
    "summary": {
      "totalSec": 12600,
      "totalMin": 210,
      "capacityMin": 1200,
      "weekStart": "2026-10-26",
      "weekEnd": "2026-11-01"
    }
  }
}
```

**API-29** · `POST /focus-sessions` (+`taskId` 🔴) · layar 14

```json
{
  "statusCode": 201,
  "message": "Sesi fokus tersimpan",
  "data": {
    "id": "…",
    "startedAt": "…",
    "endedAt": "…",
    "durationSec": 3000,
    "durationMin": 50,
    "task": { "key": "NB-12", … }
  }
}
```

#### Notifikasi & push

**API-30** · `GET /me/notification-settings` · layar 15

```json
{
  "statusCode": 200,
  "message": "Berhasil",
  "data": {
    "sound": true,
    "desktop": true,
    "types": {
      "mention": true,
      "assigned": true,
      "approval": true,
      "due": false
    },
    "version": "…"
  }
}
```

**API-31** · `PATCH /me/notification-settings` · layar 15

```json
{
  "statusCode": 200,
  "message": "Pengaturan disimpan",
  "data": { "sound": true, "desktop": true, "types": { … }, "version": "…" }
}
```

**API-32** · `POST /me/devices` · layar 15a

```json
{
  "statusCode": 201,
  "message": "Perangkat terdaftar",
  "data": { "id": "0195…" }
}
```

**API-33** · `DELETE /me/devices/{deviceId}` · layar Logout

```json
{ "statusCode": 200, "message": "Perangkat dihapus", "data": null }
```

#### Chat & realtime

**API-34** · `GET /chat/channels` · layar 06

```json
{
  "statusCode": 200,
  "message": "Berhasil",
  "data": [
    {
      "id": "…",
      "type": "direct",
      "name": "Rina A",
      "directUser": { "id": "…", "fullName": "Rina A" },
      "unreadCount": 2,
      "lastMessage": { "text": "Oke siap", "createdAt": "…" },
      "isStarred": false,
      "isMuted": false
    }
  ]
}
```

**API-35** · `GET /chat/channels/{channelId}/messages` · layar 06b

```json
{
  "statusCode": 200,
  "message": "Berhasil",
  "data": [
    {
      "id": "…",
      "author": { "id": "…", "fullName": "Rina A" },
      "text": "Sudah aku upload",
      "files": [],
      "reactions": [{ "emoji": "👍", "count": 2, "mine": true }],
      "isPinned": false,
      "editedAt": null,
      "createdAt": "…",
      "version": "…"
    }
  ],
  "meta": { "nextCursor": "…" }
}
```

**API-36** · `POST /chat/channels/{channelId}/messages` · layar 06b

```json
{
  "statusCode": 201,
  "message": "Pesan terkirim",
  "data": {
    "id": "…",
    "author": { … },
    "text": "Oke, aku cek",
    "createdAt": "…",
    …
  }
}
```

**API-37** · `POST /chat/channels/{channelId}/read` · layar 06b

```json
{ "statusCode": 200, "message": "Berhasil", "data": null }
```

**API-38** · `PATCH /chat/messages/{messageId}` · layar 06b

```json
{
  "statusCode": 200,
  "message": "Pesan diubah",
  "data": { "id": "…", "text": "Oke, aku cek sore ini", "editedAt": "…", … }
}
```

**API-39** · `DELETE /chat/messages/{messageId}` · layar 06b

```json
{ "statusCode": 200, "message": "Pesan dihapus", "data": null }
```

**API-40** · `POST /chat/messages/{messageId}/pin` · layar 06b

```json
{
  "statusCode": 200,
  "message": "Pesan disematkan",
  "data": { "isPinned": true }
}
```

**API-41** · `POST /chat/upload` (multipart) · layar 06b

```json
{
  "statusCode": 200,
  "message": "File diunggah",
  "data": {
    "name": "denah.pdf",
    "size": 1203311,
    "url": "https://…",
    "mime": "application/pdf"
  }
}
```

**API-42** · `POST /chat/direct` · layar 06c

```json
{
  "statusCode": 200,
  "message": "Berhasil",
  "data": {
    "id": "…",
    "type": "direct",
    "name": "Rina A",
    "directUser": { … },
    "unreadCount": 0
  }
}
```

**API-43** · `GET /directory/members` · layar 06c

```json
{
  "statusCode": 200,
  "message": "Berhasil",
  "data": [
    {
      "id": "…",
      "fullName": "Rina A",
      "avatarUrl": null,
      "initials": "RA",
      "isActive": true
    }
  ]
}
```

**API-44** · `GET /boards/{boardId}/chat/channels` + messages · layar 06 (channel board)

Sama dengan API-34/API-35, item `type: "board"` dan `boardId` terisi.

**API-45** · `POST /broadcasting/auth` · layar Realtime

```json
{ "auth": "proms-key:5f1c…" }
```

**Tanpa envelope**, karena library Pusher/Reverb di app membaca format ini langsung.

---

## 5. Detail per endpoint

Semua bentuk respons di bagian ini adalah **isi field `data`** di envelope (§3.2), dan "Response 204" berarti `200` dengan `"data": null`.
Bentuk skema mengacu ke `components/schemas` di `proms-api.yaml`. `*` = wajib. Kolom "Efek di DB" adalah apa yang harus
ditulis Spring di **satu transaksi**.

### 5.1 Auth & profil

**API-01 `POST /auth/login`** (🔴 untuk produksi)
- Request: `{ email*, password*, tenant? }`
- Response 200: `{ token, tokenType: "Bearer", user: UserRef & {email, locale, timezone}, tenant: {id, slug, name}, workspaceRole }`
- Error: `401 INVALID_CREDENTIALS`, `403 ACCOUNT_INACTIVE`, `403 TENANT_ACCESS_REVOKED`, `404 TENANT_NOT_FOUND`
- Efek di DB: insert `platform.personal_access_tokens` (`name = 'mobile:<deviceName>'`, `token = sha256(plain)`).
  Kembalikan `"<id>|<plain>"`.
- Catatan: password disimpan bcrypt (`Hash::check` Laravel) → pakai `BCryptPasswordEncoder` (kompatibel `$2y$`).
  Tambahkan rate limit (mis. 5 percobaan per menit per email+IP).

**API-02 `POST /auth/logout`**: hapus baris token yang sedang dipakai dan device push milik token itu (API-33). Response `200`, `data: null`.

**API-03 `GET /me`** → `Me`: `user`, `tenant` (status, timezone), `workspaceRole`, `features {points, standup}`, `otherTenants[]`.
- Layar 02 muncul bila `otherTenants.length > 0`. Ganti workspace = ganti header `X-Tenant`; token tetap.
- `features.standup = false` → sembunyikan kartu standup di Home.

### 5.2 My Tasks & Home

**API-04 `GET /me/tasks`** → `data: TaskSummary[]`, `meta: { nextCursor }`
- Kontrak sekarang: `includeDone`, `limit`, `cursor`.
- **Perlu ditambah ke kontrak** untuk segmen layar 04: `due=today|overdue|week|all` (dihitung dengan zona waktu tenant) dan
  `groupBy` tidak perlu (app mengelompokkan per `boardId`/`boardName`).
- **Perlu ditambah** endpoint ringkasan untuk kartu Home, agar app tidak menarik semua task:
  `GET /me/tasks/summary` → `{ today, overdue, thisWeek, all }` (🔴).
- Isi task = task yang di-*assign* ke saya, `deleted_at is null`, board aktif, kolom bukan `done` kecuali `includeDone`.
- `dueState` (`late|soon|ok|none`) dan `daysLate` dihitung server (lihat `app/Support/Task/DueState.php`).

### 5.3 Inbox

**API-05 `GET /inbox?type=&status=unread|all|archived&limit&cursor`** → `data: Notification[]`, `meta: { nextCursor, unreadCount }`
- Filter app: Semua = tanpa `type`; Mention = `mention`; Approval = `approval`; Assigned = `assigned`; Due = `due`.
- Notifikasi yang `snoozed_until > now()` tidak tampil kecuali difilter.
- `Notification.approval.canDecide` dihitung server dengan aturan di API-08.
- **Jangan kirim email aktor** (Laravel saat ini mengirim `actor_email`; ini melanggar kontrak).

**API-06 `PATCH /inbox/{notificationId}`** body `{ read?, archived?, snoozedUntil? }` → `Notification`
- Satu endpoint menggantikan lima rute Laravel. `snoozedUntil: null` = batal snooze.
- Swipe "tandai dibaca" → `{read: true}`. Snooze "Besok 08:00" → app menghitung waktunya dalam zona waktu tenant, lalu mengirim ISO UTC.
- Efek di DB: update `proms.notifications` (`read_at`, `archived_at`, `snoozed_until`) dengan syarat `recipient_id = :userId`.
  Bukan milik user → `404 NOTIFICATION_NOT_FOUND`.

**API-07 `POST /inbox/read-all`** → `{ updatedCount }`.

**API-08 `POST /inbox/{notificationId}/decision`** (🟠 tulis ke kontrak)
- Body `{ decision*: "approved"|"rejected", reason?: string }`. `reason` wajib bila `rejected`.
- Response: `Notification`
- Error: `422 BR-NT-INVALID-DECISION`, `422 BR-NT-REASON-REQUIRED`, `403 FORBIDDEN`, `404 NOTIFICATION_NOT_FOUND`
- Siapa yang boleh memutuskan (dari `DecideNotification.php`):

| `entity_type` | Yang boleh |
|---|---|
| `expense` (default) | workspace role `finance` / `owner` |
| `timesheet` | workspace `owner` atau `pm` di board terkait |
| `deliverable` | role board `admin` / `pm` / `guest` |

- Efek di DB: update `notifications.decision`, `decision_reason`, `read_at`.
- ⚠ Laravel saat ini **hanya mengubah baris notifikasi**, tidak mengubah status expense/timesheet/deliverable-nya.
  Perlu konfirmasi apakah ini memang disengaja (keputusan #6).

### 5.4 Standup

**API-09 `GET /me/standup/today`** → `StandupToday { reportDate, required, reason, report, deadlineTime, blockingBanner }`
- `required = false` dengan `reason` (`non_work_day`, `holiday`, `time_off`, `not_joined`, `disabled`) → kartu standup disembunyikan.
- Status "Terlambat" (layar 13 Late) = `now > deadlineTime` di zona waktu tenant dan `report == null`.

**API-10 `POST /me/standups`** (Idempotency) body `StandupReportCreate { plan*, done*, blockers? }` → `201 StandupReport`
- Mapping field layar: Kemarin = `done`, Hari ini = `plan`, Blocker = `blockers`.
- Error: `409 BR-SU-01` (sudah mengisi hari ini), `409 BR-SU-05` (standup dinonaktifkan), `422 VALIDATION`
- Efek di DB: insert `proms.standup_reports` (`is_late` dihitung dari policy) **dan** insert `proms.point_entries` (BR-PT-40).
  Logika poin ada di `SubmitStandup.php`. Harus diport sama persis (lihat §8, parity test).

**API-11 `PATCH /me/standups/{standupId}`** (If-Match) → `StandupReport`. Hanya laporan hari ini; lewat hari → `409 BR-SU-02`.
`submitted_at` dan `is_late` tidak berubah saat diedit (BR-SU-04).

### 5.5 Boards

**API-12 `GET /boards?tab=&q=&sort=&limit&cursor`** → `data: BoardSummary[]`, `meta: { nextCursor, summary }`
- `mine` = board aktif tempat saya member. `shared` = aktif, `is_owner = false`, dan `role != 'admin'`. `archived` = `archived_at is not null`.
  Ikuti `app/Domain/Board/Actions/ListBoards.php`.
- `health` dan `healthReason` dibaca dari kolom `boards.health`/`health_reason`. Di Laravel kolom ini sering kosong, dan
  `ListBoards` memakai fallback: ada task overdue → `at_risk` ("N task overdue"), selain itu `on_track`. Rumus ini tidak tertulis
  di BR, jadi Spring meniru fallback yang sama sampai PO menetapkan rumus health (keputusan #8).
- `contractValue` hanya diisi untuk role workspace `finance`/`owner`. Mobile tidak memakainya, jadi kirim `null` saja.

**API-13 `GET /boards/{boardId}`** → `BoardDetail` (kolom + `wipLimit` + `category`, workstream, member, `permissions`)
- Dipakai sekali saat membuka board. Data `columns` dipakai sheet ganti status (09a), `members` untuk sheet assignee (09b) dan daftar `@mention`.

**API-14 `GET /boards/{boardId}/tasks?workstreamId=&assigneeId=&q=&limit&cursor`** → `data: TaskSummary[]`, `meta: { nextCursor }`, urut `column` lalu `position`.
- Guest hanya melihat task `shared_with_client = true`.

### 5.6 Task

**API-15 `GET /tasks/{taskId}`** → `Task`, termasuk `subtasks`, `checklist`, `dependencies`, `attachments`, dan `permissions`.
- App memakai `permissions.canEdit`, `canMoveToDone`, `canEditDueDate` untuk menyembunyikan aksi, bukan menebak dari role.

**API-16 `GET /task-keys/{taskKey}`** → `Task` (mis. `NB-12`). Untuk deep link `proms://task/NB-12` dan hasil search.
- Parse `{boardKey}-{number}`, cari board dengan `key` di tenant aktif, lalu cek hak via §2.5. Tidak ketemu → `404 TASK_NOT_FOUND`.

**API-17 `PATCH /tasks/{taskId}`** (If-Match) body `TaskPatch` (camelCase) → `Task`
- Dipakai mobile untuk: `assigneeIds`, `dueDate`, `priority`, `title`, dan status checklist/subtask (lewat endpoint terkait).
- Error: `403 FORBIDDEN_TASK_EDIT`, `403 FORBIDDEN_TASK_PROPERTY`, `403 FORBIDDEN_START_TASK`, `404 MILESTONE_NOT_FOUND`, `409 BR-MS-09`, `409 STALE_VERSION`
- Efek di DB: update `proms.tasks`; diff `proms.task_assignees`; satu baris `task_activities` per field; outbox `TaskUpdated` (`addedAssigneeIds`).
- ⚠ **Logika tersembunyi** di `UpdateTask.php`: bila `startDate` diisi untuk pertama kali dan task masih di kolom
  `not_started`/"Backlog", task **otomatis dipindah** ke kolom `in_progress` pertama. Ada juga activity tambahan dan outbox `TaskMoved`.
  Spring wajib meniru ini.

**API-18 `PATCH /tasks/{taskId}/move`** (If-Match) body `TaskMove { columnId*, beforeTaskId?, afterTaskId?, blockedReason? }` → `Task`
- Mobile tidak pakai drag, jadi kirim `beforeTaskId`/`afterTaskId` = null (taruh di akhir kolom). Swipe kanan = kolom berikutnya
  menurut `position` kolom, tetapi **berhenti sebelum kolom `done`** bila `permissions.canMoveToDone = false`.
- Aturan (`app/Domain/Board/Rules/WorkflowGuard.php`), urutan cek:
  1. Hak pindah → kontrak: `403 BR-WF-06` (Laravel saat ini: `403 FORBIDDEN_TASK_MOVE`)
  2. Ke kolom `done` hanya PM/admin → `403 BR-WF-07`
  3. WIP limit kolom tujuan penuh → `409 BR-WF-08`
  4. Ke kolom `blocked` tanpa `blockedReason` → `422 BR-WF-09`
- `position` memakai **fractional index** string (`app/Support/Ordering/FractionalIndex.php`). Port algoritmanya ke Java dengan hasil identik;
  jangan pakai integer, karena web memakai urutan yang sama.
- Efek di DB: update `tasks.column_id`, `position`, `is_blocked`, `blocked_reason`; `task_activities` (`field = 'column'`);
  outbox `TaskMoved`, ditambah `TaskCompleted` (masuk `done`) atau `TaskReopened` (keluar dari `done`).
  `completed_at` diisi trigger.

**API-19 `POST /boards/{boardId}/tasks`** (Idempotency) body `TaskCreate { title*, workstreamId*, columnId?, assigneeIds?, dueDate?, priority? }` → `201 Task`
- Quick add (11) wajib mengirim `workstreamId`. App memakai workstream default board (pertama) bila user tidak memilih.
- Error: `403 FORBIDDEN_TASK_CREATE`, `404 WORKSTREAM_NOT_FOUND`, `404 COLUMN_NOT_FOUND`, `409 BR-MS-09`
- Efek di DB: `insert into proms.tasks (...) returning number` (key = `boardKey-number` untuk toast "NB-23 dibuat");
  `task_assignees`; `task_activities` (`created`); outbox `TaskCreated` (+`TaskCompleted` bila langsung di kolom done).

### 5.7 Komentar & aktivitas

**API-20 `GET /tasks/{taskId}/comments?limit&cursor`** → `data: Comment[]`, `meta: { nextCursor }`. Guest hanya `visibility = client`.

**API-21 `POST /tasks/{taskId}/comments`** (Idempotency) body `CommentCreate { body*, visibility? }` → `201 Comment`
- `body` = dokumen editor (JSON yang sama dengan web; lihat `app/Support/Document/EditorDocument.php`).
  Mention = node `mention` (`attrs.id = userId`); reference task = node `#NB-12`. App mobile wajib menghasilkan format JSON yang sama.
- Efek di DB: `proms.comments` (+`body_text` hasil ekstrak), `proms.comment_mentions` per user yang di-*mention*,
  `task_activities` (`commented`), outbox `CommentAdded`.

**API-22 `GET /tasks/{taskId}/activities?limit&cursor`** → riwayat untuk baris abu-abu di layar 10.

### 5.8 Lampiran (🟠, tulis ke kontrak dulu)

**API-24 `GET /tasks/{taskId}/attachments`** → `data: Attachment[]`

**API-25 `POST /tasks/{taskId}/attachments`** (multipart `file`, atau JSON `{url, title}` untuk tautan) → `201 Attachment`
- Batas **25 MB**, ekstensi berbahaya ditolak → `422 BR-TS-05`. Viewer → `403 FORBIDDEN_ATTACHMENT_UPLOAD`.
- Efek di DB: insert `proms.attachments` (`storage_key`), `task_activities` (`field = 'attachment'`). File ke object storage (§2.12).
- Usul: tambahkan `Idempotency-Key`, karena upload dari HP sering diulang saat sinyal jelek.

**API-26 `GET /attachments/{attachmentId}/download`** → `data: { url, expiresAt }` berisi **pre-signed URL** (berlaku 5 menit). App membuka URL itu langsung;
file tidak di-*stream* lewat Spring. Redirect `302` tidak dipakai supaya respons tetap ber-envelope.
Hak dicek lewat task → board (§2.5).

### 5.9 Search

**API-27 `GET /search?q=&types=board,task,comment,person`** → `{ exactTask: TaskSummary|null, results: SearchHit[] }`
- `q` 1–100 karakter. Bila `q` cocok pola key (`NB-12`), isi `exactTask`.
- Hanya data dari board tempat user menjadi member. Task di board lain tidak boleh muncul, termasuk dari hitungan.
- Riwayat pencarian disimpan di app (lokal), bukan di server.

### 5.10 Focus timer

**API-28 `GET /focus-sessions?from=&to=`** → `data: FocusSession[]`, `meta: { summary: { totalSec, totalMin, capacityMin, weekStart, weekEnd } }`

**API-29 `POST /focus-sessions`** (Idempotency) body `{ startedAt*, endedAt*, taskId? }` → `201 FocusSession`
- Timer berjalan **di app** (Live Activity/notifikasi persisten). Server baru dipanggil saat "Stop & simpan".
  Tidak ada endpoint start/pause. Jeda dihitung di app, dan yang dikirim adalah rentang efektif.
- Durasi wajib 1 menit–16 jam → `422 BR-FC-01` (`field: endedAt`). DB juga punya constraint `ended_at > started_at` dan `duration_sec between 60 and 57600`.
- Efek di DB: insert `proms.focus_sessions`, `point_entries` (BR-PT-30), sesuai `LogFocusSession.php`.
- 🔴 `taskId` butuh migrasi baru (MOB-B02): kolom `task_id` nullable + `tenantForeign()`. Respons ditambah `task: TaskRef|null`.

### 5.11 Notifikasi & push

**API-30/31 `GET`/`PATCH /me/notification-settings`** → `NotificationSettings { sound, desktop, types{...}, version }`
- Toggle per jenis di layar 15 = `types.<NotificationType>`. Usul: tambah field `push` (boolean global untuk mobile) ke kontrak.

**API-32 `POST /me/devices`** (🔴, MOB-B01) body `{ platform*: "ios"|"android", pushToken*, provider*: "expo"|"fcm"|"apns", deviceName?, appVersion? }` → `201 { id }`
- Upsert berdasarkan `pushToken`. Satu token terikat ke satu `personal_access_tokens.id` supaya logout menghapusnya.
- Butuh tabel baru, mis. `platform.user_devices` (migrasi Laravel). Pengiriman push dikerjakan **listener outbox Laravel**
  (di tempat notifikasi dibuat), bukan Spring. Dengan begitu push juga jalan untuk aksi dari web.

**API-33 `DELETE /me/devices/{deviceId}`** → `200`, `data: null`

### 5.12 Chat (🟠, tulis ke kontrak dulu)

Rute Laravel sudah ada (`WorkspaceChatController`, `ChatController`), tetapi input dan output masih snake_case dan belum ada di kontrak.
Usulan kontrak (camelCase) untuk Spring:

| ID | Endpoint | Request | Isi `data` |
|---|---|---|---|
| API-34 | `GET /chat/channels` | – | `ChatChannel[]`: `id, type (public\|private\|direct\|board), name, boardId?, directUser?: UserRef, unreadCount, lastMessage?: {text, createdAt, author}, isStarred, isMuted` |
| API-35 | `GET /chat/channels/{id}/messages?before=&limit=40` | – | `ChatMessage[]`, cursor di `meta.nextCursor`: `id, author: UserRef, text, files[], parentId?, reactions[{emoji,count,mine}], isPinned, editedAt?, createdAt, version` |
| API-36 | `POST /chat/channels/{id}/messages` (Idempotency) | `{ text?, parentId?, files?: [{name,url,size,mime}] }` | `201 ChatMessage` |
| API-37 | `POST /chat/channels/{id}/read` | – | `null` |
| API-38 | `PATCH /chat/messages/{id}` | `{ text* }` (pesan sendiri) | `ChatMessage` |
| API-39 | `DELETE /chat/messages/{id}` | – | `null` |
| API-40 | `POST /chat/messages/{id}/pin` | – | `{ isPinned }` |
| API-41 | `POST /chat/upload` | multipart `file` (≤ 25 MB) | `{ name, size, url, mime }` |
| API-42 | `POST /chat/direct` | `{ targetUserId* }` | `ChatChannel` (dibuat bila belum ada) |
| API-43 | `GET /directory/members?q=` | – | `UserRef[]` anggota aktif workspace |
| API-44 | `GET /boards/{boardId}/chat/channels` & `.../messages` | sama dengan di atas | channel board; hak lewat §2.5 |

- Efek di DB (`SendWorkspaceMessage.php`): insert `proms.chat_messages`; upsert `proms.chat_read_markers` pengirim;
  insert **langsung** `proms.notifications` untuk mention di chat (bukan lewat outbox). Port logika mention-nya.
- Error: `404 CHANNEL_NOT_FOUND`, `403 ACCESS_DENIED` (bukan anggota channel private), `404 USER_NOT_FOUND`.
- Broadcast: lihat §2.11 (`private-tenant.{tenantId}.chat.{channelId}`, event `message.created`/`updated`/`deleted`).

---

## 6. Endpoint di luar v1 (jangan dibangun dulu)

Points/leaderboard, MoM, Docs, milestone CRUD, workstream/kolom CRUD, member & undangan board, workspace admin, kalender
dan libur, team activity, finance, trash, saved views, board groups, dependency (tulis), reminders, standup recap/compliance.
Kalau hasil brainstorming fitur tambahan disetujui (rekap standup PM, dependency baca-saja, chat → task), endpoint-nya
sudah ada di kontrak/Laravel dan bisa ditambahkan ke tabel §4 nanti.

---

## 7. Penyimpangan Laravel dari kontrak (Spring ikut kolom "Kontrak")

| Hal | Kontrak (setelah diperbarui) | Laravel sekarang |
|---|---|---|
| My tasks | `GET /me/tasks`, camelCase, `TaskSummary[]` | `GET /my-tasks`, snake_case, waktu Asia/Jakarta *hardcoded* |
| Update inbox | `PATCH /inbox/{id}` | 4× `POST` + `DELETE` |
| Daftar inbox | cursor + `unreadCount` | tanpa paging; ada `actor_email` (melanggar "tanpa email") |
| Task by key | `GET /task-keys/{key}` | tidak ada |
| Input task | camelCase (`assigneeIds`, `dueDate`) | snake_case (`assignee_ids`, `due_date`) |
| Format respons | Envelope `{statusCode, message, data, meta?, error?}` (§3.2) | Objek langsung; error `problem+json` |
| Validasi | `422`, `error.code = VALIDATION` + `error.fields` | `422 VALIDATION_FAILED` + `invalid_params` |
| Header tenant | `400 TENANT_REQUIRED` | `400 TENANT_HEADER_REQUIRED` |
| Bukan anggota | `403 TENANT_ACCESS_REVOKED` | `403 TENANT_ACCESS_FORBIDDEN` (middleware) |
| Tanpa hak pindah task | `403 BR-WF-06` | `403 FORBIDDEN_TASK_MOVE` |
| Tanggal di respons | ISO UTC | campuran `Y-m-d H:i:s` WIB |

Perbaikan di Laravel termasuk Fase E di `docs/analysis/review-checklist.md` dan bukan tanggung jawab tim Spring.
Karena web akan pindah ke Java, sebaiknya **jangan berinvestasi besar memperbaiki penyimpangan ini di Laravel**. Perbaikan
dilakukan sekali saja: frontend web disesuaikan ke kontrak saat modulnya dipindah ke Spring (§11).

---

## 8. Kualitas: cara memastikan Spring = Laravel

1. **Kontrak dulu.** Item 🟠/🔴 ditulis ke `proms-api.yaml` dan di-review sebelum diimplementasi. App mobile memakai mock
   dari kontrak (Prism/MSW) supaya tidak menunggu backend.
2. **Contract test.** Validasi setiap respons Spring terhadap OpenAPI (mis. `swagger-request-validator` di test MockMvc).
3. **Parity test untuk logika berat.** Logika yang diport harus menghasilkan baris DB yang sama dengan Laravel untuk input yang sama:
   `WorkflowGuard` (BR-WF-06..09), auto-move saat start task, `FractionalIndex`, poin standup (BR-PT-40), poin focus (BR-PT-30),
   `DueState`, mention chat.
4. **Test isolasi tenant wajib** di setiap endpoint: user tenant A meminta resource tenant B → `404`, list → 0 baris.
5. **Integration test dengan Postgres asli** (Testcontainers), dengan skema dari `php artisan migrate --database=migration`
   (dump `pg_dump --schema-only` dipakai sebagai fixture Spring). RLS tidak bisa diuji dengan H2.

---

## 9. Keputusan terbuka

| # | Pertanyaan | Usulan |
|---|---|---|
| 1 | Login produksi: Keycloak OIDC (sesuai kontrak) atau token Sanctum dari endpoint login baru? | Keycloak OIDC sejak awal, karena Java akan jadi backend tunggal. Kalau belum siap sebelum rilis mobile, pakai token format Sanctum sebagai jembatan (§2.3) |
| 2 | Host API mobile: domain sama dengan web (routing per path di gateway) atau host terpisah? | Host terpisah (`m-api.proms.id`), path tetap `/api/v1/...`, supaya routing gateway sederhana |
| 3 | Penyimpanan file bersama | S3/MinIO untuk kedua backend sebelum layar 09d dan file chat dikerjakan |
| 4 | Realtime dari Spring | Opsi A (§2.11): publish ke Reverb lewat Pusher HTTP API; nanti ganti ke server Pusher-compatible non-PHP |
| 5 | Member boleh quick add task? (kode: tidak) | Konfirmasi PO. Kalau tidak boleh, sembunyikan FAB untuk member di board itu |
| 6 | Approve/reject hanya menandai notifikasi, bukan entitasnya | Konfirmasi ke PO dan pemilik modul finance/timesheet |
| 7 | Siapa pemilik kontrak tambahan (🟠/🔴)? | Backend dev menulis, PO + mobile dev me-review (MOB-B03) |
| 8 | Rumus health board (kolom sering kosong, fallback di `ListBoards`) | PO menetapkan rumus. Sementara itu Spring meniru fallback Laravel |

---

## 10. Urutan kerja yang disarankan (backend Spring)

| Kode | Pekerjaan | Endpoint | Bergantung |
|---|---|---|---|
| MOB-S01 | Kerangka Spring: 2 DataSource, `TenantContext` + `set_config` per transaksi, envelope respons (§3.2), If-Match, Idempotency, Outbox writer, Testcontainers | – | – |
| MOB-S02 | Auth token (validasi Sanctum) + resolve tenant + `GET /me` | API-01..03 | S01, keputusan #1 |
| MOB-S03 | Ubah `proms-api.yaml` ke envelope standar (§3.2), lalu tulis kontrak 🟠/🔴 | API-08, 16, 24–26, 32–45, `me/tasks` filter | – (paralel S01) |
| MOB-S04 | Board & task baca | API-12..16, 20, 22 | S02 |
| MOB-S05 | My tasks + ringkasan Home | API-04 | S04 |
| MOB-S06 | Task tulis: patch, move (WorkflowGuard, FractionalIndex), create, komentar | API-17..19, 21 | S04 |
| MOB-S07 | Inbox + decision | API-05..08 | S02 |
| MOB-S08 | Standup + focus (port poin) | API-09..11, 28, 29 | S02, MOB-B02 |
| MOB-S09 | Search | API-27 | S04 |
| MOB-S10 | Lampiran (object storage) | API-24..26 | keputusan #3 |
| MOB-S11 | Chat + realtime | API-34..45 | S03, keputusan #4 |
| MOB-S12 | Device push + settings | API-30..33 | MOB-B01 |

Urutan layar mobile mengikuti urutan ini: Login → Home/My Tasks → Task detail → Inbox → Standup/Focus → Search → Boards → Chat.

---

## 11. Rencana jangka panjang: Java sebagai backend tunggal

Pemindahan web dilakukan **per modul** (*strangler*), bukan sekaligus. API gateway (mis. Nginx/Traefik) mengarahkan path
`/api/v1/...` ke Spring atau Laravel per modul, sehingga frontend tidak perlu tahu backend mana yang melayani.

| Fase | Isi | Selesai bila |
|---|---|---|
| **1. Mobile** (sekarang) | Spring melayani endpoint §4 untuk app mobile. Laravel melayani web, memiliki skema, dan menjalankan worker outbox | App mobile v1 rilis |
| **2. Web pindah per modul** | Modul yang sudah ada di Spring (Task, Inbox, Board baca, Standup, Focus, Search, Chat) dilengkapi untuk kebutuhan web lalu path-nya dialihkan di gateway. Frontend web disesuaikan ke kontrak (camelCase, path kontrak). Modul berikutnya: Member & undangan, Milestone, struktur board, Workspace admin, Points, MoM, Docs, Finance | Semua path `/api/v1` dilayani Spring |
| **3. Infrastruktur pindah** | Dispatcher outbox + listener ditulis ulang di Java (pindah per `event_type`, satu dispatcher per jenis event). Scheduler (pengingat standup, dsb.) pindah. Kepemilikan skema pindah ke Flyway dengan *baseline* skema terakhir dari Laravel | Laravel tidak lagi menulis ke DB |
| **4. Laravel dimatikan** | Hapus Laravel, Reverb diganti server Pusher-compatible | – |

Hal yang sudah harus diputuskan sejak fase 1 supaya fase 2–4 tidak membongkar ulang:

1. **Struktur modul Spring per domain** (`task`, `inbox`, `board`, `standup`, …), bukan per layar mobile. Endpoint mobile hanyalah
   bagian dari modul tersebut.
2. **Tidak ada endpoint khusus mobile kecuali benar-benar perlu** (mis. `GET /me/tasks/summary`, `POST /me/devices`). Semua
   tetap ditulis di kontrak yang sama, karena web nanti memakai kontrak itu juga.
3. **Parity test (§8) dijadikan regression suite.** Test yang membandingkan perilaku Spring dan Laravel sekarang akan menjadi
   jaring pengaman saat setiap modul web dialihkan.
4. **Auth memakai Keycloak OIDC** (keputusan #1), supaya web tidak perlu pindah mekanisme login dua kali.
5. **Aturan bisnis yang sama hidup di dua backend selama transisi.** Setiap perubahan aturan (BR-WF, poin, standup) pada modul
   yang dipakai web dan mobile wajib diterapkan di Laravel dan Spring sekaligus, dan parity test menjaganya. Begitu modul web
   dialihkan ke Spring, kode Laravel untuk modul itu dihapus supaya tidak ada dua sumber logika.
