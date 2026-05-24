export const environment = {
  production: false,
  // Em dev tudo passa pelo proxy.conf.json definido no angular.json,
  // então a app só conhece o prefixo `/api`.
  apiUrl: '/api',
};
