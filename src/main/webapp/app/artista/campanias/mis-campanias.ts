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
                  <td class="text-end">
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
  readonly estados = ESTADOS_CAMPANIA;

  private readonly http = inject(HttpClient);

  ngOnInit(): void {
    this.http.get<Campania[]>(`${serverApiUrl}api/mis-campanias`).subscribe(campanias => this.campanias.set(campanias));
  }
}
