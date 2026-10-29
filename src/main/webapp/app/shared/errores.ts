import { HttpErrorResponse } from '@angular/common/http';

/**
 * Textos de las reglas de negocio del backend: responde 400 con `"message": "error.<clave>"`.
 */
const ERRORES: Partial<Record<string, string>> = {
  'error.validation': 'Revisá los datos del formulario.',
  'error.noexiste': 'No existe o no tenés acceso.',
  'error.motivorequerido': 'Indicá el motivo del rechazo.',
  // Artistas (RF-03)
  'error.dniinvalido': 'El DNI tiene que tener 7 u 8 dígitos.',
  'error.cuitinvalido': 'El CUIT no es válido: revisá los números.',
  'error.cuitnocoincide': 'El CUIT no corresponde al DNI.',
  'error.dniusado': 'Ya hay un artista registrado con ese DNI.',
  'error.cuitusado': 'Ya hay un artista registrado con ese CUIT.',
  'error.yaaprobado': 'La cuenta de artista ya está aprobada.',
  'error.nopendiente': 'La solicitud ya fue revisada.',
  // Campañas (RF-04 a RF-07)
  'error.noesartista': 'Necesitás una cuenta de artista aprobada para crear campañas.',
  'error.videoinvalido': 'El video tiene que ser un link de YouTube.',
  'error.noeditable': 'Solo se puede modificar una campaña en borrador.',
  'error.sinimagenes': 'Subí al menos una imagen antes de enviarla a revisión.',
  'error.fechacierre': 'La fecha de cierre tiene que ser después de hoy y como máximo dentro de 60 días.',
  'error.entregaantesdelcierre': 'La entrega estimada de cada recompensa tiene que ser después del cierre.',
  'error.imagentipo': 'La imagen tiene que ser JPG, PNG o WebP.',
  'error.imagenesmaximas': 'Una campaña puede tener hasta 6 imágenes.',
  // Aportes (RF-10 a RF-13)
  'error.campaniacerrada': 'La campaña ya no está recibiendo aportes.',
  'error.propiacampania': 'No podés aportar a tu propia campaña.',
  'error.agotada': 'Esa recompensa se agotó.',
  'error.montominimo': 'El monto es menor al mínimo de la recompensa.',
  'error.sindireccion': 'Esta recompensa se envía por correo: cargá tu dirección de envío antes de aportar.',
  // Novedades, entregas y reclamos (RF-15 a RF-22)
  'error.nopublica': 'Las novedades se publican cuando la campaña ya está publicada.',
  'error.noentregable': 'Las entregas se marcan mientras la campaña está exitosa (todavía no cerrada).',
  'error.sinrecompensa': 'Un aporte libre no tiene recompensa que entregar.',
  'error.noreclamable': 'Solo se reclama la recompensa de un aporte confirmado en una campaña que llegó a la meta.',
  'error.reclamoabierto': 'Ya tenés un reclamo abierto sobre este aporte.',
  'error.respuestarequerida': 'Escribí la respuesta para el fan.',
  'error.yaresuelto': 'El reclamo ya está resuelto.',
  // Mercado Pago (etapa 6)
  'error.sinmercadopago': 'Para cobrar, el artista tiene que vincular su cuenta de Mercado Pago.',
  'error.vinculacionvencida': 'El link para vincular Mercado Pago venció. Probá de nuevo desde Mis campañas.',
  'error.mercadopagoapagado': 'Mercado Pago no está activo en este entorno.',
  // Arrepentimiento (RNF-12)
  'error.noarrepentible': 'Solo se puede revocar un aporte confirmado dentro de los 10 días del pago.',
  'error.noreembolsable': 'Solo se reembolsa un aporte confirmado, desde un pedido de arrepentimiento.',
  'error.reembolsofallido': 'Mercado Pago no pudo hacer el reembolso. Probá de nuevo más tarde.',
};

export function mensajeDeError(error: HttpErrorResponse): string {
  if (error.status === 413) {
    return 'La imagen pesa más de 5 MB.';
  }
  return ERRORES[error.error?.message] ?? 'No se pudo completar la operación. Probá de nuevo.';
}
