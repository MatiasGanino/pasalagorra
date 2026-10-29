import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { serverApiUrl } from 'app/config';
import { Artista, ESTADOS_ARTISTA, EstadoArtista } from 'app/artista/artista.model';

@Component({
  selector: 'jhi-admin-artistas',
  imports: [DatePipe, RouterLink],
  templateUrl: './artistas.html',
})
export default class AdminArtistas implements OnInit {
  readonly artistas = signal<Artista[]>([]);
  readonly estado = signal<EstadoArtista>('PENDIENTE');
  readonly estados = ESTADOS_ARTISTA;
  readonly filtros: EstadoArtista[] = ['PENDIENTE', 'APROBADO', 'RECHAZADO'];

  private readonly http = inject(HttpClient);

  ngOnInit(): void {
    this.cargar();
  }

  filtrar(estado: EstadoArtista): void {
    this.estado.set(estado);
    this.cargar();
  }

  private cargar(): void {
    this.http
      .get<Artista[]>(`${serverApiUrl}api/admin/artistas`, { params: { estado: this.estado() } })
      .subscribe(artistas => this.artistas.set(artistas));
  }
}
