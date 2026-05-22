# --- 阶段 1：Node 构建 dist ---
FROM node:20-alpine AS build
WORKDIR /build

COPY package.json package-lock.json* ./
RUN npm ci || npm install

COPY . .
# 构建时注入的环境变量；compose 通过 build.args 传入
ARG VITE_AMAP_KEY
ENV VITE_AMAP_KEY=$VITE_AMAP_KEY
RUN npm run build

# --- 阶段 2：Nginx 纯静态托管（HTTPS 由外层 OpenResty 终结，本镜像只跑 HTTP）---
FROM nginx:1.27-alpine
COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
