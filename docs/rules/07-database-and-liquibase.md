# Rule 07: Database & Liquibase Rules

## 1. Table & Column Naming
- Database tables: Plural `snake_case` (e.g. `products`, `sales_orders`, `users`).
- Column names: `snake_case` (e.g. `first_name`, `created_at`, `order_date`).
- Primary keys: UUID `id`.
- Foreign keys: `<singular_table>_id` (e.g. `customer_id`, `product_id`).

## 2. Liquibase Migration Authority
- Hibernate DDL auto is set to `validate` or `none`.
- ❌ **NEVER rely on Hibernate to create or alter tables**.
- All schema changes must be written as Liquibase changelogs in `src/main/resources/db/changelog/`.
- Every migration must specify an `author` and descriptive `id`.
- Tables must have audit columns: `created_at` (timestamp, not null).
