# 部署指南

## 一、阿里云服务器配置

### 1. 购买服务器
- 推荐配置：2核4G，带宽3-5Mbps
- 操作系统：Ubuntu 22.04
- 安全组开放端口：22, 80, 443

### 2. 连接服务器
```bash
ssh root@你的服务器公网IP
```

## 二、服务器环境安装

```bash
# 安装 Docker
curl -fsSL https://get.docker.com | sh
systemctl enable docker
systemctl start docker

# 安装 Docker Compose
apt update
apt install docker-compose-plugin -y

# 安装 Git
apt install git -y
```

## 三、部署项目

### 方式一：从 Git 拉取部署

```bash
# 克隆项目
git clone 你的仓库地址
cd super-ai-agents-root

# 配置 API Key
# 编辑 backend/src/main/resources/application.yml
# 填入 MiniMax、阿里云百练和搜索 API Key

# 构建并启动
docker compose up -d --build
```

### 方式二：本地构建后上传

```bash
# 本地执行（在你的电脑上）
# 1. 构建 backend jar 包
cd backend
./mvnw clean package -DskipTests

# 2. 上传到服务器
scp -r ../super-ai-agents-root root@你的服务器IP:/root/

# 服务器上执行
cd /root/super-ai-agents-root
docker compose up -d --build
```

## 四、验证部署

```bash
# 查看容器状态
docker compose ps

# 查看日志
docker compose logs -f

# 测试访问
curl http://localhost/api/
```

## 五、公网访问

部署完成后，通过以下地址访问：
- 前端：http://你的公网IP
- 后端 API：http://你的公网IP/api
- Swagger 文档：http://你的公网IP/api/swagger-ui.html

## 六、配置域名（可选）

如果有域名，可以在阿里云配置解析到服务器IP，然后配置 HTTPS。

### 使用 Nginx + Let's Encrypt

```bash
# 安装 certbot
apt install certbot python3-certbot-nginx -y

# 申请证书（先停止 80 端口）
docker compose down
certbot certonly --standalone -d 你的域名

# 证书路径
# /etc/letsencrypt/live/你的域名/fullchain.pem
# /etc/letsencrypt/live/你的域名/privkey.pem
```

## 七、常用命令

```bash
# 启动
docker compose up -d

# 停止
docker compose down

# 重启
docker compose restart

# 查看日志
docker compose logs -f backend
docker compose logs -f frontend

# 重新构建
docker compose up -d --build
```

## 八、注意事项

1. **API Key 安全**：不要将 API Key 提交到 Git 仓库
2. **安全组**：确保阿里云安全组开放了必要端口
3. **防火墙**：Ubuntu 默认没有防火墙，如有需要配置 ufw
4. **资源监控**：使用 `docker stats` 监控容器资源使用
