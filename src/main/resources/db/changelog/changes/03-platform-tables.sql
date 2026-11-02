--liquibase formatted sql

--changeset hyproms:03-tbl-audit_logs
CREATE TABLE platform.audit_logs (
    id bigint NOT NULL,
    tenant_id uuid,
    actor_user_id uuid,
    impersonated_by uuid,
    action text NOT NULL,
    entity_type text,
    entity_id uuid,
    old_values jsonb,
    new_values jsonb,
    ip inet,
    created_at timestamp with time zone DEFAULT now() NOT NULL
)
PARTITION BY RANGE (created_at);

ALTER TABLE platform.audit_logs ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME platform.audit_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);
--rollback DROP TABLE IF EXISTS platform.audit_logs CASCADE;

--changeset hyproms:03-tbl-audit_logs_default
CREATE TABLE platform.audit_logs_default (
    id bigint NOT NULL,
    tenant_id uuid,
    actor_user_id uuid,
    impersonated_by uuid,
    action text NOT NULL,
    entity_type text,
    entity_id uuid,
    old_values jsonb,
    new_values jsonb,
    ip inet,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

ALTER TABLE ONLY platform.audit_logs ATTACH PARTITION platform.audit_logs_default DEFAULT;
--rollback DROP TABLE IF EXISTS platform.audit_logs_default CASCADE;

--changeset hyproms:03-tbl-invitations
CREATE TABLE platform.invitations (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    email public.citext NOT NULL,
    workspace_role text DEFAULT 'member'::text NOT NULL,
    token_hash text NOT NULL,
    invited_by uuid NOT NULL,
    expires_at timestamp with time zone DEFAULT (now() + '7 days'::interval) NOT NULL,
    accepted_at timestamp with time zone,
    revoked_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT invitations_workspace_role_check CHECK ((workspace_role = ANY (ARRAY['admin'::text, 'finance'::text, 'member'::text])))
);

ALTER TABLE ONLY platform.invitations FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS platform.invitations CASCADE;

--changeset hyproms:03-tbl-member_groups
CREATE TABLE platform.member_groups (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    name text NOT NULL,
    "position" integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by uuid,
    CONSTRAINT member_groups_name_check CHECK (((char_length(name) >= 1) AND (char_length(name) <= 60)))
);

ALTER TABLE ONLY platform.member_groups FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS platform.member_groups CASCADE;

--changeset hyproms:03-tbl-personal_access_tokens
CREATE TABLE platform.personal_access_tokens (
    id bigint NOT NULL,
    tokenable_type text NOT NULL,
    tokenable_id uuid NOT NULL,
    name text NOT NULL,
    token character varying(64) NOT NULL,
    abilities text,
    last_used_at timestamp with time zone,
    expires_at timestamp with time zone,
    created_at timestamp with time zone,
    updated_at timestamp with time zone
);

ALTER TABLE platform.personal_access_tokens ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME platform.personal_access_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);
--rollback DROP TABLE IF EXISTS platform.personal_access_tokens CASCADE;

--changeset hyproms:03-tbl-plans
CREATE TABLE platform.plans (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    code text NOT NULL,
    name text NOT NULL,
    price_monthly bigint NOT NULL,
    currency character(3) DEFAULT 'IDR'::bpchar NOT NULL,
    limits jsonb DEFAULT '{}'::jsonb NOT NULL,
    features text[] DEFAULT '{}'::text[] NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT plans_price_monthly_check CHECK ((price_monthly >= 0))
);
--rollback DROP TABLE IF EXISTS platform.plans CASCADE;

--changeset hyproms:03-tbl-subscriptions
CREATE TABLE platform.subscriptions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    plan_id uuid NOT NULL,
    status text NOT NULL,
    current_period_start timestamp with time zone NOT NULL,
    current_period_end timestamp with time zone NOT NULL,
    grace_until timestamp with time zone,
    provider text,
    provider_ref text,
    cancelled_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT subscriptions_check CHECK ((current_period_end > current_period_start)),
    CONSTRAINT subscriptions_status_check CHECK ((status = ANY (ARRAY['trial'::text, 'active'::text, 'past_due'::text, 'cancelled'::text])))
);

ALTER TABLE ONLY platform.subscriptions FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS platform.subscriptions CASCADE;

