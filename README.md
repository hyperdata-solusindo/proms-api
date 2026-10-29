# Platform Service (ERP Backend)

Spring Boot backend service untuk platform ERP.

---

## Prasyarat & Panduan Instalasi (Prerequisites & Installation)

Berikut kebutuhan sistem (tools & dependencies) yang perlu diinstal sebelum menjalankan project ini:

| Requirement | Versi Rekomendasi | Keterangan |
| :--- | :--- | :--- |
| **Java Development Kit (JDK)** | **OpenJDK 21** / Eclipse Temurin 21 | Wajib untuk build & run lokal (`build.gradle.kts` menargetkan Java 21) |
| **Gradle** | Wrapper (`./gradlew`) | Tidak perlu diinstal manual, sudah disertakan dalam repositori |
| **Docker & Docker Compose** | Versi terbaru | Direkomendasikan untuk menjalankan database PostgreSQL & kontainerisasi |
| **Git** | Versi terbaru | Version control system |

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

Project ini mendukung file `.env` di root direktori project untuk memuat variabel environment lokal (seperti `JAVA_HOME` dan konfigurasi database) secara otomatis saat menjalankan `./gradlew` maupun Docker Compose.

### 1. Salin Template Environment

Salin file template `.env.example` ke `.env`:

```bash
cp .env.example .env
```

### 2. Atur `JAVA_HOME` (Untuk Menjalankan Lokal Tanpa Docker)

Buka file `.env` dan pastikan direktori `JAVA_HOME` sesuai dengan instalasi JDK 21 di mesin Anda:

- **macOS (Homebrew Apple Silicon)**:
  ```env
  JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
  ```
- **macOS (Homebrew Intel)**:
  ```env
  JAVA_HOME=/usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
  ```
- **Linux (Ubuntu/Debian)**:
  ```env
  JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
  ```

---

## Menjalankan dengan Docker & Docker Compose

Project ini telah dilengkapi dengan [Dockerfile](file:///home/kholifan/html/project-erp/platform/docker/Dockerfile) dan [docker-compose.yml](file:///home/kholifan/html/project-erp/platform/docker-compose.yml) yang mengimplementasikan **Best Practice Init-Container**:

- **`erp-postgres`**: Database PostgreSQL 16 (Port host: `5443` -> Port container: `5432`).
- **`erp-migration`** (_Init Container_): Menjalankan migrasi DDL via Liquibase hingga selesai tuntas (`exit 0`), lalu berhenti.
- **`erp-platform`** (_App Server_): Spring Boot backend (Port host: `8080`) yang otomatis menyala **setelah** migrasi berhasil (`condition: service_completed_successfully`), dengan auto-migration dinonaktifkan (`LIQUIBASE_ENABLED: "false"`).

### 1. Menjalankan Seluruh Layanan

Jalankan seluruh stack container di latar belakang:

```bash
docker compose up -d
# atau via package script:
npm run docker:up
```

Docker Compose akan otomatis mengurutkan startup:

1. `erp-postgres` start dan dicek kesehatannya (_healthy_).
2. `erp-migration` dijalankan untuk mengeksekusi changelog DDL.
3. `erp-platform` dinyalakan setelah migrasi sukses.

### 2. Menjalankan Hanya PostgreSQL (Untuk Pengembangan Lokal)

Jika Anda ingin menjalankan database di Docker namun menjalankan Spring Boot via `./gradlew bootRun` di mesin host:

```bash
docker compose up -d postgres
# atau
npm run docker:db
```

Setelah PostgreSQL siap, jalankan aplikasi lokal:

```bash
./gradlew bootRun
```

### 3. Menjalankan CLI Database Manual (Migrasi, Seeder, Refresh)

Anda dapat menjalankan command CLI melalui service `migration`:

```bash
# Menjalankan migrasi skema database
docker compose run --rm migration db:migrate
# atau: npm run docker:migrate

# Menjalankan seeder database
docker compose run --rm migration db:seed
# atau: npm run docker:seed

# Me-refresh database (drop-all, migrate, & seed)
docker compose run --rm migration db:refresh
# atau: npm run docker:refresh
```

### 4. Melihat Log Aplikasi

```bash
docker compose logs -f app
# atau
npm run docker:logs
```

### 5. Membangun Ulang Image (Rebuild)

Setelah melakukan perubahan kode:

```bash
docker compose up -d --build
# atau
docker compose build
```

### 6. Menghentikan Kontainer

```bash
docker compose down
# Untuk menghapus volume data database juga (reset data):
docker compose down -v
```

---

## Menjalankan Aplikasi Secara Lokal (Tanpa Docker)

Jalankan Spring Boot secara lokal menggunakan Gradle wrapper:

```bash
# Menjalankan aplikasi Spring Boot
./gradlew bootRun

# Memeriksa versi Gradle & JVM yang aktif
./gradlew -v

# Menjalankan test
./gradlew test

# Melakukan build artifact
./gradlew build
```

---

## Catatan Git

File `.env` sudah terdaftar di [.gitignore](file:///home/kholifan/html/project-erp/platform/.gitignore) sehingga nilai konfigurasi sensitif atau path lokal masing-masing developer tidak akan ter-commit ke repository.
