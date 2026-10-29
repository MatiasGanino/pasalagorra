import { CurrencyPipe } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { serverApiUrl } from 'app/config';
import { mensajeDeError } from 'app/shared/errores';

import { Aporte } from '../aporte.model';

/**
 * Checkout de la pasarela simulada (hasta la etapa 6, Mercado Pago). Aprobar o rechazar confirma el pago por el mismo
 * camino que la notificación real.
 */
@Component({
  selector: 'jhi-pago-simulado',
  imports: [CurrencyPipe, RouterLink],
  template: `
    <div class="d-flex justify-content-center">
      <div class="col-md-6">
        @if (aporte(); as a) {
          @if (a.estado === 'PENDIENTE') {
            <div class="card">
              <div class="card-header bg-info-subtle"><strong>Pago simulado</strong> · acá va el checkout de Mercado Pago</div>
              <div class="card-body">
                <p class="mb-1">{{ a.campaniaTitulo }}</p>
                <p class="text-muted small">{{ a.recompensa ?? 'Aporte libre' }}</p>
                <p class="display-6">{{ a.monto | currency: 'ARS' : 'symbol-narrow' : '1.0-2' }}</p>
                <div class="d-flex gap-2">
                  <button type="button" class="btn btn-success flex-fill" [disabled]="ocupado()" (click)="pagar(true)">Pagar</button>
                  <button type="button" class="btn btn-outline-danger" [disabled]="ocupado()" (click)="pagar(false)">
                    Rechazar el pago
                  </button>
                </div>
              </div>
            </div>
          } @else if (a.estado === 'APROBADO') {
            <div class="alert alert-success">
              <h2 class="h4">¡Gracias por tu aporte!</h2>
              <p class="mb-0">Te mandamos el comprobante por mail. Si la campaña no llega a la meta, te devolvemos el aporte.</p>
            </div>
          } @else if (a.estado === 'RECHAZADO') {
            <div class="alert alert-danger">
              <h2 class="h4">El pago no se hizo</h2>
              <p class="mb-0">No se cobró nada. Podés volver a intentarlo desde la campaña.</p>
            </div>
          } @else {
            <div class="alert alert-info">
              <h2 class="h4">Te devolvimos el aporte</h2>
              <p class="mb-0">La recompensa se agotó o la campaña cerró antes de que se acreditara el pago.</p>
            </div>
          }
          @if (a.estado !== 'PENDIENTE') {
            <p>
              <a [routerLink]="['/campania', a.campaniaSlug]">Volver a la campaña</a> ·
              <a routerLink="/account/aportes">Mis aportes</a>
            </p>
          }
        }
        @if (error(); as mensaje) {
          <div class="alert alert-danger">{{ mensaje }}</div>
        }
      </div>
    </div>
  `,
})
export default class PagoSimulado implements OnInit {
  readonly referencia = input.required<string>();
  readonly aporte = signal<Aporte | null>(null);
  readonly ocupado = signal(false);
  readonly error = signal<string | null>(null);

  private readonly http = inject(HttpClient);

  ngOnInit(): void {
    this.http.get<Aporte>(this.url()).subscribe({
      next: aporte => this.aporte.set(aporte),
      error: (error: HttpErrorResponse) => this.error.set(mensajeDeError(error)),
    });
  }

  pagar(aprobado: boolean): void {
    this.ocupado.set(true);
    this.http.post<Aporte>(`${this.url()}/pago-simulado`, { aprobado }).subscribe({
      next: aporte => {
        this.aporte.set(aporte);
        this.ocupado.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(mensajeDeError(error));
        this.ocupado.set(false);
      },
    });
  }

  private url(): string {
    return `${serverApiUrl}api/aportes/${this.referencia()}`;
  }
}
