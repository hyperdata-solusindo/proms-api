--liquibase formatted sql

--changeset hyproms:08-trg-audit_logs_no_update
CREATE TRIGGER audit_logs_no_update BEFORE DELETE OR UPDATE ON platform.audit_logs FOR EACH ROW EXECUTE FUNCTION platform.block_audit_change();
--rollback DROP TRIGGER IF EXISTS audit_logs_no_update ON platform.audit_logs;

--changeset hyproms:08-trg-member_groups_touch
CREATE TRIGGER member_groups_touch BEFORE UPDATE ON platform.member_groups FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS member_groups_touch ON platform.member_groups;

--changeset hyproms:08-trg-plans_touch
CREATE TRIGGER plans_touch BEFORE UPDATE ON platform.plans FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS plans_touch ON platform.plans;

--changeset hyproms:08-trg-subscriptions_touch
CREATE TRIGGER subscriptions_touch BEFORE UPDATE ON platform.subscriptions FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS subscriptions_touch ON platform.subscriptions;

--changeset hyproms:08-trg-teams_touch
CREATE TRIGGER teams_touch BEFORE UPDATE ON platform.teams FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS teams_touch ON platform.teams;

--changeset hyproms:08-trg-tenant_members_touch
CREATE TRIGGER tenant_members_touch BEFORE UPDATE ON platform.tenant_members FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS tenant_members_touch ON platform.tenant_members;

--changeset hyproms:08-trg-tenants_touch
CREATE TRIGGER tenants_touch BEFORE UPDATE ON platform.tenants FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS tenants_touch ON platform.tenants;

--changeset hyproms:08-trg-user_push_subscriptions_touch
CREATE TRIGGER user_push_subscriptions_touch BEFORE UPDATE ON platform.user_push_subscriptions FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS user_push_subscriptions_touch ON platform.user_push_subscriptions;

--changeset hyproms:08-trg-users_touch
CREATE TRIGGER users_touch BEFORE UPDATE ON platform.users FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS users_touch ON platform.users;

--changeset hyproms:08-trg-work_locations_touch
CREATE TRIGGER work_locations_touch BEFORE UPDATE ON platform.work_locations FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS work_locations_touch ON platform.work_locations;

--changeset hyproms:08-trg-board_columns_touch
CREATE TRIGGER board_columns_touch BEFORE UPDATE ON proms.board_columns FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS board_columns_touch ON proms.board_columns;

--changeset hyproms:08-trg-board_contracts_touch
CREATE TRIGGER board_contracts_touch BEFORE UPDATE ON proms.board_contracts FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS board_contracts_touch ON proms.board_contracts;

--changeset hyproms:08-trg-board_document_access_touch
CREATE TRIGGER board_document_access_touch BEFORE UPDATE ON proms.board_document_access FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS board_document_access_touch ON proms.board_document_access;

--changeset hyproms:08-trg-board_documents_touch
CREATE TRIGGER board_documents_touch BEFORE UPDATE ON proms.board_documents FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS board_documents_touch ON proms.board_documents;

--changeset hyproms:08-trg-board_groups_touch
CREATE TRIGGER board_groups_touch BEFORE UPDATE ON proms.board_groups FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS board_groups_touch ON proms.board_groups;

--changeset hyproms:08-trg-board_members_touch
CREATE TRIGGER board_members_touch BEFORE UPDATE ON proms.board_members FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS board_members_touch ON proms.board_members;

--changeset hyproms:08-trg-board_pins_touch
CREATE TRIGGER board_pins_touch BEFORE UPDATE ON proms.board_pins FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS board_pins_touch ON proms.board_pins;

--changeset hyproms:08-trg-board_saved_views_touch
CREATE TRIGGER board_saved_views_touch BEFORE UPDATE ON proms.board_saved_views FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS board_saved_views_touch ON proms.board_saved_views;

--changeset hyproms:08-trg-boards_touch
CREATE TRIGGER boards_touch BEFORE UPDATE ON proms.boards FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS boards_touch ON proms.boards;

--changeset hyproms:08-trg-chat_channels_touch
CREATE TRIGGER chat_channels_touch BEFORE UPDATE ON proms.chat_channels FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS chat_channels_touch ON proms.chat_channels;

--changeset hyproms:08-trg-chat_messages_touch
CREATE TRIGGER chat_messages_touch BEFORE UPDATE ON proms.chat_messages FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS chat_messages_touch ON proms.chat_messages;

--changeset hyproms:08-trg-comments_touch
CREATE TRIGGER comments_touch BEFORE UPDATE ON proms.comments FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS comments_touch ON proms.comments;

--changeset hyproms:08-trg-dashboard_reminders_touch
CREATE TRIGGER dashboard_reminders_touch BEFORE UPDATE ON proms.dashboard_reminders FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS dashboard_reminders_touch ON proms.dashboard_reminders;

--changeset hyproms:08-trg-expenses_touch
CREATE TRIGGER expenses_touch BEFORE UPDATE ON proms.expenses FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS expenses_touch ON proms.expenses;

