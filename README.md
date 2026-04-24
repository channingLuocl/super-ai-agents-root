# Super AI Agents Backend

## 环境要求

- Java 21
- Maven 3.x
- Redis Stack（带 RediSearch 模块），端口 6380
- Node.js（用于 amap-maps MCP 服务）

## 快速启动

### 1. 启动 Redis

确保 Redis Stack 运行在 6380 端口：

```bash
redis-server --port 6380 --loadmodule /path/to/redisearch.so
```

或使用 Redis Stack 容器：

```bash
docker run -d -p 6380:6379 redis/redis-stack-server:latest
```

### 2. 编译 MCP 子模块

```bash
cd backend/image-search-mcp-server
./mvnw clean package -DskipTests
cd ../..
```
或者idea打开image-search-mcp-server然后去compile出jar包

### 3. 配置 API Key

直接修改 `backend/src/main/resources/application.yml` 中的以下配置：

```yaml
spring:
  ai:
    openai:
      api-key: 你的MiniMax-API-Key
      base-url: https://api.minimaxi.com
    dashscope:
      api-key: 你的阿里云百练API-Key
search-api:
  api-key: 你的搜索API-Key
```

### 4. 编译运行主应用

```bash
cd backend
./mvnw clean compile -DskipTests
./mvnw spring-boot:run
```

或使用 IDE 直接运行 `SuperAiAgentsApplication.java`。

## 验证

- 接口文档：http://localhost:8123/api/swagger-ui.html
- Redis 向量库索引：`spring_ai_index`，key 前缀 `doc:`
