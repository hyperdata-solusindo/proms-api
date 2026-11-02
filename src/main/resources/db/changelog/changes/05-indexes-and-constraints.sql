--liquibase formatted sql

--changeset hyproms:05-idx-platform_audit_logs
ALTER TABLE ONLY platform.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id, created_at);
CREATE INDEX audit_logs_entity ON ONLY platform.audit_logs USING btree (entity_type, entity_id);
CREATE INDEX audit_logs_tenant_time ON ONLY platform.audit_logs USING btree (tenant_id, created_at DESC);
--rollback DROP INDEX IF EXISTS platform.audit_logs_tenant_time CASCADE;
--rollback DROP INDEX IF EXISTS platform.audit_logs_entity CASCADE;
--rollback ALTER TABLE ONLY platform.audit_logs DROP CONSTRAINT IF EXISTS audit_logs_pkey CASCADE;

--changeset hyproms:05-idx-platform_audit_logs_default
ALTER TABLE ONLY platform.audit_logs_default
    ADD CONSTRAINT audit_logs_default_pkey PRIMARY KEY (id, created_at);
CREATE INDEX audit_logs_default_entity_type_entity_id_idx ON platform.audit_logs_default USING btree (entity_type, entity_id);
CREATE INDEX audit_logs_default_tenant_id_created_at_idx ON platform.audit_logs_default USING btree (tenant_id, created_at DESC);
ALTER INDEX platform.audit_logs_pkey ATTACH PARTITION platform.audit_logs_default_pkey;
ALTER INDEX platform.audit_logs_entity ATTACH PARTITION platform.audit_logs_default_entity_type_entity_id_idx;
ALTER INDEX platform.audit_logs_tenant_time ATTACH PARTITION platform.audit_logs_default_tenant_id_created_at_idx;
--rollback DROP INDEX IF EXISTS platform.audit_logs_default_tenant_id_created_at_idx CASCADE;
--rollback DROP INDEX IF EXISTS platform.audit_logs_default_entity_type_entity_id_idx CASCADE;
--rollback ALTER TABLE ONLY platform.audit_logs_default DROP CONSTRAINT IF EXISTS audit_logs_default_pkey CASCADE;

--changeset hyproms:05-idx-platform_invitations
ALTER TABLE ONLY platform.invitations
    ADD CONSTRAINT invitations_pkey PRIMARY KEY (id);
ALTER TABLE ONLY platform.invitations
    ADD CONSTRAINT invitations_token_hash_key UNIQUE (token_hash);
--rollback ALTER TABLE ONLY platform.invitations DROP CONSTRAINT IF EXISTS invitations_token_hash_key CASCADE;
--rollback ALTER TABLE ONLY platform.invitations DROP CONSTRAINT IF EXISTS invitations_pkey CASCADE;

--changeset hyproms:05-idx-platform_member_groups
ALTER TABLE ONLY platform.member_groups
    ADD CONSTRAINT member_groups_pkey PRIMARY KEY (id);
ALTER TABLE ONLY platform.member_groups
    ADD CONSTRAINT member_groups_tenant_id_id_key UNIQUE (tenant_id, id);
CREATE UNIQUE INDEX member_groups_name_uq ON platform.member_groups USING btree (tenant_id, lower(name)) WHERE (deleted_at IS NULL);
--rollback DROP INDEX IF EXISTS platform.member_groups_name_uq CASCADE;
--rollback ALTER TABLE ONLY platform.member_groups DROP CONSTRAINT IF EXISTS member_groups_tenant_id_id_key CASCADE;
--rollback ALTER TABLE ONLY platform.member_groups DROP CONSTRAINT IF EXISTS member_groups_pkey CASCADE;

--changeset hyproms:05-idx-platform_personal_access_tokens
ALTER TABLE ONLY platform.personal_access_tokens
    ADD CONSTRAINT personal_access_tokens_pkey PRIMARY KEY (id);
ALTER TABLE ONLY platform.personal_access_tokens
    ADD CONSTRAINT personal_access_tokens_token_key UNIQUE (token);
CREATE INDEX pat_tokenable ON platform.personal_access_tokens USING btree (tokenable_type, tokenable_id);
--rollback DROP INDEX IF EXISTS platform.pat_tokenable CASCADE;
--rollback ALTER TABLE ONLY platform.personal_access_tokens DROP CONSTRAINT IF EXISTS personal_access_tokens_token_key CASCADE;
--rollback ALTER TABLE ONLY platform.personal_access_tokens DROP CONSTRAINT IF EXISTS personal_access_tokens_pkey CASCADE;

--changeset hyproms:05-idx-platform_plans
ALTER TABLE ONLY platform.plans
    ADD CONSTRAINT plans_pkey PRIMARY KEY (id);
ALTER TABLE ONLY platform.plans
    ADD CONSTRAINT plans_code_key UNIQUE (code);
--rollback ALTER TABLE ONLY platform.plans DROP CONSTRAINT IF EXISTS plans_code_key CASCADE;
--rollback ALTER TABLE ONLY platform.plans DROP CONSTRAINT IF EXISTS plans_pkey CASCADE;

--changeset hyproms:05-idx-platform_subscriptions
ALTER TABLE ONLY platform.subscriptions
    ADD CONSTRAINT subscriptions_pkey PRIMARY KEY (id);
CREATE UNIQUE INDEX subscriptions_one_open ON platform.subscriptions USING btree (tenant_id) WHERE (status <> 'cancelled'::text);
--rollback DROP INDEX IF EXISTS platform.subscriptions_one_open CASCADE;
--rollback ALTER TABLE ONLY platform.subscriptions DROP CONSTRAINT IF EXISTS subscriptions_pkey CASCADE;

--changeset hyproms:05-idx-platform_team_members
ALTER TABLE ONLY platform.team_members
    ADD CONSTRAINT team_members_pkey PRIMARY KEY (team_id, user_id);
--rollback ALTER TABLE ONLY platform.team_members DROP CONSTRAINT IF EXISTS team_members_pkey CASCADE;

--changeset hyproms:05-idx-platform_teams
ALTER TABLE ONLY platform.teams
    ADD CONSTRAINT teams_pkey PRIMARY KEY (id);
ALTER TABLE ONLY platform.teams
    ADD CONSTRAINT teams_tenant_id_id_key UNIQUE (tenant_id, id);
CREATE UNIQUE INDEX teams_name_uq ON platform.teams USING btree (tenant_id, lower(name)) WHERE (deleted_at IS NULL);
--rollback DROP INDEX IF EXISTS platform.teams_name_uq CASCADE;
--rollback ALTER TABLE ONLY platform.teams DROP CONSTRAINT IF EXISTS teams_tenant_id_id_key CASCADE;
--rollback ALTER TABLE ONLY platform.teams DROP CONSTRAINT IF EXISTS teams_pkey CASCADE;

--changeset hyproms:05-idx-platform_tenant_members
ALTER TABLE ONLY platform.tenant_members
    ADD CONSTRAINT tenant_members_pkey PRIMARY KEY (id);
ALTER TABLE ONLY platform.tenant_members
    ADD CONSTRAINT tenant_members_tenant_id_id_key UNIQUE (tenant_id, id);
ALTER TABLE ONLY platform.tenant_members
    ADD CONSTRAINT tenant_members_tenant_id_user_id_key UNIQUE (tenant_id, user_id);
