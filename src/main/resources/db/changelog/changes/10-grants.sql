--liquibase formatted sql
--changeset hyproms:10-grants splitStatements:false rollbackSplitStatements:false runOnChange:true
DO $$
DECLARE
    v_app_user text := '${app_user}';
BEGIN
    -- 1. Grant hak akses ke user aplikasi yang dikonfigurasi dinamis (dari .env / Liquibase parameter)
    IF v_app_user IS NOT NULL AND v_app_user <> '' AND EXISTS (SELECT 1 FROM pg_roles WHERE rolname = v_app_user) THEN
        EXECUTE format('GRANT USAGE ON SCHEMA platform TO %I;', v_app_user);
        EXECUTE format('GRANT USAGE ON SCHEMA proms TO %I;', v_app_user);
        EXECUTE format('GRANT ALL ON ALL FUNCTIONS IN SCHEMA platform TO %I;', v_app_user);
        EXECUTE format('GRANT ALL ON ALL FUNCTIONS IN SCHEMA proms TO %I;', v_app_user);
        EXECUTE format('GRANT SELECT,INSERT,DELETE,UPDATE ON ALL TABLES IN SCHEMA platform TO %I;', v_app_user);
        EXECUTE format('GRANT SELECT,INSERT,DELETE,UPDATE ON ALL TABLES IN SCHEMA proms TO %I;', v_app_user);
        EXECUTE format('GRANT USAGE,SELECT ON ALL SEQUENCES IN SCHEMA platform TO %I;', v_app_user);
        EXECUTE format('GRANT USAGE,SELECT ON ALL SEQUENCES IN SCHEMA proms TO %I;', v_app_user);
    END IF;

    -- 2. Backward-compatibility untuk role spesifik jika didefinisikan terpisah
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'platform_app') THEN
        EXECUTE 'GRANT USAGE ON SCHEMA platform TO platform_app;';
        EXECUTE 'GRANT ALL ON FUNCTION platform.current_tenant() TO platform_app;';
        EXECUTE 'GRANT ALL ON FUNCTION platform.current_workspace_role() TO platform_app;';
        EXECUTE 'GRANT ALL ON FUNCTION platform.tenant_member_counts() TO platform_app;';
        EXECUTE 'GRANT ALL ON FUNCTION platform.user_memberships(target_user_ids uuid[]) TO platform_app;';
        EXECUTE 'GRANT SELECT,INSERT ON TABLE platform.audit_logs TO platform_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE platform.audit_logs_default TO platform_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE platform.invitations TO platform_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE platform.member_groups TO platform_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE platform.personal_access_tokens TO platform_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE platform.plans TO platform_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE platform.subscriptions TO platform_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE platform.team_members TO platform_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE platform.teams TO platform_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE platform.tenant_members TO platform_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE platform.tenants TO platform_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE platform.user_push_subscriptions TO platform_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE platform.users TO platform_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE platform.v_current_tenant TO platform_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE platform.work_locations TO platform_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE platform.v_directory_members TO platform_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE platform.v_directory_teams TO platform_app;';
    END IF;

    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'proms_app') THEN
        EXECUTE 'GRANT USAGE ON SCHEMA platform TO proms_app;';
        EXECUTE 'GRANT USAGE ON SCHEMA proms TO proms_app;';
        EXECUTE 'GRANT ALL ON FUNCTION platform.current_tenant() TO proms_app;';
        EXECUTE 'GRANT ALL ON FUNCTION platform.current_workspace_role() TO proms_app;';
        EXECUTE 'GRANT ALL ON FUNCTION proms.find_open_invitation(p_token_hash text) TO proms_app;';
        EXECUTE 'GRANT INSERT ON TABLE platform.audit_logs TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE platform.user_push_subscriptions TO proms_app;';
        EXECUTE 'GRANT SELECT ON TABLE platform.v_current_tenant TO proms_app;';
        EXECUTE 'GRANT SELECT ON TABLE platform.v_directory_members TO proms_app;';
        EXECUTE 'GRANT SELECT ON TABLE platform.v_directory_teams TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.attachments TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.board_columns TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.board_contracts TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.board_document_access TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.board_document_versions TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.board_documents TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.board_groups TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.board_invitations TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.board_member_workstreams TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.board_members TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.board_pins TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.board_saved_views TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.boards TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.budget_categories TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.budget_lines TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.chat_channels TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.chat_messages TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.chat_reactions TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.chat_read_markers TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.checklist_items TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.comment_mentions TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.comments TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.dashboard_reminders TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.expenses TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.focus_sessions TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.form_fields TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.form_submissions TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.forms TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.holidays TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.idempotency_keys TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.invoices TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.meeting_action_items TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.meeting_attendees TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.meeting_minutes TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.milestone_workstreams TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.milestones TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.notifications TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,UPDATE ON TABLE proms.outbox_events TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.payment_terms TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT ON TABLE proms.point_entries TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT ON TABLE proms.point_policies TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.recurrence_rules TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.share_links TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,UPDATE ON TABLE proms.standup_policies TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT ON TABLE proms.standup_reminder_runs TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,UPDATE ON TABLE proms.standup_reports TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.tags TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.task_activities TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.task_assignees TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.task_dependencies TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT ON TABLE proms.task_feedback TO proms_app;';
        EXECUTE 'GRANT UPDATE(superseded_at) ON TABLE proms.task_feedback TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.task_tags TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.task_watchers TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.tasks TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.template_columns TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.time_entries TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.time_off TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.timesheets TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.user_preferences TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.v_budget_realization TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.workflow_templates TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,UPDATE ON TABLE proms.workspace_calendars TO proms_app;';
        EXECUTE 'GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE proms.workstreams TO proms_app;';
    END IF;
