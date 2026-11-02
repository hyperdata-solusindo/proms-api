--liquibase formatted sql

--changeset hyproms:09-rls-platform_member_groups
ALTER TABLE platform.member_groups ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON platform.member_groups USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON platform.member_groups;
--rollback ALTER TABLE platform.member_groups DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-platform_subscriptions
ALTER TABLE platform.subscriptions ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON platform.subscriptions USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON platform.subscriptions;
--rollback ALTER TABLE platform.subscriptions DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-platform_team_members
ALTER TABLE platform.team_members ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON platform.team_members USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON platform.team_members;
--rollback ALTER TABLE platform.team_members DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-platform_teams
ALTER TABLE platform.teams ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON platform.teams USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON platform.teams;
--rollback ALTER TABLE platform.teams DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-platform_tenant_members
ALTER TABLE platform.tenant_members ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON platform.tenant_members USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON platform.tenant_members;
--rollback ALTER TABLE platform.tenant_members DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-platform_work_locations
ALTER TABLE platform.work_locations ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON platform.work_locations USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON platform.work_locations;
--rollback ALTER TABLE platform.work_locations DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_attachments
ALTER TABLE proms.attachments ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.attachments USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.attachments;
--rollback ALTER TABLE proms.attachments DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_board_columns
ALTER TABLE proms.board_columns ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.board_columns USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.board_columns;
--rollback ALTER TABLE proms.board_columns DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_board_contracts
ALTER TABLE proms.board_contracts ENABLE ROW LEVEL SECURITY;
CREATE POLICY finance_only ON proms.board_contracts AS RESTRICTIVE USING ((platform.current_workspace_role() = ANY (ARRAY['finance'::text, 'owner'::text])));
CREATE POLICY tenant_isolation ON proms.board_contracts USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.board_contracts;
--rollback DROP POLICY IF EXISTS finance_only ON proms.board_contracts;
--rollback ALTER TABLE proms.board_contracts DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_board_document_access
ALTER TABLE proms.board_document_access ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.board_document_access USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.board_document_access;
--rollback ALTER TABLE proms.board_document_access DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_board_document_versions
ALTER TABLE proms.board_document_versions ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.board_document_versions USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.board_document_versions;
--rollback ALTER TABLE proms.board_document_versions DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_board_documents
ALTER TABLE proms.board_documents ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.board_documents USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.board_documents;
--rollback ALTER TABLE proms.board_documents DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_board_groups
ALTER TABLE proms.board_groups ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.board_groups USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.board_groups;
--rollback ALTER TABLE proms.board_groups DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_board_invitations
ALTER TABLE proms.board_invitations ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.board_invitations USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.board_invitations;
--rollback ALTER TABLE proms.board_invitations DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_board_member_workstreams
ALTER TABLE proms.board_member_workstreams ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.board_member_workstreams USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.board_member_workstreams;
--rollback ALTER TABLE proms.board_member_workstreams DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_board_members
ALTER TABLE proms.board_members ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.board_members USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.board_members;
--rollback ALTER TABLE proms.board_members DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_board_pins
ALTER TABLE proms.board_pins ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.board_pins USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.board_pins;
--rollback ALTER TABLE proms.board_pins DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_board_saved_views
ALTER TABLE proms.board_saved_views ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.board_saved_views USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.board_saved_views;
--rollback ALTER TABLE proms.board_saved_views DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_boards
ALTER TABLE proms.boards ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.boards USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.boards;
--rollback ALTER TABLE proms.boards DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_budget_categories
ALTER TABLE proms.budget_categories ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.budget_categories USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.budget_categories;
--rollback ALTER TABLE proms.budget_categories DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_budget_lines
ALTER TABLE proms.budget_lines ENABLE ROW LEVEL SECURITY;
CREATE POLICY finance_only ON proms.budget_lines AS RESTRICTIVE USING ((platform.current_workspace_role() = ANY (ARRAY['finance'::text, 'owner'::text])));
CREATE POLICY tenant_isolation ON proms.budget_lines USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.budget_lines;
--rollback DROP POLICY IF EXISTS finance_only ON proms.budget_lines;
--rollback ALTER TABLE proms.budget_lines DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_chat_channel_members
ALTER TABLE proms.chat_channel_members ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.chat_channel_members USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.chat_channel_members;
--rollback ALTER TABLE proms.chat_channel_members DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_chat_channels
ALTER TABLE proms.chat_channels ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.chat_channels USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.chat_channels;
--rollback ALTER TABLE proms.chat_channels DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_chat_messages
ALTER TABLE proms.chat_messages ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.chat_messages USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.chat_messages;
--rollback ALTER TABLE proms.chat_messages DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_chat_reactions
ALTER TABLE proms.chat_reactions ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.chat_reactions USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.chat_reactions;
--rollback ALTER TABLE proms.chat_reactions DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_chat_read_markers
ALTER TABLE proms.chat_read_markers ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.chat_read_markers USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.chat_read_markers;
--rollback ALTER TABLE proms.chat_read_markers DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_checklist_items
ALTER TABLE proms.checklist_items ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.checklist_items USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.checklist_items;
--rollback ALTER TABLE proms.checklist_items DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_comment_mentions
ALTER TABLE proms.comment_mentions ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.comment_mentions USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.comment_mentions;
--rollback ALTER TABLE proms.comment_mentions DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_comments
ALTER TABLE proms.comments ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.comments USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.comments;
--rollback ALTER TABLE proms.comments DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_dashboard_reminders
ALTER TABLE proms.dashboard_reminders ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.dashboard_reminders USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.dashboard_reminders;
--rollback ALTER TABLE proms.dashboard_reminders DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_expenses
ALTER TABLE proms.expenses ENABLE ROW LEVEL SECURITY;
CREATE POLICY expenses_visibility ON proms.expenses AS RESTRICTIVE USING (((platform.current_workspace_role() = ANY (ARRAY['finance'::text, 'owner'::text])) OR (submitted_by = (NULLIF(current_setting('app.user_id'::text, true), ''::text))::uuid)));
CREATE POLICY tenant_isolation ON proms.expenses USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.expenses;
--rollback DROP POLICY IF EXISTS expenses_visibility ON proms.expenses;
--rollback ALTER TABLE proms.expenses DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_focus_sessions
ALTER TABLE proms.focus_sessions ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.focus_sessions USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.focus_sessions;
--rollback ALTER TABLE proms.focus_sessions DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_form_fields
ALTER TABLE proms.form_fields ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.form_fields USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.form_fields;
--rollback ALTER TABLE proms.form_fields DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_form_submissions
ALTER TABLE proms.form_submissions ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.form_submissions USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.form_submissions;
--rollback ALTER TABLE proms.form_submissions DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_forms
ALTER TABLE proms.forms ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.forms USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.forms;
--rollback ALTER TABLE proms.forms DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_holidays
ALTER TABLE proms.holidays ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.holidays USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.holidays;
--rollback ALTER TABLE proms.holidays DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_idempotency_keys
ALTER TABLE proms.idempotency_keys ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.idempotency_keys USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.idempotency_keys;
--rollback ALTER TABLE proms.idempotency_keys DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_invoices
ALTER TABLE proms.invoices ENABLE ROW LEVEL SECURITY;
CREATE POLICY finance_only ON proms.invoices AS RESTRICTIVE USING ((platform.current_workspace_role() = ANY (ARRAY['finance'::text, 'owner'::text])));
CREATE POLICY tenant_isolation ON proms.invoices USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.invoices;
--rollback DROP POLICY IF EXISTS finance_only ON proms.invoices;
--rollback ALTER TABLE proms.invoices DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_meeting_action_items
ALTER TABLE proms.meeting_action_items ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.meeting_action_items USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.meeting_action_items;
--rollback ALTER TABLE proms.meeting_action_items DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_meeting_attendees
ALTER TABLE proms.meeting_attendees ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.meeting_attendees USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.meeting_attendees;
--rollback ALTER TABLE proms.meeting_attendees DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_meeting_minutes
ALTER TABLE proms.meeting_minutes ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.meeting_minutes USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.meeting_minutes;
--rollback ALTER TABLE proms.meeting_minutes DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_milestone_workstreams
ALTER TABLE proms.milestone_workstreams ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.milestone_workstreams USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.milestone_workstreams;
--rollback ALTER TABLE proms.milestone_workstreams DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_milestones
ALTER TABLE proms.milestones ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.milestones USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.milestones;
--rollback ALTER TABLE proms.milestones DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_notifications
ALTER TABLE proms.notifications ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.notifications USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.notifications;
--rollback ALTER TABLE proms.notifications DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_payment_terms
ALTER TABLE proms.payment_terms ENABLE ROW LEVEL SECURITY;
CREATE POLICY finance_only ON proms.payment_terms AS RESTRICTIVE USING ((platform.current_workspace_role() = ANY (ARRAY['finance'::text, 'owner'::text])));
CREATE POLICY tenant_isolation ON proms.payment_terms USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.payment_terms;
--rollback DROP POLICY IF EXISTS finance_only ON proms.payment_terms;
--rollback ALTER TABLE proms.payment_terms DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_point_entries
ALTER TABLE proms.point_entries ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.point_entries USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.point_entries;
--rollback ALTER TABLE proms.point_entries DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_point_policies
ALTER TABLE proms.point_policies ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.point_policies USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.point_policies;
--rollback ALTER TABLE proms.point_policies DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_recurrence_rules
ALTER TABLE proms.recurrence_rules ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.recurrence_rules USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.recurrence_rules;
--rollback ALTER TABLE proms.recurrence_rules DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_share_links
ALTER TABLE proms.share_links ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.share_links USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.share_links;
--rollback ALTER TABLE proms.share_links DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_standup_policies
ALTER TABLE proms.standup_policies ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.standup_policies USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.standup_policies;
--rollback ALTER TABLE proms.standup_policies DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_standup_reminder_runs
ALTER TABLE proms.standup_reminder_runs ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.standup_reminder_runs USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.standup_reminder_runs;
--rollback ALTER TABLE proms.standup_reminder_runs DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_standup_reports
ALTER TABLE proms.standup_reports ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.standup_reports USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.standup_reports;
--rollback ALTER TABLE proms.standup_reports DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_tags
ALTER TABLE proms.tags ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.tags USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.tags;
--rollback ALTER TABLE proms.tags DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_task_activities
ALTER TABLE proms.task_activities ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.task_activities USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.task_activities;
--rollback ALTER TABLE proms.task_activities DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_task_assignees
ALTER TABLE proms.task_assignees ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.task_assignees USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.task_assignees;
--rollback ALTER TABLE proms.task_assignees DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_task_dependencies
ALTER TABLE proms.task_dependencies ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.task_dependencies USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.task_dependencies;
--rollback ALTER TABLE proms.task_dependencies DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_task_feedback
ALTER TABLE proms.task_feedback ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.task_feedback USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.task_feedback;
--rollback ALTER TABLE proms.task_feedback DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_task_tags
ALTER TABLE proms.task_tags ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.task_tags USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.task_tags;
--rollback ALTER TABLE proms.task_tags DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_task_watchers
ALTER TABLE proms.task_watchers ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.task_watchers USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.task_watchers;
--rollback ALTER TABLE proms.task_watchers DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_tasks
ALTER TABLE proms.tasks ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.tasks USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.tasks;
--rollback ALTER TABLE proms.tasks DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_time_entries
ALTER TABLE proms.time_entries ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.time_entries USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.time_entries;
--rollback ALTER TABLE proms.time_entries DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_time_off
ALTER TABLE proms.time_off ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.time_off USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.time_off;
--rollback ALTER TABLE proms.time_off DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_timesheets
ALTER TABLE proms.timesheets ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.timesheets USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.timesheets;
--rollback ALTER TABLE proms.timesheets DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_user_preferences
ALTER TABLE proms.user_preferences ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.user_preferences USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.user_preferences;
--rollback ALTER TABLE proms.user_preferences DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_workflow_templates
ALTER TABLE proms.workflow_templates ENABLE ROW LEVEL SECURITY;
CREATE POLICY templates_read ON proms.workflow_templates FOR SELECT USING (((tenant_id IS NULL) OR (tenant_id = platform.current_tenant())));
CREATE POLICY templates_write ON proms.workflow_templates USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS templates_write ON proms.workflow_templates;
--rollback DROP POLICY IF EXISTS templates_read ON proms.workflow_templates;
--rollback ALTER TABLE proms.workflow_templates DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_workspace_calendars
ALTER TABLE proms.workspace_calendars ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.workspace_calendars USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.workspace_calendars;
--rollback ALTER TABLE proms.workspace_calendars DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-proms_workstreams
ALTER TABLE proms.workstreams ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON proms.workstreams USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON proms.workstreams;
--rollback ALTER TABLE proms.workstreams DISABLE ROW LEVEL SECURITY;

--changeset hyproms:09-rls-platform_invitations
CREATE POLICY tenant_isolation ON platform.invitations USING ((tenant_id = platform.current_tenant())) WITH CHECK ((tenant_id = platform.current_tenant()));
--rollback DROP POLICY IF EXISTS tenant_isolation ON platform.invitations;
--rollback ALTER TABLE platform.invitations DISABLE ROW LEVEL SECURITY;
