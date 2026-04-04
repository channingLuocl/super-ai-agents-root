FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

RUN apk add --no-cache curl

# 复制已编译的 jar 包
COPY dist/app.jar app.jar

RUN mkdir -p /app/tmp

EXPOSE 8123

ENV TZ=Asia/Shanghai

ENTRYPOINT ["java", "-jar", "app.jar"]