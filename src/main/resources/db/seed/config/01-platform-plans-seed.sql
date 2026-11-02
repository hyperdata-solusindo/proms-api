--liquibase formatted sql

--changeset hyproms:seed-config-01-plans splitStatements:false
INSERT INTO platform.plans (id, code, name, price_monthly, currency, limits, features, is_active)
VALUES 
    ('10000000-0000-0000-0000-000000000001', 'free', 'Free Starter', 0, 'IDR', '{"max_users": 5, "max_boards": 2}'::jsonb, ARRAY['standup'], true),
    ('10000000-0000-0000-0000-000000000002', 'pro', 'Professional Team', 150000, 'IDR', '{"max_users": 25, "max_boards": 10}'::jsonb, ARRAY['standup', 'points', 'analytics'], true),
    ('10000000-0000-0000-0000-000000000003', 'enterprise', 'Enterprise Scale', 500000, 'IDR', '{"max_users": 100, "max_boards": 50}'::jsonb, ARRAY['standup', 'points', 'analytics', 'audit_logs', 'sso'], true)
ON CONFLICT (code) DO NOTHING;
--rollback DELETE FROM platform.plans WHERE code IN ('free', 'pro', 'enterprise');
