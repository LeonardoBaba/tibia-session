import { Routes } from '@angular/router';
import { AppShell } from './layout/app-shell/app-shell';

export const routes: Routes = [
  {
    path: '',
    component: AppShell,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'hunts' },
      {
        path: 'hunts',
        loadChildren: () =>
          import('./features/hunt-history/hunt-history.routes').then((m) => m.HUNT_HISTORY_ROUTES),
      },
      {
        path: 'hunts/:id',
        loadChildren: () =>
          import('./features/hunt-detail/hunt-detail.routes').then((m) => m.HUNT_DETAIL_ROUTES),
      },
      {
        path: 'compare',
        loadChildren: () =>
          import('./features/compare/compare.routes').then((m) => m.COMPARE_ROUTES),
      },
      {
        path: 'import',
        loadChildren: () =>
          import('./features/import/import.routes').then((m) => m.IMPORT_ROUTES),
      },
    ],
  },
];
