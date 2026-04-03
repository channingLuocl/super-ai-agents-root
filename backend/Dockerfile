# 构建阶段
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# 安装 Maven
RUN apk add --no-cache maven

# 复制 pom.xml 和源代码
COPY pom.xml .
COPY src ./src
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn ./.mvn

# 编译项目（跳过测试）
RUN mvn clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# 安装必要的工具
RUN apk add --no-cache curl

# 从构建阶段复制 jar 包
COPY --from=build /app/target/*.jar app.jar

# 创建临时目录
RUN mkdir -p /app/tmp

EXPOSE 8123

# 设置时区
ENV TZ=Asia/Shanghai

ENTRYPOINT ["java", "-jar", "app.jar"]