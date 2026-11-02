--liquibase formatted sql

--changeset hyproms:06-fk-platform_invitations
ALTER TABLE ONLY platform.invitations
    ADD CONSTRAINT invitations_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES platform.tenants(id);
--rollback ALTER TABLE ONLY platform.invitations DROP CONSTRAINT IF EXISTS invitations_tenant_id_fkey;

--changeset hyproms:06-fk-platform_member_groups
ALTER TABLE ONLY platform.member_groups
    ADD CONSTRAINT member_groups_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES platform.tenants(id);
--rollback ALTER TABLE ONLY platform.member_groups DROP CONSTRAINT IF EXISTS member_groups_tenant_id_fkey;

--changeset hyproms:06-fk-platform_subscriptions
ALTER TABLE ONLY platform.subscriptions
    ADD CONSTRAINT subscriptions_plan_id_fkey FOREIGN KEY (plan_id) REFERENCES platform.plans(id);
ALTER TABLE ONLY platform.subscriptions
    ADD CONSTRAINT subscriptions_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES platform.tenants(id);
--rollback ALTER TABLE ONLY platform.subscriptions DROP CONSTRAINT IF EXISTS subscriptions_tenant_id_fkey;
--rollback ALTER TABLE ONLY platform.subscriptions DROP CONSTRAINT IF EXISTS subscriptions_plan_id_fkey;

--changeset hyproms:06-fk-platform_team_members
ALTER TABLE ONLY platform.team_members
    ADD CONSTRAINT team_members_tenant_id_team_id_fkey FOREIGN KEY (tenant_id, team_id) REFERENCES platform.teams(tenant_id, id) ON DELETE CASCADE;
ALTER TABLE ONLY platform.team_members
    ADD CONSTRAINT team_members_user_id_fkey FOREIGN KEY (user_id) REFERENCES platform.users(id);
--rollback ALTER TABLE ONLY platform.team_members DROP CONSTRAINT IF EXISTS team_members_user_id_fkey;
--rollback ALTER TABLE ONLY platform.team_members DROP CONSTRAINT IF EXISTS team_members_tenant_id_team_id_fkey;

--changeset hyproms:06-fk-platform_teams
ALTER TABLE ONLY platform.teams
    ADD CONSTRAINT teams_deactivated_by_fkey FOREIGN KEY (deactivated_by) REFERENCES platform.users(id);
ALTER TABLE ONLY platform.teams
    ADD CONSTRAINT teams_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES platform.tenants(id);
--rollback ALTER TABLE ONLY platform.teams DROP CONSTRAINT IF EXISTS teams_tenant_id_fkey;
--rollback ALTER TABLE ONLY platform.teams DROP CONSTRAINT IF EXISTS teams_deactivated_by_fkey;

--changeset hyproms:06-fk-platform_tenant_members
ALTER TABLE ONLY platform.tenant_members
    ADD CONSTRAINT tenant_members_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES platform.tenants(id);
ALTER TABLE ONLY platform.tenant_members
    ADD CONSTRAINT tenant_members_tenant_id_location_id_fkey FOREIGN KEY (tenant_id, location_id) REFERENCES platform.work_locations(tenant_id, id);
ALTER TABLE ONLY platform.tenant_members
    ADD CONSTRAINT tenant_members_tenant_id_member_group_id_fkey FOREIGN KEY (tenant_id, member_group_id) REFERENCES platform.member_groups(tenant_id, id);
ALTER TABLE ONLY platform.tenant_members
    ADD CONSTRAINT tenant_members_user_id_fkey FOREIGN KEY (user_id) REFERENCES platform.users(id);
--rollback ALTER TABLE ONLY platform.tenant_members DROP CONSTRAINT IF EXISTS tenant_members_user_id_fkey;
--rollback ALTER TABLE ONLY platform.tenant_members DROP CONSTRAINT IF EXISTS tenant_members_tenant_id_member_group_id_fkey;
--rollback ALTER TABLE ONLY platform.tenant_members DROP CONSTRAINT IF EXISTS tenant_members_tenant_id_location_id_fkey;
--rollback ALTER TABLE ONLY platform.tenant_members DROP CONSTRAINT IF EXISTS tenant_members_tenant_id_fkey;

--changeset hyproms:06-fk-platform_user_push_subscriptions
ALTER TABLE ONLY platform.user_push_subscriptions
    ADD CONSTRAINT user_push_subscriptions_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES platform.tenants(id) ON DELETE CASCADE;
ALTER TABLE ONLY platform.user_push_subscriptions
    ADD CONSTRAINT user_push_subscriptions_user_id_fkey FOREIGN KEY (user_id) REFERENCES platform.users(id) ON DELETE CASCADE;
--rollback ALTER TABLE ONLY platform.user_push_subscriptions DROP CONSTRAINT IF EXISTS user_push_subscriptions_user_id_fkey;
--rollback ALTER TABLE ONLY platform.user_push_subscriptions DROP CONSTRAINT IF EXISTS user_push_subscriptions_tenant_id_fkey;

--changeset hyproms:06-fk-platform_work_locations
ALTER TABLE ONLY platform.work_locations
    ADD CONSTRAINT work_locations_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES platform.tenants(id);
--rollback ALTER TABLE ONLY platform.work_locations DROP CONSTRAINT IF EXISTS work_locations_tenant_id_fkey;

--changeset hyproms:06-fk-proms_attachments
ALTER TABLE ONLY proms.attachments
    ADD CONSTRAINT attachments_tenant_id_comment_id_fkey FOREIGN KEY (tenant_id, comment_id) REFERENCES proms.comments(tenant_id, id);
ALTER TABLE ONLY proms.attachments
    ADD CONSTRAINT attachments_tenant_id_task_id_fkey FOREIGN KEY (tenant_id, task_id) REFERENCES proms.tasks(tenant_id, id);
ALTER TABLE ONLY proms.attachments
    ADD CONSTRAINT attachments_uploaded_by_fkey FOREIGN KEY (uploaded_by) REFERENCES platform.users(id);
--rollback ALTER TABLE ONLY proms.attachments DROP CONSTRAINT IF EXISTS attachments_uploaded_by_fkey;
--rollback ALTER TABLE ONLY proms.attachments DROP CONSTRAINT IF EXISTS attachments_tenant_id_task_id_fkey;
--rollback ALTER TABLE ONLY proms.attachments DROP CONSTRAINT IF EXISTS attachments_tenant_id_comment_id_fkey;

--changeset hyproms:06-fk-proms_board_columns
ALTER TABLE ONLY proms.board_columns
    ADD CONSTRAINT board_columns_tenant_id_board_id_fkey FOREIGN KEY (tenant_id, board_id) REFERENCES proms.boards(tenant_id, id);
ALTER TABLE ONLY proms.board_columns
    ADD CONSTRAINT board_columns_tenant_id_workstream_id_fkey FOREIGN KEY (tenant_id, workstream_id) REFERENCES proms.workstreams(tenant_id, id);
--rollback ALTER TABLE ONLY proms.board_columns DROP CONSTRAINT IF EXISTS board_columns_tenant_id_workstream_id_fkey;
--rollback ALTER TABLE ONLY proms.board_columns DROP CONSTRAINT IF EXISTS board_columns_tenant_id_board_id_fkey;

--changeset hyproms:06-fk-proms_board_contracts
ALTER TABLE ONLY proms.board_contracts
    ADD CONSTRAINT board_contracts_tenant_id_board_id_fkey FOREIGN KEY (tenant_id, board_id) REFERENCES proms.boards(tenant_id, id) ON DELETE RESTRICT;
--rollback ALTER TABLE ONLY proms.board_contracts DROP CONSTRAINT IF EXISTS board_contracts_tenant_id_board_id_fkey;

--changeset hyproms:06-fk-proms_board_document_access
ALTER TABLE ONLY proms.board_document_access
    ADD CONSTRAINT board_document_access_granted_by_fkey FOREIGN KEY (granted_by) REFERENCES platform.users(id);
