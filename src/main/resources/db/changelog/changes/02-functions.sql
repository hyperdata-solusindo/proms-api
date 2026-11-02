--liquibase formatted sql

--changeset hyproms:02-func-block_audit_change__ splitStatements:false
CREATE OR REPLACE FUNCTION platform.block_audit_change() RETURNS trigger
    LANGUAGE plpgsql
    AS $$ begin raise exception 'audit_logs is append-only'; end $$;
--rollback DROP FUNCTION IF EXISTS platform.block_audit_change();

--changeset hyproms:02-func-current_tenant__ splitStatements:false
CREATE OR REPLACE FUNCTION platform.current_tenant() RETURNS uuid
    LANGUAGE sql STABLE
    AS $$ select nullif(current_setting('app.tenant_id', true), '')::uuid $$;
--rollback DROP FUNCTION IF EXISTS platform.current_tenant();

--changeset hyproms:02-func-current_workspace_role__ splitStatements:false
CREATE OR REPLACE FUNCTION platform.current_workspace_role() RETURNS text
    LANGUAGE sql STABLE
    AS $$ select coalesce(current_setting('app.workspace_role', true), '') $$;
--rollback DROP FUNCTION IF EXISTS platform.current_workspace_role();

--changeset hyproms:02-func-touch_updated_at__ splitStatements:false
CREATE OR REPLACE FUNCTION platform.touch_updated_at() RETURNS trigger
    LANGUAGE plpgsql
    AS $$ begin new.updated_at := now(); return new; end $$;
--rollback DROP FUNCTION IF EXISTS platform.touch_updated_at();

--changeset hyproms:02-func-assign_task_number__ splitStatements:false
CREATE OR REPLACE FUNCTION proms.assign_task_number() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
  if new.number is null then
    update proms.boards set task_seq = task_seq + 1
     where id = new.board_id and tenant_id = new.tenant_id returning task_seq into new.number;
    if new.number is null then
      raise exception 'board % not found in current tenant', new.board_id;
    end if;
  end if;
  return new;
end $$;
--rollback DROP FUNCTION IF EXISTS proms.assign_task_number();

--changeset hyproms:02-func-check_dependency_cycle__ splitStatements:false
CREATE OR REPLACE FUNCTION proms.check_dependency_cycle() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
  if exists (
    with recursive chain(task_id) as (
      select new.blocked_task_id
      union
      select d.blocked_task_id from proms.task_dependencies d join chain c on d.blocking_task_id = c.task_id
    ) select 1 from chain where task_id = new.blocking_task_id)
  then raise exception 'dependency cycle detected'; end if;
  return new;
end $$;
--rollback DROP FUNCTION IF EXISTS proms.check_dependency_cycle();

--changeset hyproms:02-func-check_subtask_depth__ splitStatements:false
CREATE OR REPLACE FUNCTION proms.check_subtask_depth() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
  if new.parent_id is not null and exists
     (select 1 from proms.tasks p where p.id = new.parent_id and p.parent_id is not null) then
    raise exception 'subtask cannot have its own subtask';
  end if;
  return new;
end $$;
--rollback DROP FUNCTION IF EXISTS proms.check_subtask_depth();

--changeset hyproms:02-func-check_terms_total__ splitStatements:false
CREATE OR REPLACE FUNCTION proms.check_terms_total() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
declare cid uuid := coalesce(new.contract_id, old.contract_id); total numeric;
begin
  select coalesce(sum(percent),0) into total from proms.payment_terms where contract_id = cid;
  if total not in (0, 100) then
    raise exception 'payment terms for contract % total %, must be 100', cid, total;
  end if;
  return null;
end $$;
--rollback DROP FUNCTION IF EXISTS proms.check_terms_total();

--changeset hyproms:02-func-sync_completed_at__ splitStatements:false
CREATE OR REPLACE FUNCTION proms.sync_completed_at() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
declare cat text;
begin
  select category into cat from proms.board_columns where id = new.column_id;
  if cat = 'done' and (tg_op = 'INSERT' or old.column_id is distinct from new.column_id or new.completed_at is null) then
    new.completed_at := coalesce(new.completed_at, now());
  elsif cat <> 'done' then
    new.completed_at := null;
  end if;
  return new;
end $$;
--rollback DROP FUNCTION IF EXISTS proms.sync_completed_at();
