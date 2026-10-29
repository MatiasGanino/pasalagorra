import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CampaniaResumen } from '../campania.model';
import Progreso from '../progreso/progreso';

/**
 * Tarjeta de una campaña para los listados: portada con zoom al pasar el mouse, un chip con el plazo y el progreso.
 */
@Component({
  selector: 'jhi-tarjeta-campania',
  imports: [RouterLink, Progreso],
  template: `
    @let c = campania();
    <a class="tarjeta-campania" [routerLink]="['/campania', c.slug]">
      <div class="tarjeta-imagen">
        @if (c.portada) {
          <img [src]="c.portada" [alt]="c.titulo" loading="lazy" />
        }
        <span class="chip {{ chip().clase }}">{{ chip().texto }}</span>
      </div>
      <div class="tarjeta-cuerpo">
        <h3 class="tarjeta-titulo">{{ c.titulo }}</h3>
        <p class="small text-body-secondary mb-3">{{ c.nombreArtistico }}</p>
        <div class="mt-auto">
          <jhi-progreso
            [recaudado]="c.recaudado"
            [meta]="c.meta"
            [porcentaje]="c.porcentaje"
            [diasRestantes]="c.diasRestantes"
            [estado]="c.estado"
          />
        </div>
      </div>
    </a>
  `,
})
export default class TarjetaCampania {
  readonly campania = input.required<CampaniaResumen>();

  readonly chip = computed(() => {
    const c = this.campania();
    if (c.estado === 'EXITOSA' || c.estado === 'CERRADA') {
      return { texto: '✓ Meta alcanzada', clase: 'chip-exito' };
    }
    if (c.diasRestantes <= 3) {
      return { texto: c.diasRestantes === 0 ? '¡Último día!' : `¡Últimos ${c.diasRestantes} días!`, clase: 'chip-alerta' };
    }
    return { texto: `${c.diasRestantes} días`, clase: 'chip-claro' };
  });
}
