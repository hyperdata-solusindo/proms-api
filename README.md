# Platform Service (ERP Backend)

Spring Boot backend service untuk platform ERP.

---

## Konfigurasi IDE & Editor (VS Code / Antigravity)

Project ini telah dikonfigurasi agar ramah bagi developer yang menggunakan **VS Code** atau **Antigravity** dengan integrasi pemformatan kode (_code formatter_) otomatis yang selaras antara editor lokal dan build pipeline.

### Ekstensi yang Direkomendasikan

Buka menu Extensions (`Ctrl+Shift+X` / `Cmd+Shift+X`) dan pasang ekstensi resmi berikut:

- **Language Support for Java(TM) by Red Hat** (`redhat.java`) — _Language Server, IntelliSense, navigasi kode, dan default Java formatter_.
- **Extension Pack for Java** (`vscjava.vscode-java-pack`) — _Dukungan debugging & test runner_.

### Sinkronisasi Formatter Red Hat Java & Spotless

Proyek ini menggunakan profil Eclipse Formatter yang selaras dengan task `./gradlew spotlessCheck`:

- **File Profil**: [`.vscode/eclipse-java-formatter.xml`](.vscode/eclipse-java-formatter.xml)
- **Nama Profil**: `PromsJavaFormat`
- **Spesifikasi Standar**:
  - Batas lebar baris (_maximum line width_): **120 karakter**
  - Ukuran tabulasi & indentasi: **4 spasi**
  - Kelanjutan baris (_continuation indent_): **1 level (+4 spasi)**
  - Dukungan Fluent API / Builder pattern (misal: YAVI `ValidatorBuilder`)
  - Standar tanda kurung: Egyptian Braces / 1TBS (`end_of_line`)

Pengaturan workspace di [`.vscode/settings.json`](.vscode/settings.json) telah otomatis mengaktifkan:

```json
{
  "[java]": {
    "editor.defaultFormatter": "redhat.java",
    "editor.tabSize": 4,
    "editor.insertSpaces": true
  },
  "editor.formatOnSave": true,
  "java.format.enabled": true,
  "java.format.settings.url": ".vscode/eclipse-java-formatter.xml",
  "java.format.settings.profile": "PromsJavaFormat"
}
```

> 💡 **Verifikasi Settings di Editor**:
> Jika Anda mengubah pengaturan formatter di UI VS Code / Antigravity (`Ctrl+,`), pastikan nilainya sesuai:
>
> - **`Java > Format > Settings: Url`**: `.vscode/eclipse-java-formatter.xml`
> - **`Java > Format > Settings: Profile`**: `PromsJavaFormat`
>
> Dengan konfigurasi ini, setiap kali Anda menyimpan file (`Ctrl+S`), Red Hat Java akan otomatis merapikan kode dengan format yang **100% lolos uji `./gradlew spotlessCheck`**.

---

## Prasyarat & Panduan Instalasi (Prerequisites & Installation)

Berikut kebutuhan sistem (tools & dependencies) yang perlu diinstal sebelum menjalankan project ini:

| Requirement                    | Versi Rekomendasi                   | Keterangan                                                              |
| :----------------------------- | :---------------------------------- | :---------------------------------------------------------------------- |
| **Java Development Kit (JDK)** | **OpenJDK 21** / Eclipse Temurin 21 | Wajib untuk build & run lokal (`build.gradle.kts` menargetkan Java 21)  |
| **Gradle**                     | Wrapper (`./gradlew`)               | Tidak perlu diinstal manual, sudah disertakan dalam repositori          |
| **Docker & Docker Compose**    | Versi terbaru                       | Direkomendasikan untuk menjalankan database PostgreSQL & kontainerisasi |
| **Git**                        | Versi terbaru                       | Version control system                                                  |

---

### Cara Install Requirement

#### 1. Instalasi Java Development Kit (JDK 21)

Pilih salah satu metode instalasi sesuai OS yang Anda gunakan:

##### Opsi A: Menggunakan Package Manager Sistem

- **Ubuntu / Debian**:

  ```bash
  sudo apt update
  sudo apt install -y openjdk-21-jdk
  ```

  Verifikasi instalasi dan lokasi instalasi:

  ```bash
  java -version
  # Cek letak JAVA_HOME:
  update-alternatives --list java
  # Contoh letak default: /usr/lib/jvm/java-21-openjdk-amd64
  ```

