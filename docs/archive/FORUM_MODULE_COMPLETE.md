# 答疑大厅模块开发完成报告

## 一、模块概述

答疑大厅（Forum）是一个基于 Spring Boot + Vue3 的问答社区模块，支持学生、教师发帖提问、回复解答，并集成 AI 智能补位功能。

---

## 二、后端 API 接口清单

### 基础信息
- **根路径**: `/api/forum`
- **认证方式**: JWT Token

### 接口列表

| 方法 | 路径 | 说明 | 认证要求 |
|------|------|------|----------|
| GET | `/list` | 获取帖子列表 | 否 |
| GET | `/{id}` | 获取帖子详情 | 否 |
| POST | `/create` | 创建帖子 | 是 |
| POST | `/{id}/reply` | 发布回复 | 是 |
| POST | `/{id}/ai-help` | 求助 AI | 是 |
| POST | `/{postId}/accept/{replyId}` | 采纳答案 | 是 |
| POST | `/{id}/like` | 点赞帖子 | 是 |
| POST | `/reply/{id}/like` | 点赞回复 | 是 |

### 参数说明

#### 1. 获取帖子列表 `GET /list`

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| sortBy | string | 否 | latest | 排序：latest=最新，bounty=高悬赏 |
| category | string | 否 | - | 分类：CONCEPT/HOMEWORK/EXAM/OTHER |
| status | integer | 否 | - | 状态：0=进行中，1=已解决，2=已关闭 |
| pageNum | integer | 否 | 1 | 页码 |
| pageSize | integer | 否 | 20 | 每页数量 |

#### 2. 创建帖子 `POST /create`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 是 | 标题（最长 200 字） |
| content | string | 是 | 内容（支持 Markdown） |
| category | string | 否 | 分类，默认 OTHER |
| bountyScore | integer | 否 | 悬赏分，默认 0 |
| file | MultipartFile | 否 | 附件（Word/PDF/TXT） |

---

## 三、数据库表结构

### 1. forum_posts（帖子表）

```sql
CREATE TABLE `forum_posts` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '发帖人 ID',
    `course_id` BIGINT DEFAULT NULL COMMENT '关联课程 ID',
    `knowledge_point_id` BIGINT DEFAULT NULL COMMENT '关联知识点 ID',
    `title` VARCHAR(200) NOT NULL COMMENT '帖子标题',
    `content` TEXT NOT NULL COMMENT '帖子内容',
    `attachment_url` VARCHAR(500) DEFAULT NULL COMMENT '附件路径',
    `attachment_name` VARCHAR(255) DEFAULT NULL COMMENT '附件名称',
    `bounty_score` INT DEFAULT 0 COMMENT '悬赏分数',
    `category` VARCHAR(50) DEFAULT NULL COMMENT '分类',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-进行中，1-已解决，2-已关闭',
    `view_count` INT DEFAULT 0 COMMENT '浏览次数',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `reply_count` INT DEFAULT 0 COMMENT '回复数',
    `is_top` TINYINT DEFAULT 0 COMMENT '是否置顶',
    `is_anonymous` TINYINT DEFAULT 0 COMMENT '是否匿名',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答疑大厅帖子表';
```

### 2. forum_replies（回复表）

```sql
CREATE TABLE `forum_replies` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `post_id` BIGINT NOT NULL COMMENT '帖子 ID',
    `user_id` BIGINT NOT NULL COMMENT '回复人 ID（0 表示 AI 助手）',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父回复 ID',
    `content` TEXT NOT NULL COMMENT '回复内容',
    `attachment_url` VARCHAR(500) DEFAULT NULL COMMENT '附件路径',
    `attachment_name` VARCHAR(255) DEFAULT NULL COMMENT '附件名称',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `is_accepted` TINYINT DEFAULT 0 COMMENT '是否被采纳',
    `is_ai_generated` TINYINT DEFAULT 0 COMMENT '是否 AI 生成',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答疑大厅回复表';
```

