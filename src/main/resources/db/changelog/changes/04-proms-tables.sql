--liquibase formatted sql

--changeset hyproms:04-tbl-attachments
CREATE TABLE proms.attachments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    task_id uuid,
    comment_id uuid,
    file_name text NOT NULL,
    mime_type text NOT NULL,
    size_bytes bigint NOT NULL,
    storage_key text NOT NULL,
    uploaded_by uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by uuid,
    CONSTRAINT attachments_size_bytes_check CHECK (((size_bytes > 0) AND (size_bytes <= 26214400)))
);

ALTER TABLE ONLY proms.attachments FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.attachments CASCADE;

--changeset hyproms:04-tbl-board_columns
CREATE TABLE proms.board_columns (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    board_id uuid NOT NULL,
    workstream_id uuid,
    name text NOT NULL,
    category text NOT NULL,
    color text,
    wip_limit integer,
    "position" text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    CONSTRAINT board_columns_category_check CHECK ((category = ANY (ARRAY['not_started'::text, 'in_progress'::text, 'blocked'::text, 'done'::text]))),
    CONSTRAINT board_columns_wip_limit_check CHECK ((wip_limit > 0))
);

ALTER TABLE ONLY proms.board_columns FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.board_columns CASCADE;

--changeset hyproms:04-tbl-board_contracts
CREATE TABLE proms.board_contracts (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    board_id uuid NOT NULL,
    contract_value bigint NOT NULL,
    currency character(3) DEFAULT 'IDR'::bpchar NOT NULL,
    contract_no text,
    signed_on date,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT board_contracts_contract_value_check CHECK ((contract_value >= 0))
);

ALTER TABLE ONLY proms.board_contracts FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.board_contracts CASCADE;

--changeset hyproms:04-tbl-board_document_access
CREATE TABLE proms.board_document_access (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    document_id uuid NOT NULL,
    user_id uuid NOT NULL,
    role character varying(10) DEFAULT 'editor'::character varying NOT NULL,
    granted_by uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT board_document_access_role_check CHECK (((role)::text = ANY ((ARRAY['editor'::character varying, 'viewer'::character varying])::text[])))
);

ALTER TABLE ONLY proms.board_document_access FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.board_document_access CASCADE;

--changeset hyproms:04-tbl-board_document_versions
CREATE TABLE proms.board_document_versions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    document_id uuid NOT NULL,
    version_number integer NOT NULL,
    title character varying(255) NOT NULL,
    content_json jsonb,
    content_html text,
    created_by uuid,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE ONLY proms.board_document_versions FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.board_document_versions CASCADE;

--changeset hyproms:04-tbl-board_documents
CREATE TABLE proms.board_documents (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    board_id uuid NOT NULL,
    title character varying(255) DEFAULT 'Dokumen Tanpa Judul'::character varying NOT NULL,
    icon character varying(64) DEFAULT 'file-text'::character varying NOT NULL,
    visibility_type character varying(20) DEFAULT 'all_members'::character varying NOT NULL,
    default_member_permission character varying(10) DEFAULT 'edit'::character varying NOT NULL,
    content_json jsonb,
    content_html text,
    yjs_state text,
    version_number integer DEFAULT 1 NOT NULL,
    is_pinned boolean DEFAULT false NOT NULL,
    created_by uuid,
    updated_by uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    CONSTRAINT board_documents_permission_check CHECK (((default_member_permission)::text = ANY ((ARRAY['edit'::character varying, 'view'::character varying])::text[]))),
    CONSTRAINT board_documents_visibility_check CHECK (((visibility_type)::text = ANY ((ARRAY['all_members'::character varying, 'restricted'::character varying])::text[])))
);

ALTER TABLE ONLY proms.board_documents FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.board_documents CASCADE;

--changeset hyproms:04-tbl-board_groups
CREATE TABLE proms.board_groups (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    name text NOT NULL,
    "position" integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by uuid,
    CONSTRAINT board_groups_name_check CHECK (((char_length(name) >= 1) AND (char_length(name) <= 60)))
);

ALTER TABLE ONLY proms.board_groups FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.board_groups CASCADE;

--changeset hyproms:04-tbl-board_invitations
CREATE TABLE proms.board_invitations (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    board_id uuid NOT NULL,
    email public.citext NOT NULL,
    board_role text NOT NULL,
    workstream_ids uuid[] DEFAULT '{}'::uuid[] NOT NULL,
    token_hash text NOT NULL,
    invited_by uuid NOT NULL,
    expires_at timestamp with time zone DEFAULT (now() + '7 days'::interval) NOT NULL,
    accepted_at timestamp with time zone,
    revoked_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    email_failed_at timestamp with time zone,
    CONSTRAINT board_invitations_board_role_check CHECK ((board_role = ANY (ARRAY['admin'::text, 'pm'::text, 'member'::text, 'viewer'::text, 'guest'::text]))),
    CONSTRAINT board_invitations_check CHECK ((((board_role = ANY (ARRAY['member'::text, 'viewer'::text])) AND (cardinality(workstream_ids) >= 1)) OR ((board_role = ANY (ARRAY['admin'::text, 'pm'::text, 'guest'::text])) AND (cardinality(workstream_ids) = 0))))
);

