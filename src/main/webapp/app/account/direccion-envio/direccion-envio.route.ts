import { Route } from '@angular/router';

import { userRouteAccessService } from 'app/core/auth';

import DireccionEnvioComponent from './direccion-envio';

const direccionEnvioRoute: Route = {
  path: 'direccion-envio',
  component: DireccionEnvioComponent,
  title: 'global.menu.account.direccionEnvio',
  canActivate: [userRouteAccessService],
};

export default direccionEnvioRoute;