END $$;

--rollback DO $$
--rollback DECLARE
--rollback     v_app_user text := '${app_user}';
--rollback BEGIN
--rollback     IF v_app_user IS NOT NULL AND v_app_user <> '' AND EXISTS (SELECT 1 FROM pg_roles WHERE rolname = v_app_user) THEN
--rollback         EXECUTE format('REVOKE ALL ON ALL TABLES IN SCHEMA platform FROM %I;', v_app_user);
--rollback         EXECUTE format('REVOKE ALL ON ALL TABLES IN SCHEMA proms FROM %I;', v_app_user);
--rollback         EXECUTE format('REVOKE ALL ON ALL FUNCTIONS IN SCHEMA platform FROM %I;', v_app_user);
--rollback         EXECUTE format('REVOKE ALL ON ALL FUNCTIONS IN SCHEMA proms FROM %I;', v_app_user);
--rollback         EXECUTE format('REVOKE ALL ON ALL SEQUENCES IN SCHEMA platform FROM %I;', v_app_user);
--rollback         EXECUTE format('REVOKE ALL ON ALL SEQUENCES IN SCHEMA proms FROM %I;', v_app_user);
--rollback         EXECUTE format('REVOKE USAGE ON SCHEMA platform FROM %I;', v_app_user);
--rollback         EXECUTE format('REVOKE USAGE ON SCHEMA proms FROM %I;', v_app_user);
--rollback     END IF;
--rollback     IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'platform_app') THEN
--rollback         EXECUTE 'REVOKE ALL ON ALL TABLES IN SCHEMA platform FROM platform_app;';
--rollback         EXECUTE 'REVOKE ALL ON ALL FUNCTIONS IN SCHEMA platform FROM platform_app;';
--rollback         EXECUTE 'REVOKE USAGE ON SCHEMA platform FROM platform_app;';
--rollback     END IF;
--rollback     IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'proms_app') THEN
--rollback         EXECUTE 'REVOKE ALL ON ALL TABLES IN SCHEMA proms FROM proms_app;';
--rollback         EXECUTE 'REVOKE ALL ON ALL FUNCTIONS IN SCHEMA proms FROM proms_app;';
--rollback         EXECUTE 'REVOKE USAGE ON SCHEMA proms FROM proms_app;';
--rollback         EXECUTE 'REVOKE ALL ON ALL TABLES IN SCHEMA platform FROM proms_app;';
--rollback         EXECUTE 'REVOKE ALL ON ALL FUNCTIONS IN SCHEMA platform FROM proms_app;';
--rollback         EXECUTE 'REVOKE USAGE ON SCHEMA platform FROM proms_app;';
--rollback     END IF;
--rollback END $$;