--changeset hyproms:03-tbl-team_members
CREATE TABLE platform.team_members (
    tenant_id uuid NOT NULL,
    team_id uuid NOT NULL,
    user_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    is_lead boolean DEFAULT false NOT NULL
);

ALTER TABLE ONLY platform.team_members FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS platform.team_members CASCADE;

--changeset hyproms:03-tbl-teams
CREATE TABLE platform.teams (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    name text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    deactivated_at timestamp with time zone,
    deactivated_by uuid
);

ALTER TABLE ONLY platform.teams FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS platform.teams CASCADE;

--changeset hyproms:03-tbl-tenant_members
CREATE TABLE platform.tenant_members (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    user_id uuid NOT NULL,
    role text DEFAULT 'member'::text NOT NULL,
    status text DEFAULT 'active'::text NOT NULL,
    weekly_capacity_min integer DEFAULT 2400 NOT NULL,
    hourly_cost bigint,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    location_id uuid,
    member_group_id uuid,
    CONSTRAINT tenant_members_hourly_cost_check CHECK ((hourly_cost >= 0)),
    CONSTRAINT tenant_members_role_check CHECK ((role = ANY (ARRAY['owner'::text, 'admin'::text, 'finance'::text, 'member'::text, 'guest'::text]))),
    CONSTRAINT tenant_members_status_check CHECK ((status = ANY (ARRAY['active'::text, 'deactivated'::text]))),
    CONSTRAINT tenant_members_weekly_capacity_min_check CHECK ((weekly_capacity_min >= 0))
);

ALTER TABLE ONLY platform.tenant_members FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS platform.tenant_members CASCADE;

--changeset hyproms:03-tbl-tenants
CREATE TABLE platform.tenants (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name text NOT NULL,
    slug public.citext NOT NULL,
    status text DEFAULT 'trial'::text NOT NULL,
    timezone text DEFAULT 'Asia/Jakarta'::text NOT NULL,
    branding jsonb DEFAULT '{}'::jsonb NOT NULL,
    settings jsonb DEFAULT '{}'::jsonb NOT NULL,
    trial_ends_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    max_boards integer,
    max_users integer,
    CONSTRAINT tenants_max_boards_check CHECK (((max_boards IS NULL) OR (max_boards > 0))),
    CONSTRAINT tenants_max_users_check CHECK (((max_users IS NULL) OR (max_users > 0))),
    CONSTRAINT tenants_status_check CHECK ((status = ANY (ARRAY['trial'::text, 'active'::text, 'past_due'::text, 'read_only'::text, 'suspended'::text, 'cancelled'::text])))
);
--rollback DROP TABLE IF EXISTS platform.tenants CASCADE;

--changeset hyproms:03-tbl-user_push_subscriptions
CREATE TABLE platform.user_push_subscriptions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    endpoint text NOT NULL,
    public_key text NOT NULL,
    auth_token text NOT NULL,
    content_encoding character varying(32) DEFAULT 'aes128gcm'::character varying NOT NULL,
    user_agent character varying(255),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);
--rollback DROP TABLE IF EXISTS platform.user_push_subscriptions CASCADE;

--changeset hyproms:03-tbl-users
CREATE TABLE platform.users (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    idp_subject text NOT NULL,
    email public.citext NOT NULL,
    full_name text NOT NULL,
    avatar_url text,
    locale text DEFAULT 'id'::text NOT NULL,
    timezone text,
    is_platform_staff boolean DEFAULT false NOT NULL,
    platform_role text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    password text,
    is_active boolean DEFAULT true NOT NULL,
    CONSTRAINT users_check CHECK (((platform_role IS NULL) OR is_platform_staff)),
    CONSTRAINT users_locale_check CHECK ((locale = ANY (ARRAY['id'::text, 'en'::text]))),
    CONSTRAINT users_platform_role_check CHECK ((platform_role = ANY (ARRAY['super_admin'::text, 'support'::text, 'billing'::text])))
);
--rollback DROP TABLE IF EXISTS platform.users CASCADE;

--changeset hyproms:03-tbl-work_locations
CREATE TABLE platform.work_locations (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    name text NOT NULL,
    "position" integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by uuid,
    CONSTRAINT work_locations_name_check CHECK (((char_length(name) >= 1) AND (char_length(name) <= 60)))
);

ALTER TABLE ONLY platform.work_locations FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS platform.work_locations CASCADE;
