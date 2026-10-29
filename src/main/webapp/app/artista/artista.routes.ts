import { Routes } from '@angular/router';

import { userRouteAccessService } from 'app/core/auth';

const routes: Routes = [
  {
    path: 'solicitud',
    loadComponent: () => import('./solicitud/solicitud-artista'),
    title: 'global.menu.account.artista',
    canActivate: [userRouteAccessService],
  },
];

export default routes;