---

## 四、核心功能代码

### 1. AI 智能补位（ForumService.java）

```java
/**
 * AI 智能补位 - 自动生成回复
 * 当用户点击"求助 AI"按钮时，读取帖子内容及附件，调用大模型生成解答
 */
@Transactional
public Map<String, Object> aiHelpWithPost(Long postId, Long currentUserId) {
    ForumPost post = baseMapper.selectById(postId);
    if (post == null) {
        throw new BusinessException("帖子不存在");
    }

    // 1. 构建 Prompt，包含帖子内容和附件内容
    StringBuilder promptBuilder = new StringBuilder();
    promptBuilder.append("你是一位专业的教育答疑助手...");
    promptBuilder.append("【问题标题】").append(post.getTitle()).append("\n");
    promptBuilder.append("【问题内容】").append(post.getContent()).append("\n");

    // 2. 读取 Word 附件内容
    if (post.getAttachmentUrl() != null) {
        String attachmentContent = readAttachmentContent(post.getAttachmentUrl());
        if (attachmentContent != null) {
            promptBuilder.append("\n【附件内容】\n").append(attachmentContent);
        }
    }

    // 3. 调用 Dashscope AI
    String aiResponse = callDashscopeAI(promptBuilder.toString());

    // 4. 插入回复（user_id = 0 表示 AI 助手）
    ForumReply aiReply = new ForumReply();
    aiReply.setPostId(postId);
    aiReply.setUserId(0L);
    aiReply.setContent(aiResponse);
    aiReply.setIsAiGenerated(1);
    replyMapper.insert(aiReply);

    // 5. 更新帖子回复数
    post.setReplyCount(post.getReplyCount() + 1);
    baseMapper.updateById(post);

    return result;
}
```

### 2. 文件上传路径规范

```java
/**
 * 保存上传的文件（复用 /uploads 路径规范）
 * 返回 /uploads/ 开头的路径，避免 /api/uploads 重复拼接问题
 */
private Map<String, String> saveFile(MultipartFile file, String type) throws IOException {
    String projectDir = System.getProperty("user.dir");
    String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    Path dirPath = Paths.get(projectDir, uploadDir, type, datePath);

    Files.createDirectories(dirPath);

    String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
    String newFilename = UUID.randomUUID().toString() + extension;

    Path filePath = dirPath.resolve(newFilename);
    file.transferTo(filePath.toFile());

    // 返回 /uploads/ 开头的路径
    Map<String, String> result = new HashMap<>();
    result.put("url", "/uploads/" + type + "/" + datePath + "/" + newFilename);
    result.put("name", file.getOriginalFilename());
    return result;
}
```

---

## 五、前端页面结构

### 页面文件

| 文件 | 路径 | 说明 |
|------|------|------|
| Index.vue | `smartedu-ui/src/views/forum/Index.vue` | 广场页面（列表） |
| Detail.vue | `smartedu-ui/src/views/forum/Detail.vue` | 详情页面（左右布局） |
| forum.ts | `smartedu-ui/src/api/forum.ts` | API 接口封装 |

### 路由配置

```typescript
{
  path: '/forum',
  component: () => import('@/layout/index.vue'),
  redirect: '/forum/list',
  meta: { title: '答疑大厅', requiresAuth: false },
  children: [
    {
      path: 'list',
      name: 'ForumList',
      component: () => import('@/views/forum/Index.vue'),
      meta: { title: '答疑大厅', icon: 'ChatDotRound' }
    },
    {
      path: ':id',
      name: 'ForumDetail',
      component: () => import('@/views/forum/Detail.vue'),
      meta: { title: '帖子详情' }
    }
  ]
}
```

---

## 六、路径兼容性说明

### URL 格式规范

