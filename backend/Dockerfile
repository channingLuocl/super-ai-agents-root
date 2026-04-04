FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# 安装 curl 和 Node.js (npx 需要)
RUN apk add --no-cache curl nodejs npm

# 预安装 amap-maps MCP server npm 包（避免启动时下载超时）
RUN npm install -g @amap/amap-maps-mcp-server

# 复制已编译的 jar 包
COPY dist/app.jar app.jar

# 复制 MCP server jar 包
COPY dist/mcp/image-search-mcp-server.jar mcp/image-search-mcp-server.jar

# 创建临时目录
RUN mkdir -p /app/tmp

EXPOSE 8123

ENV TZ=Asia/Shanghai

ENTRYPOINT ["java", "-jar", "app.jar"]