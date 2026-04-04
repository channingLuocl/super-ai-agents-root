# 构建阶段 - 使用 Maven 官方镜像
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# 复制项目文件
COPY pom.xml .
COPY src ./src

# 编译项目（跳过测试）
RUN mvn clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# 安装 curl
RUN apk add --no-cache curl

# 从构建阶段复制 jar 包
COPY --from=build /app/target/*.jar app.jar

# 创建临时目录
RUN mkdir -p /app/tmp

EXPOSE 8123

ENV TZ=Asia/Shanghai

ENTRYPOINT ["java", "-jar", "app.jar"]