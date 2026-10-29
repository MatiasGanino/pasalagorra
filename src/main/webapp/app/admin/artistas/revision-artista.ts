import { DatePipe } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { serverApiUrl } from 'app/config';
import { Artista, ESTADOS_ARTISTA, mensajeDeError } from 'app/artista/artista.model';

@Component({
  selector: 'jhi-revision-artista',
  imports: [DatePipe, FormsModule, RouterLink],
  templateUrl: './revision-artista.html',
})
export default class RevisionArtista implements OnInit {
  readonly id = input.required<string>();
  readonly artista = signal<Artista | null>(null);
  readonly error = signal<string | null>(null);
  readonly estados = ESTADOS_ARTISTA;
  motivo = '';

  private readonly http = inject(HttpClient);

  ngOnInit(): void {
    this.http.get<Artista>(this.url()).subscribe(artista => this.artista.set(artista));
  }

  aprobar(): void {
    this.revisar('aprobar', {});
  }

  rechazar(): void {
    this.revisar('rechazar', { motivo: this.motivo });
  }

  private revisar(accion: 'aprobar' | 'rechazar', cuerpo: object): void {
    this.error.set(null);
    this.http.post<Artista>(`${this.url()}/${accion}`, cuerpo).subscribe({
      next: artista => this.artista.set(artista),
      error: (error: HttpErrorResponse) => this.error.set(mensajeDeError(error)),
    });
  }

  private url(): string {
    return `${serverApiUrl}api/admin/artistas/${this.id()}`;
  }
}
