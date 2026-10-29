import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Campania, ESTADOS_CAMPANIA, Novedad } from 'app/campania/campania.model';
import { serverApiUrl } from 'app/config';
import { mensajeDeError } from 'app/shared/errores';

export interface AporteCampania {
  id: number;
  referencia: string;
  fan: string;
  email: string | null;
  recompensaId: number | null;
  recompensa: string | null;
  monto: number;
  fechaPago: string | null;
  entregado: boolean;
  fechaEntrega: string | null;
  direccionEnvio: string | null;
}

interface ResumenRecompensa {
  recompensa: string;
  aportes: number;
  entregados: number;
  monto: number;
  conRecompensa: boolean;
}

/**
 * Panel del artista sobre una campaña publicada: aportes, resumen por recompensa y CSV (RF-17), entregas (RF-18) y
 * novedades (RF-15).
 */
@Component({
  selector: 'jhi-panel-campania',
  imports: [CurrencyPipe, DatePipe, FormsModule, RouterLink],
  templateUrl: './panel-campania.html',
})
export default class PanelCampania implements OnInit {
  readonly id = input.required<string>();
  readonly campania = signal<Campania | null>(null);
  readonly aportes = signal<AporteCampania[]>([]);
  readonly novedades = signal<Novedad[]>([]);
  readonly error = signal<string | null>(null);
  readonly mensaje = signal<string | null>(null);
  readonly ocupado = signal(false);
  readonly estados = ESTADOS_CAMPANIA;
  titulo = '';
  texto = '';

  readonly total = computed(() => this.aportes().reduce((suma, a) => suma + a.monto, 0));

  readonly resumen = computed<ResumenRecompensa[]>(() => {
    const filas = new Map<string, ResumenRecompensa>();
    for (const a of this.aportes()) {
      const clave = a.recompensaId === null ? 'libre' : String(a.recompensaId);
      const fila = filas.get(clave) ?? {
        recompensa: a.recompensa ?? 'Aporte libre',
        aportes: 0,
        entregados: 0,
        monto: 0,
        conRecompensa: a.recompensaId !== null,
      };
      fila.aportes++;
      fila.entregados += a.entregado ? 1 : 0;
      fila.monto += a.monto;
      filas.set(clave, fila);
    }
    return [...filas.values()];
  });

  private readonly http = inject(HttpClient);

  ngOnInit(): void {
    this.http.get<Campania>(this.url()).subscribe(campania => {
      this.campania.set(campania);
      this.cargarNovedades(campania.slug);
    });
    this.http.get<AporteCampania[]>(`${this.url()}/aportes`).subscribe(aportes => this.aportes.set(aportes));
  }

  descargarCsv(): void {
    this.http.get(`${this.url()}/aportes.csv`, { responseType: 'blob' }).subscribe(csv => {
      const link = document.createElement('a');
      link.href = URL.createObjectURL(csv);
      link.download = `aportes-${this.campania()?.slug ?? this.id()}.csv`;
      link.click();
      URL.revokeObjectURL(link.href);
    });
  }

  marcar(aporte: AporteCampania, entregado: boolean): void {
    this.error.set(null);
    this.http.post<AporteCampania>(`${this.url()}/aportes/${aporte.id}/entregado`, { entregado }).subscribe({
      next: actualizado => {
        this.aportes.update(aportes => aportes.map(a => (a.id === actualizado.id ? actualizado : a)));
        // Con la última entrega la campaña pasa a cerrada.
        this.http.get<Campania>(this.url()).subscribe(campania => this.campania.set(campania));
      },
      error: (error: HttpErrorResponse) => this.error.set(mensajeDeError(error)),
    });
  }

  publicarNovedad(): void {
    this.error.set(null);
    this.mensaje.set(null);
    this.ocupado.set(true);
    this.http.post<Novedad>(`${this.url()}/novedades`, { titulo: this.titulo, texto: this.texto }).subscribe({
      next: novedad => {
        this.novedades.update(novedades => [novedad, ...novedades]);
        this.titulo = '';
        this.texto = '';
        this.mensaje.set('Novedad publicada: les mandamos un mail a los que aportaron.');
        this.ocupado.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(mensajeDeError(error));
        this.ocupado.set(false);
      },
    });
  }

  private cargarNovedades(slug: string): void {
    this.http.get<Novedad[]>(`${serverApiUrl}api/public/campanias/${slug}/novedades`).subscribe(novedades => this.novedades.set(novedades));
  }

  private url(): string {
    return `${serverApiUrl}api/mis-campanias/${this.id()}`;
  }
}
