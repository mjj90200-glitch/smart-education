# 智慧教育平台 - 作业模块 AI 辅助功能实施指南

## 一、方案概述

本方案为智慧教育平台（SmartEdu-Platform）的作业模块添加了完整的 AI 辅助教师功能，包括：

1. **AI 智能批改** - 支持多种题型的自动批改
2. **AI 辅助命题** - 基于知识点自动生成题目
3. **AI 学情分析** - 班级和个人的学情报告
4. **AI 教学辅助** - 讲评建议、资源推荐等

## 二、已创建的文件清单

### 2.1 文档类
| 文件名 | 说明 |
|--------|------|
| `AI_HOMEWORK_SOLUTION.md` | 完整的 AI 辅助功能设计方案 |
| `AI_HOMEWORK_IMPLEMENTATION.md` | 本实施指南 |

### 2.2 数据库脚本
| 文件名 | 说明 |
|--------|------|
| `database/ai-homework-init.sql` | AI 功能数据库初始化脚本 |
| `database/fix-test-users.sql` | 测试账户密码修复脚本 |

### 2.3 后端代码
| 文件路径 | 说明 |
|----------|------|
| `smartedu-backend/.../service/AIGradeServiceEnhanced.java` | 增强版 AI 批改服务 |
| `smartedu-backend/.../service/AIAnalysisService.java` | AI 学情分析服务 |
| `smartedu-backend/.../controller/AIAnalysisController.java` | AI 分析控制器 |
| `smartedu-backend/.../config/DataInitializer.java` | 数据初始化配置（已修复） |

### 2.4 前端代码
| 文件路径 | 说明 |
|----------|------|
| `smartedu-ui/src/views/teacher/LearningAnalysis.vue` | 学情分析页面 |
| `smartedu-ui/src/api/teacher.ts` | 教师端 API（已更新） |

## 三、快速开始

### 3.1 数据库初始化

1. **修复测试账户（必须先执行）**
```bash
mysql -u root -p smartedu_platform < database/fix-test-users.sql
```

2. **初始化 AI 功能表结构（使用简化版脚本）**
```bash
mysql -u root -p smartedu_platform < database/ai-homework-init-simple.sql
```

**说明**：
- `ai-homework-init-simple.sql` - 简化版脚本，自动检测字段是否存在，不会报错
- `ai-homework-init.sql` - 完整版脚本（使用存储过程方式）

如果以上命令执行失败，可以登录 MySQL 手动执行：
```sql
USE smartedu_platform;
SOURCE database/ai-homework-init-simple.sql;
```

### 3.2 配置文件更新

在 `application.yml` 中添加以下配置：

```yaml
ai:
  bailian:
    api-key: ${AI_BAILIAN_API_KEY:sk-your-api-key-here}
    model: qwen-coder-plus
    endpoint: https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation

  grading:
    enabled: true
    timeout: 30
    max-concurrent: 5
    log-detail: true

  generation:
    model: qwen-plus
    max-batch-size: 20
```

### 3.3 启动后端

```bash
cd smartedu-backend
mvn spring-boot:run
```

### 3.4 启动前端

```bash
cd smartedu-ui
npm install
npm run dev
```

## 四、功能使用说明

### 4.1 AI 作业批改

**教师端操作**：
1. 进入「作业管理」页面
2. 找到需要批改的作业
3. 点击「AI 批改」按钮
4. 系统自动批改所有待批改提交

**支持的批改类型**：
- 客观题（选择/判断/填空）- 精确匹配答案
- 主观题（简答/论述）- AI 智能评分
- 代码题 - 测试用例 + AI 代码质量评估
- 作文/文档 - AI 内容评阅

### 4.2 学情分析

**访问路径**：教师端 → 学情分析

**功能模块**：
1. **班级概览** - 查看班级整体学习情况
   - 参考人数、平均分、及格率、迟交率
   - 分数分布柱状图
   - 成绩排名 TOP 10
   - AI 生成的学情分析报告

2. **学生详情** - 查看单个学生学习情况
   - 基本信息和提交统计
   - 学习趋势折线图
   - 薄弱知识点识别
   - AI 生成的学习建议

3. **学习预警** - 需要关注的学生名单
   - 低分预警（平均分<60）
   - 迟交预警（迟交率>30%）
   - 缺交预警
   - 成绩下滑预警

4. **教学建议** - 针对作业的教学建议
   - 选择作业
   - 生成统计分析和教学建议
   - AI 补充建议

