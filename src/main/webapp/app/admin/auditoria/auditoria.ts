import { DatePipe } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { serverApiUrl } from 'app/config';

type Entidad = 'CAMPANIA' | 'ARTISTA' | 'APORTE' | 'RECLAMO';

interface CambioEstado {
  id: number;
  entidad: Entidad;
  entidadId: number;
  estadoAnterior: string | null;
  estadoNuevo: string;
  usuario: string;
  detalle: string | null;
  fecha: string;
}

/**
 * Historial de cambios de estado (RNF-13): los últimos 200, de todo o de una entidad.
 */
@Component({
  selector: 'jhi-admin-auditoria',
  imports: [DatePipe, FormsModule],
  template: `
    <h2>Auditoría</h2>

    <form class="row g-2 mb-3" (ngSubmit)="cargar()">
      <div class="col-sm-4">
        <select class="form-select" name="entidad" [(ngModel)]="entidad" aria-label="Entidad">
          <option value="">Todo</option>
          @for (e of entidades; track e) {
            <option [value]="e">{{ nombres[e] }}</option>
          }
        </select>
      </div>
      <div class="col-sm-3">
        <input
          class="form-control"
          name="id"
          type="number"
          min="1"
          [(ngModel)]="id"
          [disabled]="!entidad"
          placeholder="Id"
          aria-label="Id"
        />
      </div>
      <div class="col-sm-2 d-grid">
        <button type="submit" class="btn btn-outline-primary">Filtrar</button>
      </div>
    </form>

    <div class="table-responsive">
      <table class="table table-striped align-middle small">
        <thead>
          <tr>
            <th scope="col">Fecha</th>
            <th scope="col">Qué</th>
            <th scope="col">Cambio</th>
            <th scope="col">Quién</th>
            <th scope="col">Detalle</th>
          </tr>
        </thead>
        <tbody>
          @for (cambio of cambios(); track cambio.id) {
            <tr>
              <td class="text-nowrap">{{ cambio.fecha | date: 'dd/MM/yyyy HH:mm:ss' }}</td>
              <td>{{ nombres[cambio.entidad] }} {{ cambio.entidadId }}</td>
              <td>
                {{ cambio.estadoAnterior ?? '—' }} → <strong>{{ cambio.estadoNuevo }}</strong>
              </td>
              <td>{{ cambio.usuario }}</td>
              <td>{{ cambio.detalle }}</td>
            </tr>
          } @empty {
            <tr>
              <td colspan="5" class="text-muted">Sin cambios registrados.</td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export default class AdminAuditoria implements OnInit {
  readonly cambios = signal<CambioEstado[]>([]);
  readonly entidades: Entidad[] = ['CAMPANIA', 'ARTISTA', 'APORTE', 'RECLAMO'];
  readonly nombres: Record<Entidad, string> = { CAMPANIA: 'Campaña', ARTISTA: 'Artista', APORTE: 'Aporte', RECLAMO: 'Reclamo' };
  entidad: Entidad | '' = '';
  id: number | null = null;

  private readonly http = inject(HttpClient);

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    let params = new HttpParams();
    if (this.entidad) {
      params = params.set('entidad', this.entidad);
      if (this.id) {
        params = params.set('id', this.id);
      }
    }
    this.http.get<CambioEstado[]>(`${serverApiUrl}api/admin/auditoria`, { params }).subscribe(cambios => this.cambios.set(cambios));
  }
}
