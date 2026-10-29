import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { DomSanitizer, SafeResourceUrl, Title } from '@angular/platform-browser';

import { serverApiUrl } from 'app/config';

import { CampaniaPublica, ESTADOS_CAMPANIA } from '../campania.model';
import Progreso from '../progreso/progreso';

/**
 * Página pública de una campaña (RF-08).
 */
@Component({
  selector: 'jhi-campania-publica',
  imports: [CurrencyPipe, DatePipe, Progreso],
  templateUrl: './campania-publica.html',
})
export default class CampaniaPublicaComponent implements OnInit {
  readonly slug = input.required<string>();
  readonly campania = signal<CampaniaPublica | null>(null);
  readonly noExiste = signal(false);
  readonly imagenActual = signal(0);
  readonly estados = ESTADOS_CAMPANIA;
  video: SafeResourceUrl | null = null;

  private readonly http = inject(HttpClient);
  private readonly title = inject(Title);
  private readonly sanitizer = inject(DomSanitizer);

  ngOnInit(): void {
    this.http.get<CampaniaPublica>(`${serverApiUrl}api/public/campanias/${this.slug()}`).subscribe({
      next: campania => {
        this.campania.set(campania);
        this.title.setTitle(`${campania.titulo} · ${campania.nombreArtistico}`);
        this.video = campania.videoYoutubeId
          ? this.sanitizer.bypassSecurityTrustResourceUrl(`https://www.youtube-nocookie.com/embed/${campania.videoYoutubeId}`)
          : null;
      },
      error: () => this.noExiste.set(true),
    });
  }
}
