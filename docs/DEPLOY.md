# Deploy y puesta en producción

Guía para subir pasalagorra a un servidor propio (un VPS con Ubuntu 24.04 alcanza para empezar: 2 GB de RAM, 1 vCPU).
Todo lo sensible va en variables de entorno, nunca en el repositorio.

## 1. Lo que necesitás tener

| Qué                                                                 | Para qué               | Dónde se saca                                                                                                                            |
| ------------------------------------------------------------------- | ---------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| Servidor con Ubuntu y un dominio (por ejemplo `pasalagorra.com.ar`) | Correr la aplicación   | Cualquier VPS (DigitalOcean, Hetzner, Donweb, etc.) y NIC Argentina                                                                      |
| Aplicación de Mercado Pago                                          | Cobrar con split       | [Mercado Pago Developers](https://www.mercadopago.com.ar/developers/panel/app) → Crear aplicación → Checkout Pro, modelo **marketplace** |
| Cuenta SMTP                                                         | Mandar los mails       | Brevo, Amazon SES, Mailgun o similar                                                                                                     |
| Cliente OAuth de Google (opcional)                                  | "Continuar con Google" | [Google Cloud Console](https://console.cloud.google.com/apis/credentials) → Credenciales → ID de cliente OAuth (aplicación web)          |

### Mercado Pago

En la aplicación de Mercado Pago:

1. **URL de redireccionamiento** (OAuth): `https://TU-DOMINIO/artista/mercadopago`.
2. **Webhooks** → URL de producción `https://TU-DOMINIO/api/public/mercadopago/webhook`, evento **Pagos**. Copiá la
   **clave secreta** que te muestra: es `MP_WEBHOOK_SECRET`.
3. De **Credenciales de producción** sacá el `Access Token`, el `Client ID` y el `Client Secret`.
4. Antes de producción, probá todo con las **credenciales de prueba** y usuarios de prueba (un vendedor para el
   artista y un comprador para el fan).

Cada artista vincula su cuenta desde **Mis campañas → Vincular Mercado Pago**; sin eso no puede enviar campañas a
revisión. La comisión de la plataforma se cobra en origen (`marketplace_fee`).

### Google (opcional)

En el cliente OAuth: **URI de redireccionamiento autorizado** `https://TU-DOMINIO/login/oauth2/code/google`.

## 2. Preparar el servidor

```bash
sudo apt update && sudo apt install -y openjdk-21-jre-headless postgresql nginx certbot python3-certbot-nginx
sudo -u postgres psql -c "CREATE ROLE pasalagorra LOGIN PASSWORD 'UNA-CLAVE-LARGA'"
sudo -u postgres psql -c "CREATE DATABASE pasalagorra OWNER pasalagorra"
sudo useradd --system --home /opt/pasalagorra --shell /usr/sbin/nologin pasalagorra
sudo mkdir -p /opt/pasalagorra /var/lib/pasalagorra/imagenes /etc/pasalagorra
sudo chown -R pasalagorra: /opt/pasalagorra /var/lib/pasalagorra
```

## 3. Variables de entorno

`/etc/pasalagorra/pasalagorra.env` (permisos `600`, dueño root):

```bash
SPRING_PROFILES_ACTIVE=prod
BASE_URL=https://pasalagorra.com.ar

DB_URL=jdbc:postgresql://localhost:5432/pasalagorra
DB_USERNAME=pasalagorra
DB_PASSWORD=UNA-CLAVE-LARGA

# openssl rand -base64 64 | tr -d '\n'
JWT_SECRET=...

# Primer administrador (se crea al arrancar si no hay ninguno)
ADMIN_EMAIL=vos@tu-mail.com
ADMIN_PASSWORD=una-clave-de-8-o-mas

MAIL_HOST=smtp-relay.brevo.com
MAIL_PORT=587
MAIL_USERNAME=...
MAIL_PASSWORD=...
MAIL_FROM=pasalagorra <no-responder@pasalagorra.com.ar>

IMAGENES_DIR=/var/lib/pasalagorra/imagenes

PASARELA=mercadopago
MP_ACCESS_TOKEN=APP_USR-...
MP_CLIENT_ID=...
MP_CLIENT_SECRET=...
MP_WEBHOOK_SECRET=...
# openssl rand -base64 32   (¡guardala! sin ella no se pueden leer los tokens de los artistas)
MP_CLAVE_CIFRADO=...

# Opcional: login con Google
APPLICATION_GOOGLE_CLIENT_ID=...
APPLICATION_GOOGLE_CLIENT_SECRET=...
```

Para un entorno de prueba sin Mercado Pago: `PASARELA=simulada` (los pagos se aprueban con un botón: **nunca en
producción**).

## 4. Compilar y subir

En tu máquina (o en el CI):

```bash
./mvnw -Pprod -DskipTests clean package
scp target/pasalagorra-*.jar servidor:/opt/pasalagorra/pasalagorra.jar
```

El jar trae el frontend compilado. Flyway crea o actualiza la base al arrancar.

## 5. Servicio con systemd

`/etc/systemd/system/pasalagorra.service`:

```ini
[Unit]
Description=pasalagorra
After=network.target postgresql.service

[Service]
User=pasalagorra
WorkingDirectory=/opt/pasalagorra
EnvironmentFile=/etc/pasalagorra/pasalagorra.env
ExecStart=/usr/bin/java -Xmx512m -jar /opt/pasalagorra/pasalagorra.jar
Restart=always
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload && sudo systemctl enable --now pasalagorra
journalctl -u pasalagorra -f
```

## 6. nginx con HTTPS

`/etc/nginx/sites-available/pasalagorra`:

```nginx
server {
    server_name pasalagorra.com.ar;
    client_max_body_size 6m;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/pasalagorra /etc/nginx/sites-enabled/
sudo certbot --nginx -d pasalagorra.com.ar
```

## 7. Backups

Diarios, con una copia fuera del servidor:

```bash
# /etc/cron.daily/pasalagorra-backup
pg_dump -U pasalagorra pasalagorra | gzip > /var/backups/pasalagorra-$(date +%F).sql.gz
tar czf /var/backups/imagenes-$(date +%F).tgz -C /var/lib/pasalagorra imagenes
find /var/backups -name 'pasalagorra-*' -mtime +14 -delete
```

## 8. Después del primer arranque

1. Entrá con `ADMIN_EMAIL` / `ADMIN_PASSWORD` y cambiá la contraseña.
2. Revisá que llegue un mail (por ejemplo, registrando una cuenta de prueba).
3. Con las credenciales de prueba de Mercado Pago: vinculá un artista, publicá una campaña, aportá con el comprador de
   prueba y fijate que el aporte quede confirmado (Administración → Aportes) y que el webhook figure como entregado en el
   panel de Mercado Pago.
