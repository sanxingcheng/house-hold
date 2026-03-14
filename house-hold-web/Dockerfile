# 前端构建：在 house-hold-web 目录下执行 docker build -t house-hold-web:latest .
# 可通过 --build-arg VITE_API_BASE_URL=/api 指定生产环境 API 前缀（与 Ingress 或网关路径一致）
ARG VITE_API_BASE_URL=
FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm ci --ignore-scripts

ARG VITE_API_BASE_URL
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
COPY . .
RUN npm run build

FROM nginx:alpine
RUN apk add --no-cache dumb-init
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
ENTRYPOINT ["dumb-init", "nginx", "-g", "daemon off;"]
