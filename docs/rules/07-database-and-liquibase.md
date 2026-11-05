To prevent Liquibase's SQL parser from breaking statements prematurely:
- **MUST** append `splitStatements:false` to the changeset header.
- If the rollback block also contains procedural SQL (such as `DO $$ ... $$`), **MUST** also append `rollbackSplitStatements:false`:
  ```sql
  --changeset hyproms:02-func-touch_updated_at__ splitStatements:false
  CREATE OR REPLACE FUNCTION platform.touch_updated_at() RETURNS trigger AS $$
  BEGIN
      NEW.updated_at = now();
      RETURN NEW;
  END;
  $$ LANGUAGE plpgsql;
  --rollback DROP FUNCTION IF EXISTS platform.touch_updated_at();
  ```

## 6. Migration Lifecycle & Helper Tool (`./scripts/db.sh`)
Developers must use `./scripts/db.sh` for local database lifecycle management:

- **Migrate (Apply all pending changesets)**:
  ```bash
  ./scripts/db.sh migrate
  ```
- **Rollback (Revert last changeset)**:
  ```bash
  ./scripts/db.sh rollback
  ```
- **Rollback Multiple Steps**:
  ```bash
  ./scripts/db.sh rollback 5
  ```
- **Rollback to Release Tag**:
  ```bash
  ./scripts/db.sh rollback-to v1.0.0
  ```
- **Tag Database**:
  ```bash
  ./scripts/db.sh tag v1.0.0
  ```
- **Fresh / Refresh (Reset all schemas & re-migrate from scratch - LOCAL ONLY)**:
  ```bash
  ./scripts/db.sh fresh
  ```
- **Check Migration Status**:
  ```bash
  ./scripts/db.sh status
  ```

> ⚠️ **Production Safety Guard:**
> `fresh` / `dropAll` commands are strictly blocked in production environments (`SPRING_PROFILES_ACTIVE=prod` or `ENVIRONMENT=prod`). Schema evolution in production is strictly forward-migrating or explicitly tagged rollback.

---

## 7. Database Seeders Separation (Configuration vs Dummy)
Database seeders dipisahkan secara ketat dari migrasi DDL (`db/changelog/`):

1. **Struktur Direktori**:
   - `src/main/resources/db/seed/config/`: Master/system configuration data (misal: default `platform.plans`). Berlaku untuk semua environment (**dev, staging, prod**).
   - `src/main/resources/db/seed/dummy/`: Mock/fixture test data (misal: test user `admin@hyperdata.id`, test tenant `hyperdata`, sample products). **Hanya untuk dev & staging (DILARANG di prod)**.
   - Master files: `db/seed/db.seed-config-master.yaml` dan `db/seed/db.seed-dummy-master.yaml`.

2. **Perintah Seeder via `./scripts/db.sh`**:
   - **`./scripts/db.sh seed-config`**: Menjalankan seeder konfigurasi (dev, staging, prod).
   - **`./scripts/db.sh seed-dummy`**: Menjalankan seeder data dummy (hanya dev & staging, otomatis di-block di prod).
   - **`./scripts/db.sh seed`**: Menjalankan kedua seeder secara berurutan (hanya dev & staging).

---

## 8. DOs and DON'Ts (Anti-Patterns & Best Practices)

Tabel panduan cepat untuk mencegah kesalahan umum (*common pitfalls*) saat menulis atau memodifikasi migrasi database:

| Topik | ✅ DO (Wajib / Dianjurkan) | ❌ DON'T (Dilarang Keras) |
| :--- | :--- | :--- |
| **Changeset Granularity** | Pisahkan 1 objek database (tabel, view, trigger, dll.) ke dalam 1 changeset unik (`Atomic Changeset`). | **JANGAN** menggabungkan puluhan tabel atau DDL yang tidak berhubungan ke dalam satu changeset raksasa. |
| **Changeset Immutability** | Perlakukan changeset yang sudah di-*merge* / dieksekusi sebagai **immutable**. Buat changeset baru jika perlu koreksi. | **JANGAN PERNAH** mengedit, menghapus, atau mereorder changeset lama yang sudah pernah jalan (akan memicu *Checksum Mismatch Error*). |
| **Rollback Placement** | Tulis `--rollback` langsung tepat di bawah baris DDL dalam changeset yang sama. | **JANGAN** menumpuk semua `--rollback` di bagian paling bawah file, dan **JANGAN** membuat changeset tanpa `--rollback`. |
| **Rollback Statement** | Berikan statement pembatalan yang presisi (misal `DROP TABLE IF EXISTS ... CASCADE`). | **JANGAN** menggunakan `--rollback empty` kecuali pada operasi data purge/destruktif yang memang tidak bisa dikembalikan (dan wajib beri alasan). |
| **Foreign Key (Baseline)** | Pisahkan pembuatan FK ke file terpisah setelah seluruh tabel terdaftar (`06-foreign-keys.sql`). | **JANGAN** menulis FK inline di DDL baseline sebelum tabel relasinya dibuat (memicu *Circular Reference / Table Not Found*). |
| **Procedural / PL/pgSQL** | Pasang `splitStatements:false` pada header changeset function, trigger, atau blok `DO $$`. | **JANGAN** membiarkan Liquibase memecah blok kode PL/pgSQL berdasarkan titik koma (`;`) yang ada di dalam body fungsi. |
| **Spring JPA / Hibernate** | Pasang `spring.jpa.hibernate.ddl-auto=none` atau `validate`. Liquibase adalah satu-satunya otoritas skema. | **JANGAN** gunakan `ddl-auto=update` atau `create` di konfigurasi manapun. |
| **Direct DB Modification** | Semua perubahan skema wajib dicatat melalui file changeset Liquibase. | **JANGAN** melakukan `ALTER TABLE`, `CREATE INDEX`, dll. secara manual langsung via GUI (DBeaver, pgAdmin) tanpa changeset. |
| **Environment Safety** | Gunakan command `./scripts/db.sh fresh` atau `dropAll` hanya di mesin lokal dev. | **JANGAN PERNAH** menjalankan script reset / destructive drop di environment Staging atau Production. |
| **Index Creation (Big Tables)** | Untuk tabel besar atau produksi, pertimbangkan pembuatan index dengan pola non-blocking jika didukung. | **JANGAN** mengunci tabel dengan lock eksklusif lama saat menambahkan constraint/index di jam operasional tinggi. |
| **Seeder Separation** | Pisahkan data konfigurasi (`seed-config`) dari data dummy pengujian (`seed-dummy`). | **JANGAN** mencampur data dummy developer ke dalam file migrasi DDL utama atau master changelog production. |
| **Dynamic Scaling & Multi-Replica** | Jalankan migrasi via Dedicated One-Shot Job sebelum container aplikasi menyala (`LIQUIBASE_ENABLED=false` pada backend server). | **JANGAN** membiarkan banyak container aplikasi menjalankan auto-migrasi bersamaan saat booting (memicu lock deadlock). |

---

## 9. Dynamic Scaling & Dedicated Migration Runner (Production Standard)

Untuk mendukung **horizontal dynamic scaling** (multi-replica container):
1. **Container Server Backend**:
   - Pada environment Staging & Production, environment variable `LIQUIBASE_ENABLED` pada container aplikasi utama **wajib diset `false`**.
2. **Dedicated One-Shot Runner**:
   - Skrip deployment / CD pipeline wajib menjalankan kontainer migrasi one-shot terpisah (`db:migrate`) sampai selesai dan sukses (*exit 0*) sebelum menaikkan versi container server yang baru.

