# SmartEdu-Platform 智慧教育平台

## 一、项目概述

SmartEdu-Platform 是一个前后端分离的智慧教育平台，旨在为学生、教师和管理员提供一站式教学服务。平台集成了知识图谱可视化、智能作业批改、个性化学习计划、AI答疑、学情分析等多项智慧教育功能。

---

## 二、技术栈详解

### 2.1 后端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| **Spring Boot** | 3.2.0 | 核心框架，提供RESTful API服务 |
| **Spring Security** | - | 安全框架，实现认证与授权 |
| **Spring Validation** | - | 参数校验 |
| **Spring Actuator** | - | 健康检查与监控 |
| **MyBatis-Plus** | 3.5.5 | ORM框架，简化数据库操作 |
| **MySQL** | 8.0 | 主数据库 |
| **JWT (JJWT)** | 0.12.3 | 无状态认证，Token生成与验证 |
| **SpringDoc OpenAPI** | 2.2.0 | API文档生成（Swagger UI） |
| **Apache POI** | 5.2.5 | Word文档解析，题目导入 |
| **Apache PDFBox** | 3.0.1 | PDF文档解析，题目导入 |
| **Lombok** | 1.18.36 | 代码简化，自动生成getter/setter |
| **Jackson** | - | JSON序列化/反序列化 |
| **Java** | 17 | 编程语言版本 |

### 2.2 前端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| **Vue 3** | 3.4.0 | 核心前端框架（Composition API） |
| **Vite** | 5.0.0 | 构建工具，开发服务器 |
| **TypeScript** | 5.3.0 | 类型安全的JavaScript |
| **Element Plus** | 2.5.0 | UI组件库 |
| **@element-plus/icons-vue** | 2.3.1 | Element Plus图标库 |
| **Pinia** | 2.1.7 | 状态管理 |
| **pinia-plugin-persistedstate** | 3.2.1 | Pinia持久化插件 |
| **Vue Router** | 4.2.5 | 路由管理 |
| **Axios** | 1.6.5 | HTTP请求库 |
| **ECharts** | 5.4.3 | 数据可视化图表 |
| **vue-echarts** | 6.6.9 | Vue封装的ECharts组件 |
| **dayjs** | 1.11.10 | 日期处理库 |
| **lodash-es** | 4.17.21 | 工具函数库 |
| **vue-draggable-plus** | 0.6.1 | 拖拽组件 |
| **cropperjs** | 1.6.2 | 图片裁剪 |
| **Sass** | 1.69.0 | CSS预处理器 |
| **unplugin-auto-import** | 0.17.3 | 自动导入API |
| **unplugin-vue-components** | 0.26.0 | 组件自动注册 |

---

## 三、项目架构

### 3.1 目录结构

```
SmartEdu-Platform/
├── database/                      # 数据库相关
│   └── schema.sql                 # MySQL DDL脚本（建表+初始化数据）
│
├── smartedu-backend/              # 后端服务（Spring Boot）
│   ├── src/main/java/com/smartedu/
│   │   ├── controller/            # 控制器层（REST API）
│   │   ├── service/               # 业务逻辑层
│   │   ├── mapper/                # 数据访问层（MyBatis-Plus）
│   │   ├── entity/                # 实体类（对应数据库表）
│   │   ├── dto/                   # 数据传输对象（请求参数）
│   │   ├── vo/                    # 视图对象（响应数据）
│   │   ├── common/                # 公共类
│   │   │   ├── result/            # 统一响应封装
│   │   │   └── exception/         # 异常处理
│   │   ├── config/                # 配置类
│   │   ├── security/              # 安全相关（JWT、过滤器）
│   │   └ task/                    # 定时任务
│   │   └ SmartEduApplication.java # 启动类
│   │
│   ├── src/main/resources/
│   │   ├── application.yml        # 配置文件
│   │   └ mapper/                  # MyBatis XML映射文件
│   │
│   └ pom.xml                      # Maven依赖配置
│
└── smartedu-ui/                   # 前端应用（Vue 3）
    ├── src/
    │   ├── api/                   # API接口封装
    │   ├── assets/                # 静态资源
    │   ├── components/            # 公共组件
    │   ├── layout/                # 布局组件
    │   ├── router/                # 路由配置
    │   ├── store/                 # Pinia状态管理
    │   ├── styles/                # 全局样式
    │   ├── utils/                 # 工具函数
    │   ├── views/                 # 页面组件
    │   │   ├── auth/              # 认证页面（登录/注册）
    │   │   ├── student/           # 学生端页面
    │   │   ├── teacher/           # 教师端页面
    │   │   ├── admin/             # 管理端页面
    │   │   ├── home/              # 首页
    │   │   ├── profile/           # 个人中心
    │   │   ├── video/             # 视频学习
    │   │   ├── question-hall/     # 答疑大厅
    │   │
    │   ├── App.vue                # 根组件
    │   ├── main.ts                # 入口文件
    │
    ├── index.html                 # HTML入口
    ├── package.json               # NPM依赖配置
    ├── vite.config.js             # Vite配置
    ├── tsconfig.json              # TypeScript配置
```