ALTER TABLE ONLY proms.board_invitations FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.board_invitations CASCADE;

--changeset hyproms:04-tbl-board_member_workstreams
CREATE TABLE proms.board_member_workstreams (
    tenant_id uuid NOT NULL,
    board_member_id uuid NOT NULL,
    workstream_id uuid NOT NULL
);

ALTER TABLE ONLY proms.board_member_workstreams FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.board_member_workstreams CASCADE;

--changeset hyproms:04-tbl-board_members
CREATE TABLE proms.board_members (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    board_id uuid NOT NULL,
    user_id uuid NOT NULL,
    role text NOT NULL,
    is_owner boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT board_members_check CHECK (((NOT is_owner) OR (role = 'admin'::text))),
    CONSTRAINT board_members_role_check CHECK ((role = ANY (ARRAY['admin'::text, 'pm'::text, 'member'::text, 'viewer'::text, 'guest'::text])))
);

ALTER TABLE ONLY proms.board_members FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.board_members CASCADE;

--changeset hyproms:04-tbl-board_pins
CREATE TABLE proms.board_pins (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    board_id uuid NOT NULL,
    user_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);

ALTER TABLE ONLY proms.board_pins FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.board_pins CASCADE;

--changeset hyproms:04-tbl-board_saved_views
CREATE TABLE proms.board_saved_views (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    board_id uuid NOT NULL,
    user_id uuid NOT NULL,
    name character varying(100) NOT NULL,
    view_type text NOT NULL,
    filters jsonb DEFAULT '{}'::jsonb NOT NULL,
    sort_config jsonb DEFAULT '{}'::jsonb NOT NULL,
    group_by text,
    is_default boolean DEFAULT false NOT NULL,
    "position" integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT board_saved_views_view_type_check CHECK ((view_type = ANY (ARRAY['list'::text, 'board'::text, 'timeline'::text, 'calendar'::text])))
);

ALTER TABLE ONLY proms.board_saved_views FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.board_saved_views CASCADE;

--changeset hyproms:04-tbl-boards
CREATE TABLE proms.boards (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    key text NOT NULL,
    name text NOT NULL,
    description text,
    client_name text,
    group_name text,
    owner_id uuid NOT NULL,
    pm_id uuid,
    status text DEFAULT 'active'::text NOT NULL,
    start_date date,
    due_date date,
    shared_columns boolean DEFAULT true NOT NULL,
    settings jsonb DEFAULT '{"wipBlock": false, "msRequired": false, "onlyPmDone": false, "allCanSeeAll": true, "blockedReason": true, "msAutoComplete": true, "msDraftInvoice": true}'::jsonb NOT NULL,
    task_seq integer DEFAULT 0 NOT NULL,
    health text,
    health_reason text,
    progress_pct smallint,
    health_computed_at timestamp with time zone,
    archived_at timestamp with time zone,
    archived_by uuid,
    created_by uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by uuid,
    board_group_id uuid,
    CONSTRAINT boards_check CHECK (((start_date IS NULL) OR (due_date IS NULL) OR (start_date <= due_date))),
    CONSTRAINT boards_health_check CHECK ((health = ANY (ARRAY['on_track'::text, 'at_risk'::text, 'off_track'::text, 'needs_review'::text, 'done'::text]))),
    CONSTRAINT boards_key_check CHECK ((key ~ '^[A-Z]{2,5}$'::text)),
    CONSTRAINT boards_progress_pct_check CHECK (((progress_pct >= 0) AND (progress_pct <= 100))),
    CONSTRAINT boards_status_check CHECK ((status = ANY (ARRAY['active'::text, 'finished'::text, 'archived'::text])))
);

ALTER TABLE ONLY proms.boards FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.boards CASCADE;

--changeset hyproms:04-tbl-budget_categories
CREATE TABLE proms.budget_categories (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    code text,
    name text NOT NULL,
    "position" integer DEFAULT 0 NOT NULL
);

ALTER TABLE ONLY proms.budget_categories FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.budget_categories CASCADE;

--changeset hyproms:04-tbl-budget_lines
CREATE TABLE proms.budget_lines (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    board_id uuid NOT NULL,
    category_id uuid NOT NULL,
    amount bigint NOT NULL,
    CONSTRAINT budget_lines_amount_check CHECK ((amount >= 0))
);

ALTER TABLE ONLY proms.budget_lines FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.budget_lines CASCADE;

--changeset hyproms:04-tbl-chat_channel_members
CREATE TABLE proms.chat_channel_members (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    channel_id uuid NOT NULL,
    user_id uuid NOT NULL,
    role text DEFAULT 'member'::text NOT NULL,
    is_starred boolean DEFAULT false NOT NULL,
    is_muted boolean DEFAULT false NOT NULL,
    joined_at timestamp with time zone DEFAULT now() NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by uuid,
    CONSTRAINT chat_channel_members_role_check CHECK ((role = ANY (ARRAY['creator'::text, 'admin'::text, 'member'::text])))
);
--rollback DROP TABLE IF EXISTS proms.chat_channel_members CASCADE;

