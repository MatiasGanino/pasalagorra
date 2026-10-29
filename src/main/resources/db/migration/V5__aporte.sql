-- RF-10 a RF-13: aportes y pagos

CREATE TABLE aporte (
    id               BIGINT         NOT NULL,
    campania_id      BIGINT         NOT NULL,
    user_id          BIGINT         NOT NULL,
    recompensa_id    BIGINT,
    monto            NUMERIC(14, 2) NOT NULL,
    estado           VARCHAR(25)    NOT NULL,
    referencia       VARCHAR(40)    NOT NULL,
    pago_id          VARCHAR(64),
    fecha_creacion   TIMESTAMP      NOT NULL,
    fecha_pago       TIMESTAMP,
    fecha_reembolso  TIMESTAMP,
    CONSTRAINT pk_aporte PRIMARY KEY (id),
    CONSTRAINT ux_aporte__referencia UNIQUE (referencia),
    CONSTRAINT ux_aporte__pago UNIQUE (pago_id),
    CONSTRAINT fk_aporte__campania FOREIGN KEY (campania_id) REFERENCES campania (id),
    CONSTRAINT fk_aporte__user FOREIGN KEY (user_id) REFERENCES jhi_user (id),
    CONSTRAINT fk_aporte__recompensa FOREIGN KEY (recompensa_id) REFERENCES recompensa (id)
);

CREATE INDEX ix_aporte__campania_estado ON aporte (campania_id, estado);
CREATE INDEX ix_aporte__user ON aporte (user_id);
