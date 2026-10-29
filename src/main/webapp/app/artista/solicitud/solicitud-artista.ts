import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { serverApiUrl } from 'app/config';
import { PROVINCIAS } from 'app/shared/provincias';

import { Artista, ESTADOS_ARTISTA, mensajeDeError } from '../artista.model';

@Component({
  selector: 'jhi-solicitud-artista',
  imports: [ReactiveFormsModule],
  templateUrl: './solicitud-artista.html',
})
export default class SolicitudArtista implements OnInit {
  readonly artista = signal<Artista | null>(null);
  readonly cargando = signal(true);
  readonly enviada = signal(false);
  readonly error = signal<string | null>(null);
  readonly provincias = PROVINCIAS;
  readonly estados = ESTADOS_ARTISTA;

  form = new FormGroup({
    nombreArtistico: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(100)] }),
    nombreCompleto: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(100)] }),
    dni: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(/^[\d.]{7,10}$/)] }),
    cuit: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(/^\d{2}-?\d{7,8}-?\d$/)] }),
    ciudad: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(100)] }),
    provincia: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    biografia: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(2000)] }),
  });

  private readonly url = `${serverApiUrl}api/artista/solicitud`;
  private readonly http = inject(HttpClient);

  ngOnInit(): void {
    this.http.get<Artista | null>(this.url).subscribe({
      next: artista => {
        this.mostrar(artista);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }

  editable(): boolean {
    return this.artista()?.estado !== 'APROBADO';
  }

  enviar(): void {
    this.enviada.set(false);
    this.error.set(null);
    this.http.put<Artista>(this.url, this.form.getRawValue()).subscribe({
      next: artista => {
        this.mostrar(artista);
        this.enviada.set(true);
      },
      error: (error: HttpErrorResponse) => this.error.set(mensajeDeError(error)),
    });
  }

  private mostrar(artista: Artista | null): void {
    this.artista.set(artista);
    if (artista) {
      this.form.patchValue({ ...artista, biografia: artista.biografia ?? '' });
      if (artista.estado === 'APROBADO') {
        this.form.disable();
      }
    }
  }
}
