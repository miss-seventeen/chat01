# chat01 Frontend

Vue 3 + Vite 前端，配套后端 Spring Boot `ChatController` (`/chat/*`).

## 启动

```bash
npm install
npm run dev
```

默认 dev 端口 5173，通过 Vite 代理将 `/chat` 请求转发到 `http://localhost:8080`（在 `vite.config.js` 中可修改）。

## 目录结构

```
frontend/
├── index.html
├── package.json
├── vite.config.js
└── src/
    ├── main.js
    ├── App.vue
    ├── api/chat.js          # 后端接口封装
    ├── assets/style.css     # 全局样式
    ├── components/
    │   ├── Sidebar.vue      # 侧边栏 + 会话列表
    │   ├── MessageList.vue  # 消息列表
    │   └── MessageInput.vue # 输入框
    └── views/ChatView.vue   # 聊天主视图
```

## 构建

```bash
npm run build
```

产物在 `dist/`。
