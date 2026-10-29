import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import Marca from 'app/layouts/marca/marca';

/**
 * Pie con los términos y el botón de arrepentimiento, visible en todas las páginas (RNF-12).
 */
@Component({
  selector: 'jhi-footer',
  templateUrl: './footer.html',
  styleUrl: './footer.scss',
  imports: [RouterLink, Marca],
})
export default class Footer {
  readonly anio = new Date().getFullYear();
}
