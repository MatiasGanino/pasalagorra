import { beforeEach, describe, expect, it } from 'vitest';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import Home from './home';

describe('Home Component', () => {
  let fixture: ComponentFixture<Home>;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    fixture = TestBed.createComponent(Home);
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('muestra las campañas publicadas', () => {
    fixture.detectChanges();

    httpMock.expectOne('api/public/campanias').flush([
      {
        id: 1,
        slug: 'disco-nuevo',
        titulo: 'Disco nuevo',
        nombreArtistico: 'Los Pasalagorra',
        portada: null,
        meta: 500000,
        recaudado: 250000,
        porcentaje: 50,
        diasRestantes: 10,
        estado: 'PUBLICADA',
      },
    ]);
    fixture.detectChanges();

    expect(fixture.componentInstance.campanias()).toHaveLength(1);
    expect(fixture.nativeElement.textContent).toContain('Disco nuevo');
    expect(fixture.nativeElement.textContent).toContain('Quedan 10 días');
  });
});
