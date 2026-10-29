import { DOCUMENT } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';

import { AccountService } from 'app/core/auth';
import { StateStorageService } from 'app/core/auth/state-storage.service';

/**
 * Vuelta del login con Google: el backend manda el JWT en el fragmento de la URL ({@code #token=...}).
 */
@Component({
  selector: 'jhi-vuelta-google',
  template: `
    <div class="tarjeta-acceso text-center">
      <h1 class="h4">Entrando con Google…</h1>
    </div>
  `,
})
export default class VueltaGoogle implements OnInit {
  private readonly document = inject(DOCUMENT);
  private readonly router = inject(Router);
  private readonly accountService = inject(AccountService);
  private readonly stateStorageService = inject(StateStorageService);

  ngOnInit(): void {
    const token = new URLSearchParams(this.document.location.hash.slice(1)).get('token');
    if (!token) {
      this.router.navigate(['/login'], { queryParams: { google: 'error' } });
      return;
    }
    this.stateStorageService.storeAuthenticationToken(token, true);
    this.accountService.identity(true).subscribe(() => {
      const anterior = this.stateStorageService.getUrl();
      this.stateStorageService.clearUrl();
      this.router.navigateByUrl(anterior ?? '/', { replaceUrl: true });
    });
  }
}
