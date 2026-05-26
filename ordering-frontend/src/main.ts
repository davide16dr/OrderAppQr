// SockJS (CommonJS) expects a Node-like `global` reference.
// In the browser we map it to `window` to avoid runtime crashes.
(window as any).global ??= window;
(window as any).process ??= { env: {} };

import { bootstrapApplication } from '@angular/platform-browser';
import { registerLocaleData } from '@angular/common';
import localeIt from '@angular/common/locales/it';
import { appConfig } from './app/app.config';
import { App } from './app/app';

registerLocaleData(localeIt, 'it-IT');

bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));