--changeset hyproms:04-tbl-chat_channels
CREATE TABLE proms.chat_channels (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    board_id uuid,
    workstream_id uuid,
    name public.citext NOT NULL,
    description text DEFAULT ''::text,
    is_default boolean DEFAULT false NOT NULL,
    created_by uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    scope text DEFAULT 'workspace'::text NOT NULL,
    type text DEFAULT 'public'::text NOT NULL,
    slug text,
    topic text DEFAULT ''::text,
    is_archived boolean DEFAULT false NOT NULL,
    archived_at timestamp with time zone,
    archived_by uuid,
    deleted_at timestamp with time zone,
    deleted_by uuid,
    CONSTRAINT chat_channels_scope_check CHECK ((scope = ANY (ARRAY['workspace'::text, 'board'::text]))),
    CONSTRAINT chat_channels_type_check CHECK ((type = ANY (ARRAY['public'::text, 'private'::text, 'direct_message'::text])))
);

ALTER TABLE ONLY proms.chat_channels FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.chat_channels CASCADE;

--changeset hyproms:04-tbl-chat_messages
CREATE TABLE proms.chat_messages (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    channel_id uuid NOT NULL,
    user_id uuid NOT NULL,
    parent_id uuid,
    text text NOT NULL,
    files jsonb DEFAULT '[]'::jsonb,
    is_pinned boolean DEFAULT false NOT NULL,
    pinned_at timestamp with time zone,
    pinned_by uuid,
    task_id uuid,
    edited_at timestamp with time zone,
    deleted_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);

ALTER TABLE ONLY proms.chat_messages FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.chat_messages CASCADE;

--changeset hyproms:04-tbl-chat_reactions
CREATE TABLE proms.chat_reactions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    message_id uuid NOT NULL,
    user_id uuid NOT NULL,
    emoji text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

ALTER TABLE ONLY proms.chat_reactions FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.chat_reactions CASCADE;

--changeset hyproms:04-tbl-chat_read_markers
CREATE TABLE proms.chat_read_markers (
    tenant_id uuid NOT NULL,
    user_id uuid NOT NULL,
    channel_id uuid NOT NULL,
    last_read_at timestamp with time zone DEFAULT now() NOT NULL
);

ALTER TABLE ONLY proms.chat_read_markers FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.chat_read_markers CASCADE;

--changeset hyproms:04-tbl-checklist_items
CREATE TABLE proms.checklist_items (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    task_id uuid NOT NULL,
    title text NOT NULL,
    is_done boolean DEFAULT false NOT NULL,
    "position" integer DEFAULT 0 NOT NULL,
    done_by uuid,
    done_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

ALTER TABLE ONLY proms.checklist_items FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.checklist_items CASCADE;

--changeset hyproms:04-tbl-comment_mentions
CREATE TABLE proms.comment_mentions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    comment_id uuid NOT NULL,
    user_id uuid,
    referenced_task uuid,
    CONSTRAINT comment_mentions_check CHECK (((user_id IS NULL) <> (referenced_task IS NULL)))
);

ALTER TABLE ONLY proms.comment_mentions FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.comment_mentions CASCADE;

--changeset hyproms:04-tbl-comments
CREATE TABLE proms.comments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    task_id uuid NOT NULL,
    author_id uuid NOT NULL,
    body jsonb NOT NULL,
    body_text text DEFAULT ''::text NOT NULL,
    visibility text DEFAULT 'internal'::text NOT NULL,
    edited_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by uuid,
    CONSTRAINT comments_visibility_check CHECK ((visibility = ANY (ARRAY['internal'::text, 'client'::text])))
);

ALTER TABLE ONLY proms.comments FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.comments CASCADE;

--changeset hyproms:04-tbl-dashboard_reminders
CREATE TABLE proms.dashboard_reminders (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    user_id uuid NOT NULL,
    text text NOT NULL,
    done boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT dashboard_reminders_text_check CHECK ((length(btrim(text)) > 0))
);

ALTER TABLE ONLY proms.dashboard_reminders FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.dashboard_reminders CASCADE;

--changeset hyproms:04-tbl-expenses
CREATE TABLE proms.expenses (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    board_id uuid NOT NULL,
    category_id uuid NOT NULL,
    name text NOT NULL,
    amount bigint NOT NULL,
    spent_on date NOT NULL,
    receipt_attachment_id uuid,
    status text DEFAULT 'submitted'::text NOT NULL,
    submitted_by uuid NOT NULL,
    decided_by uuid,
    decided_at timestamp with time zone,
    reject_reason text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT expenses_amount_check CHECK ((amount > 0)),
    CONSTRAINT expenses_check CHECK (((amount <= 1000000) OR (receipt_attachment_id IS NOT NULL))),
    CONSTRAINT expenses_check1 CHECK (((status <> 'rejected'::text) OR (reject_reason IS NOT NULL))),
    CONSTRAINT expenses_status_check CHECK ((status = ANY (ARRAY['submitted'::text, 'approved'::text, 'rejected'::text])))
);

ALTER TABLE ONLY proms.expenses FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.expenses CASCADE;

--changeset hyproms:04-tbl-focus_sessions
CREATE TABLE proms.focus_sessions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    user_id uuid NOT NULL,
    started_at timestamp with time zone NOT NULL,
    ended_at timestamp with time zone NOT NULL,
    duration_sec integer NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT focus_sessions_duration_check CHECK (((duration_sec >= 60) AND (duration_sec <= 57600))),
    CONSTRAINT focus_sessions_range_check CHECK ((ended_at > started_at))
);

