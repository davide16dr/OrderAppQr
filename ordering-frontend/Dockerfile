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
