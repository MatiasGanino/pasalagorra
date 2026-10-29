import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { serverApiUrl } from 'app/config';
import AparecerDirective from 'app/shared/efectos/aparecer.directive';
import { mensajeDeError } from 'app/shared/errores';

import { Aporte, ESTADOS_APORTE } from '../aporte.model';

/**
 * Aportes del fan (RF-12), con la entrega de la recompensa (RF-18) y el reclamo si no llegó (RF-22).
 */
@Component({
  selector: 'jhi-mis-aportes',
  imports: [CurrencyPipe, DatePipe, FormsModule, RouterLink, AparecerDirective],
  styles: `
    .aporte {
      padding: 1.25rem 1.4rem;
    }
    .monto {
      font-family: 'Bricolage Grotesque Variable', system-ui, sans-serif;
      font-weight: 800;
      font-size: 1.35rem;
    }
  `,
  template: `
    <div class="encabezado-pagina">
      <h1>Mis aportes</h1>
      <p>Los proyectos que bancaste, el estado de cada pago y de tu recompensa.</p>
    </div>
    @if (mensaje(); as mensaje) {
      <div class="alert alert-success">{{ mensaje }}</div>
    }
    @if (aportes(); as aportes) {
      @if (aportes.length === 0) {
        <div class="panel-suave text-center py-5">
          <h2 class="h4">Todavía no bancaste ningún proyecto</h2>
          <p class="text-body-secondary">Elegí una campaña y aportá: si no llega a la meta, te devolvemos todo.</p>
          <a class="btn btn-primary" routerLink="/">Ver las campañas</a>
        </div>
      } @else {
        <div class="d-flex flex-column gap-3">
          @for (aporte of aportes; track aporte.id; let i = $index) {
            <article class="aporte panel-suave" [jhiAparecer]="(i < 6 ? i * 0.05 : 0) + 's'">
              <div class="d-flex flex-wrap justify-content-between gap-3">
                <div>
                  <a class="h5 d-block mb-1 text-body" [routerLink]="['/campania', aporte.campaniaSlug]">{{ aporte.campaniaTitulo }}</a>
                  <div class="d-flex flex-wrap gap-1 align-items-center">
                    <span class="chip">{{ aporte.recompensa ?? 'Aporte libre' }}</span>
                    @if (aporte.entregado) {
                      <span class="chip chip-exito">✓ Recompensa entregada</span>
                    }
                    @if (aporte.reclamosAbiertos.includes('ARREPENTIMIENTO')) {
                      <span class="chip chip-primario">Arrepentimiento pedido</span>
                    } @else if (aporte.reclamosAbiertos.includes('INCUMPLIMIENTO')) {
                      <span class="chip chip-alerta">Reclamo en revisión</span>
                    }
                  </div>
                </div>
                <div class="text-sm-end">
                  <div class="monto">{{ aporte.monto | currency: 'ARS' : 'symbol-narrow' : '1.0-2' }}</div>
                  <span class="badge {{ estados[aporte.estado].clase }}">{{ estados[aporte.estado].texto }}</span>
                </div>
              </div>
              <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mt-3 small text-body-secondary">
                <span>{{ aporte.fechaCreacion | date: "d 'de' MMMM yyyy" }} · comprobante {{ aporte.referencia.slice(0, 8) }}</span>
                @if (reclamable(aporte) && !aporte.reclamosAbiertos.includes('INCUMPLIMIENTO') && reclamando() !== aporte.referencia) {
                  <button type="button" class="btn btn-sm btn-link text-danger p-0" (click)="abrirReclamo(aporte)">
                    ¿No te llegó la recompensa?
                  </button>
                }
              </div>
              @if (reclamando() === aporte.referencia) {
                <div class="mt-3">
                  <label class="form-label" for="reclamo">¿Qué pasó con tu recompensa?</label>
                  <textarea id="reclamo" class="form-control mb-2" rows="3" maxlength="2000" [(ngModel)]="texto"></textarea>
                  @if (error(); as error) {
                    <div class="alert alert-danger">{{ error }}</div>
                  }
                  <button type="button" class="btn btn-danger btn-sm me-2" [disabled]="!texto.trim()" (click)="reclamar(aporte)">
                    Enviar reclamo
                  </button>
                  <button type="button" class="btn btn-link btn-sm" (click)="reclamando.set(null)">Cancelar</button>
                </div>
              }
            </article>
          }
        </div>
      }
      <p class="small text-body-secondary mt-4">
        ¿Te arrepentiste de un aporte de los últimos 10 días? Usá el <a routerLink="/arrepentimiento">botón de arrepentimiento</a>.
      </p>
    }
  `,
})
export default class MisAportes implements OnInit {
  readonly aportes = signal<Aporte[] | null>(null);
  readonly reclamando = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly mensaje = signal<string | null>(null);
  readonly estados = ESTADOS_APORTE;
  texto = '';

  private readonly http = inject(HttpClient);

  ngOnInit(): void {
    this.cargar();
  }

  reclamable(aporte: Aporte): boolean {
    return (
      aporte.estado === 'APROBADO' &&
      aporte.recompensa !== null &&
      (aporte.campaniaEstado === 'EXITOSA' || aporte.campaniaEstado === 'CERRADA')
    );
  }

  abrirReclamo(aporte: Aporte): void {
    this.texto = '';
    this.error.set(null);
    this.mensaje.set(null);
    this.reclamando.set(aporte.referencia);
  }

  reclamar(aporte: Aporte): void {
    this.http.post(`${serverApiUrl}api/aportes/${aporte.referencia}/reclamos`, { texto: this.texto }).subscribe({
      next: () => {
        this.reclamando.set(null);
        this.mensaje.set('Recibimos tu reclamo: lo revisamos y te respondemos por mail.');
        this.cargar();
      },
      error: (error: HttpErrorResponse) => this.error.set(mensajeDeError(error)),
    });
  }

  private cargar(): void {
    this.http.get<Aporte[]>(`${serverApiUrl}api/aportes`).subscribe(aportes => this.aportes.set(aportes));
  }
}
