# 阿里云百炼 AI 配置指南

## 功能说明

本项目已集成阿里云百炼 AI，实现以下功能：

### 1. 客观题自动批改
- **题型**：单选题、多选题、判断题、填空题
- **方式**：规则比对（无需 AI）
- **准确率**：100%

### 2. 主观题智能批改
- **题型**：简答题、论述题、计算题
- **方式**：阿里云百炼 Qwen-Coder-Plus 模型
- **功能**：答案比对、得分评定、评语生成

---

## API Key 获取步骤

### 第一步：访问阿里云百炼控制台

1. 打开阿里云百炼官网：https://bailian.console.aliyun.com/
2. 使用阿里云账号登录

### 第二步：创建/选择应用

1. 进入「应用中心」
2. 点击「创建应用」或直接使用已有应用
3. 选择模型：`qwen-coder-plus`（推荐）或 `qwen-turbo`

### 第三步：获取 API Key

1. 进入应用详情页
2. 点击「API-KEY 管理」
3. 创建或复制已有 API Key（格式：`sk-xxxxxxxx`）

### 第四步：配置到项目

编辑文件：`smartedu-backend/src/main/resources/application.yml`

```yaml
ai:
  bailian:
    # 将 sk-your-api-key-here 替换为你的实际 API Key
    api-key: sk-xxxxxxxxxxxxxxxxxxxxxxxx
    model: qwen-coder-plus
    endpoint: https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation
```

---

## 费用说明

- **计费方式**：按 Token 计费
- **qwen-coder-plus**：约 0.02 元/千 Token（输入）+ 0.06 元/千 Token（输出）
- **单次批改成本**：约 0.01-0.05 元（取决于题目和答案长度）
- **免费额度**：新用户通常有免费试用额度

---

## 使用方式

### 前端操作

1. 教师登录系统
2. 进入「作业管理」页面
3. 找到已布置的作业
4. 点击「AI 批改」按钮
5. 系统自动批改所有未批改的提交

### 批改结果

- **客观题**：即时判分，完全正确得满分
- **主观题**：AI 根据参考答案和评分标准给分
- **评语**：AI 生成个性化评语

---

## 常见问题

### Q1: API Key 无效？
**A**: 检查以下几点：
- API Key 格式是否正确（应以 `sk-` 开头）
- 是否已开通百炼服务
- 账户余额是否充足

### Q2: 批改速度慢？
**A**: AI 批改需要调用远程 API，通常 1-3 秒完成。如果网络较慢，可以：
- 使用 `qwen-turbo` 模型（更快但精度略低）
- 检查服务器网络连接

### Q3: 批改结果不准确？
**A**: 可以尝试：
- 优化题目参考答案的详细程度
- 调整提示词模板（修改 `AIGradeService.java` 中的 `buildGradingPrompt` 方法）
- 切换到更高级的模型

### Q4: 没有 API Key 能用吗？
**A**: 可以。系统会降级为规则比对模式：
- 客观题：正常批改
- 主观题：返回满分（需教师手动复核）

---

## 技术实现

### 后端核心类

- `AIGradeService.java` - AI 批改服务
- `HomeworkService.java` - 作业批改逻辑（集成 AI）

### API 端点

```
POST /api/teacher/homework/{id}/auto-grade
```

### 请求示例

```bash
curl -X POST http://localhost:8080/api/teacher/homework/1/auto-grade \
  -H "Authorization: Bearer <token>"
```

### 响应示例

```json
{
  "code": 200,
  "message": "成功批改 5 份作业（AI 批改 3 份）",
  "data": {
    "gradedCount": 5,
    "aiGradedCount": 3,
    "message": "成功批改 5 份作业（AI 批改 3 份）"
  }
}
```

---

## 下一步优化建议

1. **批量批改**：支持选择多个作业批量批改
2. **批改记录**：记录每次批改的详细信息
3. **AI 评分调整**：允许教师调整 AI 给出的分数
4. **评语模板**：自定义 AI 评语风格

---

**文档更新日期**：2026-03-24
**项目版本**：v1.0.0
