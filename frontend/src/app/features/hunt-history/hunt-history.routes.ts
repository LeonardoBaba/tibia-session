import { Routes } from '@angular/router';

export const HUNT_HISTORY_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./hunt-history.page').then((m) => m.HuntHistoryPage),
  },
];
