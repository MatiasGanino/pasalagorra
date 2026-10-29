# Pasalagorra

Plataforma argentina de crowdfunding de recompensas para proyectos musicales: el artista publica una campaña con meta,
plazo y niveles de recompensa, y los fans aportan en pesos. Modelo **todo o nada**: si la campaña no llega a la meta, se
reembolsa a todos.

Roles: `ROLE_ADMIN` (por ahora, junto con `ROLE_USER` de JHipster; fan y artista llegan en la etapa de Cuentas).

## Cómo está organizado

Aplicación JHipster 9 (monolito): API REST en Spring Boot con JWT y el frontend en Angular, en el mismo proyecto.

- `src/main/java/ar/com/pasalagorra`: backend.
- `src/main/webapp`: frontend Angular.
- `src/main/resources/db/migration`: migraciones Flyway (`V<n>__descripcion.sql`). Las entidades nuevas se generan con
  `--skip-db-changelog` y la migración se escribe a mano.
- `src/main/resources/db/dev`: usuarios de prueba (`admin`/`admin`, `user`/`user`). Solo la cargan los perfiles dev y
  test; en prod no existen.
- `specs/`: una especificación por requerimiento (`RF-xx.md`), escrita antes de programarlo.

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

Las credenciales de la base de dev están en `src/main/resources/config/application-secret-samples.yml`, que el perfil
dev carga después de `application-dev.yml`.

### Antes de commitear

- Backend: `./mvnw -Dskip.npm -Dskip.installnodenpm verify` (unitarios y de integración, contra `pasalagorra_test`).
- Frontend: `npm test` (lint y tests).

El CI (`.github/workflows/ci.yml`) corre lo mismo en cada pull request.

## Versiones

La versión va igual en `pom.xml` y `package.json`.
