# Architecture Decision Records (ADR)

Folder ini menyimpan seluruh keputusan arsitektural penting (*Architecture Decision Records*) yang diadopsi dalam repositori `proms-api-java`. Setiap catatan mendokumentasikan konteks masalah, opsi yang dipertimbangkan, keputusan final, serta konsekuensi dan trade-offs.

## Format Standar ADR

Setiap file ADR diberi nama berurutan: `XXXX-<judul-singkat>.md` dan memiliki struktur:
- **Nomor & Judul**
- **Status**: [ Proposed | Accepted | Deprecated | Superseded ]
- **Tanggal**: YYYY-MM-DD
- **Pengambil Keputusan**: Tim / Arsitek
- **Konteks & Masalah**: Latar belakang teknis atau bisnis mengapa keputusan ini dibutuhkan.
- **Keputusan**: Opsi yang dipilih dan bagaimana cara kerjanya.
- **Konsekuensi / Trade-offs**: Keuntungan (*pros*), kerugian (*cons*), dan mitigasi.
- **Pengecualian**: Kondisi khusus di mana aturan dapat dilonggarkan (jika ada).

## Indeks ADR

| Nomor | Judul | Status | Tanggal |
|---|---|---|---|
| [0001](0001-the-golden-stack-and-anti-cross-module-join.md) | The Golden Stack Pattern & Larangan SQL JOIN Lintas Modul | Accepted | 2026-11-05 |
| [0002](0002-modular-monolith-facade-boundary.md) | 2-Layer Modular Monolith & Isolasi Inter-Module Facade | Accepted | 2026-11-05 |
