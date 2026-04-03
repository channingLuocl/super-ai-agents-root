FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# 安装必要的工具
RUN apk add --no-cache curl

# 复制 jar 包
COPY target/*.jar app.jar

# 复制 MCP 配置文件
COPY src/main/resources/mcp-servers.json /app/mcp-servers.json

# 创建临时目录
RUN mkdir -p /app/tmp

EXPOSE 8123

# 设置时区
ENV TZ=Asia/Shanghai

ENTRYPOINT ["java", "-jar", "app.jar"]