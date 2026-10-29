-- =============================================
-- V1: Esquema inicial — Pasalagorra
-- =============================================

CREATE SEQUENCE sequence_generator START WITH 1050 INCREMENT BY 50;

-- ===================== JHipster core =====================

CREATE TABLE jhi_user (
    id                  BIGINT       NOT NULL,
    login               VARCHAR(50)  NOT NULL,
    password_hash       VARCHAR(60)  NOT NULL,
    first_name          VARCHAR(50),
    last_name           VARCHAR(50),
    email               VARCHAR(191),
    image_url           VARCHAR(256),
    activated           BOOLEAN      NOT NULL DEFAULT FALSE,
    lang_key            VARCHAR(10),
    activation_key      VARCHAR(20),
    reset_key           VARCHAR(20),
    created_by          VARCHAR(50)  NOT NULL,
    created_date        TIMESTAMP,
    reset_date          TIMESTAMP,
    last_modified_by    VARCHAR(50),
    last_modified_date  TIMESTAMP,
    CONSTRAINT pk_jhi_user PRIMARY KEY (id),
    CONSTRAINT ux_user_login UNIQUE (login),
    CONSTRAINT ux_user_email UNIQUE (email)
);

CREATE TABLE jhi_authority (
    name VARCHAR(50) NOT NULL,
    CONSTRAINT pk_jhi_authority PRIMARY KEY (name)
);

CREATE TABLE jhi_user_authority (
    user_id        BIGINT      NOT NULL,
    authority_name VARCHAR(50) NOT NULL,
    CONSTRAINT pk_jhi_user_authority PRIMARY KEY (user_id, authority_name),
    CONSTRAINT fk_authority_name FOREIGN KEY (authority_name) REFERENCES jhi_authority (name),
    CONSTRAINT fk_user_id FOREIGN KEY (user_id) REFERENCES jhi_user (id)
);

INSERT INTO jhi_authority (name) VALUES ('ROLE_ADMIN'), ('ROLE_USER');
