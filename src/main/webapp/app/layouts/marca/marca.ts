import { Component, input } from '@angular/core';

/**
 * Logo de pasalagorra: una gorra con una moneda que cae, en el degradé de la marca. {@code claro} es para fondos
 * oscuros.
 */
@Component({
  selector: 'jhi-marca',
  template: `
    <svg class="marca-icono" viewBox="0 0 40 40" aria-hidden="true">
      <defs>
        <linearGradient [attr.id]="idDegrade" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stop-color="#5b3df5" />
          <stop offset="0.55" stop-color="#b23cd6" />
          <stop offset="1" stop-color="#ff5a36" />
        </linearGradient>
      </defs>
      <rect width="40" height="40" rx="11" [attr.fill]="'url(#' + idDegrade + ')'" />
      <circle class="marca-moneda" cx="26.5" cy="9.5" r="3.4" fill="#ffd166" />
      <path d="M9.5 24.5c0-6.9 4.7-11.5 10.5-11.5s10.5 4.6 10.5 11.5z" fill="#fff" />
      <rect x="12" y="20.4" width="16" height="2.4" rx="1.2" fill="#b23cd6" opacity="0.55" />
      <rect x="5.5" y="24" width="29" height="4.6" rx="2.3" fill="#fff" />
    </svg>
    <span class="marca-texto" [class.text-white]="claro()">pasalagorra</span>
  `,
  styles: `
    :host {
      display: inline-flex;
      align-items: center;
      gap: 0.55rem;
    }
    .marca-icono {
      width: 2.1rem;
      height: 2.1rem;
      flex-shrink: 0;
    }
    .marca-texto {
      font-family: 'Bricolage Grotesque Variable', system-ui, sans-serif;
      font-weight: 800;
      font-size: 1.35rem;
      letter-spacing: -0.03em;
      color: #15121f;
    }
    :host-context(a:hover) .marca-moneda {
      animation: caer 0.6s cubic-bezier(0.3, 0.7, 0.4, 1.4);
    }
    @keyframes caer {
      from {
        transform: translateY(-6px);
        opacity: 0;
      }
    }
  `,
})
export default class Marca {
  readonly claro = input(false);
  /** Cada logo necesita su id de degradé: puede haber dos en la misma página (barra y pie). */
  readonly idDegrade = `pg-degrade-${Math.random().toString(36).slice(2, 8)}`;
}