ALTER TABLE ONLY proms.focus_sessions FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.focus_sessions CASCADE;

--changeset hyproms:04-tbl-form_fields
CREATE TABLE proms.form_fields (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    form_id uuid NOT NULL,
    type text NOT NULL,
    label text NOT NULL,
    help_text text,
    is_required boolean DEFAULT false NOT NULL,
    options text[],
    maps_to text DEFAULT 'none'::text NOT NULL,
    "position" integer NOT NULL,
    CONSTRAINT form_fields_check CHECK (((type <> 'dropdown'::text) OR (cardinality(options) >= 2))),
    CONSTRAINT form_fields_check1 CHECK (((maps_to = 'none'::text) OR ((type = 'short'::text) AND (maps_to = 'title'::text)) OR ((type = 'long'::text) AND (maps_to = 'description'::text)) OR ((type = 'dropdown'::text) AND (maps_to = ANY (ARRAY['priority'::text, 'tag'::text]))) OR ((type = 'date'::text) AND (maps_to = 'due'::text)) OR ((type = 'email'::text) AND (maps_to = 'submitter'::text)) OR ((type = 'file'::text) AND (maps_to = 'attachment'::text)))),
    CONSTRAINT form_fields_label_check CHECK ((length(TRIM(BOTH FROM label)) > 0)),
    CONSTRAINT form_fields_maps_to_check CHECK ((maps_to = ANY (ARRAY['title'::text, 'description'::text, 'priority'::text, 'due'::text, 'tag'::text, 'attachment'::text, 'submitter'::text, 'none'::text]))),
    CONSTRAINT form_fields_type_check CHECK ((type = ANY (ARRAY['short'::text, 'long'::text, 'dropdown'::text, 'date'::text, 'email'::text, 'file'::text])))
);

ALTER TABLE ONLY proms.form_fields FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.form_fields CASCADE;

--changeset hyproms:04-tbl-form_submissions
CREATE TABLE proms.form_submissions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    form_id uuid NOT NULL,
    task_id uuid,
    submitter_email public.citext,
    submitter_user_id uuid,
    answers jsonb NOT NULL,
    ip inet,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

ALTER TABLE ONLY proms.form_submissions FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.form_submissions CASCADE;

--changeset hyproms:04-tbl-forms
CREATE TABLE proms.forms (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    board_id uuid NOT NULL,
    name text NOT NULL,
    description text,
    is_active boolean DEFAULT false NOT NULL,
    access text DEFAULT 'link_public'::text NOT NULL,
    public_token text,
    in_portal boolean DEFAULT false NOT NULL,
    captcha boolean DEFAULT true NOT NULL,
    notify boolean DEFAULT true NOT NULL,
    confirm_message text,
    target_workstream_id uuid NOT NULL,
    target_column_id uuid NOT NULL,
    target_assignee_id uuid,
    target_priority text DEFAULT 'medium'::text NOT NULL,
    target_tag_id uuid,
    created_by uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    CONSTRAINT forms_access_check CHECK ((access = ANY (ARRAY['link_public'::text, 'guests'::text, 'internal'::text]))),
    CONSTRAINT forms_check CHECK (((access <> 'link_public'::text) OR captcha)),
    CONSTRAINT forms_target_priority_check CHECK ((target_priority = ANY (ARRAY['low'::text, 'medium'::text, 'high'::text, 'urgent'::text])))
);

ALTER TABLE ONLY proms.forms FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.forms CASCADE;

--changeset hyproms:04-tbl-holidays
CREATE TABLE proms.holidays (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    date date NOT NULL,
    name text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid
);

ALTER TABLE ONLY proms.holidays FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.holidays CASCADE;

--changeset hyproms:04-tbl-idempotency_keys
CREATE TABLE proms.idempotency_keys (
    tenant_id uuid NOT NULL,
    user_id uuid NOT NULL,
    key text NOT NULL,
    method text NOT NULL,
    path text NOT NULL,
    request_hash text NOT NULL,
    status_code smallint,
    response_body jsonb,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    expires_at timestamp with time zone DEFAULT (now() + '24:00:00'::interval) NOT NULL,
    CONSTRAINT idempotency_keys_key_check CHECK (((length(key) >= 8) AND (length(key) <= 128)))
);

ALTER TABLE ONLY proms.idempotency_keys FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.idempotency_keys CASCADE;

--changeset hyproms:04-tbl-invoices
CREATE TABLE proms.invoices (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    payment_term_id uuid NOT NULL,
    invoice_no text,
    status text DEFAULT 'draft'::text NOT NULL,
    amount bigint NOT NULL,
    issued_on date,
    due_on date,
    paid_on date,
    auto_created boolean DEFAULT false NOT NULL,
    file_key text,
    void_reason text,
    created_by uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT invoices_amount_check CHECK ((amount >= 0)),
    CONSTRAINT invoices_check CHECK (((status <> 'void'::text) OR (void_reason IS NOT NULL))),
    CONSTRAINT invoices_check1 CHECK (((status = 'draft'::text) OR (invoice_no IS NOT NULL))),
    CONSTRAINT invoices_status_check CHECK ((status = ANY (ARRAY['draft'::text, 'sent'::text, 'paid'::text, 'overdue'::text, 'void'::text])))
);

