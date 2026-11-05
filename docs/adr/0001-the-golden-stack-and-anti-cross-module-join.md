# ADR 0001: The Golden Stack Pattern & Larangan SQL JOIN Lintas Modul

- **Status**: Accepted
- **Tanggal**: 2026-11-05
- **Pengambil Keputusan**: Backend Engineering & Architecture Team
- **Rujukan Aturan**: [Rule 11: The Golden Stack Pattern](../../rules/11-the-golden-stack.md), [AGENTS.md](../../../AGENTS.md)

---

## 1. Konteks & Masalah

Pada arsitektur Modular Monolith dengan banyak domain bisnis (seperti `identity`, `platform`, `sample/product`, `sales`, dll.), tabel database sering kali memiliki relasi konseptual lintas modul (misalnya transaksi penjualan mengacu ke `user_id` atau `product_id`).

Secara tradisional, pengembang sering kali:
1. Menulis **SQL JOIN lintas tabel modul lain** secara langsung di JPA repository.
2. Melakukan looping pemanggilan API/Facade di dalam kode Java (*N+1 query problem*).

Masalah yang timbul:
- **Tight Database Coupling**: Perubahan skema internal pada Modul B merusak query SQL milik Modul A.
- **Menghambat Migrasi Masa Depan**: Database yang saling terkait via SQL JOIN membuat pemisahan modul menjadi microservices di masa depan menjadi hampir mustahil tanpa refactoring total.
- **Degradasi Performa N+1**: Looping pemanggilan inter-module facade lambat dan membebani network/database pool.

---

## 2. Keputusan

Kami memutuskan untuk **melarang keras SQL JOIN lintas modul** dan mengadopsi pola **The Golden Stack** untuk seluruh kebutuhan pengayaan data (*data enrichment*) dan datatable:

Pola ini terdiri dari 3 pilar:
1. **Pillar 1 — L1 In-Memory Caching (Caffeine & Spring Cache)**:
   - Data master baca-tinggi di-cache di batas `*Facade` atau query service dengan isolasi tenant context. Latensi pembacaan turun hingga sub-mikrodetik (`< 0.001 ms`).
2. **Pillar 2 — `Lookups` Batch Processing Engine (`com.hyperdata.proms.common.util.Lookups`)**:
   - Mengekstrak sekumpulan ID unik dalam sekali jalan (`Lookups.toSet()`).
   - Mengambil data relasi dalam 1 batch query via Facade (`facade.findAllByIds(ids)`).
   - Mengindeks hasil batch ke in-memory map O(1) (`Lookups.toMap()`) dengan perlindungan null-safety dan penjaminan urutan pagination.
3. **Pillar 3 — Declarative Assembly (MapStruct)**:
   - Menggabungkan entity lokal dengan in-memory map menjadi DTO Response secara bersih dan deklaratif tanpa boilerplate.

---

## 3. Konsekuensi & Trade-Offs

### Keuntungan (Pros):
- **Isolasi Penuh**: Skema database masing-masing modul sepenuhnya independen.
- **Bebas Masalah N+1**: Tidak ada query berulang dalam iterasi loop; hanya ada 1 query root lokal + batch query per relasi modul luar.
- **Kesiapan Distribusi**: Jika salah satu modul dipindahkan menjadi service terpisah (microservice), logika layer service pemanggil tidak perlu diubah, hanya implementasi `Default<Feature>Facade` yang diarahkan ke REST/gRPC client.

### Kerugian & Biaya (Cons):
- Memerlukan sedikit lebih banyak kode setup (ekstraksi ID, query batch, map assembly) dibandingkan menulis satu query SQL JOIN kompleks.
- Memory JVM menampung map hasil batch selama durasi request (namun terkendali karena data dipaginasi: misalnya 20–100 item).

### Pengecualian:
- Tidak ada pengecualian untuk query runtime transactional/HTTP. Untuk reporting analitik skala besar (OLAP / BI / Data Warehouse), query agregasi dilakukan di luar monolit (via ETL / Read-Replica / Lakehouse), bukan melalui monolit runtime.
