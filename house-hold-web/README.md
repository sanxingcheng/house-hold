# house-hold-web

迭代一前端：Vue 3 + TypeScript + Vue Router + Pinia + Axios，登录/注册与路由守卫。

## 环境

- Node 18+
- 后端 Gateway 运行在 `http://localhost:8080`（或通过 Vite 代理 `/api` 转发）

## 运行

```bash
npm install
npm run dev
```

访问 http://localhost:5173

## 构建

```bash
npm run build
```

产出在 `dist/`，需配置生产环境 `VITE_API_BASE_URL` 为实际 Gateway 地址。
