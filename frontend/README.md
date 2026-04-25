# Frontend

`frontend/` 是“馋嘴小迪”美食助手的前端项目，基于 Vue 3 + Vite 构建。

当前版本已经收敛为单入口聊天界面，所有消息统一通过 SSE 调用后端美食 Agent。

## 技术栈

- Vue 3
- Vue Router
- Axios
- SSE (Server-Sent Events)
- Vite
- marked（Markdown 渲染）

## 本地开发

### 环境要求

- Node.js >= 16
- npm >= 7

### 安装依赖

```bash
cd /Users/luocl/Desktop/super-ai-agents-root/frontend
npm install
```

### 启动开发服务器

```bash
npm run dev
```

默认访问地址：

- [http://localhost:3000](http://localhost:3000)

### 生产构建

```bash
npm run build
```

## 当前页面能力

- 聊天会话列表
- 新建 / 切换 / 删除会话
- 单 SSE 流式聊天
- Markdown 消息渲染
- 用户画像弹窗
- 浏览器定位按钮

## 当前聊天接口

前端现在只使用一个聊天 SSE 接口：

- `GET /api/ai/food/agent/stream`

请求参数：

- `message`: 用户消息
- `chatId`: 当前会话 ID
- `longitude`: 可选，经度
- `latitude`: 可选，纬度

前端 API 封装位置：

- [/Users/luocl/Desktop/super-ai-agents-root/frontend/src/api/index.js](/Users/luocl/Desktop/super-ai-agents-root/frontend/src/api/index.js)

核心页面位置：

- [/Users/luocl/Desktop/super-ai-agents-root/frontend/src/views/Home.vue](/Users/luocl/Desktop/super-ai-agents-root/frontend/src/views/Home.vue)

## 定位行为说明

前端不会在页面加载时自动申请定位权限。

只有两种情况下会尝试获取浏览器定位：

1. 用户主动点击输入框左侧的定位按钮
2. 用户发送的问题命中了位置相关关键词，例如：
   - `附近`
   - `位置`
   - `餐厅`
   - `出去吃`

如果定位失败，仍会继续发送消息，只是不附带经纬度。

## 会话相关接口

除了聊天 SSE，前端还会调用以下接口管理聊天记录：

- `GET /api/chat/conversations`
- `POST /api/chat/conversations`
- `GET /api/chat/conversations/{chatId}`
- `PUT /api/chat/conversations/{chatId}/messages`
- `DELETE /api/chat/conversations/{chatId}`
- `GET /api/chat/current`
- `PUT /api/chat/current/{chatId}`
- `GET /api/memory/profile/{userId}`

## 当前约束

- 前端已移除“普通聊天 / RAG 模式切换”
- 页面默认只面向美食 Agent，不再承载其他主题应用
- SSE 现在只渲染最终回答，不再展示后端工具 JSON 调试流
