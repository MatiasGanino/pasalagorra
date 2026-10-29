import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * Pie con los términos y el botón de arrepentimiento, visible en todas las páginas (RNF-12).
 */
@Component({
  selector: 'jhi-footer',
  templateUrl: './footer.html',
  imports: [RouterLink],
})
export default class Footer {}
