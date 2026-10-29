import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { serverApiUrl } from 'app/config';
import { mensajeDeError } from 'app/shared/errores';

/**
 * Vuelta del OAuth de Mercado Pago: llega con {@code code} y {@code state} y termina la vinculación (etapa 6).
 */
@Component({
  selector: 'jhi-vincular-mercadopago',
  imports: [RouterLink],
  template: `
    <div class="tarjeta-acceso text-center">
      @if (error(); as error) {
        <h1 class="h4">No pudimos vincular tu cuenta</h1>
        <p class="text-body-secondary">{{ error }}</p>
      } @else if (listo()) {
        <h1 class="h4">¡Listo, Mercado Pago está vinculado!</h1>
        <p class="text-body-secondary">Los aportes a tus campañas van a llegar directo a tu cuenta, descontada la comisión.</p>
      } @else {
        <h1 class="h4">Vinculando tu cuenta de Mercado Pago…</h1>
      }
      <a class="btn btn-primary mt-3" routerLink="/artista/campanias">Ir a mis campañas</a>
    </div>
  `,
})
export default class VincularMercadoPago implements OnInit {
  readonly code = input<string>();
  readonly state = input<string>();
  readonly listo = signal(false);
  readonly error = signal<string | null>(null);

  private readonly http = inject(HttpClient);

  ngOnInit(): void {
    if (!this.code() || !this.state()) {
      this.error.set('Mercado Pago no devolvió la autorización. Probá de nuevo desde Mis campañas.');
      return;
    }
    this.http.post(`${serverApiUrl}api/artista/mercadopago/vincular`, { code: this.code(), state: this.state() }).subscribe({
      next: () => this.listo.set(true),
      error: (error: HttpErrorResponse) => this.error.set(mensajeDeError(error)),
    });
  }
}
