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