ALTER TABLE ONLY proms.invoices FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.invoices CASCADE;

--changeset hyproms:04-tbl-meeting_action_items
CREATE TABLE proms.meeting_action_items (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    meeting_id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    title character varying(255) NOT NULL,
    description text,
    assignee_id uuid,
    due_date date,
    task_id uuid,
    is_converted_to_task boolean DEFAULT false NOT NULL,
    status character varying(30) DEFAULT 'pending'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    module character varying(100)
);

ALTER TABLE ONLY proms.meeting_action_items FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.meeting_action_items CASCADE;

--changeset hyproms:04-tbl-meeting_attendees
CREATE TABLE proms.meeting_attendees (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    meeting_id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    user_id uuid,
    name character varying(255) NOT NULL,
    email character varying(255),
    role_name character varying(255),
    attendance_status character varying(30) DEFAULT 'present'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

ALTER TABLE ONLY proms.meeting_attendees FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.meeting_attendees CASCADE;

--changeset hyproms:04-tbl-meeting_minutes
CREATE TABLE proms.meeting_minutes (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    board_id uuid NOT NULL,
    reference_no character varying(50) NOT NULL,
    title character varying(255) NOT NULL,
    meeting_date timestamp with time zone NOT NULL,
    duration_minutes integer DEFAULT 60 NOT NULL,
    location character varying(255),
    agenda text,
    discussion_notes text,
    status character varying(30) DEFAULT 'published'::character varying NOT NULL,
    followup_items jsonb,
    decisions jsonb,
    created_by uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    notary_name character varying(255)
);

ALTER TABLE ONLY proms.meeting_minutes FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.meeting_minutes CASCADE;

--changeset hyproms:04-tbl-milestone_workstreams
CREATE TABLE proms.milestone_workstreams (
    tenant_id uuid NOT NULL,
    milestone_id uuid NOT NULL,
    workstream_id uuid NOT NULL
);

ALTER TABLE ONLY proms.milestone_workstreams FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.milestone_workstreams CASCADE;

--changeset hyproms:04-tbl-milestones
CREATE TABLE proms.milestones (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    board_id uuid NOT NULL,
    name text NOT NULL,
    target_date date NOT NULL,
    status text DEFAULT 'open'::text NOT NULL,
    completed_at timestamp with time zone,
    health text,
    progress_pct smallint,
    health_computed_at timestamp with time zone,
    "position" integer DEFAULT 0 NOT NULL,
    created_by uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by uuid,
    CONSTRAINT milestones_health_check CHECK ((health = ANY (ARRAY['no_tasks'::text, 'on_track'::text, 'at_risk'::text, 'off_track'::text, 'done'::text]))),
    CONSTRAINT milestones_progress_pct_check CHECK (((progress_pct >= 0) AND (progress_pct <= 100))),
    CONSTRAINT milestones_status_check CHECK ((status = ANY (ARRAY['open'::text, 'completed'::text])))
);

ALTER TABLE ONLY proms.milestones FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.milestones CASCADE;

--changeset hyproms:04-tbl-notifications
CREATE TABLE proms.notifications (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    recipient_id uuid NOT NULL,
    type text NOT NULL,
    actor_id uuid,
    board_id uuid,
    task_id uuid,
    entity_type text,
    entity_id uuid,
    payload jsonb DEFAULT '{}'::jsonb NOT NULL,
    group_key text,
    read_at timestamp with time zone,
    snoozed_until timestamp with time zone,
    archived_at timestamp with time zone,
    decision text,
    decision_reason text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT notifications_check CHECK ((actor_id IS DISTINCT FROM recipient_id)),
    CONSTRAINT notifications_check1 CHECK (((decision IS DISTINCT FROM 'rejected'::text) OR (decision_reason IS NOT NULL))),
    CONSTRAINT notifications_decision_check CHECK ((decision = ANY (ARRAY['approved'::text, 'rejected'::text]))),
    CONSTRAINT notifications_type_check CHECK ((type = ANY (ARRAY['mention'::text, 'assigned'::text, 'status'::text, 'due'::text, 'approval'::text, 'form'::text, 'milestone'::text, 'comment'::text, 'board_member'::text, 'feedback'::text, 'standup'::text])))
);

ALTER TABLE ONLY proms.notifications FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.notifications CASCADE;

--changeset hyproms:04-tbl-outbox_events
CREATE TABLE proms.outbox_events (
    id bigint NOT NULL,
    tenant_id uuid NOT NULL,
    event_type text NOT NULL,
    aggregate_id uuid,
    payload jsonb NOT NULL,
    actor_id uuid,
    occurred_at timestamp with time zone DEFAULT now() NOT NULL,
    dispatched_at timestamp with time zone,
    attempts smallint DEFAULT 0 NOT NULL,
    last_error text
);

ALTER TABLE proms.outbox_events ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME proms.outbox_events_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);
--rollback DROP TABLE IF EXISTS proms.outbox_events CASCADE;

