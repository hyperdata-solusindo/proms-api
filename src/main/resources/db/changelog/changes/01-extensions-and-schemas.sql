--liquibase formatted sql

--changeset hyproms:01-schemas-and-extensions

CREATE SCHEMA IF NOT EXISTS platform;

CREATE SCHEMA IF NOT EXISTS proms;

CREATE EXTENSION IF NOT EXISTS citext WITH SCHEMA public;

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;

--rollback DROP EXTENSION IF EXISTS pgcrypto;
--rollback DROP EXTENSION IF EXISTS citext;
--rollback DROP SCHEMA IF EXISTS proms CASCADE;
--rollback DROP SCHEMA IF EXISTS platform CASCADE;