ALTER TABLE ONLY proms.board_document_access
    ADD CONSTRAINT board_document_access_tenant_id_document_id_fkey FOREIGN KEY (tenant_id, document_id) REFERENCES proms.board_documents(tenant_id, id);
ALTER TABLE ONLY proms.board_document_access
    ADD CONSTRAINT board_document_access_user_id_fkey FOREIGN KEY (user_id) REFERENCES platform.users(id);
--rollback ALTER TABLE ONLY proms.board_document_access DROP CONSTRAINT IF EXISTS board_document_access_user_id_fkey;
--rollback ALTER TABLE ONLY proms.board_document_access DROP CONSTRAINT IF EXISTS board_document_access_tenant_id_document_id_fkey;
--rollback ALTER TABLE ONLY proms.board_document_access DROP CONSTRAINT IF EXISTS board_document_access_granted_by_fkey;

--changeset hyproms:06-fk-proms_board_document_versions
ALTER TABLE ONLY proms.board_document_versions
    ADD CONSTRAINT board_document_versions_created_by_fkey FOREIGN KEY (created_by) REFERENCES platform.users(id);
ALTER TABLE ONLY proms.board_document_versions
    ADD CONSTRAINT board_document_versions_tenant_id_document_id_fkey FOREIGN KEY (tenant_id, document_id) REFERENCES proms.board_documents(tenant_id, id);
--rollback ALTER TABLE ONLY proms.board_document_versions DROP CONSTRAINT IF EXISTS board_document_versions_tenant_id_document_id_fkey;
--rollback ALTER TABLE ONLY proms.board_document_versions DROP CONSTRAINT IF EXISTS board_document_versions_created_by_fkey;

--changeset hyproms:06-fk-proms_board_documents
ALTER TABLE ONLY proms.board_documents
    ADD CONSTRAINT board_documents_created_by_fkey FOREIGN KEY (created_by) REFERENCES platform.users(id);
ALTER TABLE ONLY proms.board_documents
    ADD CONSTRAINT board_documents_tenant_id_board_id_fkey FOREIGN KEY (tenant_id, board_id) REFERENCES proms.boards(tenant_id, id);
ALTER TABLE ONLY proms.board_documents
    ADD CONSTRAINT board_documents_updated_by_fkey FOREIGN KEY (updated_by) REFERENCES platform.users(id);
--rollback ALTER TABLE ONLY proms.board_documents DROP CONSTRAINT IF EXISTS board_documents_updated_by_fkey;
--rollback ALTER TABLE ONLY proms.board_documents DROP CONSTRAINT IF EXISTS board_documents_tenant_id_board_id_fkey;
--rollback ALTER TABLE ONLY proms.board_documents DROP CONSTRAINT IF EXISTS board_documents_created_by_fkey;

--changeset hyproms:06-fk-proms_board_groups
ALTER TABLE ONLY proms.board_groups
    ADD CONSTRAINT board_groups_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES platform.tenants(id);
--rollback ALTER TABLE ONLY proms.board_groups DROP CONSTRAINT IF EXISTS board_groups_tenant_id_fkey;

--changeset hyproms:06-fk-proms_board_invitations
ALTER TABLE ONLY proms.board_invitations
    ADD CONSTRAINT board_invitations_invited_by_fkey FOREIGN KEY (invited_by) REFERENCES platform.users(id);
ALTER TABLE ONLY proms.board_invitations
    ADD CONSTRAINT board_invitations_tenant_id_board_id_fkey FOREIGN KEY (tenant_id, board_id) REFERENCES proms.boards(tenant_id, id);
--rollback ALTER TABLE ONLY proms.board_invitations DROP CONSTRAINT IF EXISTS board_invitations_tenant_id_board_id_fkey;
--rollback ALTER TABLE ONLY proms.board_invitations DROP CONSTRAINT IF EXISTS board_invitations_invited_by_fkey;

--changeset hyproms:06-fk-proms_board_member_workstreams
ALTER TABLE ONLY proms.board_member_workstreams
    ADD CONSTRAINT board_member_workstreams_tenant_id_board_member_id_fkey FOREIGN KEY (tenant_id, board_member_id) REFERENCES proms.board_members(tenant_id, id) ON DELETE CASCADE;
ALTER TABLE ONLY proms.board_member_workstreams
    ADD CONSTRAINT board_member_workstreams_tenant_id_workstream_id_fkey FOREIGN KEY (tenant_id, workstream_id) REFERENCES proms.workstreams(tenant_id, id) ON DELETE CASCADE;
--rollback ALTER TABLE ONLY proms.board_member_workstreams DROP CONSTRAINT IF EXISTS board_member_workstreams_tenant_id_workstream_id_fkey;
--rollback ALTER TABLE ONLY proms.board_member_workstreams DROP CONSTRAINT IF EXISTS board_member_workstreams_tenant_id_board_member_id_fkey;

--changeset hyproms:06-fk-proms_board_members
ALTER TABLE ONLY proms.board_members
    ADD CONSTRAINT board_members_tenant_id_board_id_fkey FOREIGN KEY (tenant_id, board_id) REFERENCES proms.boards(tenant_id, id);
ALTER TABLE ONLY proms.board_members
    ADD CONSTRAINT board_members_user_id_fkey FOREIGN KEY (user_id) REFERENCES platform.users(id);
--rollback ALTER TABLE ONLY proms.board_members DROP CONSTRAINT IF EXISTS board_members_user_id_fkey;
--rollback ALTER TABLE ONLY proms.board_members DROP CONSTRAINT IF EXISTS board_members_tenant_id_board_id_fkey;

--changeset hyproms:06-fk-proms_board_pins
ALTER TABLE ONLY proms.board_pins
    ADD CONSTRAINT board_pins_tenant_id_board_id_fkey FOREIGN KEY (tenant_id, board_id) REFERENCES proms.boards(tenant_id, id) ON DELETE CASCADE;
ALTER TABLE ONLY proms.board_pins
    ADD CONSTRAINT board_pins_user_id_fkey FOREIGN KEY (user_id) REFERENCES platform.users(id) ON DELETE CASCADE;
--rollback ALTER TABLE ONLY proms.board_pins DROP CONSTRAINT IF EXISTS board_pins_user_id_fkey;
--rollback ALTER TABLE ONLY proms.board_pins DROP CONSTRAINT IF EXISTS board_pins_tenant_id_board_id_fkey;

--changeset hyproms:06-fk-proms_board_saved_views
ALTER TABLE ONLY proms.board_saved_views
    ADD CONSTRAINT board_saved_views_tenant_id_board_id_fkey FOREIGN KEY (tenant_id, board_id) REFERENCES proms.boards(tenant_id, id);
ALTER TABLE ONLY proms.board_saved_views
    ADD CONSTRAINT board_saved_views_user_id_fkey FOREIGN KEY (user_id) REFERENCES platform.users(id);
--rollback ALTER TABLE ONLY proms.board_saved_views DROP CONSTRAINT IF EXISTS board_saved_views_user_id_fkey;
--rollback ALTER TABLE ONLY proms.board_saved_views DROP CONSTRAINT IF EXISTS board_saved_views_tenant_id_board_id_fkey;

--changeset hyproms:06-fk-proms_boards
ALTER TABLE ONLY proms.boards
    ADD CONSTRAINT boards_owner_id_fkey FOREIGN KEY (owner_id) REFERENCES platform.users(id);
ALTER TABLE ONLY proms.boards
    ADD CONSTRAINT boards_pm_id_fkey FOREIGN KEY (pm_id) REFERENCES platform.users(id);
ALTER TABLE ONLY proms.boards
    ADD CONSTRAINT boards_tenant_id_board_group_id_fkey FOREIGN KEY (tenant_id, board_group_id) REFERENCES proms.board_groups(tenant_id, id);
ALTER TABLE ONLY proms.boards
    ADD CONSTRAINT boards_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES platform.tenants(id);