### 3.2 后端分层架构

```
Controller层（API接口）
    ↓
Service层（业务逻辑）
    ↓
Mapper层（数据访问）
    ↓
Entity层（数据实体）
    ↓
MySQL数据库
```

### 3.3 前端架构

```
Views（页面组件）
    ↓
Components（公共组件）
    ↓
API（接口调用）→ Axios → 后端API
    ↓
Store（状态管理）← Pinia
    ↓
Router（路由控制）
```

---

## 四、数据库设计

### 4.1 数据表清单

| 表名 | 说明 | 主要字段 |
|------|------|----------|
| `users` | 用户表 | id, username, password, real_name, role, grade, major |
| `courses` | 课程表 | id, course_name, course_code, teacher_id, credit |
| `knowledge_points` | 知识点表 | id, course_id, parent_id, name, difficulty_level |
| `knowledge_relations` | 知识点关联表 | source_kp_id, target_kp_id, relation_type |
| `questions` | 题目表 | id, question_type, content, answer, difficulty_level |
| `error_logs` | 错题记录表 | user_id, question_id, error_type, review_status |
| `homework` | 作业表 | id, title, course_id, teacher_id, question_ids |
| `homework_submissions` | 作业提交表 | homework_id, user_id, answers, score |
| `learning_plans` | 学习计划表 | user_id, title, start_date, end_date, progress |
| `learning_resources` | 学习资源表 | title, resource_type, url, course_id |
| `learning_analytics` | 学情分析表 | user_id, report_type, study_time_hours, correct_rate |
| `qa_items` | 问答表 | user_id, title, content, answer, answer_type |
| `news` | 新闻表 | title, summary, image_url, news_type |
| `question_topic` | 问题话题表 | 用户发布的讨论帖子 |
| `question_reply` | 问题回复表 | 帖子回复 |
| `question_like` | 问题点赞表 | 点赞记录 |
| `video_collection` | 视频集合表 | 视频学习资源 |

### 4.2 角色设计

系统支持三种用户角色：

| 角色 | 角色 code | 功能权限 |
|------|----------|----------|
| 学生 | STUDENT | 学习看板、知识图谱、学习计划、错题分析、作业提交、答疑大厅、视频学习 |
| 教师 | TEACHER | 教学看板、作业管理、学情分析、智能备课、课堂评估 |
| 管理员 | ADMIN | 新闻管理、视频审核、首页推荐、教师端功能 |

---

## 五、核心功能模块

### 5.1 学生端功能

#### 5.1.1 学习看板（Dashboard）
- 学习数据概览
- 今日任务提醒
- 学习进度可视化
- 班级排名展示

#### 5.1.2 知识图谱可视化
- 课程知识点树状结构展示
- 知识点依赖关系可视化
- 知识点掌握程度标识
- 支持ECharts交互式图表

#### 5.1.3 个性化学习计划
- 目标设定（成绩提升、证书考取、技能掌握）
- 计划周期设置
- 每日学习时长规划
- 进度跟踪与完成度统计

#### 5.1.4 错题智能分析
- 错题自动收集
- 错误类型分类（概念错误、计算错误、理解错误、粗心错误）
- 艾宾浩斯记忆曲线复习提醒
- 错题知识点关联分析
- 复习状态跟踪

#### 5.1.5 作业管理
- 待完成作业列表
- 作业详情查看
- 在线答题提交
- 作业成绩查看
- 历史作业记录

#### 5.1.6 答疑大厅
- 问题发布
- AI自动回答
- 教师/学生回答
- 问题分类（概念理解、习题答疑、其他）
- 问题点赞与浏览统计

### 5.2 教师端功能

#### 5.2.1 教学看板
- 课程教学数据概览
- 学生提交统计
- 批改任务提醒
- 教学进度可视化

#### 5.2.2 作业管理
- 作业创建（支持两种模式）
  - **题库模式**：从题库选择题组卷
  - **快速作业模式**：上传Word/PDF文档自动解析题目
- 作业发布与截止时间设置
- 自动/手动批改切换
- 提交次数限制
- 答题时长限制
- 批改结果统计
- AI智能批改（简答题、计算题）

