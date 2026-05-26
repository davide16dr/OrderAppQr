// This file can be replaced during build by using the `configurations` builder option.
// `ng build` replaces `environment.ts` with `environment.prod.ts`.

const runtimeEnv = (globalThis as {
  process?: {
    env?: Record<string, string | undefined>;
  };
}).process?.env;

const apiBaseUrl = runtimeEnv?.['ANGULAR_APP_API_URL'] || 'https://orderapp-backend.onrender.com';

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
