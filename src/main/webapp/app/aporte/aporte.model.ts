import { EstadoCampania } from 'app/campania/campania.model';

export type EstadoAporte = 'PENDIENTE' | 'APROBADO' | 'RECHAZADO' | 'REEMBOLSADO' | 'REEMBOLSO_PENDIENTE';

export const ESTADOS_APORTE: Record<EstadoAporte, { texto: string; clase: string }> = {
  PENDIENTE: { texto: 'Esperando el pago', clase: 'bg-secondary' },
  APROBADO: { texto: 'Pago confirmado', clase: 'bg-success' },
  RECHAZADO: { texto: 'Pago rechazado', clase: 'bg-danger' },
  REEMBOLSADO: { texto: 'Reembolsado', clase: 'bg-info text-dark' },
  REEMBOLSO_PENDIENTE: { texto: 'Reembolso en curso', clase: 'bg-warning text-dark' },
};

export interface Aporte {
  id: number;
  referencia: string;
  campaniaTitulo: string;
  campaniaSlug: string;
  campaniaEstado: EstadoCampania;
  recompensa: string | null;
  monto: number;
  estado: EstadoAporte;
  fechaCreacion: string;
  fechaPago: string | null;
  entregado: boolean;
  reclamosAbiertos: TipoReclamo[];
}

export type TipoReclamo = 'INCUMPLIMIENTO' | 'ARREPENTIMIENTO';

export type EstadoReclamo = 'ABIERTO' | 'RESUELTO';

export interface Reclamo {
  id: number;
  tipo: TipoReclamo;
  texto: string;
  estado: EstadoReclamo;
  respuesta: string | null;
  fecha: string;
  fechaResolucion: string | null;
  aporteReferencia: string;
  monto: number;
  recompensa: string | null;
  entregado: boolean;
  fechaPago: string | null;
  campaniaTitulo: string;
  campaniaSlug: string;
  nombreArtistico: string;
  fanLogin: string;
  fanEmail: string | null;
}