--rollback ALTER TABLE ONLY proms.boards DROP CONSTRAINT IF EXISTS boards_tenant_id_fkey;
--rollback ALTER TABLE ONLY proms.boards DROP CONSTRAINT IF EXISTS boards_tenant_id_board_group_id_fkey;
--rollback ALTER TABLE ONLY proms.boards DROP CONSTRAINT IF EXISTS boards_pm_id_fkey;
--rollback ALTER TABLE ONLY proms.boards DROP CONSTRAINT IF EXISTS boards_owner_id_fkey;

--changeset hyproms:06-fk-proms_budget_categories
ALTER TABLE ONLY proms.budget_categories
    ADD CONSTRAINT budget_categories_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES platform.tenants(id);
--rollback ALTER TABLE ONLY proms.budget_categories DROP CONSTRAINT IF EXISTS budget_categories_tenant_id_fkey;

--changeset hyproms:06-fk-proms_budget_lines
ALTER TABLE ONLY proms.budget_lines
    ADD CONSTRAINT budget_lines_tenant_id_board_id_fkey FOREIGN KEY (tenant_id, board_id) REFERENCES proms.boards(tenant_id, id);
ALTER TABLE ONLY proms.budget_lines
    ADD CONSTRAINT budget_lines_tenant_id_category_id_fkey FOREIGN KEY (tenant_id, category_id) REFERENCES proms.budget_categories(tenant_id, id);
--rollback ALTER TABLE ONLY proms.budget_lines DROP CONSTRAINT IF EXISTS budget_lines_tenant_id_category_id_fkey;
--rollback ALTER TABLE ONLY proms.budget_lines DROP CONSTRAINT IF EXISTS budget_lines_tenant_id_board_id_fkey;

--changeset hyproms:06-fk-proms_chat_channel_members
ALTER TABLE ONLY proms.chat_channel_members
    ADD CONSTRAINT chat_channel_members_deleted_by_fkey FOREIGN KEY (deleted_by) REFERENCES platform.users(id);
ALTER TABLE ONLY proms.chat_channel_members
    ADD CONSTRAINT chat_channel_members_tenant_id_channel_id_fkey FOREIGN KEY (tenant_id, channel_id) REFERENCES proms.chat_channels(tenant_id, id) ON DELETE CASCADE;
ALTER TABLE ONLY proms.chat_channel_members
    ADD CONSTRAINT chat_channel_members_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES platform.tenants(id);
ALTER TABLE ONLY proms.chat_channel_members
    ADD CONSTRAINT chat_channel_members_user_id_fkey FOREIGN KEY (user_id) REFERENCES platform.users(id);
--rollback ALTER TABLE ONLY proms.chat_channel_members DROP CONSTRAINT IF EXISTS chat_channel_members_user_id_fkey;
--rollback ALTER TABLE ONLY proms.chat_channel_members DROP CONSTRAINT IF EXISTS chat_channel_members_tenant_id_fkey;
--rollback ALTER TABLE ONLY proms.chat_channel_members DROP CONSTRAINT IF EXISTS chat_channel_members_tenant_id_channel_id_fkey;
--rollback ALTER TABLE ONLY proms.chat_channel_members DROP CONSTRAINT IF EXISTS chat_channel_members_deleted_by_fkey;

--changeset hyproms:06-fk-proms_chat_channels
ALTER TABLE ONLY proms.chat_channels
    ADD CONSTRAINT chat_channels_archived_by_fkey FOREIGN KEY (archived_by) REFERENCES platform.users(id);
ALTER TABLE ONLY proms.chat_channels
    ADD CONSTRAINT chat_channels_created_by_fkey FOREIGN KEY (created_by) REFERENCES platform.users(id);
ALTER TABLE ONLY proms.chat_channels
    ADD CONSTRAINT chat_channels_deleted_by_fkey FOREIGN KEY (deleted_by) REFERENCES platform.users(id);
ALTER TABLE ONLY proms.chat_channels
    ADD CONSTRAINT chat_channels_tenant_id_board_id_fkey FOREIGN KEY (tenant_id, board_id) REFERENCES proms.boards(tenant_id, id) ON DELETE CASCADE;
ALTER TABLE ONLY proms.chat_channels
    ADD CONSTRAINT chat_channels_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES platform.tenants(id);
ALTER TABLE ONLY proms.chat_channels
    ADD CONSTRAINT chat_channels_tenant_id_workstream_id_fkey FOREIGN KEY (tenant_id, workstream_id) REFERENCES proms.workstreams(tenant_id, id) ON DELETE SET NULL;
--rollback ALTER TABLE ONLY proms.chat_channels DROP CONSTRAINT IF EXISTS chat_channels_tenant_id_workstream_id_fkey;
--rollback ALTER TABLE ONLY proms.chat_channels DROP CONSTRAINT IF EXISTS chat_channels_tenant_id_fkey;
--rollback ALTER TABLE ONLY proms.chat_channels DROP CONSTRAINT IF EXISTS chat_channels_tenant_id_board_id_fkey;
--rollback ALTER TABLE ONLY proms.chat_channels DROP CONSTRAINT IF EXISTS chat_channels_deleted_by_fkey;
--rollback ALTER TABLE ONLY proms.chat_channels DROP CONSTRAINT IF EXISTS chat_channels_created_by_fkey;
--rollback ALTER TABLE ONLY proms.chat_channels DROP CONSTRAINT IF EXISTS chat_channels_archived_by_fkey;

--changeset hyproms:06-fk-proms_chat_messages
ALTER TABLE ONLY proms.chat_messages
    ADD CONSTRAINT chat_messages_pinned_by_fkey FOREIGN KEY (pinned_by) REFERENCES platform.users(id);
ALTER TABLE ONLY proms.chat_messages
    ADD CONSTRAINT chat_messages_tenant_id_channel_id_fkey FOREIGN KEY (tenant_id, channel_id) REFERENCES proms.chat_channels(tenant_id, id) ON DELETE CASCADE;
ALTER TABLE ONLY proms.chat_messages
    ADD CONSTRAINT chat_messages_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES platform.tenants(id);
ALTER TABLE ONLY proms.chat_messages
    ADD CONSTRAINT chat_messages_tenant_id_parent_id_fkey FOREIGN KEY (tenant_id, parent_id) REFERENCES proms.chat_messages(tenant_id, id) ON DELETE CASCADE;
ALTER TABLE ONLY proms.chat_messages
    ADD CONSTRAINT chat_messages_tenant_id_task_id_fkey FOREIGN KEY (tenant_id, task_id) REFERENCES proms.tasks(tenant_id, id) ON DELETE SET NULL;
ALTER TABLE ONLY proms.chat_messages
    ADD CONSTRAINT chat_messages_user_id_fkey FOREIGN KEY (user_id) REFERENCES platform.users(id);
--rollback ALTER TABLE ONLY proms.chat_messages DROP CONSTRAINT IF EXISTS chat_messages_user_id_fkey;
--rollback ALTER TABLE ONLY proms.chat_messages DROP CONSTRAINT IF EXISTS chat_messages_tenant_id_task_id_fkey;
--rollback ALTER TABLE ONLY proms.chat_messages DROP CONSTRAINT IF EXISTS chat_messages_tenant_id_parent_id_fkey;
--rollback ALTER TABLE ONLY proms.chat_messages DROP CONSTRAINT IF EXISTS chat_messages_tenant_id_fkey;
--rollback ALTER TABLE ONLY proms.chat_messages DROP CONSTRAINT IF EXISTS chat_messages_tenant_id_channel_id_fkey;
--rollback ALTER TABLE ONLY proms.chat_messages DROP CONSTRAINT IF EXISTS chat_messages_pinned_by_fkey;

--changeset hyproms:06-fk-proms_chat_reactions
ALTER TABLE ONLY proms.chat_reactions
    ADD CONSTRAINT chat_reactions_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES platform.tenants(id);
ALTER TABLE ONLY proms.chat_reactions
    ADD CONSTRAINT chat_reactions_tenant_id_message_id_fkey FOREIGN KEY (tenant_id, message_id) REFERENCES proms.chat_messages(tenant_id, id) ON DELETE CASCADE;
