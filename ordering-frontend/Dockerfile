# Stage 1: Build Angular
FROM node:22-alpine AS builder

# URL del backend — passato come build arg su Railway
# Esempio: docker build --build-arg API_URL=https://api.tuodominio.com .
ARG API_URL=""
ENV VITE_API_URL=$API_URL

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build -- --configuration production

# Stage 2: Serve con Nginx
FROM nginx:1.27-alpine

ARG API_URL=""

COPY --from=builder /app/dist/ordering-frontend/browser /usr/share/nginx/html

# Inietta l'URL del backend nell'index.html a runtime
RUN if [ -n "$API_URL" ]; then \
      sed -i "s|<head>|<head><script>window.__API_URL__='${API_URL}';</script>|" /usr/share/nginx/html/index.html; \
    fi

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
