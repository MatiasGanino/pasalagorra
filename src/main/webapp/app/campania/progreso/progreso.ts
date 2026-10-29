import { CurrencyPipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';

import { EstadoCampania } from '../campania.model';

/**
 * Barra de progreso con lo recaudado, la meta y los días restantes (RF-08).
 */
@Component({
  selector: 'jhi-progreso',
  imports: [CurrencyPipe],
  template: `
    <div class="progress mb-2" role="progressbar" [attr.aria-valuenow]="porcentaje()" aria-valuemin="0" aria-valuemax="100">
      <div class="progress-bar" [class.bg-success]="porcentaje() >= 100" [style.width.%]="ancho()"></div>
    </div>
    <div class="d-flex justify-content-between small">
      <span>
        <strong>{{ recaudado() | currency: 'ARS' : 'symbol-narrow' : '1.0-0' }}</strong>
        de {{ meta() | currency: 'ARS' : 'symbol-narrow' : '1.0-0' }} ({{ porcentaje() }}%)
      </span>
      <span>{{ textoDias() }}</span>
    </div>
  `,
})
export default class Progreso {
  readonly recaudado = input.required<number>();
  readonly meta = input.required<number>();
  readonly porcentaje = input.required<number>();
  readonly diasRestantes = input.required<number>();
  readonly estado = input.required<EstadoCampania>();

  readonly ancho = computed(() => Math.min(this.porcentaje(), 100));
  readonly textoDias = computed(() => {
    if (this.estado() !== 'PUBLICADA') {
      return 'Finalizada';
    }
    const dias = this.diasRestantes();
    return dias === 0 ? 'Último día' : dias === 1 ? 'Queda 1 día' : `Quedan ${dias} días`;
  });
}