ALTER TABLE ONLY proms.chat_reactions
    ADD CONSTRAINT chat_reactions_user_id_fkey FOREIGN KEY (user_id) REFERENCES platform.users(id);
--rollback ALTER TABLE ONLY proms.chat_reactions DROP CONSTRAINT IF EXISTS chat_reactions_user_id_fkey;
--rollback ALTER TABLE ONLY proms.chat_reactions DROP CONSTRAINT IF EXISTS chat_reactions_tenant_id_message_id_fkey;
--rollback ALTER TABLE ONLY proms.chat_reactions DROP CONSTRAINT IF EXISTS chat_reactions_tenant_id_fkey;

--changeset hyproms:06-fk-proms_chat_read_markers
ALTER TABLE ONLY proms.chat_read_markers
    ADD CONSTRAINT chat_read_markers_tenant_id_channel_id_fkey FOREIGN KEY (tenant_id, channel_id) REFERENCES proms.chat_channels(tenant_id, id) ON DELETE CASCADE;
ALTER TABLE ONLY proms.chat_read_markers
    ADD CONSTRAINT chat_read_markers_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES platform.tenants(id);
ALTER TABLE ONLY proms.chat_read_markers
    ADD CONSTRAINT chat_read_markers_user_id_fkey FOREIGN KEY (user_id) REFERENCES platform.users(id);
--rollback ALTER TABLE ONLY proms.chat_read_markers DROP CONSTRAINT IF EXISTS chat_read_markers_user_id_fkey;
--rollback ALTER TABLE ONLY proms.chat_read_markers DROP CONSTRAINT IF EXISTS chat_read_markers_tenant_id_fkey;
--rollback ALTER TABLE ONLY proms.chat_read_markers DROP CONSTRAINT IF EXISTS chat_read_markers_tenant_id_channel_id_fkey;

--changeset hyproms:06-fk-proms_checklist_items
ALTER TABLE ONLY proms.checklist_items
    ADD CONSTRAINT checklist_items_tenant_id_task_id_fkey FOREIGN KEY (tenant_id, task_id) REFERENCES proms.tasks(tenant_id, id) ON DELETE CASCADE;
--rollback ALTER TABLE ONLY proms.checklist_items DROP CONSTRAINT IF EXISTS checklist_items_tenant_id_task_id_fkey;

--changeset hyproms:06-fk-proms_comment_mentions
ALTER TABLE ONLY proms.comment_mentions
    ADD CONSTRAINT comment_mentions_tenant_id_comment_id_fkey FOREIGN KEY (tenant_id, comment_id) REFERENCES proms.comments(tenant_id, id) ON DELETE CASCADE;
ALTER TABLE ONLY proms.comment_mentions
    ADD CONSTRAINT comment_mentions_tenant_id_referenced_task_fkey FOREIGN KEY (tenant_id, referenced_task) REFERENCES proms.tasks(tenant_id, id);
ALTER TABLE ONLY proms.comment_mentions
    ADD CONSTRAINT comment_mentions_user_id_fkey FOREIGN KEY (user_id) REFERENCES platform.users(id);
--rollback ALTER TABLE ONLY proms.comment_mentions DROP CONSTRAINT IF EXISTS comment_mentions_user_id_fkey;
--rollback ALTER TABLE ONLY proms.comment_mentions DROP CONSTRAINT IF EXISTS comment_mentions_tenant_id_referenced_task_fkey;
--rollback ALTER TABLE ONLY proms.comment_mentions DROP CONSTRAINT IF EXISTS comment_mentions_tenant_id_comment_id_fkey;

--changeset hyproms:06-fk-proms_comments
ALTER TABLE ONLY proms.comments
    ADD CONSTRAINT comments_author_id_fkey FOREIGN KEY (author_id) REFERENCES platform.users(id);
ALTER TABLE ONLY proms.comments
    ADD CONSTRAINT comments_tenant_id_task_id_fkey FOREIGN KEY (tenant_id, task_id) REFERENCES proms.tasks(tenant_id, id);
--rollback ALTER TABLE ONLY proms.comments DROP CONSTRAINT IF EXISTS comments_tenant_id_task_id_fkey;
--rollback ALTER TABLE ONLY proms.comments DROP CONSTRAINT IF EXISTS comments_author_id_fkey;

--changeset hyproms:06-fk-proms_dashboard_reminders
ALTER TABLE ONLY proms.dashboard_reminders
    ADD CONSTRAINT dashboard_reminders_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES platform.tenants(id);
ALTER TABLE ONLY proms.dashboard_reminders
    ADD CONSTRAINT dashboard_reminders_user_id_fkey FOREIGN KEY (user_id) REFERENCES platform.users(id);
--rollback ALTER TABLE ONLY proms.dashboard_reminders DROP CONSTRAINT IF EXISTS dashboard_reminders_user_id_fkey;
--rollback ALTER TABLE ONLY proms.dashboard_reminders DROP CONSTRAINT IF EXISTS dashboard_reminders_tenant_id_fkey;

--changeset hyproms:06-fk-proms_expenses
ALTER TABLE ONLY proms.expenses
    ADD CONSTRAINT expenses_decided_by_fkey FOREIGN KEY (decided_by) REFERENCES platform.users(id);
ALTER TABLE ONLY proms.expenses
    ADD CONSTRAINT expenses_submitted_by_fkey FOREIGN KEY (submitted_by) REFERENCES platform.users(id);
ALTER TABLE ONLY proms.expenses
    ADD CONSTRAINT expenses_tenant_id_board_id_fkey FOREIGN KEY (tenant_id, board_id) REFERENCES proms.boards(tenant_id, id);
ALTER TABLE ONLY proms.expenses
    ADD CONSTRAINT expenses_tenant_id_category_id_fkey FOREIGN KEY (tenant_id, category_id) REFERENCES proms.budget_categories(tenant_id, id);
ALTER TABLE ONLY proms.expenses
    ADD CONSTRAINT expenses_tenant_id_receipt_attachment_id_fkey FOREIGN KEY (tenant_id, receipt_attachment_id) REFERENCES proms.attachments(tenant_id, id);
--rollback ALTER TABLE ONLY proms.expenses DROP CONSTRAINT IF EXISTS expenses_tenant_id_receipt_attachment_id_fkey;
--rollback ALTER TABLE ONLY proms.expenses DROP CONSTRAINT IF EXISTS expenses_tenant_id_category_id_fkey;
--rollback ALTER TABLE ONLY proms.expenses DROP CONSTRAINT IF EXISTS expenses_tenant_id_board_id_fkey;
--rollback ALTER TABLE ONLY proms.expenses DROP CONSTRAINT IF EXISTS expenses_submitted_by_fkey;
--rollback ALTER TABLE ONLY proms.expenses DROP CONSTRAINT IF EXISTS expenses_decided_by_fkey;

--changeset hyproms:06-fk-proms_focus_sessions
ALTER TABLE ONLY proms.focus_sessions
    ADD CONSTRAINT focus_sessions_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES platform.tenants(id);
ALTER TABLE ONLY proms.focus_sessions
    ADD CONSTRAINT focus_sessions_user_id_fkey FOREIGN KEY (user_id) REFERENCES platform.users(id);
--rollback ALTER TABLE ONLY proms.focus_sessions DROP CONSTRAINT IF EXISTS focus_sessions_user_id_fkey;
--rollback ALTER TABLE ONLY proms.focus_sessions DROP CONSTRAINT IF EXISTS focus_sessions_tenant_id_fkey;

--changeset hyproms:06-fk-proms_form_fields
ALTER TABLE ONLY proms.form_fields
    ADD CONSTRAINT form_fields_tenant_id_form_id_fkey FOREIGN KEY (tenant_id, form_id) REFERENCES proms.forms(tenant_id, id) ON DELETE CASCADE;
--rollback ALTER TABLE ONLY proms.form_fields DROP CONSTRAINT IF EXISTS form_fields_tenant_id_form_id_fkey;

