import { CurrencyPipe, DOCUMENT } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CampaniaResumen } from 'app/campania/campania.model';
import TarjetaCampania from 'app/campania/tarjeta/tarjeta-campania';
import { serverApiUrl } from 'app/config';
import AparecerDirective from 'app/shared/efectos/aparecer.directive';

/**
 * Inicio: qué es pasalagorra, cómo funciona y las campañas, primero las publicadas que cierran antes y después las que
 * llegaron a la meta.
 */
@Component({
  selector: 'jhi-home',
  templateUrl: './home.html',
  styleUrl: './home.scss',
  imports: [CurrencyPipe, RouterLink, TarjetaCampania, AparecerDirective],
})
export default class Home implements OnInit {
  readonly campanias = signal<CampaniaResumen[] | null>(null);
  readonly enCurso = computed(() => (this.campanias() ?? []).filter(c => c.estado === 'PUBLICADA'));
  readonly logradas = computed(() => (this.campanias() ?? []).filter(c => c.estado !== 'PUBLICADA'));
  readonly destacadas = computed(() =>
    this.enCurso()
      .filter(c => c.portada !== null)
      .slice(0, 3),
  );
  readonly totalRecaudado = computed(() => this.enCurso().reduce((suma, c) => suma + c.recaudado, 0));

  readonly pasos = [
    { titulo: 'Elegí un proyecto', texto: 'Discos, giras, videoclips: cada campaña tiene una meta y un plazo.' },
    { titulo: 'Aportá y elegí tu recompensa', texto: 'Desde un agradecimiento hasta el vinilo firmado o un ensayo abierto.' },
    { titulo: 'Todo o nada', texto: 'Si no se llega a la meta, se te devuelve el aporte completo, sin trámites.' },
  ];

  private readonly http = inject(HttpClient);
  private readonly document = inject(DOCUMENT);

  ngOnInit(): void {
    this.http.get<CampaniaResumen[]>(`${serverApiUrl}api/public/campanias`).subscribe(campanias => this.campanias.set(campanias));
  }

  irACampanias(): void {
    this.document.getElementById('campanias')?.scrollIntoView({ behavior: 'smooth' });
  }
}
