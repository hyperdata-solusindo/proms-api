# ADR 0002: 2-Layer Modular Monolith & Isolasi Inter-Module Facade

- **Status**: Accepted
- **Tanggal**: 2026-11-05
- **Pengambil Keputusan**: Backend Engineering & Architecture Team
- **Rujukan Aturan**: [Rule 01: Architecture & Structure](../../rules/01-architecture-and-structure.md), [AGENTS.md](../../../AGENTS.md)

---

## 1. Konteks & Masalah

Arsitektur monolit tradisional sering kali membusuk (*software erosion*) menjadi "Big Ball of Mud", di mana komponen dari sembarang layer mengimpor dan memanipulasi langsung repository, entity JPA, atau service internal dari komponen lain.

Hal ini menyebabkan:
1. **Kebocoran Abstraksi**: Mengubah detail implementasi di satu modul merusak modul-modul lain di sekitarnya (*cascading failures*).
2. **Kebocoran JPA Entity**: Entity JPA yang diteruskan lintas modul memicu transaksi panjang (*lazy loading exceptions*, *detached entity issues*, dan *unintended dirty checks*).
3. **Keterikatan Tak Terkendali**: Modul tidak memiliki batas (*boundary*) yang jelas tentang apa yang publik dan apa yang privat.

---

## 2. Keputusan

Kami mengadopsi pola **2-Layer Modular Monolith**:
`com.hyperdata.proms.module.<domain>.<feature>`

Dengan aturan batas ketat:
1. **Enkapsulasi Privat**: Seluruh sub-package `constant`, `service`, `repository`, `entity`, `mapper`, `controller`, internal `dto`, dan `client/impl` adalah **privat internal milik fitur tersebut**. Modul lain dilarang mengimpor kelas dari sub-package ini.
2. **Public Gateway (Client Contract Interface)**: Satu-satunya pintu masuk komunikasi dari modul luar adalah interface di `client/contract/<Feature>Client.java` dan Java 21 `record` DTO di `client/dto/` (diimplementasikan oleh `@Component` di `client/impl/<Feature>ClientImpl.java`).
3. **Larangan Kebocoran Entity**: Controller dan Client dilarang keras mengembalikan entity JPA `@Entity`. Seluruh data yang keluar wajib dibungkus dalam Java 21 `record` DTO.
4. **Pencegahan Otomatis via ArchUnit**: Aturan batas antar-modul dan larangan pengembalian entity dipaksakan secara otomatis di build pipeline melalui unit test arsitektur `ArchitectureTest.java`.

---

## 3. Konsekuensi & Trade-Offs

### Keuntungan (Pros):
- **Batas Modul Terjaga**: Developer maupun AI Agent tidak dapat secara tidak sengaja mengimpor atau memanipulasi dependensi internal modul lain.
- **Enkapsulasi Data**: Entity JPA tidak bocor ke luar modul, menghilangkan bug transaksi dan lazy-loading.
- **Refactoring Aman**: Modul dapat mengubah struktur internal (misalnya mengganti JPA dengan JDBC template atau mengganti nama tabel) tanpa merusak modul pemanggil, selama kontrak publik `*Client` tetap dipertahankan.

### Kerugian & Biaya (Cons):
- Diperlukan pembuatan DTO publik di `client/dto/` dan mapper untuk mengonversi entity ke Client DTO.
- Mengharuskan tim untuk memikirkan kontrak publik sebelum mengimplementasikan fitur lintas modul.

### Pengecualian:
- Layer `common/security` (seperti `TokenAuthenticationFilter`) diperbolehkan mengonsumsi Client kontrak modul `identity` (`AuthClient`, `UserClient`), namun tetap terikat aturan dilarang menyentuh sub-package internal modul.
