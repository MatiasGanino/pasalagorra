-- RF-04 a RF-08: campañas, recompensas e imágenes

CREATE TABLE campania (
    id                    BIGINT         NOT NULL,
    artista_id            BIGINT         NOT NULL,
    titulo                VARCHAR(120)   NOT NULL,
    slug                  VARCHAR(140)   NOT NULL,
    descripcion           TEXT           NOT NULL,
    video_url             VARCHAR(255),
    meta                  NUMERIC(14, 2) NOT NULL,
    fecha_cierre          DATE           NOT NULL,
    estado                VARCHAR(20)    NOT NULL,
    motivo_rechazo        VARCHAR(500),
    fecha_creacion        TIMESTAMP      NOT NULL,
    fecha_envio_revision  TIMESTAMP,
    fecha_publicacion     TIMESTAMP,
    CONSTRAINT pk_campania PRIMARY KEY (id),
    CONSTRAINT ux_campania__slug UNIQUE (slug),
    CONSTRAINT fk_campania__artista FOREIGN KEY (artista_id) REFERENCES artista (id)
);

CREATE INDEX ix_campania__estado ON campania (estado, fecha_cierre);

CREATE TABLE recompensa (
    id                      BIGINT         NOT NULL,
    campania_id             BIGINT         NOT NULL,
    titulo                  VARCHAR(120)   NOT NULL,
    descripcion             VARCHAR(2000)  NOT NULL,
    monto_minimo            NUMERIC(14, 2) NOT NULL,
    stock                   INTEGER,
    cantidad_vendida        INTEGER        NOT NULL DEFAULT 0,
    fecha_entrega_estimada  DATE           NOT NULL,
    requiere_envio          BOOLEAN        NOT NULL,
    orden                   INTEGER        NOT NULL,
    version                 BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT pk_recompensa PRIMARY KEY (id),
    CONSTRAINT fk_recompensa__campania FOREIGN KEY (campania_id) REFERENCES campania (id),
    CONSTRAINT ck_recompensa__stock CHECK (stock IS NULL OR cantidad_vendida <= stock)
);

CREATE TABLE campania_imagen (
    id           BIGINT       NOT NULL,
    campania_id  BIGINT       NOT NULL,
    archivo      VARCHAR(100) NOT NULL,
    orden        INTEGER      NOT NULL,
    CONSTRAINT pk_campania_imagen PRIMARY KEY (id),
    CONSTRAINT fk_campania_imagen__campania FOREIGN KEY (campania_id) REFERENCES campania (id)
);
