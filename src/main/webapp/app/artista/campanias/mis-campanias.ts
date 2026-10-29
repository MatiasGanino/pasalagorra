import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Campania, ESTADOS_CAMPANIA } from 'app/campania/campania.model';
import { serverApiUrl } from 'app/config';

@Component({
  selector: 'jhi-mis-campanias',
  imports: [CurrencyPipe, DatePipe, RouterLink],
  template: `
    <div class="d-flex justify-content-between align-items-center mb-3">
      <h2 class="mb-0">Mis campañas</h2>
      <a class="btn btn-primary" routerLink="/artista/campanias/nueva">Nueva campaña</a>
    </div>

    @if (mercadoPago(); as mp) {
      @if (mp.habilitado && !mp.vinculada) {
        <div class="panel-suave d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
          <div>
            <strong>Vinculá tu cuenta de Mercado Pago</strong>
            <p class="small text-body-secondary mb-0">
              Los aportes llegan directo a tu cuenta, descontada la comisión. Sin vincularla no podés enviar campañas a revisión.
            </p>
          </div>
          <button type="button" class="btn btn-primary" (click)="vincularMercadoPago()">Vincular Mercado Pago</button>
        </div>
      } @else if (mp.vinculada) {
        <p class="small mb-3"><span class="chip chip-exito">✓ Mercado Pago vinculado</span></p>
      }
    }

    @if (campanias(); as campanias) {
      @if (campanias.length === 0) {
        <p class="text-muted">Todavía no creaste ninguna campaña.</p>
      } @else {
        <div class="table-responsive">
          <table class="table table-striped align-middle">
            <thead>
              <tr>
                <th scope="col">Campaña</th>
                <th scope="col">Estado</th>
                <th scope="col">Meta</th>
                <th scope="col">Cierre</th>
                <th scope="col"></th>
              </tr>
            </thead>
            <tbody>
              @for (campania of campanias; track campania.id) {
                <tr>
                  <td>{{ campania.titulo }}</td>
                  <td>
                    <span class="badge {{ estados[campania.estado].clase }}">{{ estados[campania.estado].texto }}</span>
                  </td>
                  <td>{{ campania.meta | currency: 'ARS' : 'symbol-narrow' : '1.0-0' }}</td>
                  <td>{{ campania.fechaCierre | date: 'dd/MM/yyyy' }}</td>
                  <td class="text-end text-nowrap">
                    @if (publica(campania)) {
                      <a class="btn btn-sm btn-primary me-1" [routerLink]="['/artista/campanias', campania.id, 'panel']">Panel</a>
                    }
                    <a class="btn btn-sm btn-outline-primary" [routerLink]="['/artista/campanias', campania.id]">
                      {{ campania.estado === 'BORRADOR' ? 'Editar' : 'Ver' }}
                    </a>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    }
  `,
})
export default class MisCampanias implements OnInit {
  readonly campanias = signal<Campania[] | null>(null);
  readonly mercadoPago = signal<{ habilitado: boolean; vinculada: boolean } | null>(null);
  readonly estados = ESTADOS_CAMPANIA;

  private readonly http = inject(HttpClient);

  ngOnInit(): void {
    this.http.get<Campania[]>(`${serverApiUrl}api/mis-campanias`).subscribe(campanias => this.campanias.set(campanias));
    this.http
      .get<{ habilitado: boolean; vinculada: boolean }>(`${serverApiUrl}api/artista/mercadopago`)
      .subscribe(estado => this.mercadoPago.set(estado));
  }

  vincularMercadoPago(): void {
    this.http.get<{ url: string }>(`${serverApiUrl}api/artista/mercadopago/autorizacion`).subscribe(({ url }) => {
      window.location.href = url;
    });
  }

  publica(campania: Campania): boolean {
    return campania.estado !== 'BORRADOR' && campania.estado !== 'EN_REVISION';
  }
}