--changeset hyproms:06-fk-proms_form_submissions
ALTER TABLE ONLY proms.form_submissions
    ADD CONSTRAINT form_submissions_submitter_user_id_fkey FOREIGN KEY (submitter_user_id) REFERENCES platform.users(id);
ALTER TABLE ONLY proms.form_submissions
    ADD CONSTRAINT form_submissions_tenant_id_form_id_fkey FOREIGN KEY (tenant_id, form_id) REFERENCES proms.forms(tenant_id, id);
ALTER TABLE ONLY proms.form_submissions
    ADD CONSTRAINT form_submissions_tenant_id_task_id_fkey FOREIGN KEY (tenant_id, task_id) REFERENCES proms.tasks(tenant_id, id);
--rollback ALTER TABLE ONLY proms.form_submissions DROP CONSTRAINT IF EXISTS form_submissions_tenant_id_task_id_fkey;
--rollback ALTER TABLE ONLY proms.form_submissions DROP CONSTRAINT IF EXISTS form_submissions_tenant_id_form_id_fkey;
--rollback ALTER TABLE ONLY proms.form_submissions DROP CONSTRAINT IF EXISTS form_submissions_submitter_user_id_fkey;

--changeset hyproms:06-fk-proms_forms
ALTER TABLE ONLY proms.forms
    ADD CONSTRAINT forms_target_assignee_id_fkey FOREIGN KEY (target_assignee_id) REFERENCES platform.users(id);
ALTER TABLE ONLY proms.forms
    ADD CONSTRAINT forms_tenant_id_board_id_fkey FOREIGN KEY (tenant_id, board_id) REFERENCES proms.boards(tenant_id, id);
ALTER TABLE ONLY proms.forms
    ADD CONSTRAINT forms_tenant_id_target_column_id_fkey FOREIGN KEY (tenant_id, target_column_id) REFERENCES proms.board_columns(tenant_id, id);
ALTER TABLE ONLY proms.forms
    ADD CONSTRAINT forms_tenant_id_target_tag_id_fkey FOREIGN KEY (tenant_id, target_tag_id) REFERENCES proms.tags(tenant_id, id);
ALTER TABLE ONLY proms.forms
    ADD CONSTRAINT forms_tenant_id_target_workstream_id_fkey FOREIGN KEY (tenant_id, target_workstream_id) REFERENCES proms.workstreams(tenant_id, id);
--rollback ALTER TABLE ONLY proms.forms DROP CONSTRAINT IF EXISTS forms_tenant_id_target_workstream_id_fkey;
--rollback ALTER TABLE ONLY proms.forms DROP CONSTRAINT IF EXISTS forms_tenant_id_target_tag_id_fkey;
--rollback ALTER TABLE ONLY proms.forms DROP CONSTRAINT IF EXISTS forms_tenant_id_target_column_id_fkey;
--rollback ALTER TABLE ONLY proms.forms DROP CONSTRAINT IF EXISTS forms_tenant_id_board_id_fkey;
--rollback ALTER TABLE ONLY proms.forms DROP CONSTRAINT IF EXISTS forms_target_assignee_id_fkey;

--changeset hyproms:06-fk-proms_holidays
ALTER TABLE ONLY proms.holidays
    ADD CONSTRAINT holidays_created_by_fkey FOREIGN KEY (created_by) REFERENCES platform.users(id);
ALTER TABLE ONLY proms.holidays
    ADD CONSTRAINT holidays_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES platform.tenants(id);
--rollback ALTER TABLE ONLY proms.holidays DROP CONSTRAINT IF EXISTS holidays_tenant_id_fkey;
--rollback ALTER TABLE ONLY proms.holidays DROP CONSTRAINT IF EXISTS holidays_created_by_fkey;

--changeset hyproms:06-fk-proms_idempotency_keys
ALTER TABLE ONLY proms.idempotency_keys
    ADD CONSTRAINT idempotency_keys_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES platform.tenants(id);
--rollback ALTER TABLE ONLY proms.idempotency_keys DROP CONSTRAINT IF EXISTS idempotency_keys_tenant_id_fkey;

--changeset hyproms:06-fk-proms_invoices
ALTER TABLE ONLY proms.invoices
    ADD CONSTRAINT invoices_tenant_id_payment_term_id_fkey FOREIGN KEY (tenant_id, payment_term_id) REFERENCES proms.payment_terms(tenant_id, id) ON DELETE RESTRICT;
--rollback ALTER TABLE ONLY proms.invoices DROP CONSTRAINT IF EXISTS invoices_tenant_id_payment_term_id_fkey;

--changeset hyproms:06-fk-proms_meeting_action_items
ALTER TABLE ONLY proms.meeting_action_items
    ADD CONSTRAINT meeting_action_items_assignee_id_fkey FOREIGN KEY (assignee_id) REFERENCES platform.users(id) ON DELETE SET NULL;
ALTER TABLE ONLY proms.meeting_action_items
    ADD CONSTRAINT meeting_action_items_meeting_id_fkey FOREIGN KEY (meeting_id) REFERENCES proms.meeting_minutes(id) ON DELETE CASCADE;
ALTER TABLE ONLY proms.meeting_action_items
    ADD CONSTRAINT meeting_action_items_task_id_fkey FOREIGN KEY (task_id) REFERENCES proms.tasks(id) ON DELETE SET NULL;
ALTER TABLE ONLY proms.meeting_action_items
    ADD CONSTRAINT meeting_action_items_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES platform.tenants(id);
--rollback ALTER TABLE ONLY proms.meeting_action_items DROP CONSTRAINT IF EXISTS meeting_action_items_tenant_id_fkey;
--rollback ALTER TABLE ONLY proms.meeting_action_items DROP CONSTRAINT IF EXISTS meeting_action_items_task_id_fkey;
--rollback ALTER TABLE ONLY proms.meeting_action_items DROP CONSTRAINT IF EXISTS meeting_action_items_meeting_id_fkey;
--rollback ALTER TABLE ONLY proms.meeting_action_items DROP CONSTRAINT IF EXISTS meeting_action_items_assignee_id_fkey;

--changeset hyproms:06-fk-proms_meeting_attendees
ALTER TABLE ONLY proms.meeting_attendees
    ADD CONSTRAINT meeting_attendees_meeting_id_fkey FOREIGN KEY (meeting_id) REFERENCES proms.meeting_minutes(id) ON DELETE CASCADE;
ALTER TABLE ONLY proms.meeting_attendees
    ADD CONSTRAINT meeting_attendees_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES platform.tenants(id);
ALTER TABLE ONLY proms.meeting_attendees
    ADD CONSTRAINT meeting_attendees_user_id_fkey FOREIGN KEY (user_id) REFERENCES platform.users(id) ON DELETE SET NULL;
--rollback ALTER TABLE ONLY proms.meeting_attendees DROP CONSTRAINT IF EXISTS meeting_attendees_user_id_fkey;
--rollback ALTER TABLE ONLY proms.meeting_attendees DROP CONSTRAINT IF EXISTS meeting_attendees_tenant_id_fkey;
--rollback ALTER TABLE ONLY proms.meeting_attendees DROP CONSTRAINT IF EXISTS meeting_attendees_meeting_id_fkey;

--changeset hyproms:06-fk-proms_meeting_minutes
ALTER TABLE ONLY proms.meeting_minutes
    ADD CONSTRAINT meeting_minutes_board_id_fkey FOREIGN KEY (board_id) REFERENCES proms.boards(id);
ALTER TABLE ONLY proms.meeting_minutes
    ADD CONSTRAINT meeting_minutes_created_by_fkey FOREIGN KEY (created_by) REFERENCES platform.users(id);
ALTER TABLE ONLY proms.meeting_minutes
    ADD CONSTRAINT meeting_minutes_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES platform.tenants(id);
--rollback ALTER TABLE ONLY proms.meeting_minutes DROP CONSTRAINT IF EXISTS meeting_minutes_tenant_id_fkey;
--rollback ALTER TABLE ONLY proms.meeting_minutes DROP CONSTRAINT IF EXISTS meeting_minutes_created_by_fkey;
--rollback ALTER TABLE ONLY proms.meeting_minutes DROP CONSTRAINT IF EXISTS meeting_minutes_board_id_fkey;

