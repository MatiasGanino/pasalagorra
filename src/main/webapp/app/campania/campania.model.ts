export type EstadoCampania = 'BORRADOR' | 'EN_REVISION' | 'PUBLICADA' | 'EXITOSA' | 'FALLIDA' | 'CERRADA';

export const ESTADOS_CAMPANIA: Record<EstadoCampania, { texto: string; clase: string }> = {
  BORRADOR: { texto: 'Borrador', clase: 'bg-secondary' },
  EN_REVISION: { texto: 'En revisión', clase: 'bg-warning text-dark' },
  PUBLICADA: { texto: 'Publicada', clase: 'bg-primary' },
  EXITOSA: { texto: 'Exitosa', clase: 'bg-success' },
  FALLIDA: { texto: 'No llegó a la meta', clase: 'bg-danger' },
  CERRADA: { texto: 'Cerrada', clase: 'bg-dark' },
};

export interface Recompensa {
  id: number;
  titulo: string;
  descripcion: string;
  montoMinimo: number;
  stock: number | null;
  cantidadVendida: number;
  disponibles: number | null;
  fechaEntregaEstimada: string;
  requiereEnvio: boolean;
}

/** Campaña completa, para el artista dueño y el administrador. */
export interface Campania {
  id: number;
  titulo: string;
  slug: string;
  descripcion: string;
  videoUrl: string | null;
  meta: number;
  fechaCierre: string;
  estado: EstadoCampania;
  motivoRechazo: string | null;
  fechaCreacion: string;
  fechaEnvioRevision: string | null;
  fechaPublicacion: string | null;
  nombreArtistico: string;
  artistaLogin: string;
  recompensas: Recompensa[];
  imagenes: { id: number; url: string }[];
}

export interface CampaniaPublica {
  id: number;
  slug: string;
  titulo: string;
  descripcion: string;
  videoYoutubeId: string | null;
  meta: number;
  recaudado: number;
  aportantes: number;
  porcentaje: number;
  diasRestantes: number;
  fechaCierre: string;
  estado: EstadoCampania;
  nombreArtistico: string;
  ciudad: string;
  provincia: string;
  comisionPorcentaje: number;
  imagenes: string[];
  recompensas: Recompensa[];
}

export interface CampaniaResumen {
  id: number;
  slug: string;
  titulo: string;
  nombreArtistico: string;
  portada: string | null;
  meta: number;
  recaudado: number;
  porcentaje: number;
  diasRestantes: number;
  estado: EstadoCampania;
}
