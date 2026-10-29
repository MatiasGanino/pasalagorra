# Pendientes

Lo que falta para salir a producción. Casi todo necesita cuentas o datos externos.

## Necesitan cuentas externas

- **Mercado Pago (etapa 6).** Implementar `PasarelaPago` con Checkout Pro y split (marketplace): OAuth de cada artista
  para vincular su cuenta, `crearCobro` con la comisión en `marketplace_fee`, webhook que llame a
  `PagoService.confirmarPago` (validando la firma) y `reembolsar` con la API de devoluciones. Se activa con
  `application.pagos.pasarela`. Con split, los reembolsos (todo o nada y arrepentimiento) salen de la cuenta del
  artista: definir qué pasa si no tiene saldo (hoy queda `REEMBOLSO_PENDIENTE` y se reintenta).
- **Login con Google.** OAuth2 client de Spring Security que emita el mismo JWT y vincule la cuenta por email.
- **Proveedor de mail** (SES, Brevo, Mailgun u otro): configurar `spring.mail` en prod y el remitente
  (`jhipster.mail.from`), con SPF/DKIM del dominio. En dev los mails siguen yendo al log.
- **Hosting y deploy.** Servidor con Java 21 y PostgreSQL, dominio y HTTPS. Definir `jhipster.mail.base-url` (también la
  usan las meta tags de Open Graph), el secreto del JWT, la carpeta de imágenes (`application.imagenes.directorio`) con
  backup, y backups de la base. Agregar el deploy al CI.

## Sin cuentas externas, para más adelante

- **Términos y condiciones**: es un borrador; lo tiene que revisar un abogado, junto con el botón de arrepentimiento
  sin sesión (RNF-12) y la política de datos personales (Ley 25.326, registro de la base en la AAIP).
- **Comisión por campaña**: el reporte de RF-23 usa el porcentaje configurado hoy. Si el porcentaje cambia, guardarlo
  en cada campaña al publicarla.
- **Facturación** de la comisión (AFIP/ARCA).
- **Contacto con el artista** por un reclamo: hoy lo hace el administrador por fuera de la plataforma.
- **Imágenes**: hoy en disco; si se pasa a varios servidores, mover a un almacenamiento de objetos.