--changeset hyproms:06-fk-proms_milestone_workstreams
ALTER TABLE ONLY proms.milestone_workstreams
    ADD CONSTRAINT milestone_workstreams_tenant_id_milestone_id_fkey FOREIGN KEY (tenant_id, milestone_id) REFERENCES proms.milestones(tenant_id, id) ON DELETE CASCADE;
ALTER TABLE ONLY proms.milestone_workstreams
    ADD CONSTRAINT milestone_workstreams_tenant_id_workstream_id_fkey FOREIGN KEY (tenant_id, workstream_id) REFERENCES proms.workstreams(tenant_id, id) ON DELETE CASCADE;
--rollback ALTER TABLE ONLY proms.milestone_workstreams DROP CONSTRAINT IF EXISTS milestone_workstreams_tenant_id_workstream_id_fkey;
--rollback ALTER TABLE ONLY proms.milestone_workstreams DROP CONSTRAINT IF EXISTS milestone_workstreams_tenant_id_milestone_id_fkey;

--changeset hyproms:06-fk-proms_milestones
ALTER TABLE ONLY proms.milestones
    ADD CONSTRAINT milestones_tenant_id_board_id_fkey FOREIGN KEY (tenant_id, board_id) REFERENCES proms.boards(tenant_id, id);
--rollback ALTER TABLE ONLY proms.milestones DROP CONSTRAINT IF EXISTS milestones_tenant_id_board_id_fkey;

--changeset hyproms:06-fk-proms_notifications
ALTER TABLE ONLY proms.notifications
    ADD CONSTRAINT notifications_recipient_id_fkey FOREIGN KEY (recipient_id) REFERENCES platform.users(id);
ALTER TABLE ONLY proms.notifications
    ADD CONSTRAINT notifications_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES platform.tenants(id);
--rollback ALTER TABLE ONLY proms.notifications DROP CONSTRAINT IF EXISTS notifications_tenant_id_fkey;
--rollback ALTER TABLE ONLY proms.notifications DROP CONSTRAINT IF EXISTS notifications_recipient_id_fkey;

--changeset hyproms:06-fk-proms_payment_terms
ALTER TABLE ONLY proms.payment_terms
    ADD CONSTRAINT payment_terms_tenant_id_contract_id_fkey FOREIGN KEY (tenant_id, contract_id) REFERENCES proms.board_contracts(tenant_id, id) ON DELETE CASCADE;
ALTER TABLE ONLY proms.payment_terms
    ADD CONSTRAINT payment_terms_tenant_id_milestone_id_fkey FOREIGN KEY (tenant_id, milestone_id) REFERENCES proms.milestones(tenant_id, id);
--rollback ALTER TABLE ONLY proms.payment_terms DROP CONSTRAINT IF EXISTS payment_terms_tenant_id_milestone_id_fkey;
--rollback ALTER TABLE ONLY proms.payment_terms DROP CONSTRAINT IF EXISTS payment_terms_tenant_id_contract_id_fkey;

--changeset hyproms:06-fk-proms_point_entries
ALTER TABLE ONLY proms.point_entries
    ADD CONSTRAINT point_entries_created_by_fkey FOREIGN KEY (created_by) REFERENCES platform.users(id);
ALTER TABLE ONLY proms.point_entries
    ADD CONSTRAINT point_entries_tenant_id_board_id_fkey FOREIGN KEY (tenant_id, board_id) REFERENCES proms.boards(tenant_id, id);
ALTER TABLE ONLY proms.point_entries
    ADD CONSTRAINT point_entries_tenant_id_feedback_id_fkey FOREIGN KEY (tenant_id, feedback_id) REFERENCES proms.task_feedback(tenant_id, id);
ALTER TABLE ONLY proms.point_entries
    ADD CONSTRAINT point_entries_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES platform.tenants(id);
ALTER TABLE ONLY proms.point_entries
    ADD CONSTRAINT point_entries_tenant_id_reverses_id_fkey FOREIGN KEY (tenant_id, reverses_id) REFERENCES proms.point_entries(tenant_id, id);
ALTER TABLE ONLY proms.point_entries
    ADD CONSTRAINT point_entries_tenant_id_task_id_fkey FOREIGN KEY (tenant_id, task_id) REFERENCES proms.tasks(tenant_id, id);
ALTER TABLE ONLY proms.point_entries
    ADD CONSTRAINT point_entries_user_id_fkey FOREIGN KEY (user_id) REFERENCES platform.users(id);
--rollback ALTER TABLE ONLY proms.point_entries DROP CONSTRAINT IF EXISTS point_entries_user_id_fkey;
--rollback ALTER TABLE ONLY proms.point_entries DROP CONSTRAINT IF EXISTS point_entries_tenant_id_task_id_fkey;
--rollback ALTER TABLE ONLY proms.point_entries DROP CONSTRAINT IF EXISTS point_entries_tenant_id_reverses_id_fkey;
--rollback ALTER TABLE ONLY proms.point_entries DROP CONSTRAINT IF EXISTS point_entries_tenant_id_fkey;
--rollback ALTER TABLE ONLY proms.point_entries DROP CONSTRAINT IF EXISTS point_entries_tenant_id_feedback_id_fkey;
--rollback ALTER TABLE ONLY proms.point_entries DROP CONSTRAINT IF EXISTS point_entries_tenant_id_board_id_fkey;
--rollback ALTER TABLE ONLY proms.point_entries DROP CONSTRAINT IF EXISTS point_entries_created_by_fkey;

--changeset hyproms:06-fk-proms_point_policies
ALTER TABLE ONLY proms.point_policies
    ADD CONSTRAINT point_policies_created_by_fkey FOREIGN KEY (created_by) REFERENCES platform.users(id);
ALTER TABLE ONLY proms.point_policies
    ADD CONSTRAINT point_policies_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES platform.tenants(id);
--rollback ALTER TABLE ONLY proms.point_policies DROP CONSTRAINT IF EXISTS point_policies_tenant_id_fkey;
--rollback ALTER TABLE ONLY proms.point_policies DROP CONSTRAINT IF EXISTS point_policies_created_by_fkey;

--changeset hyproms:06-fk-proms_recurrence_rules
ALTER TABLE ONLY proms.recurrence_rules
    ADD CONSTRAINT recurrence_rules_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES platform.tenants(id);
ALTER TABLE ONLY proms.recurrence_rules
    ADD CONSTRAINT recurrence_rules_tenant_id_template_task_id_fkey FOREIGN KEY (tenant_id, template_task_id) REFERENCES proms.tasks(tenant_id, id);
--rollback ALTER TABLE ONLY proms.recurrence_rules DROP CONSTRAINT IF EXISTS recurrence_rules_tenant_id_template_task_id_fkey;
--rollback ALTER TABLE ONLY proms.recurrence_rules DROP CONSTRAINT IF EXISTS recurrence_rules_tenant_id_fkey;

--changeset hyproms:06-fk-proms_share_links
ALTER TABLE ONLY proms.share_links
    ADD CONSTRAINT share_links_tenant_id_board_id_fkey FOREIGN KEY (tenant_id, board_id) REFERENCES proms.boards(tenant_id, id);
--rollback ALTER TABLE ONLY proms.share_links DROP CONSTRAINT IF EXISTS share_links_tenant_id_board_id_fkey;

--changeset hyproms:06-fk-proms_standup_policies
ALTER TABLE ONLY proms.standup_policies
    ADD CONSTRAINT standup_policies_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES platform.tenants(id);
ALTER TABLE ONLY proms.standup_policies
    ADD CONSTRAINT standup_policies_updated_by_fkey FOREIGN KEY (updated_by) REFERENCES platform.users(id);
--rollback ALTER TABLE ONLY proms.standup_policies DROP CONSTRAINT IF EXISTS standup_policies_updated_by_fkey;
--rollback ALTER TABLE ONLY proms.standup_policies DROP CONSTRAINT IF EXISTS standup_policies_tenant_id_fkey;

