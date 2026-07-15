# Stage 1: Build Angular
FROM node:22-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build -- --configuration production

# Stage 2: Serve con Nginx
FROM nginx:1.27-alpine

COPY --from=builder /app/dist/ordering-frontend/browser /usr/share/nginx/html

# Placeholder per API_URL — sostituito dallo script 40 all'avvio
RUN sed -i "s|<head>|<head><script>window.__API_URL__='__PLACEHOLDER_API_URL__';</script>|" /usr/share/nginx/html/index.html

# Script 39: configura la porta di nginx dalla variabile $PORT di Railway (prima del 40)
RUN printf '#!/bin/sh\nPORT=${PORT:-80}\nsed -i "s/NGINX_LISTEN_PORT/$PORT/" /etc/nginx/conf.d/default.conf\n' \
    > /docker-entrypoint.d/39-configure-port.sh \
    && chmod +x /docker-entrypoint.d/39-configure-port.sh

# Script 40: inietta API_URL a runtime
RUN printf '#!/bin/sh\nif [ -n "$API_URL" ]; then\n  sed -i "s|__PLACEHOLDER_API_URL__|$API_URL|g" /usr/share/nginx/html/index.html\nfi\n' \
    > /docker-entrypoint.d/40-inject-api-url.sh \
    && chmod +x /docker-entrypoint.d/40-inject-api-url.sh

# Config nginx — NGINX_LISTEN_PORT viene sostituito dallo script 39 all'avvio
RUN printf 'server {\n\
  listen NGINX_LISTEN_PORT;\n\
  root /usr/share/nginx/html;\n\
  index index.html;\n\
  add_header X-Frame-Options "SAMEORIGIN" always;\n\
  add_header X-Content-Type-Options "nosniff" always;\n\
  add_header Referrer-Policy "strict-origin-when-cross-origin" always;\n\
  location / {\n\
    try_files $uri $uri/ /index.html;\n\
  }\n\
  gzip on;\n\
  gzip_types text/plain text/css application/javascript application/json;\n\
}\n' > /etc/nginx/conf.d/default.conf

EXPOSE 80
