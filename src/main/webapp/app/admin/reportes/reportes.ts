import { CurrencyPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';

import { serverApiUrl } from 'app/config';

interface RecaudacionMensual {
  mes: string;
  campanias: number;
  aportes: number;
  recaudado: number;
  comision: number;
}

/**
 * Recaudación y comisiones por mes (RF-23).
 */
@Component({
  selector: 'jhi-admin-reportes',
  imports: [CurrencyPipe],
  template: `
    <h2>Recaudación y comisiones</h2>
    <p class="text-muted small">
      Aportes confirmados de las campañas que llegaron a la meta, por mes de cierre. Las que no llegaron no suman: se reembolsaron. La
      comisión usa el porcentaje configurado hoy.
    </p>

    <div class="table-responsive">
      <table class="table table-striped align-middle">
        <thead>
          <tr>
            <th scope="col">Mes</th>
            <th scope="col" class="text-end">Campañas</th>
            <th scope="col" class="text-end">Aportes</th>
            <th scope="col" class="text-end">Recaudado</th>
            <th scope="col" class="text-end">Comisión</th>
          </tr>
        </thead>
        <tbody>
          @for (fila of meses(); track fila.mes) {
            <tr>
              <td>{{ nombreMes(fila.mes) }}</td>
              <td class="text-end">{{ fila.campanias }}</td>
              <td class="text-end">{{ fila.aportes }}</td>
              <td class="text-end">{{ fila.recaudado | currency: 'ARS' : 'symbol-narrow' : '1.0-2' }}</td>
              <td class="text-end">{{ fila.comision | currency: 'ARS' : 'symbol-narrow' : '1.2-2' }}</td>
            </tr>
          } @empty {
            <tr>
              <td colspan="5" class="text-muted">Todavía no cerró ninguna campaña exitosa.</td>
            </tr>
          }
        </tbody>
        @if (meses().length > 1) {
          <tfoot>
            <tr class="fw-bold">
              <td>Total</td>
              <td class="text-end">{{ total().campanias }}</td>
              <td class="text-end">{{ total().aportes }}</td>
              <td class="text-end">{{ total().recaudado | currency: 'ARS' : 'symbol-narrow' : '1.0-2' }}</td>
              <td class="text-end">{{ total().comision | currency: 'ARS' : 'symbol-narrow' : '1.2-2' }}</td>
            </tr>
          </tfoot>
        }
      </table>
    </div>
  `,
})
export default class AdminReportes implements OnInit {
  readonly meses = signal<RecaudacionMensual[]>([]);
  readonly total = computed(() =>
    this.meses().reduce(
      (suma, m) => ({
        campanias: suma.campanias + m.campanias,
        aportes: suma.aportes + m.aportes,
        recaudado: suma.recaudado + m.recaudado,
        comision: suma.comision + m.comision,
      }),
      { campanias: 0, aportes: 0, recaudado: 0, comision: 0 },
    ),
  );

  private readonly http = inject(HttpClient);

  ngOnInit(): void {
    this.http.get<RecaudacionMensual[]>(`${serverApiUrl}api/admin/reportes/recaudacion`).subscribe(meses => this.meses.set(meses));
  }

  nombreMes(mes: string): string {
    const [anio, numero] = mes.split('-').map(Number);
    const nombre = new Date(anio, numero - 1, 1).toLocaleDateString('es-AR', { month: 'long', year: 'numeric' });
    return nombre.charAt(0).toUpperCase() + nombre.slice(1);
  }
}
