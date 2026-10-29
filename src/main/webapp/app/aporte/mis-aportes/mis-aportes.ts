import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { serverApiUrl } from 'app/config';
import { mensajeDeError } from 'app/shared/errores';

import { Aporte, ESTADOS_APORTE } from '../aporte.model';

/**
 * Aportes del fan (RF-12), con la entrega de la recompensa (RF-18) y el reclamo si no llegó (RF-22).
 */
@Component({
  selector: 'jhi-mis-aportes',
  imports: [CurrencyPipe, DatePipe, FormsModule, RouterLink],
  template: `
    <h2>Mis aportes</h2>
    @if (mensaje(); as mensaje) {
      <div class="alert alert-success">{{ mensaje }}</div>
    }
    @if (aportes(); as aportes) {
      @if (aportes.length === 0) {
        <p class="text-muted">Todavía no hiciste ningún aporte. <a routerLink="/">Mirá las campañas</a>.</p>
      } @else {
        <div class="table-responsive">
          <table class="table table-striped align-middle">
            <thead>
              <tr>
                <th scope="col">Campaña</th>
                <th scope="col">Recompensa</th>
                <th scope="col">Monto</th>
                <th scope="col">Fecha</th>
                <th scope="col">Estado</th>
                <th scope="col"></th>
              </tr>
            </thead>
            <tbody>
              @for (aporte of aportes; track aporte.id) {
                <tr>
                  <td>
                    <a [routerLink]="['/campania', aporte.campaniaSlug]">{{ aporte.campaniaTitulo }}</a>
                  </td>
                  <td>
                    {{ aporte.recompensa ?? 'Aporte libre' }}
                    @if (aporte.entregado) {
                      <br /><span class="badge bg-success">Recompensa entregada</span>
                    }
                  </td>
                  <td>{{ aporte.monto | currency: 'ARS' : 'symbol-narrow' : '1.0-2' }}</td>
                  <td>{{ aporte.fechaCreacion | date: 'dd/MM/yyyy' }}</td>
                  <td>
                    <span class="badge {{ estados[aporte.estado].clase }}">{{ estados[aporte.estado].texto }}</span>
                  </td>
                  <td class="text-end">
                    @if (aporte.reclamosAbiertos.includes('INCUMPLIMIENTO')) {
                      <span class="badge bg-warning text-dark">Reclamo en revisión</span>
                    } @else if (reclamable(aporte) && reclamando() !== aporte.referencia) {
                      <button type="button" class="btn btn-sm btn-outline-danger" (click)="abrirReclamo(aporte)">
                        Reportar incumplimiento
                      </button>
                    }
                  </td>
                </tr>
                @if (reclamando() === aporte.referencia) {
                  <tr>
                    <td colspan="6">
                      <label class="form-label" for="reclamo">¿Qué pasó con tu recompensa?</label>
                      <textarea id="reclamo" class="form-control mb-2" rows="3" maxlength="2000" [(ngModel)]="texto"></textarea>
                      @if (error(); as error) {
                        <div class="alert alert-danger">{{ error }}</div>
                      }
                      <button type="button" class="btn btn-danger btn-sm me-2" [disabled]="!texto.trim()" (click)="reclamar(aporte)">
                        Enviar reclamo
                      </button>
                      <button type="button" class="btn btn-link btn-sm" (click)="reclamando.set(null)">Cancelar</button>
                    </td>
                  </tr>
                }
              }
            </tbody>
          </table>
        </div>
      }
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
