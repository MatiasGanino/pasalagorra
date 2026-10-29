-- RF-03: alta de artista con validación de identidad

INSERT INTO jhi_authority (name) VALUES ('ROLE_ARTISTA');

CREATE TABLE artista (
    id               BIGINT        NOT NULL,
    user_id          BIGINT        NOT NULL,
    nombre_artistico VARCHAR(100)  NOT NULL,
    nombre_completo  VARCHAR(100)  NOT NULL,
    dni              VARCHAR(8)    NOT NULL,
    cuit             VARCHAR(11)   NOT NULL,
    ciudad           VARCHAR(100)  NOT NULL,
    provincia        VARCHAR(60)   NOT NULL,
    biografia        VARCHAR(2000),
    estado           VARCHAR(20)   NOT NULL,
    motivo_rechazo   VARCHAR(500),
    fecha_solicitud  TIMESTAMP     NOT NULL,
    fecha_revision   TIMESTAMP,
    CONSTRAINT pk_artista PRIMARY KEY (id),
    CONSTRAINT ux_artista__user UNIQUE (user_id),
    CONSTRAINT ux_artista__dni UNIQUE (dni),
    CONSTRAINT ux_artista__cuit UNIQUE (cuit),
    CONSTRAINT fk_artista__user FOREIGN KEY (user_id) REFERENCES jhi_user (id)
);

CREATE INDEX ix_artista__estado ON artista (estado, fecha_solicitud);
