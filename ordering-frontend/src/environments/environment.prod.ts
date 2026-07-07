// This file can be replaced during build by using the `configurations` builder option.
// `ng build` replaces `environment.ts` with `environment.prod.ts`.

const host = window.location.hostname;
const isLocal = host === 'localhost' || host === '127.0.0.1';
const injectedApi = (window as any).__API_URL__;
// Su Railway: API_URL viene iniettato a runtime via entrypoint script nel container nginx
// Se non è impostato, l'app non ha un backend valido (errori visibili in console)
const apiBaseUrl = isLocal ? '' : (injectedApi && injectedApi !== '__PLACEHOLDER_API_URL__' ? injectedApi : '');

export const environment = {
  production: true,
  apiUrl: apiBaseUrl,
  wsUrl: apiBaseUrl.replace('https://', 'wss://').replace('http://', 'ws://'),
  appName: 'OrderApp',
  appVersion: '1.0.0',
  logLevel: 'error',
  redirectUrl: window.location.origin,
  jwtStorageKey: 'orderapp_jwt_token',
  refreshTokenStorageKey: 'orderapp_refresh_token',
  tenantIdStorageKey: 'orderapp_tenant_id',
  timeout: 30000,
  retryCount: 3,
  enableLogging: false,
};