### 4.3 API 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/ai/analysis/class/{courseId}` | GET | 获取班级学情报告 |
| `/api/ai/analysis/student/{studentId}` | GET | 获取学生个人报告 |
| `/api/ai/analysis/warnings` | GET | 获取学习预警列表 |
| `/api/ai/analysis/teaching-suggestion` | POST | 获取教学建议 |
| `/api/ai/analysis/knowledge-map/{studentId}` | GET | 获取知识点图谱 |

## 五、核心功能代码说明

### 5.1 AIGradeServiceEnhanced.java

**核心方法**：
```java
// 通用作业批改
GradeResult gradeHomework(...)

// 客观题批改
ObjectiveGradeResult gradeObjectiveQuestion(...)

// 主观题批改
GradeResult gradeSubjectiveQuestion(...)

// 代码题批改
GradeResult gradeCodeQuestion(...)

// 作文批改
GradeResult gradeEssay(...)

// 学情分析报告
LearningAnalysisReport generateLearningAnalysis(...)

// 薄弱知识点识别
List<KnowledgePointAnalysis> identifyWeakPoints(...)
```

### 5.2 AIAnalysisService.java

**核心方法**：
```java
// 班级学情报告
Map<String, Object> getClassReport(...)

// 学生个人报告
Map<String, Object> getStudentReport(...)

// 学习预警列表
List<Map<String, Object>> getLearningWarnings(...)

// 教学建议
Map<String, Object> generateTeachingSuggestion(...)

// 资源推荐
List<Map<String, Object>> recommendResources(...)

// 知识点图谱
Map<String, Object> getKnowledgeMap(...)
```

### 5.3 AI 提示词设计

方案中包含了完整的 AI 提示词设计：
- 作业批改提示词
- 主观题评分提示词
- 代码评阅提示词
- 作文评阅提示词
- 学情分析提示词
- 教学建议提示词

## 六、实施步骤

### 阶段一：基础环境搭建（1 天）
- [ ] 执行数据库初始化脚本
- [ ] 配置 AI API Key
- [ ] 测试 AI 服务连通性

### 阶段二：后端服务部署（2-3 天）
- [ ] 集成 AIGradeServiceEnhanced
- [ ] 集成 AIAnalysisService
- [ ] 部署 AIAnalysisController
- [ ] 测试 API 接口

### 阶段三：前端界面开发（3-5 天）
- [ ] 部署学情分析页面
- [ ] 集成 ECharts 图表
- [ ] 调试数据展示
- [ ] 优化用户体验

### 阶段四：测试优化（2-3 天）
- [ ] 功能测试
- [ ] 性能优化
- [ ] AI 提示词调优
- [ ] 用户验收测试

## 七、预期效果

| 指标 | 改进前 | 改进后 | 提升 |
|------|--------|--------|------|
| 作业批改时间 | 2 小时/班 | 30 分钟/班 | 75% |
| 命题效率 | 1 小时/套 | 15 分钟/套 | 75% |
| 学情掌握 | 人工统计 | 实时分析 | 100% |
| 教学精准度 | 经验驱动 | 数据驱动 | 显著提升 |

## 八、常见问题

### Q1: AI 批改不准确怎么办？
**A**: 可以调整 AI 提示词中的评分标准，或者设置 AI 评分权重（建议 AI 评分占 70%，人工复核占 30%）。

### Q2: API 调用失败？
**A**: 检查以下几点：
1. API Key 是否正确配置
2. 网络连接是否正常
3. 阿里云账户余额是否充足

### Q3: 学情分析数据不准确？
**A**: 确保数据库中有足够的作业提交数据，至少需要 5 次以上提交才能生成准确的分析报告。

### Q4: 如何禁用 AI 功能？
**A**: 在配置文件中设置 `ai.grading.enabled: false` 即可禁用 AI 批改功能。

## 九、技术支持

- 阿里云百炼 API 文档：https://help.aliyun.com/product/42154.html
- 通义千问模型文档：https://help.aliyun.com/model-studio/
- ECharts 图表库：https://echarts.apache.org/zh/index.html

## 十、后续扩展

1. **AI 答疑助手** - 学生可随时向 AI 提问
2. **AI 备课助手** - 帮助教师生成教案和 PPT
3. **AI 家长报告** - 自动生成给家长的学习报告
4. **AI 课程推荐** - 根据学生兴趣推荐课程

---

**文档版本**: v1.0
**创建日期**: 2026-03-24
**作者**: SmartEdu Team
