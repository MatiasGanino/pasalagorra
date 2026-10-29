# RNF-13 · Auditoría de cambios de estado

## Historia

Como administrador quiero saber quién cambió el estado de una campaña, un artista, un aporte o un reclamo, y cuándo.

## Decisiones

- Tabla `cambio_estado` (migración `V7`): entidad, id, estado anterior y nuevo, usuario, detalle y fecha. Solo se
  agregan filas.
- La anota `AuditoriaService.registrar` desde cada servicio que cambia un estado, en la misma transacción
  (`Propagation.MANDATORY`): si la operación se deshace, el registro también. Sin listeners de JPA ni Envers.
- El usuario es el login de la sesión, o `sistema` para la tarea de cierre y las notificaciones de la pasarela.
- Qué se registra:
  - Campaña: alta (→ borrador), envío a revisión, aprobación, rechazo (con el motivo), cierre exitoso o fallido (con lo
    recaudado), fallida → cerrada y exitosa → cerrada por entregas.
  - Artista: solicitud, aprobación y rechazo.
  - Aporte: pago aprobado o rechazado (con el id del pago), reembolso hecho o pendiente (con el motivo). No la creación:
    los pendientes que nunca se pagan serían ruido.
  - Reclamo: apertura (con el tipo) y resolución.
- **Administración → Auditoría** (`GET /api/admin/auditoria?entidad=CAMPANIA&id=12`) muestra los últimos 200 cambios,
  de todo o filtrados.

## Criterios de aceptación

- [ ] Dada una campaña aprobada por el administrador y cerrada por la tarea, el historial muestra los tres cambios con
      `admin` y `sistema`.