--rollback ALTER TABLE ONLY platform.tenant_members DROP CONSTRAINT IF EXISTS tenant_members_tenant_id_user_id_key CASCADE;
--rollback ALTER TABLE ONLY platform.tenant_members DROP CONSTRAINT IF EXISTS tenant_members_tenant_id_id_key CASCADE;
--rollback ALTER TABLE ONLY platform.tenant_members DROP CONSTRAINT IF EXISTS tenant_members_pkey CASCADE;

--changeset hyproms:05-idx-platform_tenants
ALTER TABLE ONLY platform.tenants
    ADD CONSTRAINT tenants_pkey PRIMARY KEY (id);
ALTER TABLE ONLY platform.tenants
    ADD CONSTRAINT tenants_slug_key UNIQUE (slug);
--rollback ALTER TABLE ONLY platform.tenants DROP CONSTRAINT IF EXISTS tenants_slug_key CASCADE;
--rollback ALTER TABLE ONLY platform.tenants DROP CONSTRAINT IF EXISTS tenants_pkey CASCADE;

--changeset hyproms:05-idx-platform_user_push_subscriptions
ALTER TABLE ONLY platform.user_push_subscriptions
    ADD CONSTRAINT user_push_subscriptions_pkey PRIMARY KEY (id);
ALTER TABLE ONLY platform.user_push_subscriptions
    ADD CONSTRAINT user_push_subscriptions_endpoint_key UNIQUE (endpoint);
CREATE INDEX user_push_subs_user_idx ON platform.user_push_subscriptions USING btree (user_id);
--rollback DROP INDEX IF EXISTS platform.user_push_subs_user_idx CASCADE;
--rollback ALTER TABLE ONLY platform.user_push_subscriptions DROP CONSTRAINT IF EXISTS user_push_subscriptions_endpoint_key CASCADE;
--rollback ALTER TABLE ONLY platform.user_push_subscriptions DROP CONSTRAINT IF EXISTS user_push_subscriptions_pkey CASCADE;

--changeset hyproms:05-idx-platform_users
ALTER TABLE ONLY platform.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);
ALTER TABLE ONLY platform.users
    ADD CONSTRAINT users_email_key UNIQUE (email);
ALTER TABLE ONLY platform.users
    ADD CONSTRAINT users_idp_subject_key UNIQUE (idp_subject);
--rollback ALTER TABLE ONLY platform.users DROP CONSTRAINT IF EXISTS users_idp_subject_key CASCADE;
--rollback ALTER TABLE ONLY platform.users DROP CONSTRAINT IF EXISTS users_email_key CASCADE;
--rollback ALTER TABLE ONLY platform.users DROP CONSTRAINT IF EXISTS users_pkey CASCADE;

--changeset hyproms:05-idx-platform_work_locations
ALTER TABLE ONLY platform.work_locations
    ADD CONSTRAINT work_locations_pkey PRIMARY KEY (id);
ALTER TABLE ONLY platform.work_locations
    ADD CONSTRAINT work_locations_tenant_id_id_key UNIQUE (tenant_id, id);
CREATE UNIQUE INDEX work_locations_name_uq ON platform.work_locations USING btree (tenant_id, lower(name)) WHERE (deleted_at IS NULL);
--rollback DROP INDEX IF EXISTS platform.work_locations_name_uq CASCADE;
--rollback ALTER TABLE ONLY platform.work_locations DROP CONSTRAINT IF EXISTS work_locations_tenant_id_id_key CASCADE;
--rollback ALTER TABLE ONLY platform.work_locations DROP CONSTRAINT IF EXISTS work_locations_pkey CASCADE;

--changeset hyproms:05-idx-proms_attachments
ALTER TABLE ONLY proms.attachments
    ADD CONSTRAINT attachments_pkey PRIMARY KEY (id);
ALTER TABLE ONLY proms.attachments
    ADD CONSTRAINT attachments_storage_key_key UNIQUE (storage_key);
ALTER TABLE ONLY proms.attachments
    ADD CONSTRAINT attachments_tenant_id_id_key UNIQUE (tenant_id, id);
CREATE INDEX attachments_tenant_size ON proms.attachments USING btree (tenant_id) INCLUDE (size_bytes);
--rollback DROP INDEX IF EXISTS proms.attachments_tenant_size CASCADE;
--rollback ALTER TABLE ONLY proms.attachments DROP CONSTRAINT IF EXISTS attachments_tenant_id_id_key CASCADE;
--rollback ALTER TABLE ONLY proms.attachments DROP CONSTRAINT IF EXISTS attachments_storage_key_key CASCADE;
--rollback ALTER TABLE ONLY proms.attachments DROP CONSTRAINT IF EXISTS attachments_pkey CASCADE;

--changeset hyproms:05-idx-proms_board_columns
ALTER TABLE ONLY proms.board_columns
    ADD CONSTRAINT board_columns_pkey PRIMARY KEY (id);
ALTER TABLE ONLY proms.board_columns
    ADD CONSTRAINT board_columns_tenant_id_id_key UNIQUE (tenant_id, id);
CREATE INDEX board_columns_owner ON proms.board_columns USING btree (board_id, workstream_id, "position") WHERE (deleted_at IS NULL);
--rollback DROP INDEX IF EXISTS proms.board_columns_owner CASCADE;
--rollback ALTER TABLE ONLY proms.board_columns DROP CONSTRAINT IF EXISTS board_columns_tenant_id_id_key CASCADE;
--rollback ALTER TABLE ONLY proms.board_columns DROP CONSTRAINT IF EXISTS board_columns_pkey CASCADE;

--changeset hyproms:05-idx-proms_board_contracts
ALTER TABLE ONLY proms.board_contracts
    ADD CONSTRAINT board_contracts_pkey PRIMARY KEY (id);
ALTER TABLE ONLY proms.board_contracts
    ADD CONSTRAINT board_contracts_board_id_key UNIQUE (board_id);
ALTER TABLE ONLY proms.board_contracts
    ADD CONSTRAINT board_contracts_tenant_id_id_key UNIQUE (tenant_id, id);
--rollback ALTER TABLE ONLY proms.board_contracts DROP CONSTRAINT IF EXISTS board_contracts_tenant_id_id_key CASCADE;
--rollback ALTER TABLE ONLY proms.board_contracts DROP CONSTRAINT IF EXISTS board_contracts_board_id_key CASCADE;
--rollback ALTER TABLE ONLY proms.board_contracts DROP CONSTRAINT IF EXISTS board_contracts_pkey CASCADE;

--changeset hyproms:05-idx-proms_board_document_access
ALTER TABLE ONLY proms.board_document_access
    ADD CONSTRAINT board_document_access_pkey PRIMARY KEY (id);
ALTER TABLE ONLY proms.board_document_access
    ADD CONSTRAINT board_document_access_document_id_user_id_key UNIQUE (document_id, user_id);
ALTER TABLE ONLY proms.board_document_access
    ADD CONSTRAINT board_document_access_tenant_id_id_key UNIQUE (tenant_id, id);
--rollback ALTER TABLE ONLY proms.board_document_access DROP CONSTRAINT IF EXISTS board_document_access_tenant_id_id_key CASCADE;
--rollback ALTER TABLE ONLY proms.board_document_access DROP CONSTRAINT IF EXISTS board_document_access_document_id_user_id_key CASCADE;
--rollback ALTER TABLE ONLY proms.board_document_access DROP CONSTRAINT IF EXISTS board_document_access_pkey CASCADE;