--changeset hyproms:06-fk-proms_standup_reminder_runs
ALTER TABLE ONLY proms.standup_reminder_runs
    ADD CONSTRAINT standup_reminder_runs_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES platform.tenants(id);
--rollback ALTER TABLE ONLY proms.standup_reminder_runs DROP CONSTRAINT IF EXISTS standup_reminder_runs_tenant_id_fkey;

--changeset hyproms:06-fk-proms_standup_reports
ALTER TABLE ONLY proms.standup_reports
    ADD CONSTRAINT standup_reports_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES platform.tenants(id);
ALTER TABLE ONLY proms.standup_reports
    ADD CONSTRAINT standup_reports_user_id_fkey FOREIGN KEY (user_id) REFERENCES platform.users(id);
--rollback ALTER TABLE ONLY proms.standup_reports DROP CONSTRAINT IF EXISTS standup_reports_user_id_fkey;
--rollback ALTER TABLE ONLY proms.standup_reports DROP CONSTRAINT IF EXISTS standup_reports_tenant_id_fkey;

--changeset hyproms:06-fk-proms_tags
ALTER TABLE ONLY proms.tags
    ADD CONSTRAINT tags_tenant_id_board_id_fkey FOREIGN KEY (tenant_id, board_id) REFERENCES proms.boards(tenant_id, id);
ALTER TABLE ONLY proms.tags
    ADD CONSTRAINT tags_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES platform.tenants(id);
--rollback ALTER TABLE ONLY proms.tags DROP CONSTRAINT IF EXISTS tags_tenant_id_fkey;
--rollback ALTER TABLE ONLY proms.tags DROP CONSTRAINT IF EXISTS tags_tenant_id_board_id_fkey;

--changeset hyproms:06-fk-proms_task_activities
ALTER TABLE ONLY proms.task_activities
    ADD CONSTRAINT task_activities_tenant_id_task_id_fkey FOREIGN KEY (tenant_id, task_id) REFERENCES proms.tasks(tenant_id, id);
--rollback ALTER TABLE ONLY proms.task_activities DROP CONSTRAINT IF EXISTS task_activities_tenant_id_task_id_fkey;

--changeset hyproms:06-fk-proms_task_assignees
ALTER TABLE ONLY proms.task_assignees
    ADD CONSTRAINT task_assignees_tenant_id_task_id_fkey FOREIGN KEY (tenant_id, task_id) REFERENCES proms.tasks(tenant_id, id) ON DELETE CASCADE;
ALTER TABLE ONLY proms.task_assignees
    ADD CONSTRAINT task_assignees_user_id_fkey FOREIGN KEY (user_id) REFERENCES platform.users(id);
--rollback ALTER TABLE ONLY proms.task_assignees DROP CONSTRAINT IF EXISTS task_assignees_user_id_fkey;
--rollback ALTER TABLE ONLY proms.task_assignees DROP CONSTRAINT IF EXISTS task_assignees_tenant_id_task_id_fkey;

--changeset hyproms:06-fk-proms_task_dependencies
ALTER TABLE ONLY proms.task_dependencies
    ADD CONSTRAINT task_dependencies_tenant_id_blocked_task_id_fkey FOREIGN KEY (tenant_id, blocked_task_id) REFERENCES proms.tasks(tenant_id, id) ON DELETE CASCADE;
ALTER TABLE ONLY proms.task_dependencies
    ADD CONSTRAINT task_dependencies_tenant_id_blocking_task_id_fkey FOREIGN KEY (tenant_id, blocking_task_id) REFERENCES proms.tasks(tenant_id, id) ON DELETE CASCADE;
--rollback ALTER TABLE ONLY proms.task_dependencies DROP CONSTRAINT IF EXISTS task_dependencies_tenant_id_blocking_task_id_fkey;
--rollback ALTER TABLE ONLY proms.task_dependencies DROP CONSTRAINT IF EXISTS task_dependencies_tenant_id_blocked_task_id_fkey;

--changeset hyproms:06-fk-proms_task_feedback
ALTER TABLE ONLY proms.task_feedback
    ADD CONSTRAINT task_feedback_given_by_fkey FOREIGN KEY (given_by) REFERENCES platform.users(id);
ALTER TABLE ONLY proms.task_feedback
    ADD CONSTRAINT task_feedback_tenant_id_task_id_fkey FOREIGN KEY (tenant_id, task_id) REFERENCES proms.tasks(tenant_id, id);
--rollback ALTER TABLE ONLY proms.task_feedback DROP CONSTRAINT IF EXISTS task_feedback_tenant_id_task_id_fkey;
--rollback ALTER TABLE ONLY proms.task_feedback DROP CONSTRAINT IF EXISTS task_feedback_given_by_fkey;

--changeset hyproms:06-fk-proms_task_tags
ALTER TABLE ONLY proms.task_tags
    ADD CONSTRAINT task_tags_tenant_id_tag_id_fkey FOREIGN KEY (tenant_id, tag_id) REFERENCES proms.tags(tenant_id, id) ON DELETE CASCADE;
ALTER TABLE ONLY proms.task_tags
    ADD CONSTRAINT task_tags_tenant_id_task_id_fkey FOREIGN KEY (tenant_id, task_id) REFERENCES proms.tasks(tenant_id, id) ON DELETE CASCADE;
--rollback ALTER TABLE ONLY proms.task_tags DROP CONSTRAINT IF EXISTS task_tags_tenant_id_task_id_fkey;
--rollback ALTER TABLE ONLY proms.task_tags DROP CONSTRAINT IF EXISTS task_tags_tenant_id_tag_id_fkey;

--changeset hyproms:06-fk-proms_task_watchers
ALTER TABLE ONLY proms.task_watchers
    ADD CONSTRAINT task_watchers_tenant_id_task_id_fkey FOREIGN KEY (tenant_id, task_id) REFERENCES proms.tasks(tenant_id, id) ON DELETE CASCADE;
ALTER TABLE ONLY proms.task_watchers
    ADD CONSTRAINT task_watchers_user_id_fkey FOREIGN KEY (user_id) REFERENCES platform.users(id);
--rollback ALTER TABLE ONLY proms.task_watchers DROP CONSTRAINT IF EXISTS task_watchers_user_id_fkey;
--rollback ALTER TABLE ONLY proms.task_watchers DROP CONSTRAINT IF EXISTS task_watchers_tenant_id_task_id_fkey;

--changeset hyproms:06-fk-proms_tasks
ALTER TABLE ONLY proms.tasks
    ADD CONSTRAINT tasks_tenant_id_board_id_fkey FOREIGN KEY (tenant_id, board_id) REFERENCES proms.boards(tenant_id, id);
ALTER TABLE ONLY proms.tasks
    ADD CONSTRAINT tasks_tenant_id_column_id_fkey FOREIGN KEY (tenant_id, column_id) REFERENCES proms.board_columns(tenant_id, id);
ALTER TABLE ONLY proms.tasks
    ADD CONSTRAINT tasks_tenant_id_milestone_id_fkey FOREIGN KEY (tenant_id, milestone_id) REFERENCES proms.milestones(tenant_id, id);
ALTER TABLE ONLY proms.tasks
    ADD CONSTRAINT tasks_tenant_id_parent_id_fkey FOREIGN KEY (tenant_id, parent_id) REFERENCES proms.tasks(tenant_id, id);
ALTER TABLE ONLY proms.tasks
    ADD CONSTRAINT tasks_tenant_id_recurrence_rule_id_fkey FOREIGN KEY (tenant_id, recurrence_rule_id) REFERENCES proms.recurrence_rules(tenant_id, id);
ALTER TABLE ONLY proms.tasks
    ADD CONSTRAINT tasks_tenant_id_source_form_id_fkey FOREIGN KEY (tenant_id, source_form_id) REFERENCES proms.forms(tenant_id, id);
ALTER TABLE ONLY proms.tasks
    ADD CONSTRAINT tasks_tenant_id_workstream_id_fkey FOREIGN KEY (tenant_id, workstream_id) REFERENCES proms.workstreams(tenant_id, id);