--changeset hyproms:08-trg-focus_sessions_touch
CREATE TRIGGER focus_sessions_touch BEFORE UPDATE ON proms.focus_sessions FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS focus_sessions_touch ON proms.focus_sessions;

--changeset hyproms:08-trg-forms_touch
CREATE TRIGGER forms_touch BEFORE UPDATE ON proms.forms FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS forms_touch ON proms.forms;

--changeset hyproms:08-trg-invoices_touch
CREATE TRIGGER invoices_touch BEFORE UPDATE ON proms.invoices FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS invoices_touch ON proms.invoices;

--changeset hyproms:08-trg-meeting_action_items_touch
CREATE TRIGGER meeting_action_items_touch BEFORE UPDATE ON proms.meeting_action_items FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS meeting_action_items_touch ON proms.meeting_action_items;

--changeset hyproms:08-trg-meeting_minutes_touch
CREATE TRIGGER meeting_minutes_touch BEFORE UPDATE ON proms.meeting_minutes FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS meeting_minutes_touch ON proms.meeting_minutes;

--changeset hyproms:08-trg-milestones_touch
CREATE TRIGGER milestones_touch BEFORE UPDATE ON proms.milestones FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS milestones_touch ON proms.milestones;

--changeset hyproms:08-trg-payment_terms_total
CREATE CONSTRAINT TRIGGER payment_terms_total AFTER INSERT OR DELETE OR UPDATE ON proms.payment_terms DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION proms.check_terms_total();
--rollback DROP TRIGGER IF EXISTS payment_terms_total ON proms.payment_terms;

--changeset hyproms:08-trg-payment_terms_touch
CREATE TRIGGER payment_terms_touch BEFORE UPDATE ON proms.payment_terms FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS payment_terms_touch ON proms.payment_terms;

--changeset hyproms:08-trg-standup_policies_touch
CREATE TRIGGER standup_policies_touch BEFORE UPDATE ON proms.standup_policies FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS standup_policies_touch ON proms.standup_policies;

--changeset hyproms:08-trg-standup_reports_touch
CREATE TRIGGER standup_reports_touch BEFORE UPDATE ON proms.standup_reports FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS standup_reports_touch ON proms.standup_reports;

--changeset hyproms:08-trg-task_dependencies_cycle
CREATE TRIGGER task_dependencies_cycle BEFORE INSERT OR UPDATE ON proms.task_dependencies FOR EACH ROW EXECUTE FUNCTION proms.check_dependency_cycle();
--rollback DROP TRIGGER IF EXISTS task_dependencies_cycle ON proms.task_dependencies;

--changeset hyproms:08-trg-tasks_completed_at
CREATE TRIGGER tasks_completed_at BEFORE INSERT OR UPDATE OF column_id ON proms.tasks FOR EACH ROW EXECUTE FUNCTION proms.sync_completed_at();
--rollback DROP TRIGGER IF EXISTS tasks_completed_at ON proms.tasks;

--changeset hyproms:08-trg-tasks_number
CREATE TRIGGER tasks_number BEFORE INSERT ON proms.tasks FOR EACH ROW EXECUTE FUNCTION proms.assign_task_number();
--rollback DROP TRIGGER IF EXISTS tasks_number ON proms.tasks;

--changeset hyproms:08-trg-tasks_subtask_depth
CREATE TRIGGER tasks_subtask_depth BEFORE INSERT OR UPDATE OF parent_id ON proms.tasks FOR EACH ROW EXECUTE FUNCTION proms.check_subtask_depth();
--rollback DROP TRIGGER IF EXISTS tasks_subtask_depth ON proms.tasks;

--changeset hyproms:08-trg-tasks_touch
CREATE TRIGGER tasks_touch BEFORE UPDATE ON proms.tasks FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS tasks_touch ON proms.tasks;

--changeset hyproms:08-trg-time_entries_touch
CREATE TRIGGER time_entries_touch BEFORE UPDATE ON proms.time_entries FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS time_entries_touch ON proms.time_entries;

--changeset hyproms:08-trg-timesheets_touch
CREATE TRIGGER timesheets_touch BEFORE UPDATE ON proms.timesheets FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS timesheets_touch ON proms.timesheets;

--changeset hyproms:08-trg-user_preferences_touch
CREATE TRIGGER user_preferences_touch BEFORE UPDATE ON proms.user_preferences FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS user_preferences_touch ON proms.user_preferences;

--changeset hyproms:08-trg-workflow_templates_touch
CREATE TRIGGER workflow_templates_touch BEFORE UPDATE ON proms.workflow_templates FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS workflow_templates_touch ON proms.workflow_templates;

--changeset hyproms:08-trg-workspace_calendars_touch
CREATE TRIGGER workspace_calendars_touch BEFORE UPDATE ON proms.workspace_calendars FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS workspace_calendars_touch ON proms.workspace_calendars;

--changeset hyproms:08-trg-workstreams_touch
CREATE TRIGGER workstreams_touch BEFORE UPDATE ON proms.workstreams FOR EACH ROW EXECUTE FUNCTION platform.touch_updated_at();
--rollback DROP TRIGGER IF EXISTS workstreams_touch ON proms.workstreams;
