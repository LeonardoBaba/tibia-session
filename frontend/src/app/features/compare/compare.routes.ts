import { Routes } from '@angular/router';

export const COMPARE_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./compare.page').then((m) => m.ComparePage),
  },
];