--changeset hyproms:05-idx-proms_board_document_versions
ALTER TABLE ONLY proms.board_document_versions
    ADD CONSTRAINT board_document_versions_pkey PRIMARY KEY (id);
ALTER TABLE ONLY proms.board_document_versions
    ADD CONSTRAINT board_document_versions_document_id_version_number_key UNIQUE (document_id, version_number);
ALTER TABLE ONLY proms.board_document_versions
    ADD CONSTRAINT board_document_versions_tenant_id_id_key UNIQUE (tenant_id, id);
--rollback ALTER TABLE ONLY proms.board_document_versions DROP CONSTRAINT IF EXISTS board_document_versions_tenant_id_id_key CASCADE;
--rollback ALTER TABLE ONLY proms.board_document_versions DROP CONSTRAINT IF EXISTS board_document_versions_document_id_version_number_key CASCADE;
--rollback ALTER TABLE ONLY proms.board_document_versions DROP CONSTRAINT IF EXISTS board_document_versions_pkey CASCADE;

--changeset hyproms:05-idx-proms_board_documents
ALTER TABLE ONLY proms.board_documents
    ADD CONSTRAINT board_documents_pkey PRIMARY KEY (id);
ALTER TABLE ONLY proms.board_documents
    ADD CONSTRAINT board_documents_tenant_id_id_key UNIQUE (tenant_id, id);
--rollback ALTER TABLE ONLY proms.board_documents DROP CONSTRAINT IF EXISTS board_documents_tenant_id_id_key CASCADE;
--rollback ALTER TABLE ONLY proms.board_documents DROP CONSTRAINT IF EXISTS board_documents_pkey CASCADE;

--changeset hyproms:05-idx-proms_board_groups
ALTER TABLE ONLY proms.board_groups
    ADD CONSTRAINT board_groups_pkey PRIMARY KEY (id);
ALTER TABLE ONLY proms.board_groups
    ADD CONSTRAINT board_groups_tenant_id_id_key UNIQUE (tenant_id, id);
CREATE UNIQUE INDEX board_groups_name_uq ON proms.board_groups USING btree (tenant_id, lower(name)) WHERE (deleted_at IS NULL);
--rollback DROP INDEX IF EXISTS proms.board_groups_name_uq CASCADE;
--rollback ALTER TABLE ONLY proms.board_groups DROP CONSTRAINT IF EXISTS board_groups_tenant_id_id_key CASCADE;
--rollback ALTER TABLE ONLY proms.board_groups DROP CONSTRAINT IF EXISTS board_groups_pkey CASCADE;

--changeset hyproms:05-idx-proms_board_invitations
ALTER TABLE ONLY proms.board_invitations
    ADD CONSTRAINT board_invitations_pkey PRIMARY KEY (id);
ALTER TABLE ONLY proms.board_invitations
    ADD CONSTRAINT board_invitations_token_hash_key UNIQUE (token_hash);
CREATE UNIQUE INDEX board_invitations_open ON proms.board_invitations USING btree (board_id, email) WHERE ((accepted_at IS NULL) AND (revoked_at IS NULL));
--rollback DROP INDEX IF EXISTS proms.board_invitations_open CASCADE;
--rollback ALTER TABLE ONLY proms.board_invitations DROP CONSTRAINT IF EXISTS board_invitations_token_hash_key CASCADE;
--rollback ALTER TABLE ONLY proms.board_invitations DROP CONSTRAINT IF EXISTS board_invitations_pkey CASCADE;

--changeset hyproms:05-idx-proms_board_member_workstreams
ALTER TABLE ONLY proms.board_member_workstreams
    ADD CONSTRAINT board_member_workstreams_pkey PRIMARY KEY (board_member_id, workstream_id);
--rollback ALTER TABLE ONLY proms.board_member_workstreams DROP CONSTRAINT IF EXISTS board_member_workstreams_pkey CASCADE;

--changeset hyproms:05-idx-proms_board_members
ALTER TABLE ONLY proms.board_members
    ADD CONSTRAINT board_members_pkey PRIMARY KEY (id);
ALTER TABLE ONLY proms.board_members
    ADD CONSTRAINT board_members_board_id_user_id_key UNIQUE (board_id, user_id);
ALTER TABLE ONLY proms.board_members
    ADD CONSTRAINT board_members_tenant_id_id_key UNIQUE (tenant_id, id);
CREATE UNIQUE INDEX board_members_one_owner ON proms.board_members USING btree (board_id) WHERE is_owner;
--rollback DROP INDEX IF EXISTS proms.board_members_one_owner CASCADE;
--rollback ALTER TABLE ONLY proms.board_members DROP CONSTRAINT IF EXISTS board_members_tenant_id_id_key CASCADE;
--rollback ALTER TABLE ONLY proms.board_members DROP CONSTRAINT IF EXISTS board_members_board_id_user_id_key CASCADE;
--rollback ALTER TABLE ONLY proms.board_members DROP CONSTRAINT IF EXISTS board_members_pkey CASCADE;

--changeset hyproms:05-idx-proms_board_pins
ALTER TABLE ONLY proms.board_pins
    ADD CONSTRAINT board_pins_pkey PRIMARY KEY (id);
ALTER TABLE ONLY proms.board_pins
    ADD CONSTRAINT board_pins_board_id_user_id_key UNIQUE (board_id, user_id);
ALTER TABLE ONLY proms.board_pins
    ADD CONSTRAINT board_pins_tenant_id_id_key UNIQUE (tenant_id, id);
--rollback ALTER TABLE ONLY proms.board_pins DROP CONSTRAINT IF EXISTS board_pins_tenant_id_id_key CASCADE;
--rollback ALTER TABLE ONLY proms.board_pins DROP CONSTRAINT IF EXISTS board_pins_board_id_user_id_key CASCADE;
--rollback ALTER TABLE ONLY proms.board_pins DROP CONSTRAINT IF EXISTS board_pins_pkey CASCADE;

--changeset hyproms:05-idx-proms_board_saved_views
ALTER TABLE ONLY proms.board_saved_views
    ADD CONSTRAINT board_saved_views_pkey PRIMARY KEY (id);
ALTER TABLE ONLY proms.board_saved_views
    ADD CONSTRAINT board_saved_views_board_id_user_id_name_key UNIQUE (board_id, user_id, name);
ALTER TABLE ONLY proms.board_saved_views
    ADD CONSTRAINT board_saved_views_tenant_id_id_key UNIQUE (tenant_id, id);
CREATE UNIQUE INDEX board_saved_views_one_default ON proms.board_saved_views USING btree (board_id, user_id) WHERE is_default;
--rollback DROP INDEX IF EXISTS proms.board_saved_views_one_default CASCADE;
--rollback ALTER TABLE ONLY proms.board_saved_views DROP CONSTRAINT IF EXISTS board_saved_views_tenant_id_id_key CASCADE;
--rollback ALTER TABLE ONLY proms.board_saved_views DROP CONSTRAINT IF EXISTS board_saved_views_board_id_user_id_name_key CASCADE;
--rollback ALTER TABLE ONLY proms.board_saved_views DROP CONSTRAINT IF EXISTS board_saved_views_pkey CASCADE;

