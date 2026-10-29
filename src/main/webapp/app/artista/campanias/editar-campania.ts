import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { Observable } from 'rxjs';

import { Campania, ESTADOS_CAMPANIA, Recompensa } from 'app/campania/campania.model';
import { serverApiUrl } from 'app/config';
import { mensajeDeError } from 'app/shared/errores';

type RecompensaForm = FormGroup<{
  titulo: FormControl<string>;
  descripcion: FormControl<string>;
  montoMinimo: FormControl<number | null>;
  stock: FormControl<number | null>;
  fechaEntregaEstimada: FormControl<string>;
  requiereEnvio: FormControl<boolean>;
}>;

/**
 * Alta y edición de una campaña en borrador, con sus recompensas e imágenes (RF-04, RF-05).
 */
@Component({
  selector: 'jhi-editar-campania',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './editar-campania.html',
})
export default class EditarCampania implements OnInit {
  /** El id de la ruta, o undefined para una campaña nueva. */
  readonly id = input<string>();
  readonly campania = signal<Campania | null>(null);
  readonly mensaje = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly ocupado = signal(false);
  readonly estados = ESTADOS_CAMPANIA;

  form = new FormGroup({
    titulo: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(120)] }),
    descripcion: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(20000)] }),
    videoUrl: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(255)] }),
    meta: new FormControl<number | null>(null, { validators: [Validators.required, Validators.min(1000)] }),
    fechaCierre: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    recompensas: new FormArray<RecompensaForm>([]),
  });

  private readonly url = `${serverApiUrl}api/mis-campanias`;
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  ngOnInit(): void {
    const id = this.id();
    if (id) {
      this.http.get<Campania>(`${this.url}/${id}`).subscribe(campania => this.mostrar(campania));
    } else {
      this.agregarRecompensa();
    }
  }

  editable(): boolean {
    const campania = this.campania();
    return !campania || campania.estado === 'BORRADOR';
  }

  agregarRecompensa(recompensa?: Recompensa): void {
    this.form.controls.recompensas.push(
      new FormGroup({
        titulo: new FormControl(recompensa?.titulo ?? '', {
          nonNullable: true,
          validators: [Validators.required, Validators.maxLength(120)],
        }),
        descripcion: new FormControl(recompensa?.descripcion ?? '', {
          nonNullable: true,
          validators: [Validators.required, Validators.maxLength(2000)],
        }),
        montoMinimo: new FormControl<number | null>(recompensa?.montoMinimo ?? null, [Validators.required, Validators.min(100)]),
        stock: new FormControl<number | null>(recompensa?.stock ?? null, [Validators.min(1)]),
        fechaEntregaEstimada: new FormControl(recompensa?.fechaEntregaEstimada ?? '', {
          nonNullable: true,
          validators: Validators.required,
        }),
        requiereEnvio: new FormControl(recompensa?.requiereEnvio ?? false, { nonNullable: true }),
      }),
    );
  }

  quitarRecompensa(indice: number): void {
    this.form.controls.recompensas.removeAt(indice);
  }

  guardar(): void {
    const campania = this.campania();
    const datos = this.form.getRawValue();
    const pedido = campania ? this.http.put<Campania>(`${this.url}/${campania.id}`, datos) : this.http.post<Campania>(this.url, datos);
    this.ejecutar(pedido, guardada => {
      if (!campania) {
        this.router.navigate(['/artista/campanias', guardada.id], { replaceUrl: true });
      }
      return 'Cambios guardados.';
    });
  }

  subirImagen(evento: Event): void {
    const entrada = evento.target as HTMLInputElement;
    const archivo = entrada.files?.[0];
    const campania = this.campania();
    if (!archivo || !campania) {
      return;
    }
    const datos = new FormData();
    datos.append('archivo', archivo);
    this.ejecutar(this.http.post<Campania>(`${this.url}/${campania.id}/imagenes`, datos), () => 'Imagen subida.');
    entrada.value = '';
  }

  borrarImagen(imagenId: number): void {
    const campania = this.campania()!;
    this.ejecutar(this.http.delete<Campania>(`${this.url}/${campania.id}/imagenes/${imagenId}`), () => 'Imagen borrada.');
  }

  enviarARevision(): void {
    const campania = this.campania()!;
    this.ejecutar(
      this.http.post<Campania>(`${this.url}/${campania.id}/enviar-revision`, {}),
      () => 'La campaña se envió a revisión. Te avisamos por mail cuando esté publicada.',
    );
  }

  private ejecutar(pedido: Observable<Campania>, exito: (campania: Campania) => string): void {
    this.ocupado.set(true);
    this.mensaje.set(null);
    this.error.set(null);
    pedido.subscribe({
      next: campania => {
        this.mostrar(campania);
        this.mensaje.set(exito(campania));
        this.ocupado.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(mensajeDeError(error));
        this.ocupado.set(false);
      },
    });
  }

  private mostrar(campania: Campania): void {
    this.campania.set(campania);
    this.form.patchValue({ ...campania, videoUrl: campania.videoUrl ?? '' });
    this.form.controls.recompensas.clear();
    campania.recompensas.forEach(recompensa => this.agregarRecompensa(recompensa));
    this.form.markAsPristine();
    if (campania.estado === 'BORRADOR') {
      this.form.enable();
    } else {
      this.form.disable();
    }
  }
}
