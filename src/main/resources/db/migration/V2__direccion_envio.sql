-- RF-02: dirección de envío del fan (una por cuenta)

CREATE TABLE direccion_envio (
    id             BIGINT       NOT NULL,
    user_id        BIGINT       NOT NULL,
    calle          VARCHAR(100) NOT NULL,
    numero         VARCHAR(10)  NOT NULL,
    piso_depto     VARCHAR(20),
    localidad      VARCHAR(100) NOT NULL,
    provincia      VARCHAR(60)  NOT NULL,
    codigo_postal  VARCHAR(10)  NOT NULL,
    telefono       VARCHAR(30),
    CONSTRAINT pk_direccion_envio PRIMARY KEY (id),
    CONSTRAINT ux_direccion_envio__user UNIQUE (user_id),
    CONSTRAINT fk_direccion_envio__user FOREIGN KEY (user_id) REFERENCES jhi_user (id)
);
