# Stage 1: build do bundle Angular
FROM node:24-alpine AS build
WORKDIR /app

# Aproveita cache de dependências separando o install do source code
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
# Build de produção: usa src/environments/environment.ts (apiUrl absoluto).
RUN npm run build

# Stage 2: serve estático
FROM nginx:alpine
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
