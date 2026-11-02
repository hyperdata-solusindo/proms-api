# Rule 07: Database & Liquibase Rules

## 1. Table & Column Naming
- Database tables: Plural `snake_case` (e.g. `products`, `sales_orders`, `users`).
- Column names: `snake_case` (e.g. `first_name`, `created_at`, `order_date`).
- Primary keys: UUID `id` (or BIGINT IDENTITY where partitioned/high-volume audit logs require it).
- Foreign keys: `<singular_table>_id` (e.g. `customer_id`, `product_id`).
- Tables must have audit columns: `created_at` (timestamp with time zone, not null).

## 2. Liquibase Migration Authority
- Hibernate DDL auto is set to `validate` or `none` (`spring.jpa.hibernate.ddl-auto=none`).
- ❌ **NEVER rely on Hibernate to create or alter tables**.
- All schema changes must be written as Liquibase changelogs in `src/main/resources/db/changelog/`.
- Every changeset must specify an explicit `author` and descriptive `id`.
- Master changelog is located at `src/main/resources/db/changelog/db.changelog-master.yaml`.

## 3. Placement & Separation Matrix (Enterprise Best Practice)
Terkait pemisahan file vs inline di bawah DDL tabel, aturan baku dibagi berdasarkan tipe komponen dan fase migrasi:

| Komponen | Posisi Rekomendasi | Alasan & Aturan |
| :--- | :--- | :--- |
| **Primary Key & NOT NULL** | **Bersama DDL Tabel** (Inline) | Melekat erat pada identitas entitas. Tabel tidak valid jika tidak memiliki identitas primer saat dibuat. |
| **Check Constraint** | **Bersama DDL Tabel** | Berhubungan murni dengan validasi kolom internal tabel tersebut (misal: `CHECK (char_length(name) <= 60)`). |
| **Foreign Key (FK)** | **DIPISAH FILE / DIEKSEKUSI TERPISAH** ⭐ | **Mencegah masalah *Chicken-and-Egg* (Forward & Circular Reference)** antar tabel. Seluruh tabel harus eksis sebelum relasi dipasang. |
| **Secondary Index (B-Tree, GIN, Partial)** | **Dipisah (Baseline)** / **Bersama Tabel (Fitur Baru)** | Memisahkan struktur data dasar dengan jalur optimasi query. Memudahkan *bulk insert* atau *data seeding* awal. |

### Panduan Berdasarkan Fase Pengembangan:
1. **Fase Inisialisasi / Baseline (Skema Utama)**:
   - Harus mengikuti urutan bertahap:
     1. Buat ekstensi & skema (`01-extensions-and-schemas.sql`)
     2. Buat fungsi umum (`02-functions.sql`)
     3. Buat kerangka tabel tanpa FK (`03-platform-tables.sql`, `04-proms-tables.sql`)
     4. Pasang Primary Key, Unique Constraints, dan Indeks (`05-indexes-and-constraints.sql`)
     5. Pasang Foreign Keys (`06-foreign-keys.sql`)
     6. Pasang Views, Triggers, RLS, dan Grants (`07` s/d `10`)
2. **Fase Penambahan Fitur Harian (Day-to-Day Feature Evolution)**:
   - Saat menambahkan fitur/modul baru (misalnya tabel `proms.discounts`), tabel referensi induk (`tenants`, `invoices`, dll) sudah pasti ada di database.
   - Oleh karena itu, pengembang **diperbolehkan menyatukan DDL tabel, constraint, indeks, dan FK dalam satu file changeset fitur tersebut**.

---

## 4. Atomic Changeset Strategy & Inline Rollback (Enterprise Best Practice)
To guarantee isolation, granular tracking in `DATABASECHANGELOG`, and safe LIFO (*Last In, First Out*) rollbacks, every database object must follow the **Atomic Changeset Pattern**:

1. **One Logical Object per Changeset**:
   - Each table, view, function, or trigger must reside in its own dedicated changeset.
   - Naming convention: `<file-prefix>-<type>-<object_name>`, for example:
     - Table: `--changeset hyproms:03-tbl-tenants`
     - Table: `--changeset hyproms:04-tbl-tasks`
     - Function: `--changeset hyproms:02-func-touch_updated_at__`
     - View: `--changeset hyproms:07-view-v_current_tenant`
     - Trigger: `--changeset hyproms:08-trg-boards_touch`
     - RLS: `--changeset hyproms:09-rls-platform_tenants`
     - Foreign Keys: `--changeset hyproms:06-fk-proms_tasks`

2. **Placement of `--rollback` Directly Beneath DDL**:
   - The `--rollback` statement **must be placed directly beneath the DDL statement** inside the same changeset (not lumped at the bottom of the changelog file).
   - This ensures cohesive co-location of forward DDL and reverse DDL logic.