--changeset hyproms:04-tbl-payment_terms
CREATE TABLE proms.payment_terms (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    contract_id uuid NOT NULL,
    name text NOT NULL,
    percent numeric(5,2) NOT NULL,
    amount bigint NOT NULL,
    milestone_id uuid,
    "position" integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT payment_terms_amount_check CHECK ((amount >= 0)),
    CONSTRAINT payment_terms_percent_check CHECK (((percent > (0)::numeric) AND (percent <= (100)::numeric)))
);

ALTER TABLE ONLY proms.payment_terms FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.payment_terms CASCADE;

--changeset hyproms:04-tbl-point_entries
CREATE TABLE proms.point_entries (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    user_id uuid NOT NULL,
    task_id uuid,
    board_id uuid,
    rule_code text NOT NULL,
    points integer NOT NULL,
    late_days integer,
    feedback_id uuid,
    policy_version integer NOT NULL,
    reverses_id uuid,
    reason text,
    source_event_id bigint,
    occurred_at timestamp with time zone NOT NULL,
    created_by uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT point_entries_feedback_check CHECK (((rule_code <> 'BR-PT-21'::text) OR (feedback_id IS NOT NULL))),
    CONSTRAINT point_entries_late_days_check CHECK (
CASE
    WHEN (rule_code = 'BR-PT-10'::text) THEN (COALESCE(late_days, 0) > 0)
    ELSE (late_days IS NULL)
END),
    CONSTRAINT point_entries_manual_check CHECK (((rule_code <> 'MANUAL'::text) OR ((points <> 0) AND (NULLIF(btrim(reason), ''::text) IS NOT NULL)))),
    CONSTRAINT point_entries_policy_version_check CHECK ((policy_version >= 0)),
    CONSTRAINT point_entries_reversal_check CHECK (((rule_code = 'REVERSAL'::text) = (reverses_id IS NOT NULL))),
    CONSTRAINT point_entries_rule_code_check CHECK ((rule_code = ANY (ARRAY['BR-PT-09'::text, 'BR-PT-10'::text, 'BR-PT-21'::text, 'BR-PT-30'::text, 'BR-PT-40'::text, 'REVERSAL'::text, 'MANUAL'::text]))),
    CONSTRAINT point_entries_task_check CHECK (((rule_code = ANY (ARRAY['MANUAL'::text, 'BR-PT-30'::text, 'BR-PT-40'::text, 'REVERSAL'::text])) OR (task_id IS NOT NULL)))
);

ALTER TABLE ONLY proms.point_entries FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.point_entries CASCADE;

--changeset hyproms:04-tbl-point_policies
CREATE TABLE proms.point_policies (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    version integer NOT NULL,
    settings jsonb NOT NULL,
    effective_from date NOT NULL,
    created_by uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT point_policies_settings_check CHECK ((jsonb_typeof(settings) = 'object'::text)),
    CONSTRAINT point_policies_version_check CHECK ((version >= 1))
);

ALTER TABLE ONLY proms.point_policies FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.point_policies CASCADE;

--changeset hyproms:04-tbl-recurrence_rules
CREATE TABLE proms.recurrence_rules (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    frequency text NOT NULL,
    "interval" smallint DEFAULT 1 NOT NULL,
    by_weekday smallint[],
    by_monthday smallint,
    trigger text DEFAULT 'on_done'::text NOT NULL,
    next_run_on date,
    template_task_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT recurrence_rules_by_monthday_check CHECK (((by_monthday >= 1) AND (by_monthday <= 31))),
    CONSTRAINT recurrence_rules_frequency_check CHECK ((frequency = ANY (ARRAY['daily'::text, 'weekly'::text, 'monthly'::text, 'weekdays'::text]))),
    CONSTRAINT recurrence_rules_interval_check CHECK (("interval" > 0)),
    CONSTRAINT recurrence_rules_trigger_check CHECK ((trigger = ANY (ARRAY['on_done'::text, 'on_schedule'::text])))
);

ALTER TABLE ONLY proms.recurrence_rules FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.recurrence_rules CASCADE;

--changeset hyproms:04-tbl-share_links
CREATE TABLE proms.share_links (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    board_id uuid NOT NULL,
    access text NOT NULL,
    token_hash text NOT NULL,
    expires_at timestamp with time zone,
    revoked_at timestamp with time zone,
    created_by uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT share_links_access_check CHECK ((access = ANY (ARRAY['link_public'::text, 'workspace'::text])))
);

ALTER TABLE ONLY proms.share_links FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.share_links CASCADE;

--changeset hyproms:04-tbl-standup_policies
CREATE TABLE proms.standup_policies (
    tenant_id uuid NOT NULL,
    enabled boolean DEFAULT true NOT NULL,
    reminder_enabled boolean DEFAULT true NOT NULL,
    reminder_time time without time zone DEFAULT '10:00:00'::time without time zone NOT NULL,
    deadline_time time without time zone DEFAULT '12:00:00'::time without time zone NOT NULL,
    blocking_banner boolean DEFAULT false NOT NULL,
    updated_by uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT standup_policies_time_check CHECK ((reminder_time < deadline_time))
);

ALTER TABLE ONLY proms.standup_policies FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.standup_policies CASCADE;

