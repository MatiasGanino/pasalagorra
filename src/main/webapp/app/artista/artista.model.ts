import { HttpErrorResponse } from '@angular/common/http';

export type EstadoArtista = 'PENDIENTE' | 'APROBADO' | 'RECHAZADO';

export interface Artista {
  id: number;
  login: string;
  email: string;
  nombreArtistico: string;
  nombreCompleto: string;
  dni: string;
  cuit: string;
  ciudad: string;
  provincia: string;
  biografia: string | null;
  estado: EstadoArtista;
  motivoRechazo: string | null;
  fechaSolicitud: string;
  fechaRevision: string | null;
}

export const ESTADOS_ARTISTA: Record<EstadoArtista, { texto: string; clase: string }> = {
  PENDIENTE: { texto: 'Pendiente de revisión', clase: 'bg-warning text-dark' },
  APROBADO: { texto: 'Aprobado', clase: 'bg-success' },
  RECHAZADO: { texto: 'Rechazado', clase: 'bg-danger' },
};

const ERRORES: Partial<Record<string, string>> = {
  'error.validation': 'Revisá los datos del formulario.',
  'error.dniinvalido': 'El DNI tiene que tener 7 u 8 dígitos.',
  'error.cuitinvalido': 'El CUIT no es válido: revisá los números.',
  'error.cuitnocoincide': 'El CUIT no corresponde al DNI.',
  'error.dniusado': 'Ya hay un artista registrado con ese DNI.',
  'error.cuitusado': 'Ya hay un artista registrado con ese CUIT.',
  'error.yaaprobado': 'La cuenta de artista ya está aprobada.',
  'error.motivorequerido': 'Indicá el motivo del rechazo.',
  'error.nopendiente': 'La solicitud ya fue revisada.',
  'error.noexiste': 'La solicitud no existe.',
};

/**
 * El backend indica en `message` por qué rechazó la operación (CUIT inválido, DNI repetido, etc.).
 */
export function mensajeDeError(error: HttpErrorResponse): string {
  return ERRORES[error.error?.message] ?? 'No se pudo completar la operación. Probá de nuevo.';
}
