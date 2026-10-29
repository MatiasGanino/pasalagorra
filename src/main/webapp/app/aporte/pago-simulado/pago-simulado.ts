import { CurrencyPipe } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { serverApiUrl } from 'app/config';
import { mensajeDeError } from 'app/shared/errores';

import { Aporte } from '../aporte.model';

const COLORES = ['#5b3df5', '#ff5a36', '#ffd166', '#0f9d58', '#b23cd6'];

/**
 * Checkout de la pasarela simulada (hasta la etapa 6, Mercado Pago). Aprobar o rechazar confirma el pago por el mismo
 * camino que la notificación real.
 */
@Component({
  selector: 'jhi-pago-simulado',
  imports: [CurrencyPipe, RouterLink],
  styles: `
    .tarjeta-acceso {
      position: relative;
    }
    .resultado {
      display: inline-grid;
      place-items: center;
      width: 4.5rem;
      height: 4.5rem;
      margin-bottom: 1.25rem;
      border-radius: 50%;
      font-size: 2rem;
      font-weight: 800;
      color: #fff;
      animation: latido 0.6s cubic-bezier(0.3, 1.6, 0.5, 1) both;
    }
    .resultado-ok {
      background: linear-gradient(120deg, #0f9d58, #3ccf7f);
      box-shadow: 0 16px 30px -12px rgba(15, 157, 88, 0.7);
    }
    .resultado-error {
      background: #e5383b;
    }
    .resultado-info {
      background: #1fa7c9;
    }
    .festejo {
      position: absolute;
      top: 5rem;
      left: 50%;
      pointer-events: none;
    }
    .festejo span {
      position: absolute;
      width: 8px;
      height: 14px;
      border-radius: 2px;
      opacity: 0;
      animation: papelito 1.4s cubic-bezier(0.15, 0.7, 0.3, 1) forwards;
    }
    @keyframes papelito {
      0% {
        opacity: 1;
        transform: translate(0, 0) rotate(0);
      }
      100% {
        opacity: 0;
        transform: translate(var(--x), var(--y)) rotate(var(--giro));
      }
    }
    @keyframes latido {
      from {
        transform: scale(0.3);
        opacity: 0;
      }
    }
  `,
  template: `
    <div class="tarjeta-acceso text-center">
      @if (aporte(); as a) {
        @if (a.estado === 'PENDIENTE') {
          <span class="chip chip-primario mb-3">Pago simulado · acá va Mercado Pago</span>
          <p class="text-body-secondary mb-1">Vas a aportar a</p>
          <h1 class="h4 mb-1">{{ a.campaniaTitulo }}</h1>
          <p class="text-body-secondary small">{{ a.recompensa ?? 'Aporte libre' }}</p>
          <p class="dato-grande my-4">{{ a.monto | currency: 'ARS' : 'symbol-narrow' : '1.0-2' }}</p>
          <button type="button" class="btn btn-success btn-lg w-100 mb-2" [disabled]="ocupado()" (click)="pagar(true)">Pagar</button>
          <button type="button" class="btn btn-link text-danger" [disabled]="ocupado()" (click)="pagar(false)">Rechazar el pago</button>
        } @else if (a.estado === 'APROBADO') {
          <div class="festejo" aria-hidden="true">
            @for (papel of papelitos; track $index) {
              <span [style.--x]="papel.x" [style.--y]="papel.y" [style.--giro]="papel.giro" [style.background]="papel.color"></span>
            }
          </div>
          <div class="resultado resultado-ok">✓</div>
          <h1 class="h3">¡Gracias por bancar!</h1>
          <p class="text-body-secondary">
            Tu aporte de <strong>{{ a.monto | currency: 'ARS' : 'symbol-narrow' : '1.0-2' }}</strong> a
            <strong>{{ a.campaniaTitulo }}</strong> está confirmado. Te mandamos el comprobante por mail; si la campaña no llega a la meta,
            te lo devolvemos.
          </p>
        } @else if (a.estado === 'RECHAZADO') {
          <div class="resultado resultado-error">✕</div>
          <h1 class="h3">El pago no se hizo</h1>
          <p class="text-body-secondary">No se cobró nada. Podés volver a intentarlo desde la campaña.</p>
        } @else {
          <div class="resultado resultado-info">↺</div>
          <h1 class="h3">Te devolvimos el aporte</h1>
          <p class="text-body-secondary">La recompensa se agotó o la campaña cerró antes de que se acreditara el pago.</p>
        }
        @if (a.estado !== 'PENDIENTE') {
          <div class="d-flex flex-wrap justify-content-center gap-2 mt-4">
            <a class="btn btn-primary" [routerLink]="['/campania', a.campaniaSlug]">Volver a la campaña</a>
            <a class="btn btn-outline-dark" routerLink="/account/aportes">Mis aportes</a>
          </div>
        }
      }
      @if (error(); as mensaje) {
        <div class="alert alert-danger mt-3">{{ mensaje }}</div>
      }
    </div>
  `,
})
export default class PagoSimulado implements OnInit {
  readonly referencia = input.required<string>();
  readonly aporte = signal<Aporte | null>(null);
  readonly ocupado = signal(false);
  readonly error = signal<string | null>(null);
  /** Confeti para festejar el aporte: cada papelito sale en una dirección distinta. */
  readonly papelitos = Array.from({ length: 28 }, (_, i) => {
    const angulo = (i / 28) * Math.PI * 2;
    const distancia = 90 + Math.random() * 110;
    return {
      x: `${Math.round(Math.cos(angulo) * distancia)}px`,
      y: `${Math.round(Math.sin(angulo) * distancia - 40)}px`,
      giro: `${Math.round(Math.random() * 720 - 360)}deg`,
      color: COLORES[i % COLORES.length],
    };
  });

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
