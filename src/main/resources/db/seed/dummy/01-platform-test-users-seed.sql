--liquibase formatted sql

--changeset hyproms:seed-dummy-01-tenant splitStatements:false
INSERT INTO platform.tenants (id, name, slug, status, timezone)
VALUES 
    ('e0e0e0e0-0000-0000-0000-000000000001', 'PT Hyperdata Solusindo', 'hyperdata', 'active', 'Asia/Jakarta'),
    ('e0e0e0e0-0000-0000-0000-000000000002', 'Acme Corporation', 'acme', 'active', 'Asia/Jakarta')
ON CONFLICT (slug) DO NOTHING;
--rollback DELETE FROM platform.tenants WHERE slug IN ('hyperdata', 'acme');

--changeset hyproms:seed-dummy-02-users splitStatements:false
INSERT INTO platform.users (id, idp_subject, email, full_name, password, is_active, timezone, locale)
VALUES 
    ('a0000000-0000-0000-0000-000000000001', 'local|admin@hyperdata.id', 'admin@hyperdata.id', 'Hyperdata Admin', '$2a$10$jF9klPwJbKs.E5jqh/.fYeWhS/y9EdKX9cTmZ2LxbGCB29MvFrN6C', true, 'Asia/Jakarta', 'id'),
    ('a0000000-0000-0000-0000-000000000002', 'local|user@hyperdata.id', 'user@hyperdata.id', 'Regular Member', '$2a$10$jF9klPwJbKs.E5jqh/.fYeWhS/y9EdKX9cTmZ2LxbGCB29MvFrN6C', true, 'Asia/Jakarta', 'id')
ON CONFLICT (email) DO NOTHING;
--rollback DELETE FROM platform.users WHERE email IN ('admin@hyperdata.id', 'user@hyperdata.id');

--changeset hyproms:seed-dummy-03-members splitStatements:false
INSERT INTO platform.tenant_members (id, tenant_id, user_id, role, status)
VALUES 
    ('b0000000-0000-0000-0000-000000000001', 'e0e0e0e0-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'owner', 'active'),
    ('b0000000-0000-0000-0000-000000000002', 'e0e0e0e0-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000002', 'member', 'active'),
    ('b0000000-0000-0000-0000-000000000003', 'e0e0e0e0-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'admin', 'active')
ON CONFLICT (tenant_id, user_id) DO NOTHING;
--rollback DELETE FROM platform.tenant_members WHERE id IN ('b0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000003');

--changeset hyproms:seed-dummy-04-subscriptions splitStatements:false
INSERT INTO platform.subscriptions (id, tenant_id, plan_id, status, current_period_start, current_period_end)
VALUES 
    ('c0000000-0000-0000-0000-000000000001', 'e0e0e0e0-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002', 'active', now(), now() + interval '1 year'),
    ('c0000000-0000-0000-0000-000000000002', 'e0e0e0e0-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'active', now(), now() + interval '14 days')
ON CONFLICT (id) DO NOTHING;
--rollback DELETE FROM platform.subscriptions WHERE id IN ('c0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000002');
