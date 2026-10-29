import { Routes } from '@angular/router';

import { userRouteAccessService } from 'app/core/auth';
import { Authority } from 'app/shared/jhipster/constants';

import { errorRoute } from './layouts/error/error.route';

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./home/home'),
    title: 'home.title',
  },
  {
    path: '',
    loadComponent: () => import('./layouts/navbar/navbar'),
    outlet: 'navbar',
  },
  {
    path: 'admin',
    data: {
      authorities: [Authority.ADMIN],
    },
    canActivate: [userRouteAccessService],
    loadChildren: () => import('./admin/admin.routes'),
  },
  {
    path: 'account',
    loadChildren: () => import('./account/account.route'),
  },
  {
    path: 'campania/:slug',
    loadComponent: () => import('./campania/detalle/campania-publica'),
    title: 'global.menu.campania',
  },
  {
    path: 'artista',
    loadChildren: () => import('./artista/artista.routes'),
  },
  {
    path: 'login',
    loadComponent: () => import('./login/login'),
    title: 'login.title',
  },
  {
    path: '',
    loadChildren: () => import('./entities/entity.routes'),
  },
  ...errorRoute,
];

export default routes;
