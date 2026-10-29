import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Campania, ESTADOS_CAMPANIA, EstadoCampania } from 'app/campania/campania.model';
import { serverApiUrl } from 'app/config';

@Component({
  selector: 'jhi-admin-campanias',
  imports: [CurrencyPipe, DatePipe, RouterLink],
  template: `
    <h2>Campañas</h2>

    <ul class="nav nav-tabs mb-3">
      @for (filtro of filtros; track filtro) {
        <li class="nav-item">
          <button type="button" class="nav-link" [class.active]="estado() === filtro" (click)="filtrar(filtro)">
            {{ estados[filtro].texto }}
          </button>
        </li>
      }
    </ul>

    @if (campanias().length === 0) {
      <p class="text-muted">No hay campañas en este estado.</p>
    } @else {
      <div class="table-responsive">
        <table class="table table-striped align-middle">
          <thead>
            <tr>
              <th scope="col">Campaña</th>
              <th scope="col">Artista</th>
              <th scope="col">Meta</th>
              <th scope="col">Cierre</th>
              <th scope="col"></th>
            </tr>
          </thead>
          <tbody>
            @for (campania of campanias(); track campania.id) {
              <tr>
                <td>{{ campania.titulo }}</td>
                <td>{{ campania.nombreArtistico }}</td>
                <td>{{ campania.meta | currency: 'ARS' : 'symbol-narrow' : '1.0-0' }}</td>
                <td>{{ campania.fechaCierre | date: 'dd/MM/yyyy' }}</td>
                <td class="text-end">
                  <a class="btn btn-sm btn-outline-primary" [routerLink]="['/admin/campanias', campania.id]">
                    {{ campania.estado === 'EN_REVISION' ? 'Revisar' : 'Ver' }}
                  </a>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }
  `,
})
export default class AdminCampanias implements OnInit {
  readonly campanias = signal<Campania[]>([]);
  readonly estado = signal<EstadoCampania>('EN_REVISION');
  readonly estados = ESTADOS_CAMPANIA;
  readonly filtros: EstadoCampania[] = ['EN_REVISION', 'PUBLICADA', 'EXITOSA', 'FALLIDA', 'CERRADA', 'BORRADOR'];

  private readonly http = inject(HttpClient);

  ngOnInit(): void {
    this.cargar();
  }

  filtrar(estado: EstadoCampania): void {
    this.estado.set(estado);
    this.cargar();
  }

  private cargar(): void {
    this.http
      .get<Campania[]>(`${serverApiUrl}api/admin/campanias`, { params: { estado: this.estado() } })
      .subscribe(campanias => this.campanias.set(campanias));
  }
}
