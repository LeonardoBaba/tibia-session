import { Routes } from '@angular/router';

export const HUNT_DETAIL_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./hunt-detail.page').then((m) => m.HuntDetailPage),
  },
];
