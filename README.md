# SmartEdu-Platform 智慧教育平台

> 前后端分离的智慧教育平台项目

## 项目结构

```
SmartEdu-Platform/
├── database/                 # 数据库脚本
│   └── schema.sql           # MySQL DDL 语句
├── smartedu-backend/        # Spring Boot 后端
│   ├── src/main/java/com/smartedu/
│   │   ├── controller/      # 控制器层
│   │   ├── service/         # 服务层
│   │   ├── mapper/          # 数据访问层
│   │   ├── entity/          # 实体类
│   │   ├── dto/             # 数据传输对象
│   │   ├── vo/              # 视图对象
│   │   ├── common/          # 公共类
│   │   │   ├── result/      # 统一响应
│   │   │   └── exception/   # 异常处理
│   │   ├── config/          # 配置类
│   │   └── SmartEduApplication.java
│   ├── src/main/resources/
│   │   ├── application.yml  # 配置文件
│   │   └── mapper/          # MyBatis XML
│   └── pom.xml
└── smartedu-ui/             # Vue 3 前端
    ├── src/
    │   ├── api/             # API 接口
    │   ├── assets/          # 静态资源
    │   ├── components/      # 组件
    │   ├── layout/          # 布局
    │   ├── router/          # 路由
    │   ├── store/           # 状态管理
    │   ├── styles/          # 样式
    │   ├── utils/           # 工具
    │   ├── views/           # 页面
    │   ├── App.vue
    │   └── main.ts
    ├── index.html
    ├── package.json
    ├── vite.config.js
    └── tsconfig.json
```

## 技术栈

### 后端
- Spring Boot 3.2
- Spring Security
- MyBatis-Plus
- MySQL 8.0
- JWT
- Swagger/OpenAPI

### 前端
- Vue 3
- Vite
- TypeScript
- Element Plus
- Pinia
- Vue Router
- Axios
- ECharts

## 快速开始

详细测试指南请查看 [TESTING_GUIDE.md](./TESTING_GUIDE.md)

### 数据库初始化
```bash
mysql -u root -p < database/schema.sql
```

### 后端启动
```bash
cd smartedu-backend
mvn spring-boot:run
```
访问 http://localhost:8080/api/swagger-ui.html 查看 API 文档

### 前端启动
```bash
cd smartedu-ui
npm install
npm run dev
```
访问 http://localhost:5173

## 核心功能

### 学生端
- 知识图谱可视化
- 个性化学习计划生成
- 错题智能分析
- 虚拟答疑大厅

### 教师端
- 作业自动批改
- 学情分析报告
- 智能备课资源推荐
- 课堂效果评估

## 测试账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| student001 | 123456 | 学生 |
| teacher001 | 123456 | 教师 |
| admin001 | 123456 | 管理员 |

## 开发团队

SmartEdu Team