- **macOS (via Homebrew)**:

  ```bash
  brew install openjdk@21
  ```

  Symlink agar sistem mendeteksi JDK:

  ```bash
  sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk
  ```

- **Windows**:
  Unduh installer JDK 21 (misal dari [Eclipse Temurin 21](https://adoptium.net/temurin/releases/?version=21)) atau via `winget`:
  ```powershell
  winget install EclipseAdoptium.Temurin.21.JDK
  ```

##### Opsi B: Menggunakan SDKMAN! (Multi-platform: Linux, macOS, WSL)

SDKMAN adalah manajer versi JDK yang sangat mudah digunakan:

```bash
# 1. Install SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# 2. Install JDK 21 (Eclipse Temurin)
sdk install java 21.0.6-tem

# 3. Jadikan versi aktif/default
sdk use java 21.0.6-tem
```

---

#### 2. Instalasi Docker & Docker Compose

Docker dibutuhkan jika Anda ingin menjalankan PostgreSQL atau service aplikasi dalam bentuk container:

- **Ubuntu / Debian**:
  ```bash
  sudo apt update
  sudo apt install -y docker.io docker-compose-v2
  # Izinkan user saat ini menjalankan docker tanpa sudo:
  sudo usermod -aG docker $USER
  newgrp docker
  ```
- **macOS / Windows**:
  Unduh dan pasang [Docker Desktop](https://www.docker.com/products/docker-desktop/).

Verifikasi instalasi Docker:

```bash
docker --version
docker compose version
```

---

#### 3. Gradle (Wrapper)

Project ini menggunakan **Gradle Wrapper** (`./gradlew` di Linux/macOS atau `gradlew.bat` di Windows). Anda **tidak perlu menginstal Gradle secara manual**.

Pastikan file wrapper memiliki izin eksekusi (Linux/macOS):

```bash
chmod +x ./gradlew
```

---

## Konfigurasi Lingkungan (`.env`)

Project ini mendukung file `.env` di root direktori project. Melalui konfigurasi `spring.config.import` pada [`application.properties`](file:///home/kholifan/html/project-promps/api/src/main/resources/application.properties):

```properties
spring.config.import=optional:file:./.env[.properties],optional:file:../.env[.properties]
```

Spring Boot secara **otomatis membaca seluruh key-value pairs dari `.env`** saat aplikasi dijalankan (baik via `./gradlew bootRun`, Docker, maupun dijalankan langsung dari IDE). Variabel seperti `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, dan `APP_PORT` yang ada di `.env` akan langsung mengisi placeholder `${DB_NAME}`, `${DB_PORT}`, dll. di `application.properties`.

### 1. Salin Template Environment

Salin file template `.env.example` ke `.env`:

```bash
cp .env.example .env
```

### 2. Atur `JAVA_HOME` (Untuk Menjalankan Lokal Tanpa Docker)

Buka file `.env` dan pastikan direktori `JAVA_HOME` sesuai dengan instalasi JDK 21 di mesin Anda:

- **Linux (Ubuntu/Debian)**:
  ```env
  JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
  ```
- **macOS (Homebrew Apple Silicon)**:
  ```env
  JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
  ```
- **macOS (Homebrew Intel)**:
  ```env
  JAVA_HOME=/usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
  ```

### 3. Konfigurasi Token & Sliding Expiration (Mobile-Friendly)

PAT (_Personal Access Token_) dilengkapi dengan fitur **Sliding Expiration (Rolling Session)** yang sangat ramah pengguna mobile:

```env
AUTH_TOKEN_TTL_DAYS=30
AUTH_TOKEN_SLIDING_WINDOW_DAYS=7
```

- **TTL Awal**: 30 hari sejak pengguna login.
- **Sliding Renewal**: Setiap kali token digunakan untuk mengakses API, sistem mengecek sisa masa berlaku. Jika sisa masa berlaku **kurang dari 7 hari**, server otomatis memperpanjang tanggal kadaluwarsa token (`expiresAt`) menjadi **+30 hari lagi**. Pengguna mobile yang rutin membuka aplikasi tidak akan pernah ter-logout mendadak.

---

## Panduan Menjalankan Project

Anda dapat memilih antara **Pengembangan Lokal (Recommended untuk Developer)** atau **Full Docker Stack**.

### Metode 1: Pengembangan Lokal (Recommended untuk Development)

Metode ini paling cepat untuk siklus coding harian:

#### 1. Jalankan PostgreSQL

Nyalakan service database di latar belakang:

```bash
docker compose up -d postgres
```

Database PostgreSQL akan berjalan di port `5443` (sesuai konfigurasi `.env`).

#### 2. Jalankan Migrasi Skema & Seeder

Gunakan script pembantu [`scripts/db.sh`](file:///home/kholifan/html/project-promps/api/scripts/db.sh):

```bash
# 1. Jalankan migrasi DDL skema database
./scripts/db.sh migrate

# 2. Jalankan seeder (konfigurasi master data + dummy test data)
./scripts/db.sh seed
```

#### 3. Jalankan Aplikasi Spring Boot

```bash
./gradlew bootRun
```

Aplikasi akan aktif di **`http://localhost:8080`**.

---

### Metode 2: Menjalankan Seluruh Stack dengan Docker Compose

Untuk menjalankan seluruh stack (PostgreSQL + Init-Container Migrasi Liquibase + Spring Boot Server) secara otomatis:

```bash
# 1. Jalankan seluruh layanan di latar belakang
docker compose up -d

# 2. Pantau log server aplikasi
docker compose logs -f app

# 3. Menghentikan seluruh container
docker compose down
# (Tambahkan opsi -v jika ingin mereset/menghapus data volume database: docker compose down -v)
```

---

## Manajemen Database & Seeder (`./scripts/db.sh`)

Sesuai arsitektur enterprise [Rule 07](file:///home/kholifan/html/project-promps/api/docs/rules/07-database-and-liquibase.md), migrasi skema (DDL) dan pengisian data seeder (DML) dipisahkan secara ketat:

| Perintah                      | Deskripsi                                                               | Target Environment                                  |
| :---------------------------- | :---------------------------------------------------------------------- | :-------------------------------------------------- |
| `./scripts/db.sh migrate`     | Menjalankan migrasi DDL skema database saja (tanpa data).               | Dev, Staging, Production                            |
| `./scripts/db.sh seed-config` | Menjalankan seeder konfigurasi awal/paket langganan (`platform.plans`). | Dev, Staging, Production                            |
| `./scripts/db.sh seed-dummy`  | Menjalankan seeder akun & data mock untuk keperluan testing.            | **Hanya Dev & Staging** _(Auto-abort jika di Prod)_ |
| `./scripts/db.sh seed`        | Menjalankan `seed-config` dan `seed-dummy` berurutan.                   | **Hanya Dev & Staging**                             |
| `./scripts/db.sh status`      | Memeriksa riwayat changeset yang sudah / belum diaplikasikan.           | Semua Environment                                   |
| `./scripts/db.sh fresh`       | Drop seluruh schema & migrasi ulang dari awal (Hanya Mesin Lokal).      | **DEV ONLY**                                        |

### Contoh Proses Eksekusi Seeder:

```bash
$ ./scripts/db.sh seed
============================================================
  PROMS Liquibase Migration Helper
  Target: jdbc:postgresql://localhost:5443/proms (User: platform_user)
============================================================
[ACTION] 1/2: Seeding system configurations...
Starting Liquibase at ...
Running Changeset: db/seed/config/01-platform-plans-seed.sql::seed-config-01-plans::hyproms
Liquibase: Update has been successful. Rows affected: 4
[ACTION] 2/2: Seeding test dummy data...
Starting Liquibase at ...
Running Changeset: db/seed/dummy/01-platform-test-users-seed.sql::seed-dummy-01-tenant::hyproms
Running Changeset: db/seed/dummy/01-platform-test-users-seed.sql::seed-dummy-02-users::hyproms
Running Changeset: db/seed/dummy/01-platform-test-users-seed.sql::seed-dummy-03-members::hyproms
Running Changeset: db/seed/dummy/01-platform-test-users-seed.sql::seed-dummy-04-subscriptions::hyproms
Liquibase: Update has been successful. Rows affected: 13
[SUCCESS] All seeders completed successfully.
```

---

## Dokumentasi API Interaktif (Scalar OpenAPI)

Project ini dilengkapi dokumentasi interaktif modern menggunakan **Scalar UI** yang terhubung langsung ke OpenAPI spec Springdoc:

- **Halaman Dokumentasi Scalar UI**: [`http://localhost:8080/docs`](http://localhost:8080/docs)
- **Raw OpenAPI 3 JSON Spec**: [`http://localhost:8080/v3/api-docs`](http://localhost:8080/v3/api-docs)

> 💡 **100% Otomatis**: Setiap penambahan `@RestController`, rute endpoint, dan Java 21 `record` DTO akan langsung muncul secara realtime di halaman `/docs` tanpa perlu proses generate manual.

---

## Akun Pengujian & Uji Coba Endpoint

Setelah menjalankan `./scripts/db.sh seed`, database terisi dengan data uji multi-tenant dan multi-role berikut:

### Daftar Dummy User & Tenant Siap Pakai:

|  No   | Tenant Name            | Tenant Slug     | Email                    | Password      | Full Name       | Role di Tenant | Status Akun |
| :---: | :--------------------- | :-------------- | :----------------------- | :------------ | :-------------- | :------------- | :---------- |
| **1** | PT Hyperdata Solusindo | **`hyperdata`** | **`admin@hyperdata.id`** | `password123` | Hyperdata Admin | **`owner`**    | `active`    |
| **2** | PT Hyperdata Solusindo | **`hyperdata`** | **`user@hyperdata.id`**  | `password123` | Regular Member  | **`member`**   | `active`    |
| **3** | Acme Corporation       | **`acme`**      | **`admin@hyperdata.id`** | `password123` | Hyperdata Admin | **`admin`**    | `active`    |

> 📌 **Fitur Multi-Tenancy**: Akun `admin@hyperdata.id` terdaftar di dua tenant sekaligus (`hyperdata` sebagai **owner** dan `acme` sebagai **admin**). Workspace aktif pertama akan dipilih otomatis, dan daftar workspace lain tersedia di properti `otherTenants` pada endpoint `/me`.

### 1. Test Login (Mendapatkan Token PAT)

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@hyperdata.id",
    "password": "password123"
  }'
```

Contoh respon berhasil (`200 OK`):

```json
{
  "statusCode": 200,
  "message": "Berhasil login",
  "data": {
    "token": "1|01b65a7568a0a033e56685a2a6786285ec6e131dbabe40a4815d46e6c574304b",
    "tokenType": "Bearer",
    "user": {
      "id": "a0000000-0000-0000-0000-000000000001",
      "fullName": "Hyperdata Admin",
      "email": "admin@hyperdata.id",
      "locale": "id",
      "timezone": "Asia/Jakarta"
    },
    "tenant": {
      "id": "e0e0e0e0-0000-0000-0000-000000000001",
      "slug": "hyperdata",
      "name": "PT Hyperdata Solusindo"
    },
    "workspaceRole": "owner"
  }
}
```

### 2. Test Mengambil Data Profil (`/me`)

```bash
curl -X GET http://localhost:8080/me \
  -H "Authorization: Bearer <TOKEN_PAT_DARI_LOGIN>"
```

Contoh respon berhasil (`200 OK`):

```json
{
  "statusCode": 200,
  "message": "Berhasil",
  "data": {
    "user": {
      "id": "a0000000-0000-0000-0000-000000000001",
      "fullName": "Hyperdata Admin",
      "email": "admin@hyperdata.id"
    },
    "tenant": {
      "id": "e0e0e0e0-0000-0000-0000-000000000001",
      "slug": "hyperdata",
      "name": "PT Hyperdata Solusindo",
      "status": "active",
      "timezone": "Asia/Jakarta"
    },
    "workspaceRole": "owner",
    "features": {
      "standup": true,
      "points": true
    },
    "otherTenants": [
      {
        "slug": "acme",
        "name": "Acme Corporation"
      }
    ]
  }
}
```

### 3. Test Logout (Mencabut Token)

```bash
curl -X POST http://localhost:8080/auth/logout \
  -H "Authorization: Bearer <TOKEN_PAT_DARI_LOGIN>"
```

---

## Perintah Testing & Quality Check

Sebelum membuat commit atau push kode:

```bash
# 1. Merapikan format kode secara otomatis (Spotless)
./gradlew spotlessApply

# 2. Menjalankan test unit & controller
./gradlew test

# 3. Menjalankan verifikasi kualitas lengkap (Spotless + ArchUnit + Tests)
./gradlew check
```

---

## Catatan Git

File `.env` sudah terdaftar di [.gitignore](file:///home/kholifan/html/project-promps/api/.gitignore) sehingga konfigurasi sensitif tidak akan ter-commit ke repositori.
