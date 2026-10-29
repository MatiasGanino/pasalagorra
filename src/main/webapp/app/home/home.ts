import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CampaniaResumen } from 'app/campania/campania.model';
import Progreso from 'app/campania/progreso/progreso';
import { serverApiUrl } from 'app/config';

/**
 * Inicio: las campañas publicadas, primero las que cierran antes, y las exitosas.
 */
@Component({
  selector: 'jhi-home',
  templateUrl: './home.html',
  styleUrl: './home.scss',
  imports: [RouterLink, Progreso],
})
export default class Home implements OnInit {
  readonly campanias = signal<CampaniaResumen[] | null>(null);

  private readonly http = inject(HttpClient);

  ngOnInit(): void {
    this.http.get<CampaniaResumen[]>(`${serverApiUrl}api/public/campanias`).subscribe(campanias => this.campanias.set(campanias));
  }
}