--changeset hyproms:05-idx-proms_boards
ALTER TABLE ONLY proms.boards
    ADD CONSTRAINT boards_pkey PRIMARY KEY (id);
ALTER TABLE ONLY proms.boards
    ADD CONSTRAINT boards_tenant_id_id_key UNIQUE (tenant_id, id);
CREATE UNIQUE INDEX boards_key_uq ON proms.boards USING btree (tenant_id, key);
--rollback DROP INDEX IF EXISTS proms.boards_key_uq CASCADE;
--rollback ALTER TABLE ONLY proms.boards DROP CONSTRAINT IF EXISTS boards_tenant_id_id_key CASCADE;
--rollback ALTER TABLE ONLY proms.boards DROP CONSTRAINT IF EXISTS boards_pkey CASCADE;

--changeset hyproms:05-idx-proms_budget_categories
ALTER TABLE ONLY proms.budget_categories
    ADD CONSTRAINT budget_categories_pkey PRIMARY KEY (id);
ALTER TABLE ONLY proms.budget_categories
    ADD CONSTRAINT budget_categories_tenant_id_id_key UNIQUE (tenant_id, id);
ALTER TABLE ONLY proms.budget_categories
    ADD CONSTRAINT budget_categories_tenant_id_name_key UNIQUE (tenant_id, name);
--rollback ALTER TABLE ONLY proms.budget_categories DROP CONSTRAINT IF EXISTS budget_categories_tenant_id_name_key CASCADE;
--rollback ALTER TABLE ONLY proms.budget_categories DROP CONSTRAINT IF EXISTS budget_categories_tenant_id_id_key CASCADE;
--rollback ALTER TABLE ONLY proms.budget_categories DROP CONSTRAINT IF EXISTS budget_categories_pkey CASCADE;

--changeset hyproms:05-idx-proms_budget_lines
ALTER TABLE ONLY proms.budget_lines
    ADD CONSTRAINT budget_lines_pkey PRIMARY KEY (id);
ALTER TABLE ONLY proms.budget_lines
    ADD CONSTRAINT budget_lines_board_id_category_id_key UNIQUE (board_id, category_id);
--rollback ALTER TABLE ONLY proms.budget_lines DROP CONSTRAINT IF EXISTS budget_lines_board_id_category_id_key CASCADE;
--rollback ALTER TABLE ONLY proms.budget_lines DROP CONSTRAINT IF EXISTS budget_lines_pkey CASCADE;

--changeset hyproms:05-idx-proms_chat_channel_members
ALTER TABLE ONLY proms.chat_channel_members
    ADD CONSTRAINT chat_channel_members_pkey PRIMARY KEY (id);
ALTER TABLE ONLY proms.chat_channel_members
    ADD CONSTRAINT chat_channel_members_tenant_id_channel_id_user_id_key UNIQUE (tenant_id, channel_id, user_id);
ALTER TABLE ONLY proms.chat_channel_members
    ADD CONSTRAINT chat_channel_members_tenant_id_id_key UNIQUE (tenant_id, id);
CREATE INDEX chat_channel_members_channel ON proms.chat_channel_members USING btree (tenant_id, channel_id);
CREATE INDEX chat_channel_members_user ON proms.chat_channel_members USING btree (tenant_id, user_id);
--rollback DROP INDEX IF EXISTS proms.chat_channel_members_user CASCADE;
--rollback DROP INDEX IF EXISTS proms.chat_channel_members_channel CASCADE;
--rollback ALTER TABLE ONLY proms.chat_channel_members DROP CONSTRAINT IF EXISTS chat_channel_members_tenant_id_id_key CASCADE;
--rollback ALTER TABLE ONLY proms.chat_channel_members DROP CONSTRAINT IF EXISTS chat_channel_members_tenant_id_channel_id_user_id_key CASCADE;
--rollback ALTER TABLE ONLY proms.chat_channel_members DROP CONSTRAINT IF EXISTS chat_channel_members_pkey CASCADE;

--changeset hyproms:05-idx-proms_chat_channels
ALTER TABLE ONLY proms.chat_channels
    ADD CONSTRAINT chat_channels_pkey PRIMARY KEY (id);
ALTER TABLE ONLY proms.chat_channels
    ADD CONSTRAINT chat_channels_tenant_id_id_key UNIQUE (tenant_id, id);
CREATE INDEX chat_channels_board ON proms.chat_channels USING btree (board_id);
CREATE INDEX chat_channels_tenant_scope_type ON proms.chat_channels USING btree (tenant_id, scope, type);
CREATE UNIQUE INDEX chat_channels_tenant_slug_unique ON proms.chat_channels USING btree (tenant_id, slug) WHERE ((slug IS NOT NULL) AND (deleted_at IS NULL));
--rollback DROP INDEX IF EXISTS proms.chat_channels_tenant_slug_unique CASCADE;
--rollback DROP INDEX IF EXISTS proms.chat_channels_tenant_scope_type CASCADE;
--rollback DROP INDEX IF EXISTS proms.chat_channels_board CASCADE;
--rollback ALTER TABLE ONLY proms.chat_channels DROP CONSTRAINT IF EXISTS chat_channels_tenant_id_id_key CASCADE;
--rollback ALTER TABLE ONLY proms.chat_channels DROP CONSTRAINT IF EXISTS chat_channels_pkey CASCADE;

--changeset hyproms:05-idx-proms_chat_messages
ALTER TABLE ONLY proms.chat_messages
    ADD CONSTRAINT chat_messages_pkey PRIMARY KEY (id);
ALTER TABLE ONLY proms.chat_messages
    ADD CONSTRAINT chat_messages_tenant_id_id_key UNIQUE (tenant_id, id);
CREATE INDEX chat_messages_channel ON proms.chat_messages USING btree (channel_id, created_at);
CREATE INDEX chat_messages_parent ON proms.chat_messages USING btree (parent_id) WHERE (parent_id IS NOT NULL);
CREATE INDEX chat_messages_pinned ON proms.chat_messages USING btree (channel_id) WHERE (is_pinned = true);
--rollback DROP INDEX IF EXISTS proms.chat_messages_pinned CASCADE;
--rollback DROP INDEX IF EXISTS proms.chat_messages_parent CASCADE;
--rollback DROP INDEX IF EXISTS proms.chat_messages_channel CASCADE;
--rollback ALTER TABLE ONLY proms.chat_messages DROP CONSTRAINT IF EXISTS chat_messages_tenant_id_id_key CASCADE;
--rollback ALTER TABLE ONLY proms.chat_messages DROP CONSTRAINT IF EXISTS chat_messages_pkey CASCADE;

--changeset hyproms:05-idx-proms_chat_reactions
ALTER TABLE ONLY proms.chat_reactions
    ADD CONSTRAINT chat_reactions_pkey PRIMARY KEY (id);
ALTER TABLE ONLY proms.chat_reactions
    ADD CONSTRAINT chat_reactions_tenant_id_message_id_user_id_emoji_key UNIQUE (tenant_id, message_id, user_id, emoji);
CREATE INDEX chat_reactions_message ON proms.chat_reactions USING btree (message_id);
--rollback DROP INDEX IF EXISTS proms.chat_reactions_message CASCADE;
--rollback ALTER TABLE ONLY proms.chat_reactions DROP CONSTRAINT IF EXISTS chat_reactions_tenant_id_message_id_user_id_emoji_key CASCADE;
--rollback ALTER TABLE ONLY proms.chat_reactions DROP CONSTRAINT IF EXISTS chat_reactions_pkey CASCADE;

