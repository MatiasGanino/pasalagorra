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
  'error.noenrevision': 'La campaña no está en revisión.',
};

export function mensajeDeError(error: HttpErrorResponse): string {
  if (error.status === 413) {
    return 'La imagen pesa más de 5 MB.';
  }
  return ERRORES[error.error?.message] ?? 'No se pudo completar la operación. Probá de nuevo.';
}
