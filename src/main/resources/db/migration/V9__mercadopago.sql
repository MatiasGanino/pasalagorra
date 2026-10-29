-- Etapa 6: cuenta de Mercado Pago de cada artista, vinculada por OAuth (marketplace con split).
-- Los tokens se guardan cifrados con AES-GCM (application.pagos.mercadopago.clave-cifrado).
CREATE TABLE cuenta_mercadopago (
    id                  BIGINT        NOT NULL,
    artista_id          BIGINT        NOT NULL,
    mp_user_id          BIGINT        NOT NULL,
    access_token        VARCHAR(500)  NOT NULL,
    refresh_token       VARCHAR(500)  NOT NULL,
    vence               TIMESTAMP     NOT NULL,
    fecha_vinculacion   TIMESTAMP     NOT NULL,
    CONSTRAINT pk_cuenta_mercadopago PRIMARY KEY (id),
    CONSTRAINT ux_cuenta_mercadopago__artista UNIQUE (artista_id),
    CONSTRAINT fk_cuenta_mercadopago__artista FOREIGN KEY (artista_id) REFERENCES artista (id)
);

CREATE INDEX ix_cuenta_mercadopago__mp_user ON cuenta_mercadopago (mp_user_id);