--changeset hyproms:04-tbl-standup_reminder_runs
CREATE TABLE proms.standup_reminder_runs (
    tenant_id uuid NOT NULL,
    run_date date NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

ALTER TABLE ONLY proms.standup_reminder_runs FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.standup_reminder_runs CASCADE;

--changeset hyproms:04-tbl-standup_reports
CREATE TABLE proms.standup_reports (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    user_id uuid NOT NULL,
    report_date date NOT NULL,
    plan text NOT NULL,
    done text NOT NULL,
    blockers text,
    submitted_at timestamp with time zone NOT NULL,
    is_late boolean NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT standup_reports_done_check CHECK ((length(btrim(done)) > 0)),
    CONSTRAINT standup_reports_plan_check CHECK ((length(btrim(plan)) > 0))
);

ALTER TABLE ONLY proms.standup_reports FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.standup_reports CASCADE;

--changeset hyproms:04-tbl-tags
CREATE TABLE proms.tags (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    board_id uuid,
    name public.citext NOT NULL,
    color text,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

ALTER TABLE ONLY proms.tags FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.tags CASCADE;

--changeset hyproms:04-tbl-task_activities
CREATE TABLE proms.task_activities (
    id bigint NOT NULL,
    tenant_id uuid NOT NULL,
    task_id uuid NOT NULL,
    actor_id uuid,
    action text NOT NULL,
    field text,
    old_value jsonb,
    new_value jsonb,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

ALTER TABLE ONLY proms.task_activities FORCE ROW LEVEL SECURITY;

ALTER TABLE proms.task_activities ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME proms.task_activities_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);
--rollback DROP TABLE IF EXISTS proms.task_activities CASCADE;

--changeset hyproms:04-tbl-task_assignees
CREATE TABLE proms.task_assignees (
    tenant_id uuid NOT NULL,
    task_id uuid NOT NULL,
    user_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

ALTER TABLE ONLY proms.task_assignees FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.task_assignees CASCADE;

--changeset hyproms:04-tbl-task_dependencies
CREATE TABLE proms.task_dependencies (
    tenant_id uuid NOT NULL,
    blocking_task_id uuid NOT NULL,
    blocked_task_id uuid NOT NULL,
    created_by uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT task_dependencies_check CHECK ((blocking_task_id <> blocked_task_id))
);

ALTER TABLE ONLY proms.task_dependencies FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.task_dependencies CASCADE;

--changeset hyproms:04-tbl-task_feedback
CREATE TABLE proms.task_feedback (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    task_id uuid NOT NULL,
    score smallint NOT NULL,
    comment text,
    given_by uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    superseded_at timestamp with time zone,
    CONSTRAINT task_feedback_comment_check CHECK (((score > 2) OR (NULLIF(btrim(comment), ''::text) IS NOT NULL))),
    CONSTRAINT task_feedback_score_check CHECK (((score >= 1) AND (score <= 5)))
);

ALTER TABLE ONLY proms.task_feedback FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.task_feedback CASCADE;

--changeset hyproms:04-tbl-task_tags
CREATE TABLE proms.task_tags (
    tenant_id uuid NOT NULL,
    task_id uuid NOT NULL,
    tag_id uuid NOT NULL
);

ALTER TABLE ONLY proms.task_tags FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.task_tags CASCADE;

--changeset hyproms:04-tbl-task_watchers
CREATE TABLE proms.task_watchers (
    tenant_id uuid NOT NULL,
    task_id uuid NOT NULL,
    user_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

ALTER TABLE ONLY proms.task_watchers FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.task_watchers CASCADE;

--changeset hyproms:04-tbl-tasks
CREATE TABLE proms.tasks (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    board_id uuid NOT NULL,
    number integer NOT NULL,
    parent_id uuid,
    workstream_id uuid NOT NULL,
    column_id uuid NOT NULL,
    "position" text DEFAULT 'a0'::text NOT NULL,
    title text NOT NULL,
    description jsonb,
    description_text text,
    priority text DEFAULT 'medium'::text NOT NULL,
    start_date timestamp with time zone,
    due_date date,
    milestone_id uuid,
    is_blocked boolean DEFAULT false NOT NULL,
    blocked_reason text,
    completed_at timestamp with time zone,
    shared_with_client boolean DEFAULT false NOT NULL,
    custom_values jsonb DEFAULT '{}'::jsonb NOT NULL,
    recurrence_rule_id uuid,
    source_form_id uuid,
    source_email public.citext,
    search_vector tsvector GENERATED ALWAYS AS ((setweight(to_tsvector('simple'::regconfig, COALESCE(title, ''::text)), 'A'::"char") || setweight(to_tsvector('simple'::regconfig, COALESCE(description_text, ''::text)), 'B'::"char"))) STORED,
    created_by uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by uuid,
    CONSTRAINT tasks_check CHECK (((start_date IS NULL) OR (due_date IS NULL) OR ((start_date)::date <= due_date))),
    CONSTRAINT tasks_check1 CHECK (((parent_id IS NULL) OR (parent_id <> id))),
    CONSTRAINT tasks_priority_check CHECK ((priority = ANY (ARRAY['low'::text, 'medium'::text, 'high'::text, 'urgent'::text]))),
    CONSTRAINT tasks_title_check CHECK (((char_length(title) >= 1) AND (char_length(title) <= 200)))
);

ALTER TABLE ONLY proms.tasks FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.tasks CASCADE;

--changeset hyproms:04-tbl-template_columns
CREATE TABLE proms.template_columns (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    template_id uuid NOT NULL,
    name text NOT NULL,
    category text NOT NULL,
    color text,
    wip_limit integer,
    "position" integer NOT NULL,
    CONSTRAINT template_columns_category_check CHECK ((category = ANY (ARRAY['not_started'::text, 'in_progress'::text, 'blocked'::text, 'done'::text]))),
    CONSTRAINT template_columns_wip_limit_check CHECK ((wip_limit > 0))
);
--rollback DROP TABLE IF EXISTS proms.template_columns CASCADE;

--changeset hyproms:04-tbl-time_entries
CREATE TABLE proms.time_entries (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    user_id uuid NOT NULL,
    task_id uuid NOT NULL,
    board_id uuid NOT NULL,
    work_date date NOT NULL,
    started_at timestamp with time zone,
    ended_at timestamp with time zone,
    duration_min integer NOT NULL,
    source text NOT NULL,
    note text,
    timesheet_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    CONSTRAINT time_entries_check CHECK (((ended_at IS NULL) OR (started_at IS NULL) OR (ended_at > started_at))),
    CONSTRAINT time_entries_duration_min_check CHECK ((duration_min > 0)),
    CONSTRAINT time_entries_source_check CHECK ((source = ANY (ARRAY['timer'::text, 'manual'::text])))
);

ALTER TABLE ONLY proms.time_entries FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.time_entries CASCADE;

--changeset hyproms:04-tbl-time_off
CREATE TABLE proms.time_off (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    user_id uuid NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    type text DEFAULT 'leave'::text NOT NULL,
    hours_per_day numeric(4,2) DEFAULT 8 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT time_off_check CHECK ((end_date >= start_date)),
    CONSTRAINT time_off_type_check CHECK ((type = ANY (ARRAY['leave'::text, 'sick'::text, 'other'::text])))
);

ALTER TABLE ONLY proms.time_off FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.time_off CASCADE;

--changeset hyproms:04-tbl-timesheets
CREATE TABLE proms.timesheets (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    user_id uuid NOT NULL,
    board_id uuid NOT NULL,
    week_start date NOT NULL,
    status text DEFAULT 'draft'::text NOT NULL,
    total_min integer DEFAULT 0 NOT NULL,
    capacity_min integer,
    submitted_at timestamp with time zone,
    decided_at timestamp with time zone,
    decided_by uuid,
    reject_reason text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT timesheets_check CHECK (((status <> 'rejected'::text) OR (reject_reason IS NOT NULL))),
    CONSTRAINT timesheets_status_check CHECK ((status = ANY (ARRAY['draft'::text, 'submitted'::text, 'approved'::text, 'rejected'::text]))),
    CONSTRAINT timesheets_week_start_check CHECK ((EXTRACT(isodow FROM week_start) = (1)::numeric))
);

ALTER TABLE ONLY proms.timesheets FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.timesheets CASCADE;

--changeset hyproms:04-tbl-user_preferences
CREATE TABLE proms.user_preferences (
    tenant_id uuid NOT NULL,
    user_id uuid NOT NULL,
    notification_settings jsonb DEFAULT '{}'::jsonb NOT NULL,
    dashboard_layout jsonb DEFAULT '{}'::jsonb NOT NULL,
    recent_items jsonb DEFAULT '[]'::jsonb NOT NULL,
    timer_task_id uuid,
    timer_started_at timestamp with time zone,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT user_preferences_check CHECK (((timer_task_id IS NULL) = (timer_started_at IS NULL)))
);

ALTER TABLE ONLY proms.user_preferences FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.user_preferences CASCADE;

--changeset hyproms:04-tbl-workflow_templates
CREATE TABLE proms.workflow_templates (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid,
    name text NOT NULL,
    created_by uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);

ALTER TABLE ONLY proms.workflow_templates FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.workflow_templates CASCADE;

--changeset hyproms:04-tbl-workspace_calendars
CREATE TABLE proms.workspace_calendars (
    tenant_id uuid NOT NULL,
    work_days smallint[] NOT NULL,
    updated_by uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT workspace_calendars_work_days_check CHECK ((((cardinality(work_days) >= 1) AND (cardinality(work_days) <= 7)) AND (work_days <@ '{1,2,3,4,5,6,7}'::smallint[])))
);

ALTER TABLE ONLY proms.workspace_calendars FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.workspace_calendars CASCADE;

--changeset hyproms:04-tbl-workstreams
CREATE TABLE proms.workstreams (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    board_id uuid NOT NULL,
    name text NOT NULL,
    team_id uuid,
    lead_id uuid,
    color text,
    template_id uuid,
    custom_fields jsonb DEFAULT '[]'::jsonb NOT NULL,
    "position" integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by uuid
);

ALTER TABLE ONLY proms.workstreams FORCE ROW LEVEL SECURITY;
--rollback DROP TABLE IF EXISTS proms.workstreams CASCADE;
