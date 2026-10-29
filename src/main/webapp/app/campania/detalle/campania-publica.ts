import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { DomSanitizer, SafeResourceUrl, Title } from '@angular/platform-browser';
import { Router, RouterLink } from '@angular/router';

import { serverApiUrl } from 'app/config';
import { AccountService } from 'app/core/auth';
import { StateStorageService } from 'app/core/auth/state-storage.service';
import { mensajeDeError } from 'app/shared/errores';

import { CampaniaPublica, ESTADOS_CAMPANIA } from '../campania.model';
import Progreso from '../progreso/progreso';

/**
 * Página pública de una campaña (RF-08) y punto de partida del aporte (RF-10): el fan elige recompensa y monto y pasa
 * directo al pago.
 */
@Component({
  selector: 'jhi-campania-publica',
  imports: [CurrencyPipe, DatePipe, Progreso, RouterLink],
  templateUrl: './campania-publica.html',
})
export default class CampaniaPublicaComponent implements OnInit {
  readonly slug = input.required<string>();
  readonly campania = signal<CampaniaPublica | null>(null);
  readonly noExiste = signal(false);
  readonly imagenActual = signal(0);
  readonly ocupado = signal(false);
  readonly error = signal<string | null>(null);
  readonly faltaDireccion = signal(false);
  readonly estados = ESTADOS_CAMPANIA;
  video: SafeResourceUrl | null = null;

  private readonly http = inject(HttpClient);
  private readonly title = inject(Title);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly router = inject(Router);
  private readonly accountService = inject(AccountService);
  private readonly stateStorageService = inject(StateStorageService);

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

  aportar(recompensaId: number | null, monto: number): void {
    if (!this.accountService.isAuthenticated()) {
      this.stateStorageService.storeUrl(this.router.url);
      this.router.navigate(['/login']);
      return;
    }
    this.error.set(null);
    this.faltaDireccion.set(false);
    this.ocupado.set(true);
    this.http
      .post<{ referencia: string; urlPago: string }>(`${serverApiUrl}api/aportes`, {
        campaniaId: this.campania()!.id,
        recompensaId,
        monto,
      })
      .subscribe({
        next: cobro => {
          // La pasarela simulada devuelve una ruta del sitio; Mercado Pago, una URL externa.
          if (cobro.urlPago.startsWith('http')) {
            window.location.href = cobro.urlPago;
          } else {
            this.router.navigateByUrl(cobro.urlPago);
          }
        },
        error: (error: HttpErrorResponse) => {
          this.error.set(mensajeDeError(error));
          this.faltaDireccion.set(error.error?.message === 'error.sindireccion');
          this.ocupado.set(false);
        },
      });
  }
}
