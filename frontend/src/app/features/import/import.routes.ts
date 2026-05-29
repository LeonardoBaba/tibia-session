import { Routes } from '@angular/router';

export const IMPORT_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./import.page').then((m) => m.ImportPage),
  },
];
