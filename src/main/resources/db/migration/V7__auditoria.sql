-- RNF-13: historial de cambios de estado de campañas, artistas, aportes y reclamos
CREATE TABLE cambio_estado (
    id               BIGINT       NOT NULL,
    entidad          VARCHAR(20)  NOT NULL,
    entidad_id       BIGINT       NOT NULL,
    estado_anterior  VARCHAR(25),
    estado_nuevo     VARCHAR(25)  NOT NULL,
    usuario          VARCHAR(50)  NOT NULL,
    detalle          VARCHAR(500),
    fecha            TIMESTAMP    NOT NULL,
    CONSTRAINT pk_cambio_estado PRIMARY KEY (id)
);

CREATE INDEX ix_cambio_estado__entidad ON cambio_estado (entidad, entidad_id, fecha);
CREATE INDEX ix_cambio_estado__fecha ON cambio_estado (fecha);
