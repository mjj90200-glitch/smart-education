#!/bin/bash
# ============================================================
# SmartEdu-Platform 一键部署脚本
# ============================================================
# 使用方法：
#   chmod +x deploy.sh
#   ./deploy.sh
# ============================================================

set -e  # 遇到错误立即退出

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

echo_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

echo_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo_info "============================================"
echo_info "  SmartEdu-Platform 一键部署脚本"
echo_info "============================================"
echo_info ""

# ------------------------------------------------------------
# 步骤 1: 检查环境
# ------------------------------------------------------------
echo_info "步骤 1/5: 检查环境依赖..."

command -v git >/dev/null 2>&1 || { echo_error "需要安装 git"; exit 1; }
command -v mvn >/dev/null 2>&1 || { echo_error "需要安装 Maven"; exit 1; }
command -v node >/dev/null 2>&1 || { echo_error "需要安装 Node.js"; exit 1; }
command -v docker >/dev/null 2>&1 || { echo_error "需要安装 Docker"; exit 1; }
command -v docker-compose >/dev/null 2>&1 || { echo_error "需要安装 Docker Compose"; exit 1; }

echo_info "环境检查通过"
echo_info ""

# ------------------------------------------------------------
# 步骤 2: 拉取最新代码
# ------------------------------------------------------------
echo_info "步骤 2/5: 拉取最新代码..."

if [ -d ".git" ]; then
    git pull origin main || echo_warn "Git pull 失败，继续执行..."
    echo_info "代码已更新"
else
    echo_warn "当前目录不是 Git 仓库，跳过代码更新"
fi
echo_info ""

# ------------------------------------------------------------
# 步骤 3: 构建后端
# ------------------------------------------------------------
echo_info "步骤 3/5: 构建后端..."

cd "$SCRIPT_DIR/smartedu-backend"

# 清理并打包
mvn clean package -DskipTests

if [ ! -f "target/*.jar" ]; then
    echo_error "后端构建失败：未找到 JAR 文件"
    exit 1
fi

echo_info "后端构建完成"
echo_info ""

# ------------------------------------------------------------
# 步骤 4: 构建前端
# ------------------------------------------------------------
echo_info "步骤 4/5: 构建前端..."

cd "$SCRIPT_DIR/smartedu-ui"

# 安装依赖并构建
npm install --registry=https://registry.npmmirror.com
npm run build

if [ ! -d "dist" ]; then
    echo_error "前端构建失败：未找到 dist 目录"
    exit 1
fi

echo_info "前端构建完成"
echo_info ""

# ------------------------------------------------------------
# 步骤 5: 启动 Docker 服务
# ------------------------------------------------------------
echo_info "步骤 5/5: 启动 Docker 服务..."

cd "$SCRIPT_DIR"

# 确保上传目录存在
mkdir -p "$SCRIPT_DIR/uploads"

# 确保 MySQL 数据目录存在
mkdir -p "$SCRIPT_DIR/mysql/data"

# 停止并清理旧容器
docker-compose down 2>/dev/null || true

# 启动服务
docker-compose up -d --build

echo_info ""
echo_info "============================================"
echo_info "  部署完成！"
echo_info "============================================"
echo_info ""
echo_info "  访问地址:"
echo_info "  - 前端：http://localhost"
echo_info "  - 后端 API: http://localhost:8080/api"
echo_info "  - Swagger: http://localhost:8080/api/swagger-ui.html"
echo_info ""
echo_info "  常用命令:"
echo_info "  - 查看日志：docker-compose logs -f"
echo_info "  - 停止服务：docker-compose down"
echo_info "  - 重启服务：docker-compose restart"
echo_info ""