| 环节 | 格式 | 示例 |
|------|------|------|
| 数据库存储 | `/uploads/xxx` | `/uploads/forum/2026/03/28/xxx.docx` |
| 前端请求 | `/api/uploads/xxx` | `/api/uploads/forum/2026/03/28/xxx.docx` |
| 后端映射 | `/uploads/**` → `file:uploads/` | - |

### 前端 getFileUrl() 函数

```typescript
const getFileUrl = (url: string) => {
  if (!url) return '#'
  if (url.startsWith('http')) return url
  // 如果已经是完整路径（以 /api 开头），直接返回
  if (url.startsWith('/api')) return url
  // 否则拼接 /api 前缀
  return '/api' + url
}
```

---

## 七、部署步骤

### 1. 数据库初始化

```bash
mysql -u root -p smartedu_platform < database/forum_tables.sql
```

### 2. 后端配置确认

确认 `application.yml` 中 AI 配置正确：

```yaml
ai:
  bailian:
    api-key: sk-sp-6a4265766e014f378cb68088d4541e87
    model: qwen3.5-plus
    endpoint: https://coding.dashscope.aliyuncs.com/v1/chat/completions
```

### 3. 前端构建

```bash
cd smartedu-ui
npm install  # 如需安装依赖
npm run build
```

### 4. 访问入口

- 广场页面：`http://localhost:5173/forum/list`
- 详情页面：`http://localhost:5173/forum/{id}`

---

## 八、功能特性

### ✅ 已实现功能

1. **帖子管理**
   - [x] 发帖（支持附件上传）
   - [x] 帖子列表（按最新/高悬赏排序）
   - [x] 帖子详情查看
   - [x] 分类筛选

2. **回复功能**
   - [x] 发布回复（支持附件）
   - [x] 回复列表展示
   - [x] 采纳最佳答案
   - [x] 点赞功能

3. **AI 智能补位**
   - [x] 求助 AI 按钮
   - [x] 读取帖子内容 + 附件
   - [x] 调用 Dashscope AI 生成解答
   - [x] 自动插入回复（user_id=0）
   - [x] AI 回复标识

4. **路径兼容**
   - [x] 统一使用 `/uploads/` 格式
   - [x] 前端自动拼接 `/api` 前缀
   - [x] 后端资源映射正确配置

### 📋 可扩展功能

- [ ] 楼中楼回复（已预留 parent_id 字段）
- [ ] 帖子搜索（已预留搜索框）
- [ ] 用户积分系统（悬赏分已实现）
- [ ] 消息通知
- [ ] 管理员置顶/加精

---

## 九、文件清单

### 后端文件

```
smartedu-backend/src/main/java/com/smartedu/
├── entity/
│   ├── ForumPost.java         # 帖子实体
│   └── ForumReply.java        # 回复实体
├── mapper/
│   ├── ForumPostMapper.java   # 帖子 Mapper
│   └── ForumReplyMapper.java  # 回复 Mapper
├── service/
│   └── ForumService.java      # 论坛服务（含 AI 功能）
└── controller/
    └── ForumController.java   # 论坛控制器
```

### 前端文件

```
smartedu-ui/src/
├── api/
│   └── forum.ts               # API 封装
├── router/
│   └── index.ts               # 路由配置（已更新）
└── views/
    └── forum/
        ├── Index.vue          # 广场页面
        └── Detail.vue         # 详情页面
```

### 数据库文件

```
database/
├── forum_tables.sql           # 表结构 + 测试数据
└── fix_attachment_url.sql     # 路径修复脚本
```

---

## 十、测试建议

1. **发帖测试**：上传 Word 文档，确认附件路径正确
2. **AI 求助测试**：点击"求助 AI"按钮，确认生成回复
3. **下载测试**：下载附件，确认 URL 不出现 404
4. **排序测试**：切换"最新"/"高悬赏"排序
5. **采纳测试**：发帖人采纳最佳答案

---

**开发完成日期**：2026-03-28
**开发人员**：SmartEdu Team