```sql
--liquibase formatted sql

--changeset hyproms:03-tbl-audit_logs
CREATE TABLE platform.audit_logs (
    id bigint NOT NULL,
    tenant_id uuid,
    action text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
)
PARTITION BY RANGE (created_at);
--rollback DROP TABLE IF EXISTS platform.audit_logs CASCADE;

--changeset hyproms:03-tbl-tenants
CREATE TABLE platform.tenants (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name text NOT NULL,
    slug public.citext NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);
--rollback DROP TABLE IF EXISTS platform.tenants CASCADE;
```

### Rollback Guidelines per Operation Type:
- **`CREATE TABLE ...`**:
  ```sql
  --rollback DROP TABLE IF EXISTS <schema>.<table_name> CASCADE;
  ```
- **`ALTER TABLE ... ADD COLUMN ...`**:
  ```sql
  --rollback ALTER TABLE <schema>.<table_name> DROP COLUMN IF EXISTS <column_name>;
  ```
- **`CREATE INDEX ...`**:
  ```sql
  --rollback DROP INDEX IF EXISTS <schema>.<index_name> CASCADE;
  ```
- **`ALTER TABLE ... ADD CONSTRAINT ...`**:
  ```sql
  --rollback ALTER TABLE ONLY <schema>.<table_name> DROP CONSTRAINT IF EXISTS <constraint_name> CASCADE;
  ```
- **`CREATE TRIGGER ...`**:
  ```sql
  --rollback DROP TRIGGER IF EXISTS <trigger_name> ON <schema>.<table_name>;
  ```
- **`CREATE VIEW ...`**:
  ```sql
  --rollback DROP VIEW IF EXISTS <schema>.<view_name>;
  ```
- **`CREATE OR REPLACE FUNCTION ...`**:
  ```sql
  --rollback DROP FUNCTION IF EXISTS <schema>.<function_name>(<argument_types>);
  ```
- **Irreversible changes (e.g. data purge or lossy alterations)**:
  ```sql
  --rollback empty
  ```
  *(Note: Use `--rollback empty` only when truly irreversible, and document reason in changeset comments).*

## 5. Functions, Triggers & Multi-Statement Blocks
PostgreSQL procedural blocks (`CREATE OR REPLACE FUNCTION`, `CREATE TRIGGER`, `DO $$ ... $$`) contain internal semicolons (`;`).
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

## 7. DOs and DON'Ts (Anti-Patterns & Best Practices)

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

### Contoh Kontras Anti-Pattern vs Best Practice:

#### 1. Changeset Gabungan vs Atomic Changeset

❌ **SALAH (Anti-Pattern - Non-Atomic & Rollback Tercecer):**
```sql
--liquibase formatted sql
--changeset hyproms:create_multiple_tables
CREATE TABLE platform.tenants (id uuid PRIMARY KEY, name text NOT NULL);
CREATE TABLE platform.users (id uuid PRIMARY KEY, email text NOT NULL);
CREATE TABLE platform.roles (id uuid PRIMARY KEY, name text NOT NULL);
-- Masalah: Jika tabel roles gagal, rollback parsial tidak jelas, dan tracking di DATABASECHANGELOG kabur.
```

✅ **BENAR (Atomic Changeset + Inline Rollback):**
```sql
--liquibase formatted sql

--changeset hyproms:03-tbl-tenants
CREATE TABLE platform.tenants (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name text NOT NULL
);
--rollback DROP TABLE IF EXISTS platform.tenants CASCADE;

--changeset hyproms:03-tbl-users
CREATE TABLE platform.users (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    email public.citext NOT NULL
);
--rollback DROP TABLE IF EXISTS platform.users CASCADE;
```

#### 2. Fungsi / Trigger Tanpa `splitStatements:false`

❌ **SALAH (Akan crash saat dieksekusi Liquibase):**
```sql
--changeset hyproms:func_touch
CREATE OR REPLACE FUNCTION platform.touch_updated_at() RETURNS trigger AS $$
BEGIN
    NEW.updated_at = now(); -- Liquibase memotong di sini dan mengirim query tidak lengkap ke Postgres!
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
```

✅ **BENAR (Menggunakan `splitStatements:false`):**
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

#### 3. Mengubah Changeset Lama vs Menambah Changeset Baru

❌ **SALAH (Mengedit baris DDL yang sudah pernah dijalankan):**
```diff
--changeset hyproms:03-tbl-tenants
 CREATE TABLE platform.tenants (
     id uuid DEFAULT gen_random_uuid() NOT NULL,
     name text NOT NULL,
+    is_active boolean DEFAULT true NOT NULL
 );
 -- ERROR: Checksum validation failed! Database changelog lock collision.
```

✅ **BENAR (Membuat Changeset Baru untuk Perubahan Kolom):**
```sql
--changeset hyproms:2026-11-add-is_active-to-tenants
ALTER TABLE platform.tenants ADD COLUMN is_active boolean DEFAULT true NOT NULL;
--rollback ALTER TABLE platform.tenants DROP COLUMN IF EXISTS is_active;
```
