import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { serverApiUrl } from 'app/config';
import { AlertError } from 'app/shared/alert';
import { PROVINCIAS } from 'app/shared/provincias';

export interface DireccionEnvio {
  calle: string;
  numero: string;
  pisoDepto: string | null;
  localidad: string;
  provincia: string;
  codigoPostal: string;
  telefono: string | null;
}

@Component({
  selector: 'jhi-direccion-envio',
  imports: [AlertError, ReactiveFormsModule],
  templateUrl: './direccion-envio.html',
})
export default class DireccionEnvioComponent implements OnInit {
  readonly guardada = signal(false);
  readonly provincias = PROVINCIAS;

  form = new FormGroup({
    calle: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(100)] }),
    numero: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(10)] }),
    pisoDepto: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(20)] }),
    localidad: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(100)] }),
    provincia: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    codigoPostal: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(10)] }),
    telefono: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(30)] }),
  });

  private readonly url = `${serverApiUrl}api/account/direccion-envio`;
  private readonly http = inject(HttpClient);

  ngOnInit(): void {
    this.http.get<DireccionEnvio | null>(this.url).subscribe(direccion => {
      if (direccion) {
        this.form.patchValue({ ...direccion, pisoDepto: direccion.pisoDepto ?? '', telefono: direccion.telefono ?? '' });
      }
    });
  }

  guardar(): void {
    this.guardada.set(false);
    this.http.put<DireccionEnvio>(this.url, this.form.getRawValue()).subscribe({
      next: direccion => {
        this.form.patchValue({ ...direccion, pisoDepto: direccion.pisoDepto ?? '', telefono: direccion.telefono ?? '' });
        this.guardada.set(true);
      },
      error() {
        // Lo muestra jhi-alert-error.
      },
    });
  }
}
