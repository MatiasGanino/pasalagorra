import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { NgbPagination } from '@ng-bootstrap/ng-bootstrap/pagination';

import { ESTADOS_APORTE, EstadoAporte } from 'app/aporte/aporte.model';
import { TOTAL_COUNT_RESPONSE_HEADER, serverApiUrl } from 'app/config';

interface AdminAporte {
  id: number;
  referencia: string;
  pagoId: string | null;
  campaniaTitulo: string;
  campaniaSlug: string;
  fanLogin: string;
  fanEmail: string | null;
  recompensa: string | null;
  monto: number;
  estado: EstadoAporte;
  entregado: boolean;
  fechaCreacion: string;
  fechaPago: string | null;
  fechaReembolso: string | null;
}

/**
 * Todos los aportes de la plataforma, para el administrador (RF-21).
 */
@Component({
  selector: 'jhi-admin-aportes',
  imports: [CurrencyPipe, DatePipe, FormsModule, NgbPagination, RouterLink],
  template: `
    <h2>Aportes</h2>

    <form class="row g-2 mb-3" (ngSubmit)="buscar()">
      <div class="col-sm-4">
        <select class="form-select" name="estado" [(ngModel)]="estado" (change)="buscar()" aria-label="Estado">
          <option value="">Todos los estados</option>
          @for (e of filtros; track e) {
            <option [value]="e">{{ estados[e].texto }}</option>
          }
        </select>
      </div>
      <div class="col-sm-6">
        <input class="form-control" name="texto" [(ngModel)]="texto" placeholder="Comprobante, usuario o campaña" aria-label="Buscar" />
      </div>
      <div class="col-sm-2 d-grid">
        <button type="submit" class="btn btn-outline-primary">Buscar</button>
      </div>
    </form>

    <p class="small text-muted">{{ total() }} aportes</p>
    <div class="table-responsive">
      <table class="table table-striped align-middle small">
        <thead>
          <tr>
            <th scope="col">Fecha</th>
            <th scope="col">Campaña</th>
            <th scope="col">Fan</th>
            <th scope="col">Recompensa</th>
            <th scope="col">Monto</th>
            <th scope="col">Estado</th>
            <th scope="col">Comprobante / pago</th>
          </tr>
        </thead>
        <tbody>
          @for (aporte of aportes(); track aporte.id) {
            <tr>
              <td>{{ aporte.fechaCreacion | date: 'dd/MM/yyyy HH:mm' }}</td>
              <td>
                <a [routerLink]="['/campania', aporte.campaniaSlug]">{{ aporte.campaniaTitulo }}</a>
              </td>
              <td>
                {{ aporte.fanLogin }}<br />
                <span class="text-muted">{{ aporte.fanEmail }}</span>
              </td>
              <td>
                {{ aporte.recompensa ?? 'Aporte libre' }}
                @if (aporte.entregado) {
                  <span class="badge bg-success">Entregada</span>
                }
              </td>
              <td>{{ aporte.monto | currency: 'ARS' : 'symbol-narrow' : '1.0-2' }}</td>
              <td>
                <span class="badge {{ estados[aporte.estado].clase }}">{{ estados[aporte.estado].texto }}</span>
              </td>
              <td class="text-break">
                <code>{{ aporte.referencia }}</code>
                @if (aporte.pagoId) {
                  <br /><span class="text-muted">{{ aporte.pagoId }}</span>
                }
              </td>
            </tr>
          } @empty {
            <tr>
              <td colspan="7" class="text-muted">No hay aportes con estos filtros.</td>
            </tr>
          }
        </tbody>
      </table>
    </div>

    @if (total() > porPagina) {
      <div class="d-flex justify-content-center">
        <ngb-pagination
          [collectionSize]="total()"
          [page]="pagina()"
          [pageSize]="porPagina"
          [maxSize]="5"
          [rotate]="true"
          [boundaryLinks]="true"
          (pageChange)="irA($event)"
        />
      </div>
    }
  `,
})
export default class AdminAportes implements OnInit {
  readonly aportes = signal<AdminAporte[]>([]);
  readonly total = signal(0);
  readonly pagina = signal(1);
  readonly estados = ESTADOS_APORTE;
  readonly filtros = Object.keys(ESTADOS_APORTE) as EstadoAporte[];
  readonly porPagina = 50;
  estado: EstadoAporte | '' = '';
  texto = '';

  private readonly http = inject(HttpClient);

  ngOnInit(): void {
    this.cargar();
  }

  buscar(): void {
    this.pagina.set(1);
    this.cargar();
  }

  irA(pagina: number): void {
    this.pagina.set(pagina);
    this.cargar();
  }

  private cargar(): void {
    let params = new HttpParams().set('page', this.pagina() - 1).set('size', this.porPagina);
    if (this.estado) {
      params = params.set('estado', this.estado);
    }
    if (this.texto.trim()) {
      params = params.set('texto', this.texto.trim());
    }
    this.http.get<AdminAporte[]>(`${serverApiUrl}api/admin/aportes`, { params, observe: 'response' }).subscribe(respuesta => {
      this.aportes.set(respuesta.body ?? []);
      this.total.set(Number(respuesta.headers.get(TOTAL_COUNT_RESPONSE_HEADER)));
    });
  }
}
