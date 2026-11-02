--liquibase formatted sql

--changeset hyproms:07-func-tenant_member_counts__ splitStatements:false
CREATE OR REPLACE FUNCTION platform.tenant_member_counts() RETURNS TABLE(tenant_id uuid, internal_count bigint, guest_count bigint)
    LANGUAGE sql SECURITY DEFINER
    AS $$
  select
    tm.tenant_id,
    count(*) filter (where tm.role in ('owner','admin','finance','member') and tm.status = 'active') as internal_count,
    count(*) filter (where tm.role = 'guest' and tm.status = 'active') as guest_count
  from platform.tenant_members tm
  group by tm.tenant_id;
$$;
--rollback DROP FUNCTION IF EXISTS platform.tenant_member_counts();

--changeset hyproms:07-func-user_memberships_uuid___ splitStatements:false
CREATE OR REPLACE FUNCTION platform.user_memberships(target_user_ids uuid[]) RETURNS TABLE(user_id uuid, tenant_id uuid, tenant_slug public.citext, tenant_name text, role text, status text)
    LANGUAGE sql SECURITY DEFINER
    AS $$
        select tm.user_id,
               t.id as tenant_id,
               t.slug as tenant_slug,
               t.name as tenant_name,
               tm.role,
               tm.status
        from platform.tenant_members tm
        join platform.tenants t on t.id = tm.tenant_id
        where tm.user_id = any(target_user_ids)
        order by t.name;
    $$;
--rollback DROP FUNCTION IF EXISTS platform.user_memberships(uuid[]);

--changeset hyproms:07-func-find_open_invitation_text_ splitStatements:false
CREATE OR REPLACE FUNCTION proms.find_open_invitation(p_token_hash text) RETURNS TABLE(id uuid, tenant_id uuid, board_id uuid, email public.citext, board_role text, workstream_ids uuid[], expires_at timestamp with time zone)
    LANGUAGE sql STABLE SECURITY DEFINER
    SET search_path TO 'pg_catalog', 'proms'
    AS $$
  SELECT i.id, i.tenant_id, i.board_id, i.email, i.board_role, i.workstream_ids, i.expires_at
  FROM proms.board_invitations i
  WHERE i.token_hash = p_token_hash AND i.accepted_at IS NULL AND i.revoked_at IS NULL
  LIMIT 1
$$;
--rollback DROP FUNCTION IF EXISTS proms.find_open_invitation(text);

--changeset hyproms:07-view-v_current_tenant
CREATE VIEW platform.v_current_tenant AS
 SELECT tenants.id,
    tenants.name,
    tenants.slug,
    tenants.status,
    tenants.timezone,
    tenants.branding,
    tenants.settings
   FROM platform.tenants
  WHERE (tenants.id = platform.current_tenant());
--rollback DROP VIEW IF EXISTS platform.v_current_tenant;

--changeset hyproms:07-view-v_directory_members
CREATE VIEW platform.v_directory_members AS
 SELECT tm.tenant_id,
    tm.user_id,
    u.full_name,
    u.email,
    u.avatar_url,
    u.locale,
    COALESCE(u.timezone, t.timezone) AS timezone,
    tm.role AS workspace_role,
    tm.status,
    tm.weekly_capacity_min,
        CASE
            WHEN (platform.current_workspace_role() = ANY (ARRAY['finance'::text, 'owner'::text])) THEN tm.hourly_cost
            ELSE NULL::bigint
        END AS hourly_cost,
    tm.created_at AS joined_at,
    wl.name AS location,
    mg.name AS member_group,
    tm.location_id,
    wl."position" AS location_position,
    tm.member_group_id,
    mg."position" AS member_group_position
   FROM ((((platform.tenant_members tm
     JOIN platform.users u ON ((u.id = tm.user_id)))
     JOIN platform.tenants t ON ((t.id = tm.tenant_id)))
     LEFT JOIN platform.work_locations wl ON (((wl.tenant_id = tm.tenant_id) AND (wl.id = tm.location_id) AND (wl.deleted_at IS NULL))))
     LEFT JOIN platform.member_groups mg ON (((mg.tenant_id = tm.tenant_id) AND (mg.id = tm.member_group_id) AND (mg.deleted_at IS NULL))))
  WHERE (tm.tenant_id = platform.current_tenant());
--rollback DROP VIEW IF EXISTS platform.v_directory_members;

--changeset hyproms:07-view-v_directory_teams
CREATE VIEW platform.v_directory_teams AS
 SELECT tm.tenant_id,
    t.id AS team_id,
    t.name AS team_name,
    tm.user_id,
    (t.deactivated_at IS NULL) AS team_active,
    tm.is_lead
   FROM (platform.teams t
     JOIN platform.team_members tm ON ((tm.team_id = t.id)))
  WHERE ((t.deleted_at IS NULL) AND (t.tenant_id = platform.current_tenant()));
--rollback DROP VIEW IF EXISTS platform.v_directory_teams;

--changeset hyproms:07-view-v_budget_realization
CREATE VIEW proms.v_budget_realization WITH (security_invoker='true') AS
 SELECT bl.tenant_id,
    bl.board_id,
    bl.category_id,
    bc.name AS category_name,
    bl.amount AS budget,
    COALESCE(sum(e.amount) FILTER (WHERE (e.status = 'approved'::text)), (0)::numeric) AS realized,
    ((bl.amount)::numeric - COALESCE(sum(e.amount) FILTER (WHERE (e.status = 'approved'::text)), (0)::numeric)) AS budget_left
   FROM ((proms.budget_lines bl
     JOIN proms.budget_categories bc ON ((bc.id = bl.category_id)))
     LEFT JOIN proms.expenses e ON (((e.board_id = bl.board_id) AND (e.category_id = bl.category_id))))
  GROUP BY bl.tenant_id, bl.board_id, bl.category_id, bc.name, bl.amount;
--rollback DROP VIEW IF EXISTS proms.v_budget_realization;
