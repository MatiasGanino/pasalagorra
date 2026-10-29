import { Routes } from '@angular/router';

import { userRouteAccessService } from 'app/core/auth';

import activateRoute from './activate/activate.route';
import direccionEnvioRoute from './direccion-envio/direccion-envio.route';
import passwordRoute from './password/password.route';
import passwordResetFinishRoute from './password-reset/finish/password-reset-finish.route';
import passwordResetInitRoute from './password-reset/init/password-reset-init.route';
import registerRoute from './register/register.route';
import settingsRoute from './settings/settings.route';

const accountRoutes: Routes = [
  activateRoute,
  {
    path: 'aportes',
    loadComponent: () => import('../aporte/mis-aportes/mis-aportes'),
    title: 'global.menu.account.aportes',
    canActivate: [userRouteAccessService],
  },
  direccionEnvioRoute,
  passwordRoute,
  passwordResetFinishRoute,
  passwordResetInitRoute,
  registerRoute,
  settingsRoute,
];

export default accountRoutes;
