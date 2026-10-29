# Pasalagorra

Plataforma argentina de crowdfunding de recompensas para proyectos musicales: el artista publica una campaña con meta,
plazo y niveles de recompensa, y los fans aportan en pesos. Modelo **todo o nada**: si la campaña no llega a la meta, se
reembolsa a todos.

Roles: `ROLE_USER` (fan: toda cuenta registrada), `ROLE_ARTISTA` (se suma cuando el administrador aprueba el alta de
artista) y `ROLE_ADMIN`.

En dev, los mails no se envían: se escriben en el log del backend, con los links de activación y de recuperación.

## Qué hace

| Etapa | Requerimientos                | Qué incluye                                                                                                                                                                       |
| ----- | ----------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1     | RF-01 a RF-03                 | Cuentas con activación por mail, dirección de envío y alta de artista con DNI/CUIT aprobada por el administrador.                                                                 |
| 2     | RF-04 a RF-08                 | Campañas con recompensas e imágenes, revisión del administrador y página pública `/campania/{slug}`.                                                                              |
| 3     | RF-10 a RF-13                 | Aportes con pasarela de pago simulada, comprobante por mail, stock sin sobreventa y cierre todo o nada con reembolsos.                                                            |
| 4     | RF-15 a RF-22                 | Novedades de la campaña, panel del artista con CSV, entrega de recompensas y reclamos de los fans.                                                                                |
| 5     | RF-21 a RF-24, RNF-12, RNF-13 | Panel del administrador (aportes, recaudación y comisiones por mes), vista previa al compartir (Open Graph), términos, botón de arrepentimiento y auditoría de cambios de estado. |

| 6 | RF-10, RF-01 | Mercado Pago con split (vinculación OAuth de cada artista, webhook firmado, devoluciones), login con Google y comisión fijada en cada campaña. |

Para producción hacen falta cuentas externas (servidor, Mercado Pago, mail, Google): ver
[`docs/DEPLOY.md`](docs/DEPLOY.md) y [`specs/PENDIENTES.md`](specs/PENDIENTES.md).

## Probarlo en tu máquina

Con el perfil dev se cargan datos de demostración (campañas con imágenes, aportes, novedades y un reclamo):

| Usuario           | Contraseña | Qué ve                                                                                                |
| ----------------- | ---------- | ----------------------------------------------------------------------------------------------------- |
| `admin`           | `admin`    | Administración: revisión de campañas (hay una en revisión), aportes, reclamos, recaudación, auditoría |
| `user`            | `user`     | Artista "Los Pasalagorra": panel de campañas, entregas, novedades                                     |
| `fan`             | `fan123`   | Fan: Mis aportes, una recompensa entregada, reclamos, arrepentimiento                                 |
| `murga`, `cumbia` | `user`     | Otros dos artistas                                                                                    |

Los pagos usan la pasarela simulada: al aportar aparece un botón "Pagar". Los mails se escriben en el log del backend.

## Cómo está organizado

Aplicación JHipster 9 (monolito): API REST en Spring Boot con JWT y el frontend en Angular, en el mismo proyecto.

- `src/main/java/ar/com/pasalagorra`: backend.
- `src/main/webapp`: frontend Angular.
- `src/main/resources/db/migration`: migraciones Flyway (`V<n>__descripcion.sql`). Las entidades nuevas se generan con
  `--skip-db-changelog` y la migración se escribe a mano.
- `src/main/resources/db/dev`: usuarios de prueba (`admin`/`admin`, `user`/`user`). Solo la cargan los perfiles dev y
  test; en prod no existen.
- `specs/`: una especificación por requerimiento (`RF-xx.md`, `RNF-xx.md`), escrita antes de programarlo, y los
  pendientes (`PENDIENTES.md`).

## Diseño

- Colores, tipografías y formas: `src/main/webapp/content/scss/_bootstrap-variables.scss` (violeta para acciones, coral
  "gorra" para acentos y degradés, tinta para textos). Títulos en Bricolage Grotesque e interfaz en Inter, autoalojadas
  con `@fontsource-variable` porque la CSP solo permite fuentes del mismo origen.
- Piezas compartidas en `content/scss/global.scss`: tarjeta de campaña, chips, paneles, línea de tiempo, esqueletos de
  carga y la barra para aportar en el celular. El logo es el componente `jhi-marca` (también `favicon.svg`).
- Animaciones: la directiva `jhiAparecer` hace aparecer los bloques al hacer scroll. Todas las animaciones se apagan si
  el sistema pide reducir el movimiento.

## Desarrollo local

Requisitos: Java 21, Node 24 y PostgreSQL. No se usa Docker.

1. **Base de datos** (PostgreSQL en `localhost:5432`), una sola vez:

   ```sql
   CREATE ROLE pasalagorra LOGIN PASSWORD 'pasalagorra';
   CREATE DATABASE pasalagorra OWNER pasalagorra;
   CREATE DATABASE pasalagorra_test OWNER pasalagorra;
   ```

2. **Backend** en `http://localhost:8080` (al arrancar, Flyway crea o actualiza el esquema):

   ```bash
   ./mvnw -Dskip.npm -Dskip.installnodenpm
   ```

3. **Frontend** en `http://localhost:4200`, con recarga en vivo:

   ```bash
   npm install
   npm start
   ```

Las meta tags de Open Graph de `/campania/{slug}` (RF-24) las agrega el backend, así que con `npm start` no se ven:
para probarlas, compilá el frontend (`npm run webapp:build`) y abrí la campaña en `http://localhost:8080`.

Las credenciales de la base de dev están en `src/main/resources/config/application-secret-samples.yml`, que el perfil
dev carga después de `application-dev.yml`.

### Antes de commitear

- Backend: `./mvnw -Dskip.npm -Dskip.installnodenpm verify` (unitarios y de integración, contra `pasalagorra_test`).
- Frontend: `npm test` (lint y tests).

El CI (`.github/workflows/ci.yml`) corre lo mismo en cada pull request.

## Versiones

La versión va igual en `pom.xml` y `package.json`.
