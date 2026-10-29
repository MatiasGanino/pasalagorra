# Pendientes

El código de todas las etapas está hecho. Lo que falta depende de cuentas, datos o decisiones externas.

## Para salir a producción

- **Servidor y dominio.** Elegir dónde alojarlo y seguir [`docs/DEPLOY.md`](../docs/DEPLOY.md): Java 21, PostgreSQL,
  nginx con HTTPS, variables de entorno y backups.
- **Mercado Pago.** Crear la aplicación (modelo marketplace), cargar las credenciales y el webhook, y **probar todo con
  las credenciales de prueba**: el código está testeado contra una API simulada, no contra Mercado Pago real.
  Decidir qué hacer si un artista no tiene saldo para una devolución (hoy queda `REEMBOLSO_PENDIENTE` y se reintenta).
- **Proveedor de mail.** Una cuenta SMTP y el dominio con SPF y DKIM para que los mails no caigan en spam.
- **Google** (opcional). Crear el cliente OAuth y cargar las dos variables.

## Legales y administrativos

- **Términos y condiciones**: es un borrador; lo tiene que revisar un abogado, junto con el botón de arrepentimiento
  sin sesión (RNF-12) y la política de datos personales (Ley 25.326, registro de la base en la AAIP).
- **Facturación** de la comisión (ARCA): necesita el certificado y definir el régimen.

## Para más adelante

- **Contacto con el artista** por un reclamo: hoy lo hace el administrador por fuera de la plataforma.
- **Imágenes**: hoy en disco; si se pasa a varios servidores, mover a un almacenamiento de objetos.
- **Deploy automático** desde GitHub Actions cuando esté elegido el servidor.