--changeset hyproms:05-idx-proms_chat_read_markers
ALTER TABLE ONLY proms.chat_read_markers
    ADD CONSTRAINT chat_read_markers_pkey PRIMARY KEY (tenant_id, user_id, channel_id);
--rollback ALTER TABLE ONLY proms.chat_read_markers DROP CONSTRAINT IF EXISTS chat_read_markers_pkey CASCADE;

--changeset hyproms:05-idx-proms_checklist_items
ALTER TABLE ONLY proms.checklist_items
    ADD CONSTRAINT checklist_items_pkey PRIMARY KEY (id);
--rollback ALTER TABLE ONLY proms.checklist_items DROP CONSTRAINT IF EXISTS checklist_items_pkey CASCADE;

--changeset hyproms:05-idx-proms_comment_mentions
ALTER TABLE ONLY proms.comment_mentions
    ADD CONSTRAINT comment_mentions_pkey PRIMARY KEY (id);
CREATE INDEX comment_mentions_task ON proms.comment_mentions USING btree (referenced_task);
CREATE INDEX comment_mentions_user ON proms.comment_mentions USING btree (user_id);
--rollback DROP INDEX IF EXISTS proms.comment_mentions_user CASCADE;
--rollback DROP INDEX IF EXISTS proms.comment_mentions_task CASCADE;
--rollback ALTER TABLE ONLY proms.comment_mentions DROP CONSTRAINT IF EXISTS comment_mentions_pkey CASCADE;

--changeset hyproms:05-idx-proms_comments
ALTER TABLE ONLY proms.comments
    ADD CONSTRAINT comments_pkey PRIMARY KEY (id);
ALTER TABLE ONLY proms.comments
    ADD CONSTRAINT comments_tenant_id_id_key UNIQUE (tenant_id, id);
CREATE INDEX comments_task ON proms.comments USING btree (task_id, created_at);
--rollback DROP INDEX IF EXISTS proms.comments_task CASCADE;
--rollback ALTER TABLE ONLY proms.comments DROP CONSTRAINT IF EXISTS comments_tenant_id_id_key CASCADE;
--rollback ALTER TABLE ONLY proms.comments DROP CONSTRAINT IF EXISTS comments_pkey CASCADE;

--changeset hyproms:05-idx-proms_dashboard_reminders
ALTER TABLE ONLY proms.dashboard_reminders
    ADD CONSTRAINT dashboard_reminders_pkey PRIMARY KEY (id);
CREATE INDEX dashboard_reminders_tenant_user_created_idx ON proms.dashboard_reminders USING btree (tenant_id, user_id, created_at);
--rollback DROP INDEX IF EXISTS proms.dashboard_reminders_tenant_user_created_idx CASCADE;
--rollback ALTER TABLE ONLY proms.dashboard_reminders DROP CONSTRAINT IF EXISTS dashboard_reminders_pkey CASCADE;

--changeset hyproms:05-idx-proms_expenses
ALTER TABLE ONLY proms.expenses
    ADD CONSTRAINT expenses_pkey PRIMARY KEY (id);
CREATE INDEX expenses_board_cat ON proms.expenses USING btree (board_id, category_id) WHERE (status = 'approved'::text);
--rollback DROP INDEX IF EXISTS proms.expenses_board_cat CASCADE;
--rollback ALTER TABLE ONLY proms.expenses DROP CONSTRAINT IF EXISTS expenses_pkey CASCADE;

--changeset hyproms:05-idx-proms_focus_sessions
ALTER TABLE ONLY proms.focus_sessions
    ADD CONSTRAINT focus_sessions_pkey PRIMARY KEY (id);
ALTER TABLE ONLY proms.focus_sessions
    ADD CONSTRAINT focus_sessions_tenant_id_id_key UNIQUE (tenant_id, id);
CREATE INDEX focus_sessions_tenant_user_started_idx ON proms.focus_sessions USING btree (tenant_id, user_id, started_at);
--rollback DROP INDEX IF EXISTS proms.focus_sessions_tenant_user_started_idx CASCADE;
--rollback ALTER TABLE ONLY proms.focus_sessions DROP CONSTRAINT IF EXISTS focus_sessions_tenant_id_id_key CASCADE;
--rollback ALTER TABLE ONLY proms.focus_sessions DROP CONSTRAINT IF EXISTS focus_sessions_pkey CASCADE;

--changeset hyproms:05-idx-proms_form_fields
ALTER TABLE ONLY proms.form_fields
    ADD CONSTRAINT form_fields_pkey PRIMARY KEY (id);
CREATE UNIQUE INDEX form_fields_map_uq ON proms.form_fields USING btree (form_id, maps_to) WHERE (maps_to <> ALL (ARRAY['tag'::text, 'none'::text]));
--rollback DROP INDEX IF EXISTS proms.form_fields_map_uq CASCADE;
--rollback ALTER TABLE ONLY proms.form_fields DROP CONSTRAINT IF EXISTS form_fields_pkey CASCADE;

--changeset hyproms:05-idx-proms_form_submissions
ALTER TABLE ONLY proms.form_submissions
    ADD CONSTRAINT form_submissions_pkey PRIMARY KEY (id);
CREATE INDEX form_submissions_ratelimit ON proms.form_submissions USING btree (form_id, ip, created_at);
--rollback DROP INDEX IF EXISTS proms.form_submissions_ratelimit CASCADE;
--rollback ALTER TABLE ONLY proms.form_submissions DROP CONSTRAINT IF EXISTS form_submissions_pkey CASCADE;

--changeset hyproms:05-idx-proms_forms
ALTER TABLE ONLY proms.forms
    ADD CONSTRAINT forms_pkey PRIMARY KEY (id);
ALTER TABLE ONLY proms.forms
    ADD CONSTRAINT forms_public_token_key UNIQUE (public_token);
ALTER TABLE ONLY proms.forms
    ADD CONSTRAINT forms_tenant_id_id_key UNIQUE (tenant_id, id);
--rollback ALTER TABLE ONLY proms.forms DROP CONSTRAINT IF EXISTS forms_tenant_id_id_key CASCADE;
--rollback ALTER TABLE ONLY proms.forms DROP CONSTRAINT IF EXISTS forms_public_token_key CASCADE;
--rollback ALTER TABLE ONLY proms.forms DROP CONSTRAINT IF EXISTS forms_pkey CASCADE;

--changeset hyproms:05-idx-proms_holidays
ALTER TABLE ONLY proms.holidays
    ADD CONSTRAINT holidays_pkey PRIMARY KEY (id);
ALTER TABLE ONLY proms.holidays
    ADD CONSTRAINT holidays_tenant_id_date_key UNIQUE (tenant_id, date);
--rollback ALTER TABLE ONLY proms.holidays DROP CONSTRAINT IF EXISTS holidays_tenant_id_date_key CASCADE;
--rollback ALTER TABLE ONLY proms.holidays DROP CONSTRAINT IF EXISTS holidays_pkey CASCADE;

--changeset hyproms:05-idx-proms_idempotency_keys
ALTER TABLE ONLY proms.idempotency_keys
    ADD CONSTRAINT idempotency_keys_pkey PRIMARY KEY (tenant_id, user_id, key);
