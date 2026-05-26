// Polyfills loaded before the Angular application bundle.
// Some CommonJS dependencies (e.g. SockJS) expect Node-like globals.

(() => {
  const g = globalThis as any;
  g.global ??= g;
  g.process ??= { env: {} };
})();
