import { Directive, ElementRef, OnDestroy, OnInit, inject, input } from '@angular/core';

/**
 * Hace aparecer el elemento con un fundido cuando entra en pantalla. Sin IntersectionObserver (tests, navegadores
 * viejos) se muestra directamente; con "reducir movimiento" el CSS lo deja quieto.
 */
@Directive({
  selector: '[jhiAparecer]',
  host: { class: 'aparecer', '[style.--retraso]': 'retraso()' },
})
export default class AparecerDirective implements OnInit, OnDestroy {
  /** Retraso de la animación, para escalonar elementos de una lista (por ejemplo, '0.1s'). */
  readonly retraso = input<string>('0s', { alias: 'jhiAparecer' });

  private readonly elemento = inject<ElementRef<HTMLElement>>(ElementRef);
  private observador?: IntersectionObserver;

  ngOnInit(): void {
    const elemento = this.elemento.nativeElement;
    if (typeof IntersectionObserver === 'undefined') {
      elemento.classList.add('visible');
      return;
    }
    this.observador = new IntersectionObserver(
      entradas => {
        if (entradas.some(entrada => entrada.isIntersecting)) {
          elemento.classList.add('visible');
          this.observador?.disconnect();
        }
      },
      { rootMargin: '0px 0px -8% 0px' },
    );
    this.observador.observe(elemento);
  }

  ngOnDestroy(): void {
    this.observador?.disconnect();
  }
}