#### 5.2.3 学情分析报告
- 学生学习时长统计
- 正确率分析
- 知识点掌握度分析
- 薄弱点识别
- 学习建议生成
- 班级排名对比
- 周报/月报/课程总结

#### 5.2.4 智能备课资源推荐
- 知识点相关资源推荐
- 视频资源推荐
- 文档资源推荐
- 习题资源推荐
- 资源难度匹配

#### 5.2.5 课堂效果评估
- 课堂互动数据分析
- 学生参与度评估
- 教学效果评分
- 改进建议生成

### 5.3 管理端功能

#### 5.3.1 新闻管理
- 科技前沿资讯管理
- 轮播图新闻设置
- 列表新闻管理
- 新闻自动抓取（定时任务）
- 手动添加新闻
- 新闻置顶设置

#### 5.3.2 视频审核
- 用户上传视频审核
- 视频状态管理（待审核、已通过、已拒绝）
- 视频内容预览
- 审核意见填写

#### 5.3.3 首页推荐管理
- 推荐内容配置
- 推荐顺序调整
- 推荐状态控制

### 5.4 公共功能

#### 5.4.1 首页
- 轮播图展示（科技前沿资讯）
- 新闻列表
- 平台介绍

#### 5.4.2 认证模块
- 用户登录（JWT Token认证）
- 用户注册（角色选择）
- 个人信息管理
- 密码修改
- 头像上传裁剪

#### 5.4.3 答疑大厅（公共版）
- 问题话题发布
- 问题回复讨论
- 点赞功能
- 问题详情查看

#### 5.4.4 视频学习
- 视频列表浏览
- 视频播放学习
- 视频上传（需审核）

---

## 六、特色功能实现

### 6.1 AI智能批改作业

- 后端服务：`AIGradeService`
- 支持题型：简答题、计算题、填空题
- 批改逻辑：对比学生答案与标准答案，智能评分

### 6.2 AI答疑系统

- 后端服务：`QAController`、`QaItemService`
- AI自动回答学生提问
- 支持教师补充回答
- 问题分类管理

### 6.3 文档题目解析

- 后端服务：`QuestionImportService`
- 支持格式：Word（.doc/.docx）、PDF
- 技术实现：Apache POI解析Word、Apache PDFBox解析PDF
- 自动提取题目内容、选项、答案

### 6.4 知识图谱可视化

- 前端组件：`KnowledgeGraph.vue`
- 技术实现：ECharts关系图/树图
- 数据来源：`knowledge_points`表 + `knowledge_relations`表
- 支持节点展开/折叠、知识点详情查看

### 6.5 艾宾浩斯记忆曲线复习

- 数据字段：`error_logs.next_review_time`
- 复习周期：根据遗忘曲线自动计算下次复习时间
- 复习状态：未复习、已复习、已掌握

### 6.6 学情分析报告生成

- 后端服务：`AIAnalysisService`
- 报告类型：周报、月报、课程总结
- 分析维度：学习时长、正确率、知识点掌握度、薄弱点

---

## 七、API接口概览

### 7.1 认证接口 (`/auth`)

| 接口 | 方法 | 说明 |
|------|------|------|
| `/auth/login` | POST | 用户登录 |
| `/auth/register` | POST | 用户注册 |
| `/auth/logout` | POST | 用户登出 |
| `/auth/info` | GET | 获取当前用户信息 |

### 7.2 学生端接口 (`/student`)

| 接口 | 方法 | 说明 |
|------|------|------|
| `/student/dashboard` | GET | 学习看板数据 |
| `/student/knowledge-graph` | GET | 知识图谱数据 |
| `/student/learning-plan` | GET/POST | 学习计划管理 |
| `/student/error-analysis` | GET | 错题分析数据 |
| `/student/homework` | GET | 学生作业列表 |
| `/student/homework/{id}` | GET | 作业详情 |
| `/student/homework/submit` | POST | 提交作业 |
| `/student/qa` | GET/POST | 答疑大厅 |

### 7.3 教师端接口 (`/teacher`)

| 接口 | 方法 | 说明 |
|------|------|------|
| `/teacher/dashboard` | GET | 教学看板数据 |
| `/teacher/homework` | GET/POST | 作业管理 |
| `/teacher/homework/quick` | POST | 快速作业（文档上传） |
| `/teacher/homework/grade` | POST | 作业批改 |
| `/teacher/analysis` | GET | 学情分析报告 |
| `/teacher/lesson-prep` | GET | 智能备课资源 |
| `/teacher/class-evaluation` | GET | 课堂评估数据 |

### 7.4 管理端接口 (`/admin`)

