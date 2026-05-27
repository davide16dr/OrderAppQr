export const environment = {
  production: false,
  // Use the local proxy on localhost, otherwise call the Render backend directly.
  apiUrl: window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
    ? ''
    : 'https://orderappqr.onrender.com'
};
