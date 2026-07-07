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

# Inserisce il placeholder a build-time — viene sostituito dallo script di avvio
RUN sed -i "s|<head>|<head><script>window.__API_URL__='__PLACEHOLDER_API_URL__';</script>|" /usr/share/nginx/html/index.html

# Script che gira all'avvio del container (prima che nginx parta)
# Railway inietta le variabili d'ambiente a runtime — questo script le legge
RUN printf '#!/bin/sh\nif [ -n "$API_URL" ]; then\n  sed -i "s|__PLACEHOLDER_API_URL__|$API_URL|g" /usr/share/nginx/html/index.html\nfi\n' \
    > /docker-entrypoint.d/40-inject-api-url.sh \
    && chmod +x /docker-entrypoint.d/40-inject-api-url.sh

# Config per Angular routing (SPA fallback)
RUN printf 'server {\n\
  listen 80;\n\
  root /usr/share/nginx/html;\n\
  index index.html;\n\
  location / {\n\
    try_files $uri $uri/ /index.html;\n\
  }\n\
  gzip on;\n\
  gzip_types text/plain text/css application/javascript application/json;\n\
}\n' > /etc/nginx/conf.d/default.conf

EXPOSE 80
