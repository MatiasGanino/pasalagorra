import { CurrencyPipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';

import { EstadoCampania } from '../campania.model';

/**
 * Barra de progreso con lo recaudado, la meta y los días restantes (RF-08). La barra se llena con una animación al
 * aparecer.
 */
@Component({
  selector: 'jhi-progreso',
  imports: [CurrencyPipe],
  template: `
    <div class="progress mb-2" role="progressbar" [attr.aria-valuenow]="porcentaje()" aria-valuemin="0" aria-valuemax="100">
      <div class="progress-bar" [class.completa]="porcentaje() >= 100" [style.width.%]="ancho()"></div>
    </div>
    <div class="d-flex justify-content-between align-items-baseline gap-2 small">
      <span>
        <strong class="fs-6">{{ recaudado() | currency: 'ARS' : 'symbol-narrow' : '1.0-0' }}</strong>
        <span class="text-body-secondary"> de {{ meta() | currency: 'ARS' : 'symbol-narrow' : '1.0-0' }}</span>
      </span>
      <strong [class.text-success]="porcentaje() >= 100" [class.text-primary]="porcentaje() < 100">{{ porcentaje() }}%</strong>
    </div>
    <div class="small text-body-secondary mt-1">{{ textoDias() }}</div>
  `,
})
export default class Progreso {
  readonly recaudado = input.required<number>();
  readonly meta = input.required<number>();
  readonly porcentaje = input.required<number>();
  readonly diasRestantes = input.required<number>();
  readonly estado = input.required<EstadoCampania>();

  readonly ancho = computed(() => Math.min(this.porcentaje(), 100));
  readonly textoDias = computed(() => textoDias(this.estado(), this.diasRestantes()));
}

export function textoDias(estado: EstadoCampania, dias: number): string {
  if (estado !== 'PUBLICADA') {
    return 'Finalizada';
  }
  return dias === 0 ? 'Último día' : dias === 1 ? 'Queda 1 día' : `Quedan ${dias} días`;
}
