import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { serverApiUrl } from 'app/config';

import { Aporte, ESTADOS_APORTE } from '../aporte.model';

@Component({
  selector: 'jhi-mis-aportes',
  imports: [CurrencyPipe, DatePipe, RouterLink],
  template: `
    <h2>Mis aportes</h2>
    @if (aportes(); as aportes) {
      @if (aportes.length === 0) {
        <p class="text-muted">Todavía no hiciste ningún aporte. <a routerLink="/">Mirá las campañas</a>.</p>
      } @else {
        <div class="table-responsive">
          <table class="table table-striped align-middle">
            <thead>
              <tr>
                <th scope="col">Campaña</th>
                <th scope="col">Recompensa</th>
                <th scope="col">Monto</th>
                <th scope="col">Fecha</th>
                <th scope="col">Estado</th>
              </tr>
            </thead>
            <tbody>
              @for (aporte of aportes; track aporte.id) {
                <tr>
                  <td>
                    <a [routerLink]="['/campania', aporte.campaniaSlug]">{{ aporte.campaniaTitulo }}</a>
                  </td>
                  <td>{{ aporte.recompensa ?? 'Aporte libre' }}</td>
                  <td>{{ aporte.monto | currency: 'ARS' : 'symbol-narrow' : '1.0-2' }}</td>
                  <td>{{ aporte.fechaCreacion | date: 'dd/MM/yyyy' }}</td>
                  <td>
                    <span class="badge {{ estados[aporte.estado].clase }}">{{ estados[aporte.estado].texto }}</span>
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
export default class MisAportes implements OnInit {
  readonly aportes = signal<Aporte[] | null>(null);
  readonly estados = ESTADOS_APORTE;

  private readonly http = inject(HttpClient);

  ngOnInit(): void {
    this.http.get<Aporte[]>(`${serverApiUrl}api/aportes`).subscribe(aportes => this.aportes.set(aportes));
  }
}
