-- RF-15: novedades de la campaña
CREATE TABLE novedad (
    id           BIGINT       NOT NULL,
    campania_id  BIGINT       NOT NULL,
    titulo       VARCHAR(150) NOT NULL,
    texto        TEXT         NOT NULL,
    fecha        TIMESTAMP    NOT NULL,
    CONSTRAINT pk_novedad PRIMARY KEY (id),
    CONSTRAINT fk_novedad__campania FOREIGN KEY (campania_id) REFERENCES campania (id)
);

CREATE INDEX ix_novedad__campania ON novedad (campania_id, fecha);

-- RF-18: entrega de la recompensa de cada aporte
ALTER TABLE aporte ADD COLUMN entregado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE aporte ADD COLUMN fecha_entrega TIMESTAMP;

-- RF-22: reclamos de los fans (y, más adelante, pedidos de arrepentimiento)
CREATE TABLE reclamo (
    id                BIGINT        NOT NULL,
    aporte_id         BIGINT        NOT NULL,
    tipo              VARCHAR(20)   NOT NULL,
    texto             VARCHAR(2000) NOT NULL,
    estado            VARCHAR(20)   NOT NULL,
    respuesta         VARCHAR(2000),
    fecha             TIMESTAMP     NOT NULL,
    fecha_resolucion  TIMESTAMP,
    CONSTRAINT pk_reclamo PRIMARY KEY (id),
    CONSTRAINT fk_reclamo__aporte FOREIGN KEY (aporte_id) REFERENCES aporte (id)
);

CREATE INDEX ix_reclamo__estado ON reclamo (estado, fecha);
