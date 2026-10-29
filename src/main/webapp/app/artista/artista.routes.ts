import { Routes } from '@angular/router';

import { userRouteAccessService } from 'app/core/auth';

const deArtista = { authorities: ['ROLE_ARTISTA'] };

const routes: Routes = [
  {
    path: 'solicitud',
    loadComponent: () => import('./solicitud/solicitud-artista'),
    title: 'global.menu.account.artista',
    canActivate: [userRouteAccessService],
  },
  {
    path: 'campanias',
    loadComponent: () => import('./campanias/mis-campanias'),
    title: 'global.menu.campanias',
    data: deArtista,
    canActivate: [userRouteAccessService],
  },
  {
    path: 'campanias/nueva',
    loadComponent: () => import('./campanias/editar-campania'),
    title: 'global.menu.campanias',
    data: deArtista,
    canActivate: [userRouteAccessService],
  },
  {
    path: 'campanias/:id',
    loadComponent: () => import('./campanias/editar-campania'),
    title: 'global.menu.campanias',
    data: deArtista,
    canActivate: [userRouteAccessService],
  },
];

export default routes;
