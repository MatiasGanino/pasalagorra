import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Campania, ESTADOS_CAMPANIA } from 'app/campania/campania.model';
import { serverApiUrl } from 'app/config';
import { mensajeDeError } from 'app/shared/errores';

/**
 * Revisión de una campaña por el administrador (RF-07).
 */
@Component({
  selector: 'jhi-revision-campania',
  imports: [CurrencyPipe, DatePipe, FormsModule, RouterLink],
  templateUrl: './revision-campania.html',
})
export default class RevisionCampania implements OnInit {
  readonly id = input.required<string>();
  readonly campania = signal<Campania | null>(null);
  readonly error = signal<string | null>(null);
  readonly estados = ESTADOS_CAMPANIA;
  motivo = '';

  private readonly http = inject(HttpClient);

  ngOnInit(): void {
    this.http.get<Campania>(this.url()).subscribe(campania => this.campania.set(campania));
  }

  aprobar(): void {
    this.revisar('aprobar', {});
  }

  rechazar(): void {
    this.revisar('rechazar', { motivo: this.motivo });
  }

  private revisar(accion: 'aprobar' | 'rechazar', cuerpo: object): void {
    this.error.set(null);
    this.http.post<Campania>(`${this.url()}/${accion}`, cuerpo).subscribe({
      next: campania => this.campania.set(campania),
      error: (error: HttpErrorResponse) => this.error.set(mensajeDeError(error)),
    });
  }

  private url(): string {
    return `${serverApiUrl}api/admin/campanias/${this.id()}`;
  }
}
