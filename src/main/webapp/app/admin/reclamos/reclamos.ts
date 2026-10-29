import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { EstadoReclamo, Reclamo } from 'app/aporte/aporte.model';
import { serverApiUrl } from 'app/config';
import { mensajeDeError } from 'app/shared/errores';

/**
 * Reclamos de los fans (RF-22) y pedidos de arrepentimiento (RNF-12): el administrador responde y el fan recibe la
 * respuesta por mail.
 */
@Component({
  selector: 'jhi-admin-reclamos',
  imports: [CurrencyPipe, DatePipe, FormsModule, RouterLink],
  template: `
    <h2>Reclamos</h2>

    <ul class="nav nav-tabs mb-3">
      <li class="nav-item">
        <button type="button" class="nav-link" [class.active]="estado() === 'ABIERTO'" (click)="filtrar('ABIERTO')">Abiertos</button>
      </li>
      <li class="nav-item">
        <button type="button" class="nav-link" [class.active]="estado() === 'RESUELTO'" (click)="filtrar('RESUELTO')">Resueltos</button>
      </li>
    </ul>

    @if (error(); as error) {
      <div class="alert alert-danger">{{ error }}</div>
    }

    @for (reclamo of reclamos(); track reclamo.id) {
      <div class="card mb-3">
        <div class="card-body">
          <div class="d-flex justify-content-between flex-wrap gap-2">
            <h3 class="h6 mb-1">
              <span class="badge {{ reclamo.tipo === 'ARREPENTIMIENTO' ? 'bg-info text-dark' : 'bg-warning text-dark' }} me-1">
                {{ reclamo.tipo === 'ARREPENTIMIENTO' ? 'Arrepentimiento' : 'Incumplimiento' }}
              </span>
              @if (reclamo.tipo === 'ARREPENTIMIENTO') {
                <span class="me-1">ARR-{{ reclamo.id }}</span>
              }
              <a [routerLink]="['/campania', reclamo.campaniaSlug]">{{ reclamo.campaniaTitulo }}</a>
              <span class="text-muted">de {{ reclamo.nombreArtistico }}</span>
            </h3>
            <span class="small text-muted">{{ reclamo.fecha | date: 'dd/MM/yyyy HH:mm' }}</span>
          </div>
          <p class="small mb-2">
            Fan: <strong>{{ reclamo.fanLogin }}</strong> ({{ reclamo.fanEmail }}) · aporte {{ reclamo.aporteReferencia }} de
            {{ reclamo.monto | currency: 'ARS' : 'symbol-narrow' : '1.0-2' }} pagado el {{ reclamo.fechaPago | date: 'dd/MM/yyyy' }} ·
            {{ reclamo.recompensa ?? 'Aporte libre' }}
            @if (reclamo.recompensa) {
              ({{ reclamo.entregado ? 'el artista la marcó entregada' : 'sin marcar como entregada' }})
            }
          </p>
          <p class="descripcion">{{ reclamo.texto }}</p>
          @if (reclamo.estado === 'RESUELTO') {
            <div class="alert alert-light border small mb-0">
              <strong>Respuesta ({{ reclamo.fechaResolucion | date: 'dd/MM/yyyy' }}):</strong> {{ reclamo.respuesta }}
            </div>
          } @else {
            <label class="form-label small" [for]="'respuesta' + reclamo.id">Respuesta para el fan</label>
            <textarea
              [id]="'respuesta' + reclamo.id"
              class="form-control mb-2"
              rows="2"
              maxlength="2000"
              [(ngModel)]="respuestas[reclamo.id]"
            ></textarea>
            <button type="button" class="btn btn-sm btn-primary me-2" (click)="resolver(reclamo, false)">Resolver y avisar al fan</button>
            @if (reclamo.tipo === 'ARREPENTIMIENTO') {
              <button type="button" class="btn btn-sm btn-danger" (click)="resolver(reclamo, true)">Reembolsar y avisar al fan</button>
            }
          }
        </div>
      </div>
    } @empty {
      <p class="text-muted">No hay reclamos {{ estado() === 'ABIERTO' ? 'abiertos' : 'resueltos' }}.</p>
    }
  `,
})
export default class AdminReclamos implements OnInit {
  readonly reclamos = signal<Reclamo[]>([]);
  readonly estado = signal<EstadoReclamo>('ABIERTO');
  readonly error = signal<string | null>(null);
  respuestas: Record<number, string> = {};

  private readonly http = inject(HttpClient);

  ngOnInit(): void {
    this.cargar();
  }

  filtrar(estado: EstadoReclamo): void {
    this.estado.set(estado);
    this.cargar();
  }

  resolver(reclamo: Reclamo, reembolsar: boolean): void {
    this.error.set(null);
    this.http
      .post<Reclamo>(`${serverApiUrl}api/admin/reclamos/${reclamo.id}/resolver`, {
        respuesta: this.respuestas[reclamo.id] ?? '',
        reembolsar,
      })
      .subscribe({
        next: () => this.cargar(),
        error: (error: HttpErrorResponse) => this.error.set(mensajeDeError(error)),
      });
  }

  private cargar(): void {
    this.http
      .get<Reclamo[]>(`${serverApiUrl}api/admin/reclamos`, { params: { estado: this.estado() } })
      .subscribe(reclamos => this.reclamos.set(reclamos));
  }
}
