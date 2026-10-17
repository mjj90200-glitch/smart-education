# ============================================================
# SmartEdu-Platform 前端 Dockerfile
# ============================================================

# ------------------------------------------------------------
# 阶段 1: 构建阶段
# ------------------------------------------------------------
FROM node:20-alpine AS builder

# 设置工作目录
WORKDIR /app

# 复制 package.json 和 package-lock.json (如果有)
COPY package*.json ./

# 安装依赖（使用中国镜像加速）
RUN npm install --registry=https://registry.npmmirror.com

# 复制所有源代码
COPY . .

# 执行构建（生产环境，明确指定模式）
RUN npm run build -- --mode production

# ------------------------------------------------------------
# 阶段 2: 生产阶段
# ------------------------------------------------------------
FROM nginx:stable-alpine

# 复制自定义 nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 复制构建好的静态文件到 Nginx 目录
COPY --from=builder /app/dist /usr/share/nginx/html

# 创建上传目录（用于访问后端上传的文件）
RUN mkdir -p /usr/share/nginx/html/uploads

# 设置静态文件权限
RUN chmod -R 755 /usr/share/nginx/html

# 暴露端口
EXPOSE 80

# Nginx 前台运行
CMD ["nginx", "-g", "daemon off;"]
