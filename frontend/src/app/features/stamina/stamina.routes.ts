import { Routes } from '@angular/router';

export const STAMINA_ROUTES: Routes = [
  // The menu's "Stamina" link (/stamina) lands on the target calculator.
  { path: '', pathMatch: 'full', redirectTo: 'target' },
  {
    path: 'target',
    loadComponent: () => import('./stamina-target.page').then((m) => m.StaminaTargetPage),
  },
  {
    path: 'simulator',
    loadComponent: () => import('./stamina.page').then((m) => m.StaminaPage),
  },
];