| 接口 | 方法 | 说明 |
|------|------|------|
| `/admin/news` | GET/POST/PUT/DELETE | 新闻管理 |
| `/admin/video-audit` | GET/PUT | 视频审核 |
| `/admin/home-recommend` | GET/PUT | 首页推荐管理 |

### 7.5 其他接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/news` | GET | 新闻列表（公开） |
| `/user/{id}` | GET/PUT | 用户信息管理 |
| `/courses` | GET | 课程列表 |
| `/questions` | GET/POST | 题目管理 |
| `/question-hall` | GET/POST | 答疑大厅 |
| `/video` | GET/POST | 视频学习 |

---

## 八、前端页面路由

### 8.1 路由配置

| 路径 | 页面 | 角色 | 说明 |
|------|------|------|------|
| `/login` | Login.vue | 公开 | 登录页 |
| `/register` | Register.vue | 公开 | 注册页 |
| `/home` | Home/Index.vue | 公开 | 首页 |
| `/profile` | Profile/Index.vue | 登录用户 | 个人中心 |
| `/student/dashboard` | student/Dashboard.vue | 学生 | 学习看板 |
| `/student/knowledge-graph` | student/KnowledgeGraph.vue | 学生 | 知识图谱 |
| `/student/learning-plan` | student/LearningPlan.vue | 学生 | 学习计划 |
| `/student/error-analysis` | student/ErrorAnalysis.vue | 学生 | 错题分析 |
| `/student/homework` | student/Homework.vue | 学生 | 我的作业 |
| `/student/qa-hall` | student/QAHall.vue | 学生 | 答疑大厅 |
| `/teacher/dashboard` | teacher/Dashboard.vue | 教师 | 教学看板 |
| `/teacher/homework` | teacher/HomeworkManage.vue | 教师 | 作业管理 |
| `/teacher/analysis` | teacher/AnalysisReport.vue | 教师 | 学情分析 |
| `/teacher/lesson-prep` | teacher/LessonPrep.vue | 教师 | 智能备课 |
| `/teacher/class-evaluation` | teacher/ClassEvaluation.vue | 教师 | 课堂评估 |
| `/admin/news` | admin/NewsManage.vue | 管理员 | 新闻管理 |
| `/admin/video-audit` | admin/VideoAudit.vue | 管理员 | 视频审核 |
| `/admin/home-recommend` | admin/HomeRecommendManage.vue | 管理员 | 首页推荐 |
| `/video-study/list` | video/VideoStudy.vue | 公开 | 视频学习 |
| `/question-hall/list` | question-hall/Index.vue | 登录用户 | 答疑大厅 |
| `/question-hall/:id` | question-hall/Detail.vue | 登录用户 | 帖子详情 |

### 8.2 路由守卫

- 未登录访问需认证页面 → 重定向到登录页
- 已登录访问登录/注册页 → 重定向到首页
- 角色权限不足 → 重定向到首页
- Token存储：localStorage（Pinia持久化插件）

---

## 九、启动与部署

### 9.1 数据库初始化

```bash
# 执行DDL脚本
mysql -u root -p < database/schema.sql
```

### 9.2 后端启动

```bash
cd smartedu-backend

# Maven编译运行
mvn spring-boot:run

# 或打包后运行
mvn clean package
java -jar target/smartedu-backend-1.0.0-SNAPSHOT.jar
```

访问 http://localhost:8080/api/swagger-ui.html 查看 API文档

### 9.3 前端启动

```bash
cd smartedu-ui

# 安装依赖
npm install

# 开发模式
npm run dev

# 生产构建
npm run build
```

访问 http://localhost:5173

### 9.4 测试账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| student001 | 123456 | 学生 |
| teacher001 | 123456 | 教师 |
| admin001 | 123456 | 管理员 |

---

## 十、项目特色总结

1. **前后端分离架构**：Spring Boot + Vue 3，职责清晰，易于维护
2. **JWT无状态认证**：Token机制，支持分布式部署
3. **角色权限管理**：学生/教师/管理员三角色，路由守卫控制权限
4. **AI智能功能**：作业批改、答疑系统、学情分析
5. **知识图谱可视化**：ECharts交互式图表，知识点关系清晰
6. **文档智能解析**：Word/PDF题目自动提取，提高效率
7. **艾宾浩斯复习提醒**：科学记忆曲线，提高学习效果
8. **定时任务**：新闻自动抓取更新
9. **完善的数据统计**：学习时长、正确率、知识点掌握度等多维度分析
10. **现代化前端技术**：TypeScript、Vite、Composition API，开发体验优秀

---

## 十一、开发团队

SmartEdu Team