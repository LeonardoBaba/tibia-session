import { Routes } from '@angular/router';

export const STAMINA_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./stamina.page').then((m) => m.StaminaPage),
  },
];