--rollback ALTER TABLE ONLY proms.idempotency_keys DROP CONSTRAINT IF EXISTS idempotency_keys_pkey CASCADE;

--changeset hyproms:05-idx-proms_invoices
ALTER TABLE ONLY proms.invoices
    ADD CONSTRAINT invoices_pkey PRIMARY KEY (id);
CREATE UNIQUE INDEX invoices_auto_once ON proms.invoices USING btree (payment_term_id) WHERE auto_created;
CREATE UNIQUE INDEX invoices_no_uq ON proms.invoices USING btree (tenant_id, invoice_no) WHERE (invoice_no IS NOT NULL);
--rollback DROP INDEX IF EXISTS proms.invoices_no_uq CASCADE;
--rollback DROP INDEX IF EXISTS proms.invoices_auto_once CASCADE;
--rollback ALTER TABLE ONLY proms.invoices DROP CONSTRAINT IF EXISTS invoices_pkey CASCADE;

--changeset hyproms:05-idx-proms_meeting_action_items
ALTER TABLE ONLY proms.meeting_action_items
    ADD CONSTRAINT meeting_action_items_pkey PRIMARY KEY (id);
CREATE INDEX meeting_action_items_tenant_meeting_idx ON proms.meeting_action_items USING btree (tenant_id, meeting_id);
--rollback DROP INDEX IF EXISTS proms.meeting_action_items_tenant_meeting_idx CASCADE;
--rollback ALTER TABLE ONLY proms.meeting_action_items DROP CONSTRAINT IF EXISTS meeting_action_items_pkey CASCADE;

--changeset hyproms:05-idx-proms_meeting_attendees
ALTER TABLE ONLY proms.meeting_attendees
    ADD CONSTRAINT meeting_attendees_pkey PRIMARY KEY (id);
CREATE INDEX meeting_attendees_tenant_meeting_idx ON proms.meeting_attendees USING btree (tenant_id, meeting_id);
--rollback DROP INDEX IF EXISTS proms.meeting_attendees_tenant_meeting_idx CASCADE;
--rollback ALTER TABLE ONLY proms.meeting_attendees DROP CONSTRAINT IF EXISTS meeting_attendees_pkey CASCADE;

--changeset hyproms:05-idx-proms_meeting_minutes
ALTER TABLE ONLY proms.meeting_minutes
    ADD CONSTRAINT meeting_minutes_pkey PRIMARY KEY (id);
CREATE INDEX meeting_minutes_tenant_board_idx ON proms.meeting_minutes USING btree (tenant_id, board_id);
--rollback DROP INDEX IF EXISTS proms.meeting_minutes_tenant_board_idx CASCADE;
--rollback ALTER TABLE ONLY proms.meeting_minutes DROP CONSTRAINT IF EXISTS meeting_minutes_pkey CASCADE;

--changeset hyproms:05-idx-proms_milestone_workstreams
ALTER TABLE ONLY proms.milestone_workstreams
    ADD CONSTRAINT milestone_workstreams_pkey PRIMARY KEY (milestone_id, workstream_id);
--rollback ALTER TABLE ONLY proms.milestone_workstreams DROP CONSTRAINT IF EXISTS milestone_workstreams_pkey CASCADE;

--changeset hyproms:05-idx-proms_milestones
ALTER TABLE ONLY proms.milestones
    ADD CONSTRAINT milestones_pkey PRIMARY KEY (id);
ALTER TABLE ONLY proms.milestones
    ADD CONSTRAINT milestones_tenant_id_id_key UNIQUE (tenant_id, id);
--rollback ALTER TABLE ONLY proms.milestones DROP CONSTRAINT IF EXISTS milestones_tenant_id_id_key CASCADE;
--rollback ALTER TABLE ONLY proms.milestones DROP CONSTRAINT IF EXISTS milestones_pkey CASCADE;

--changeset hyproms:05-idx-proms_notifications
ALTER TABLE ONLY proms.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);
CREATE UNIQUE INDEX notifications_group_uniq ON proms.notifications USING btree (tenant_id, recipient_id, group_key) WHERE (group_key IS NOT NULL);
CREATE INDEX notifications_inbox ON proms.notifications USING btree (recipient_id, tenant_id, created_at DESC) WHERE (archived_at IS NULL);
CREATE INDEX notifications_unread ON proms.notifications USING btree (recipient_id, tenant_id) WHERE ((read_at IS NULL) AND (archived_at IS NULL));
--rollback DROP INDEX IF EXISTS proms.notifications_unread CASCADE;
--rollback DROP INDEX IF EXISTS proms.notifications_inbox CASCADE;
--rollback DROP INDEX IF EXISTS proms.notifications_group_uniq CASCADE;
--rollback ALTER TABLE ONLY proms.notifications DROP CONSTRAINT IF EXISTS notifications_pkey CASCADE;

--changeset hyproms:05-idx-proms_outbox_events
ALTER TABLE ONLY proms.outbox_events
    ADD CONSTRAINT outbox_events_pkey PRIMARY KEY (id);
CREATE INDEX outbox_pending ON proms.outbox_events USING btree (id) WHERE (dispatched_at IS NULL);
--rollback DROP INDEX IF EXISTS proms.outbox_pending CASCADE;
--rollback ALTER TABLE ONLY proms.outbox_events DROP CONSTRAINT IF EXISTS outbox_events_pkey CASCADE;

--changeset hyproms:05-idx-proms_payment_terms
ALTER TABLE ONLY proms.payment_terms
    ADD CONSTRAINT payment_terms_pkey PRIMARY KEY (id);
ALTER TABLE ONLY proms.payment_terms
    ADD CONSTRAINT payment_terms_milestone_id_key UNIQUE (milestone_id);
ALTER TABLE ONLY proms.payment_terms
    ADD CONSTRAINT payment_terms_tenant_id_id_key UNIQUE (tenant_id, id);
--rollback ALTER TABLE ONLY proms.payment_terms DROP CONSTRAINT IF EXISTS payment_terms_tenant_id_id_key CASCADE;
--rollback ALTER TABLE ONLY proms.payment_terms DROP CONSTRAINT IF EXISTS payment_terms_milestone_id_key CASCADE;
--rollback ALTER TABLE ONLY proms.payment_terms DROP CONSTRAINT IF EXISTS payment_terms_pkey CASCADE;

--changeset hyproms:05-idx-proms_point_entries
ALTER TABLE ONLY proms.point_entries
    ADD CONSTRAINT point_entries_pkey PRIMARY KEY (id);
ALTER TABLE ONLY proms.point_entries
    ADD CONSTRAINT point_entries_tenant_id_id_key UNIQUE (tenant_id, id);
ALTER TABLE ONLY proms.point_entries
    ADD CONSTRAINT point_entries_tenant_id_source_event_id_user_id_rule_code_key UNIQUE (tenant_id, source_event_id, user_id, rule_code);