--rollback ALTER TABLE ONLY proms.tasks DROP CONSTRAINT IF EXISTS tasks_tenant_id_workstream_id_fkey;
--rollback ALTER TABLE ONLY proms.tasks DROP CONSTRAINT IF EXISTS tasks_tenant_id_source_form_id_fkey;
--rollback ALTER TABLE ONLY proms.tasks DROP CONSTRAINT IF EXISTS tasks_tenant_id_recurrence_rule_id_fkey;
--rollback ALTER TABLE ONLY proms.tasks DROP CONSTRAINT IF EXISTS tasks_tenant_id_parent_id_fkey;
--rollback ALTER TABLE ONLY proms.tasks DROP CONSTRAINT IF EXISTS tasks_tenant_id_milestone_id_fkey;
--rollback ALTER TABLE ONLY proms.tasks DROP CONSTRAINT IF EXISTS tasks_tenant_id_column_id_fkey;
--rollback ALTER TABLE ONLY proms.tasks DROP CONSTRAINT IF EXISTS tasks_tenant_id_board_id_fkey;

--changeset hyproms:06-fk-proms_template_columns
ALTER TABLE ONLY proms.template_columns
    ADD CONSTRAINT template_columns_template_id_fkey FOREIGN KEY (template_id) REFERENCES proms.workflow_templates(id) ON DELETE CASCADE;
--rollback ALTER TABLE ONLY proms.template_columns DROP CONSTRAINT IF EXISTS template_columns_template_id_fkey;

--changeset hyproms:06-fk-proms_time_entries
ALTER TABLE ONLY proms.time_entries
    ADD CONSTRAINT time_entries_tenant_id_board_id_fkey FOREIGN KEY (tenant_id, board_id) REFERENCES proms.boards(tenant_id, id);
ALTER TABLE ONLY proms.time_entries
    ADD CONSTRAINT time_entries_tenant_id_task_id_fkey FOREIGN KEY (tenant_id, task_id) REFERENCES proms.tasks(tenant_id, id);
ALTER TABLE ONLY proms.time_entries
    ADD CONSTRAINT time_entries_tenant_id_timesheet_id_fkey FOREIGN KEY (tenant_id, timesheet_id) REFERENCES proms.timesheets(tenant_id, id);
ALTER TABLE ONLY proms.time_entries
    ADD CONSTRAINT time_entries_user_id_fkey FOREIGN KEY (user_id) REFERENCES platform.users(id);
--rollback ALTER TABLE ONLY proms.time_entries DROP CONSTRAINT IF EXISTS time_entries_user_id_fkey;
--rollback ALTER TABLE ONLY proms.time_entries DROP CONSTRAINT IF EXISTS time_entries_tenant_id_timesheet_id_fkey;
--rollback ALTER TABLE ONLY proms.time_entries DROP CONSTRAINT IF EXISTS time_entries_tenant_id_task_id_fkey;
--rollback ALTER TABLE ONLY proms.time_entries DROP CONSTRAINT IF EXISTS time_entries_tenant_id_board_id_fkey;

--changeset hyproms:06-fk-proms_time_off
ALTER TABLE ONLY proms.time_off
    ADD CONSTRAINT time_off_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES platform.tenants(id);
ALTER TABLE ONLY proms.time_off
    ADD CONSTRAINT time_off_user_id_fkey FOREIGN KEY (user_id) REFERENCES platform.users(id);
--rollback ALTER TABLE ONLY proms.time_off DROP CONSTRAINT IF EXISTS time_off_user_id_fkey;
--rollback ALTER TABLE ONLY proms.time_off DROP CONSTRAINT IF EXISTS time_off_tenant_id_fkey;

--changeset hyproms:06-fk-proms_timesheets
ALTER TABLE ONLY proms.timesheets
    ADD CONSTRAINT timesheets_tenant_id_board_id_fkey FOREIGN KEY (tenant_id, board_id) REFERENCES proms.boards(tenant_id, id);
ALTER TABLE ONLY proms.timesheets
    ADD CONSTRAINT timesheets_user_id_fkey FOREIGN KEY (user_id) REFERENCES platform.users(id);
--rollback ALTER TABLE ONLY proms.timesheets DROP CONSTRAINT IF EXISTS timesheets_user_id_fkey;
--rollback ALTER TABLE ONLY proms.timesheets DROP CONSTRAINT IF EXISTS timesheets_tenant_id_board_id_fkey;

--changeset hyproms:06-fk-proms_user_preferences
ALTER TABLE ONLY proms.user_preferences
    ADD CONSTRAINT user_preferences_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES platform.tenants(id);
ALTER TABLE ONLY proms.user_preferences
    ADD CONSTRAINT user_preferences_tenant_id_timer_task_id_fkey FOREIGN KEY (tenant_id, timer_task_id) REFERENCES proms.tasks(tenant_id, id);
ALTER TABLE ONLY proms.user_preferences
    ADD CONSTRAINT user_preferences_user_id_fkey FOREIGN KEY (user_id) REFERENCES platform.users(id);
--rollback ALTER TABLE ONLY proms.user_preferences DROP CONSTRAINT IF EXISTS user_preferences_user_id_fkey;
--rollback ALTER TABLE ONLY proms.user_preferences DROP CONSTRAINT IF EXISTS user_preferences_tenant_id_timer_task_id_fkey;
--rollback ALTER TABLE ONLY proms.user_preferences DROP CONSTRAINT IF EXISTS user_preferences_tenant_id_fkey;

--changeset hyproms:06-fk-proms_workflow_templates
ALTER TABLE ONLY proms.workflow_templates
    ADD CONSTRAINT workflow_templates_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES platform.tenants(id);
--rollback ALTER TABLE ONLY proms.workflow_templates DROP CONSTRAINT IF EXISTS workflow_templates_tenant_id_fkey;

--changeset hyproms:06-fk-proms_workspace_calendars
ALTER TABLE ONLY proms.workspace_calendars
    ADD CONSTRAINT workspace_calendars_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES platform.tenants(id);
ALTER TABLE ONLY proms.workspace_calendars
    ADD CONSTRAINT workspace_calendars_updated_by_fkey FOREIGN KEY (updated_by) REFERENCES platform.users(id);
--rollback ALTER TABLE ONLY proms.workspace_calendars DROP CONSTRAINT IF EXISTS workspace_calendars_updated_by_fkey;
--rollback ALTER TABLE ONLY proms.workspace_calendars DROP CONSTRAINT IF EXISTS workspace_calendars_tenant_id_fkey;

--changeset hyproms:06-fk-proms_workstreams
ALTER TABLE ONLY proms.workstreams
    ADD CONSTRAINT workstreams_lead_id_fkey FOREIGN KEY (lead_id) REFERENCES platform.users(id);
ALTER TABLE ONLY proms.workstreams
    ADD CONSTRAINT workstreams_template_id_fkey FOREIGN KEY (template_id) REFERENCES proms.workflow_templates(id);
ALTER TABLE ONLY proms.workstreams
    ADD CONSTRAINT workstreams_tenant_id_board_id_fkey FOREIGN KEY (tenant_id, board_id) REFERENCES proms.boards(tenant_id, id);
ALTER TABLE ONLY proms.workstreams
    ADD CONSTRAINT workstreams_tenant_id_team_id_fkey FOREIGN KEY (tenant_id, team_id) REFERENCES platform.teams(tenant_id, id);
--rollback ALTER TABLE ONLY proms.workstreams DROP CONSTRAINT IF EXISTS workstreams_tenant_id_team_id_fkey;
--rollback ALTER TABLE ONLY proms.workstreams DROP CONSTRAINT IF EXISTS workstreams_tenant_id_board_id_fkey;
--rollback ALTER TABLE ONLY proms.workstreams DROP CONSTRAINT IF EXISTS workstreams_template_id_fkey;
--rollback ALTER TABLE ONLY proms.workstreams DROP CONSTRAINT IF EXISTS workstreams_lead_id_fkey;
