# RNF-12 · Términos y botón de arrepentimiento

## Historia

Como fan quiero conocer las reglas de la plataforma y poder revocar un aporte como en cualquier compra online.

## Decisiones

- Página **Términos y condiciones** (`/terminos`), con link en el pie de todas las páginas y en la de cada campaña. Es
  un **borrador**: lo tiene que revisar un abogado antes de producción.
- **Botón de arrepentimiento** (`/arrepentimiento`, también en el pie, Resolución 424/2020 y art. 34 de la Ley 24.240):
  el fan elige un aporte aprobado de los últimos 10 días corridos desde el pago y, opcionalmente, escribe el motivo.
  `POST /api/aportes/{referencia}/arrepentimiento` crea un `Reclamo` de tipo `ARREPENTIMIENTO` y el fan recibe en el
  momento, en pantalla y por mail, el código del pedido (`ARR-<id>`).
- El administrador lo ve en **Administración → Reclamos** y lo resuelve con **Reembolsar y avisar al fan**: se devuelve
  el aporte por la pasarela, la recompensa recupera la unidad y el fan recibe el mail del reembolso y la respuesta. Si
  la pasarela falla, no se resuelve nada y se puede reintentar.
- El reembolso solo se hace desde un pedido de arrepentimiento, no desde un reclamo por incumplimiento.
- Sin sesión, la página explica que se puede pedir respondiendo el mail del comprobante. La norma pide que el botón no
  exija registro previo: como para aportar ya hace falta una cuenta, se usa la sesión; a validar con el abogado.

## Criterios de aceptación

- [ ] Dado un aporte aprobado hace menos de 10 días, el fan pide el arrepentimiento y recibe el código.
- [ ] Dado el pedido, el administrador reembolsa el aporte y la recompensa vuelve a tener stock.
- [ ] No se puede pedir sobre un aporte de hace más de 10 días, no aprobado o ajeno.
