import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * Términos y condiciones (RNF-12). Es un borrador: falta que lo revise un abogado antes de salir a producción.
 */
@Component({
  selector: 'jhi-terminos',
  imports: [RouterLink],
  template: `
    <div class="col-lg-9">
      <h2>Términos y condiciones</h2>
      <div class="alert alert-warning">Borrador: este texto todavía tiene que revisarlo un abogado.</div>

      <h3 class="h5 mt-4">1. Qué es pasalagorra</h3>
      <p>
        pasalagorra es una plataforma de financiamiento colectivo de recompensas para proyectos musicales. Los artistas publican campañas
        con una meta en pesos y un plazo, y las personas que quieren apoyarlas (los fans) hacen aportes, con o sin una recompensa a cambio.
        pasalagorra no es parte del proyecto: pone en contacto a artistas y fans y gestiona los pagos.
      </p>

      <h3 class="h5 mt-4">2. Cuentas</h3>
      <p>
        Para aportar o crear campañas hace falta una cuenta con un email válido. Cada persona es responsable de los datos que carga y de
        mantener su contraseña en reserva.
      </p>

      <h3 class="h5 mt-4">3. Artistas y campañas</h3>
      <p>
        Para crear campañas, el artista valida su identidad con DNI y CUIT, y el equipo de pasalagorra aprueba el alta. Cada campaña se
        revisa antes de publicarse. El artista es el único responsable de llevar adelante el proyecto y de entregar las recompensas en las
        condiciones y fechas que publicó, y de cumplir con sus obligaciones impositivas.
      </p>

      <h3 class="h5 mt-4">4. Aportes: todo o nada</h3>
      <p>
        Un aporte no es una inversión ni una compra en una tienda: es un apoyo al proyecto. Si al cierre la campaña llega a la meta, el
        dinero queda para el artista, descontada la comisión de la plataforma que se informa en cada campaña. Si no llega, se le devuelve a
        cada fan el aporte completo, al mismo medio de pago. Los pagos se procesan con Mercado Pago.
      </p>

      <h3 class="h5 mt-4">5. Recompensas, entregas y reclamos</h3>
      <p>
        Las fechas de entrega son estimadas y pueden demorarse. Si una recompensa no llega, el fan puede reportarlo desde
        <a routerLink="/account/aportes">Mis aportes</a>; el equipo de pasalagorra media con el artista y responde por mail.
      </p>

      <h3 class="h5 mt-4">6. Derecho de arrepentimiento</h3>
      <p>
        El fan puede revocar un aporte dentro de los 10 días corridos desde el pago, sin dar motivos ni pagar costos (art. 34 de la Ley
        24.240 y Resolución 424/2020), desde el <a routerLink="/arrepentimiento">botón de arrepentimiento</a>. Recibe en el momento un
        código de su pedido y el reembolso se hace al mismo medio de pago.
      </p>

      <h3 class="h5 mt-4">7. Datos personales</h3>
      <p>
        Los datos se tratan según la Ley 25.326. Al aportar a una recompensa con envío, el artista de la campaña recibe el nombre, el email
        y la dirección de envío del fan, solo para entregarla. Cualquier persona puede pedir el acceso, la corrección o la eliminación de
        sus datos.
      </p>

      <h3 class="h5 mt-4">8. Cambios y jurisdicción</h3>
      <p>
        Estos términos pueden cambiar; los cambios se publican en esta página y no afectan a los aportes ya hechos. Se rigen por las leyes
        de la República Argentina.
      </p>
    </div>
  `,
})
export default class Terminos {}