CREATE INDEX point_entries_leaderboard ON proms.point_entries USING btree (tenant_id, occurred_at, user_id);
CREATE UNIQUE INDEX point_entries_reverses_uq ON proms.point_entries USING btree (reverses_id) WHERE (reverses_id IS NOT NULL);
CREATE INDEX point_entries_task ON proms.point_entries USING btree (task_id) WHERE (task_id IS NOT NULL);
--rollback DROP INDEX IF EXISTS proms.point_entries_task CASCADE;
--rollback DROP INDEX IF EXISTS proms.point_entries_reverses_uq CASCADE;
--rollback DROP INDEX IF EXISTS proms.point_entries_leaderboard CASCADE;
--rollback ALTER TABLE ONLY proms.point_entries DROP CONSTRAINT IF EXISTS point_entries_tenant_id_source_event_id_user_id_rule_code_key CASCADE;
--rollback ALTER TABLE ONLY proms.point_entries DROP CONSTRAINT IF EXISTS point_entries_tenant_id_id_key CASCADE;
--rollback ALTER TABLE ONLY proms.point_entries DROP CONSTRAINT IF EXISTS point_entries_pkey CASCADE;

--changeset hyproms:05-idx-proms_point_policies
ALTER TABLE ONLY proms.point_policies
    ADD CONSTRAINT point_policies_pkey PRIMARY KEY (id);
ALTER TABLE ONLY proms.point_policies
    ADD CONSTRAINT point_policies_tenant_id_version_key UNIQUE (tenant_id, version);
--rollback ALTER TABLE ONLY proms.point_policies DROP CONSTRAINT IF EXISTS point_policies_tenant_id_version_key CASCADE;
--rollback ALTER TABLE ONLY proms.point_policies DROP CONSTRAINT IF EXISTS point_policies_pkey CASCADE;

--changeset hyproms:05-idx-proms_recurrence_rules
ALTER TABLE ONLY proms.recurrence_rules
    ADD CONSTRAINT recurrence_rules_pkey PRIMARY KEY (id);
ALTER TABLE ONLY proms.recurrence_rules
    ADD CONSTRAINT recurrence_rules_tenant_id_id_key UNIQUE (tenant_id, id);
--rollback ALTER TABLE ONLY proms.recurrence_rules DROP CONSTRAINT IF EXISTS recurrence_rules_tenant_id_id_key CASCADE;
--rollback ALTER TABLE ONLY proms.recurrence_rules DROP CONSTRAINT IF EXISTS recurrence_rules_pkey CASCADE;

--changeset hyproms:05-idx-proms_share_links
ALTER TABLE ONLY proms.share_links
    ADD CONSTRAINT share_links_pkey PRIMARY KEY (id);
ALTER TABLE ONLY proms.share_links
    ADD CONSTRAINT share_links_token_hash_key UNIQUE (token_hash);
--rollback ALTER TABLE ONLY proms.share_links DROP CONSTRAINT IF EXISTS share_links_token_hash_key CASCADE;
--rollback ALTER TABLE ONLY proms.share_links DROP CONSTRAINT IF EXISTS share_links_pkey CASCADE;

--changeset hyproms:05-idx-proms_standup_policies
ALTER TABLE ONLY proms.standup_policies
    ADD CONSTRAINT standup_policies_pkey PRIMARY KEY (tenant_id);
--rollback ALTER TABLE ONLY proms.standup_policies DROP CONSTRAINT IF EXISTS standup_policies_pkey CASCADE;

--changeset hyproms:05-idx-proms_standup_reminder_runs
ALTER TABLE ONLY proms.standup_reminder_runs
    ADD CONSTRAINT standup_reminder_runs_pkey PRIMARY KEY (tenant_id, run_date);
--rollback ALTER TABLE ONLY proms.standup_reminder_runs DROP CONSTRAINT IF EXISTS standup_reminder_runs_pkey CASCADE;

--changeset hyproms:05-idx-proms_standup_reports
ALTER TABLE ONLY proms.standup_reports
    ADD CONSTRAINT standup_reports_pkey PRIMARY KEY (id);
ALTER TABLE ONLY proms.standup_reports
    ADD CONSTRAINT standup_reports_tenant_id_user_id_report_date_key UNIQUE (tenant_id, user_id, report_date);
CREATE INDEX standup_reports_tenant_id_report_date_idx ON proms.standup_reports USING btree (tenant_id, report_date);
--rollback DROP INDEX IF EXISTS proms.standup_reports_tenant_id_report_date_idx CASCADE;
--rollback ALTER TABLE ONLY proms.standup_reports DROP CONSTRAINT IF EXISTS standup_reports_tenant_id_user_id_report_date_key CASCADE;
--rollback ALTER TABLE ONLY proms.standup_reports DROP CONSTRAINT IF EXISTS standup_reports_pkey CASCADE;

--changeset hyproms:05-idx-proms_tags
ALTER TABLE ONLY proms.tags
    ADD CONSTRAINT tags_pkey PRIMARY KEY (id);
ALTER TABLE ONLY proms.tags
    ADD CONSTRAINT tags_tenant_id_id_key UNIQUE (tenant_id, id);
CREATE UNIQUE INDEX tags_name_uq ON proms.tags USING btree (tenant_id, COALESCE(board_id, '00000000-0000-0000-0000-000000000000'::uuid), name);
--rollback DROP INDEX IF EXISTS proms.tags_name_uq CASCADE;
--rollback ALTER TABLE ONLY proms.tags DROP CONSTRAINT IF EXISTS tags_tenant_id_id_key CASCADE;
--rollback ALTER TABLE ONLY proms.tags DROP CONSTRAINT IF EXISTS tags_pkey CASCADE;

--changeset hyproms:05-idx-proms_task_activities
ALTER TABLE ONLY proms.task_activities
    ADD CONSTRAINT task_activities_pkey PRIMARY KEY (id);
CREATE INDEX task_activities_task ON proms.task_activities USING btree (task_id, created_at DESC);
--rollback DROP INDEX IF EXISTS proms.task_activities_task CASCADE;
--rollback ALTER TABLE ONLY proms.task_activities DROP CONSTRAINT IF EXISTS task_activities_pkey CASCADE;

--changeset hyproms:05-idx-proms_task_assignees
ALTER TABLE ONLY proms.task_assignees
    ADD CONSTRAINT task_assignees_pkey PRIMARY KEY (task_id, user_id);
CREATE INDEX task_assignees_user ON proms.task_assignees USING btree (user_id, tenant_id);
--rollback DROP INDEX IF EXISTS proms.task_assignees_user CASCADE;
--rollback ALTER TABLE ONLY proms.task_assignees DROP CONSTRAINT IF EXISTS task_assignees_pkey CASCADE;

--changeset hyproms:05-idx-proms_task_dependencies
ALTER TABLE ONLY proms.task_dependencies
    ADD CONSTRAINT task_dependencies_pkey PRIMARY KEY (blocking_task_id, blocked_task_id);
--rollback ALTER TABLE ONLY proms.task_dependencies DROP CONSTRAINT IF EXISTS task_dependencies_pkey CASCADE;

--changeset hyproms:05-idx-proms_task_feedback
ALTER TABLE ONLY proms.task_feedback
    ADD CONSTRAINT task_feedback_pkey PRIMARY KEY (id);
ALTER TABLE ONLY proms.task_feedback
    ADD CONSTRAINT task_feedback_tenant_id_id_key UNIQUE (tenant_id, id);
