import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Aporte } from 'app/aporte/aporte.model';
import { serverApiUrl } from 'app/config';
import { AccountService } from 'app/core/auth';
import { mensajeDeError } from 'app/shared/errores';

/** Días corridos desde el pago para arrepentirse (art. 34 de la Ley 24.240). */
const DIAS_ARREPENTIMIENTO = 10;

/**
 * Botón de arrepentimiento (RNF-12, Resolución 424/2020): el fan revoca un aporte de los últimos 10 días y recibe el
 * código de su pedido.
 */
@Component({
  selector: 'jhi-arrepentimiento',
  imports: [CurrencyPipe, DatePipe, FormsModule, RouterLink],
  template: `
    <div class="col-lg-9">
      <h2>Botón de arrepentimiento</h2>
      <p>
        Podés revocar un aporte dentro de los {{ dias }} días corridos desde que lo pagaste, sin dar motivos y sin costo. Te devolvemos el
        monto completo al mismo medio de pago. Más detalles en los <a routerLink="/terminos">términos y condiciones</a>.
      </p>

      @if (codigo(); as codigo) {
        <div class="alert alert-success">
          Recibimos tu pedido. El código es <strong>{{ codigo }}</strong
          >; también te lo mandamos por mail. Te avisamos cuando se resuelva.
        </div>
      }
      @if (error(); as error) {
        <div class="alert alert-danger">{{ error }}</div>
      }

      @if (!autenticado) {
        <p>
          <a routerLink="/login">Iniciá sesión</a> para elegir el aporte. Si no podés entrar a tu cuenta, respondé el mail del comprobante
          del aporte pidiendo la revocación.
        </p>
      } @else if (aportes(); as aportes) {
        @if (aportes.length === 0) {
          <p class="text-muted">No tenés aportes de los últimos {{ dias }} días para revocar.</p>
        } @else {
          <div class="mb-3">
            <label class="form-label" for="motivo">Motivo (opcional)</label>
            <textarea id="motivo" class="form-control" rows="2" maxlength="2000" [(ngModel)]="texto"></textarea>
          </div>
          <ul class="list-group">
            @for (aporte of aportes; track aporte.id) {
              <li class="list-group-item d-flex justify-content-between align-items-center flex-wrap gap-2">
                <span>
                  <strong>{{ aporte.campaniaTitulo }}</strong> · {{ aporte.monto | currency: 'ARS' : 'symbol-narrow' : '1.0-2' }} · pagado
                  el
                  {{ aporte.fechaPago | date: 'dd/MM/yyyy' }}
                </span>
                @if (aporte.reclamosAbiertos.includes('ARREPENTIMIENTO')) {
                  <span class="badge bg-info text-dark">Pedido en curso</span>
                } @else {
                  <button type="button" class="btn btn-danger btn-sm" [disabled]="ocupado()" (click)="arrepentirse(aporte)">
                    Revocar este aporte
                  </button>
                }
              </li>
            }
          </ul>
        }
      }
    </div>
  `,
})
export default class Arrepentimiento implements OnInit {
  readonly aportes = signal<Aporte[] | null>(null);
  readonly codigo = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly ocupado = signal(false);
  readonly dias = DIAS_ARREPENTIMIENTO;
  readonly autenticado = inject(AccountService).isAuthenticated();
  texto = '';

  private readonly http = inject(HttpClient);

  ngOnInit(): void {
    if (this.autenticado) {
      this.cargar();
    }
  }

  arrepentirse(aporte: Aporte): void {
    this.error.set(null);
    this.ocupado.set(true);
    this.http
      .post<{ id: number }>(`${serverApiUrl}api/aportes/${aporte.referencia}/arrepentimiento`, { texto: this.texto.trim() || null })
      .subscribe({
        next: reclamo => {
          this.codigo.set(`ARR-${reclamo.id}`);
          this.ocupado.set(false);
          this.cargar();
        },
        error: (error: HttpErrorResponse) => {
          this.error.set(mensajeDeError(error));
          this.ocupado.set(false);
        },
      });
  }

  private cargar(): void {
    const desde = Date.now() - DIAS_ARREPENTIMIENTO * 24 * 60 * 60 * 1000;
    this.http
      .get<Aporte[]>(`${serverApiUrl}api/aportes`)
      .subscribe(aportes =>
        this.aportes.set(aportes.filter(a => a.estado === 'APROBADO' && a.fechaPago !== null && new Date(a.fechaPago).getTime() >= desde)),
      );
  }
}