CREATE UNIQUE INDEX task_feedback_active_uq ON proms.task_feedback USING btree (task_id) WHERE (superseded_at IS NULL);
--rollback DROP INDEX IF EXISTS proms.task_feedback_active_uq CASCADE;
--rollback ALTER TABLE ONLY proms.task_feedback DROP CONSTRAINT IF EXISTS task_feedback_tenant_id_id_key CASCADE;
--rollback ALTER TABLE ONLY proms.task_feedback DROP CONSTRAINT IF EXISTS task_feedback_pkey CASCADE;

--changeset hyproms:05-idx-proms_task_tags
ALTER TABLE ONLY proms.task_tags
    ADD CONSTRAINT task_tags_pkey PRIMARY KEY (task_id, tag_id);
--rollback ALTER TABLE ONLY proms.task_tags DROP CONSTRAINT IF EXISTS task_tags_pkey CASCADE;

--changeset hyproms:05-idx-proms_task_watchers
ALTER TABLE ONLY proms.task_watchers
    ADD CONSTRAINT task_watchers_pkey PRIMARY KEY (task_id, user_id);
--rollback ALTER TABLE ONLY proms.task_watchers DROP CONSTRAINT IF EXISTS task_watchers_pkey CASCADE;

--changeset hyproms:05-idx-proms_tasks
ALTER TABLE ONLY proms.tasks
    ADD CONSTRAINT tasks_pkey PRIMARY KEY (id);
ALTER TABLE ONLY proms.tasks
    ADD CONSTRAINT tasks_board_id_number_key UNIQUE (board_id, number);
ALTER TABLE ONLY proms.tasks
    ADD CONSTRAINT tasks_tenant_id_id_key UNIQUE (tenant_id, id);
CREATE INDEX tasks_board_column ON proms.tasks USING btree (board_id, column_id, "position") WHERE (deleted_at IS NULL);
CREATE INDEX tasks_due ON proms.tasks USING btree (tenant_id, due_date) WHERE ((deleted_at IS NULL) AND (completed_at IS NULL));
CREATE INDEX tasks_milestone ON proms.tasks USING btree (milestone_id) WHERE (deleted_at IS NULL);
CREATE INDEX tasks_parent ON proms.tasks USING btree (parent_id) WHERE (parent_id IS NOT NULL);
CREATE INDEX tasks_search ON proms.tasks USING gin (search_vector);
--rollback DROP INDEX IF EXISTS proms.tasks_search CASCADE;
--rollback DROP INDEX IF EXISTS proms.tasks_parent CASCADE;
--rollback DROP INDEX IF EXISTS proms.tasks_milestone CASCADE;
--rollback DROP INDEX IF EXISTS proms.tasks_due CASCADE;
--rollback DROP INDEX IF EXISTS proms.tasks_board_column CASCADE;
--rollback ALTER TABLE ONLY proms.tasks DROP CONSTRAINT IF EXISTS tasks_tenant_id_id_key CASCADE;
--rollback ALTER TABLE ONLY proms.tasks DROP CONSTRAINT IF EXISTS tasks_board_id_number_key CASCADE;
--rollback ALTER TABLE ONLY proms.tasks DROP CONSTRAINT IF EXISTS tasks_pkey CASCADE;

--changeset hyproms:05-idx-proms_template_columns
ALTER TABLE ONLY proms.template_columns
    ADD CONSTRAINT template_columns_pkey PRIMARY KEY (id);
--rollback ALTER TABLE ONLY proms.template_columns DROP CONSTRAINT IF EXISTS template_columns_pkey CASCADE;

--changeset hyproms:05-idx-proms_time_entries
ALTER TABLE ONLY proms.time_entries
    ADD CONSTRAINT time_entries_pkey PRIMARY KEY (id);
CREATE INDEX time_entries_board ON proms.time_entries USING btree (board_id, work_date) WHERE (deleted_at IS NULL);
CREATE INDEX time_entries_user_week ON proms.time_entries USING btree (user_id, work_date) WHERE (deleted_at IS NULL);
--rollback DROP INDEX IF EXISTS proms.time_entries_user_week CASCADE;
--rollback DROP INDEX IF EXISTS proms.time_entries_board CASCADE;
--rollback ALTER TABLE ONLY proms.time_entries DROP CONSTRAINT IF EXISTS time_entries_pkey CASCADE;

--changeset hyproms:05-idx-proms_time_off
ALTER TABLE ONLY proms.time_off
    ADD CONSTRAINT time_off_pkey PRIMARY KEY (id);
--rollback ALTER TABLE ONLY proms.time_off DROP CONSTRAINT IF EXISTS time_off_pkey CASCADE;

--changeset hyproms:05-idx-proms_timesheets
ALTER TABLE ONLY proms.timesheets
    ADD CONSTRAINT timesheets_pkey PRIMARY KEY (id);
ALTER TABLE ONLY proms.timesheets
    ADD CONSTRAINT timesheets_tenant_id_id_key UNIQUE (tenant_id, id);
ALTER TABLE ONLY proms.timesheets
    ADD CONSTRAINT timesheets_user_id_board_id_week_start_key UNIQUE (user_id, board_id, week_start);
--rollback ALTER TABLE ONLY proms.timesheets DROP CONSTRAINT IF EXISTS timesheets_user_id_board_id_week_start_key CASCADE;
--rollback ALTER TABLE ONLY proms.timesheets DROP CONSTRAINT IF EXISTS timesheets_tenant_id_id_key CASCADE;
--rollback ALTER TABLE ONLY proms.timesheets DROP CONSTRAINT IF EXISTS timesheets_pkey CASCADE;

--changeset hyproms:05-idx-proms_user_preferences
ALTER TABLE ONLY proms.user_preferences
    ADD CONSTRAINT user_preferences_pkey PRIMARY KEY (tenant_id, user_id);
--rollback ALTER TABLE ONLY proms.user_preferences DROP CONSTRAINT IF EXISTS user_preferences_pkey CASCADE;

--changeset hyproms:05-idx-proms_workflow_templates
ALTER TABLE ONLY proms.workflow_templates
    ADD CONSTRAINT workflow_templates_pkey PRIMARY KEY (id);
--rollback ALTER TABLE ONLY proms.workflow_templates DROP CONSTRAINT IF EXISTS workflow_templates_pkey CASCADE;

--changeset hyproms:05-idx-proms_workspace_calendars
ALTER TABLE ONLY proms.workspace_calendars
    ADD CONSTRAINT workspace_calendars_pkey PRIMARY KEY (tenant_id);
--rollback ALTER TABLE ONLY proms.workspace_calendars DROP CONSTRAINT IF EXISTS workspace_calendars_pkey CASCADE;

--changeset hyproms:05-idx-proms_workstreams
ALTER TABLE ONLY proms.workstreams
    ADD CONSTRAINT workstreams_pkey PRIMARY KEY (id);
ALTER TABLE ONLY proms.workstreams
    ADD CONSTRAINT workstreams_tenant_id_id_key UNIQUE (tenant_id, id);
CREATE UNIQUE INDEX workstreams_name_uq ON proms.workstreams USING btree (board_id, lower(name)) WHERE (deleted_at IS NULL);
--rollback DROP INDEX IF EXISTS proms.workstreams_name_uq CASCADE;
--rollback ALTER TABLE ONLY proms.workstreams DROP CONSTRAINT IF EXISTS workstreams_tenant_id_id_key CASCADE;
--rollback ALTER TABLE ONLY proms.workstreams DROP CONSTRAINT IF EXISTS workstreams_pkey CASCADE;
