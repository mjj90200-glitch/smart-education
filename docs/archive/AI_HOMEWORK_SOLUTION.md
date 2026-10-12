# 作业模块 AI 辅助功能设计方案

## 一、功能架构

```
AI 辅助教师功能
├── 1. AI 智能批改
│   ├── 1.1 客观题自动判分（选择/判断/填空）
│   ├── 1.2 主观题 AI 评分（简答/论述/计算）
│   ├── 1.3 代码题自动测试与评分
│   └── 1.4 作文/文档智能评阅
│
├── 2. AI 辅助命题
│   ├── 2.1 基于知识点自动生成题目
│   ├── 2.2 相似题目推荐
│   ├── 2.3 题目难度智能评估
│   └── 2.4 试卷智能组卷
│
├── 3. AI 学情分析
│   ├── 3.1 班级整体学情报告
│   ├── 3.2 学生个人学情档案
│   ├── 3.3 薄弱知识点识别
│   └── 3.4 学习预警系统
│
└── 4. AI 教学辅助
    ├── 4.1 作业讲评建议生成
    ├── 4.2 个性化学习资源推荐
    ├── 4.3 教学反思辅助
    └── 4.4 家长沟通报告生成
```

---

## 二、数据库设计

### 2.1 新增字段到作业提交表

```sql
--  homework_submissions 表新增字段
ALTER TABLE `homework_submissions` ADD COLUMN `ai_score` DECIMAL(5,2) DEFAULT NULL COMMENT 'AI 评分';
ALTER TABLE `homework_submissions` ADD COLUMN `ai_feedback` TEXT COMMENT 'AI 评语';
ALTER TABLE `homework_submissions` ADD COLUMN `knowledge_point_mastery` JSON DEFAULT NULL COMMENT '知识点掌握度分析';
ALTER TABLE `homework_submissions` ADD COLUMN `error_analysis` JSON DEFAULT NULL COMMENT '错误分析';
```

### 2.2 新增 AI 批改记录表

```sql
DROP TABLE IF EXISTS `ai_grade_log`;
CREATE TABLE `ai_grade_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录 ID',
    `submission_id` BIGINT NOT NULL COMMENT '提交 ID',
    `homework_id` BIGINT NOT NULL COMMENT '作业 ID',
    `student_id` BIGINT NOT NULL COMMENT '学生 ID',
    `ai_model` VARCHAR(50) DEFAULT 'qwen-coder-plus' COMMENT '使用的 AI 模型',
    `prompt_content` TEXT COMMENT 'AI 提示词',
    `ai_response` TEXT COMMENT 'AI 原始响应',
    `grade_result` JSON COMMENT '批改结果',
    `grade_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '批改时间',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-失败，1-成功',
    PRIMARY KEY (`id`),
    KEY `idx_submission_id` (`submission_id`),
    KEY `idx_homework_id` (`homework_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 批改记录表';
```

### 2.3 新增学情分析表

```sql
DROP TABLE IF EXISTS `learning_analytics_detail`;
CREATE TABLE `learning_analytics_detail` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录 ID',
    `user_id` BIGINT NOT NULL COMMENT '学生 ID',
    `course_id` BIGINT NOT NULL COMMENT '课程 ID',
    `knowledge_point_id` BIGINT DEFAULT NULL COMMENT '知识点 ID',
    `mastery_level` DECIMAL(5,2) DEFAULT 0 COMMENT '掌握度 (0-100)',
    `practice_count` INT DEFAULT 0 COMMENT '练习次数',
    `correct_count` INT DEFAULT 0 COMMENT '正确次数',
    `avg_score` DECIMAL(5,2) DEFAULT 0 COMMENT '平均得分',
    `last_practice_time` DATETIME DEFAULT NULL COMMENT '最后练习时间',
    `weak_point` TINYINT DEFAULT 0 COMMENT '是否薄弱点：0-否，1-是',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_kp` (`user_id`, `knowledge_point_id`),
    KEY `idx_course_id` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学情分析详情表';
```

### 2.4 新增 AI 题库表

```sql
DROP TABLE IF EXISTS `ai_question_bank`;
CREATE TABLE `ai_question_bank` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '题目 ID',
    `course_id` BIGINT NOT NULL COMMENT '课程 ID',
    `knowledge_point_ids` VARCHAR(500) DEFAULT NULL COMMENT '关联知识点 ID 列表',
    `question_type` VARCHAR(20) NOT NULL COMMENT '题目类型',
    `content` TEXT NOT NULL COMMENT '题目内容',
    `options` JSON DEFAULT NULL COMMENT '选项',
    `answer` TEXT NOT NULL COMMENT '答案',
    `analysis` TEXT COMMENT '解析',
    `difficulty_level` TINYINT DEFAULT 2 COMMENT '难度：1-简单，2-中等，3-困难',
    `source` VARCHAR(50) DEFAULT 'AI_GENERATED' COMMENT '来源：AI_GENERATED-AI 生成，MANUAL-手动录入',
    `usage_count` INT DEFAULT 0 COMMENT '使用次数',
    `avg_correct_rate` DECIMAL(5,2) DEFAULT NULL COMMENT '平均正确率',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-停用，1-启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_type` (`question_type`),
    KEY `idx_knowledge_points` (`knowledge_point_ids`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 题库表';
```

---

## 三、后端服务层设计

### 3.1 增强 AIGradeService.java

```java
@Service
public class AIGradeService {

    // 已有方法
    public Map<String, Object> gradeHomework(...) { }

    // 新增方法

    /**
     * 批改客观题（选择/判断/填空）
     */
    public Map<String, Object> gradeObjectiveQuestions(
        String standardAnswer,
        String studentAnswer,
        QuestionType type,
        Double fullScore
    );

    /**
     * 批改主观题（简答/论述）
     */
    public Map<String, Object> gradeSubjectiveQuestion(
        String questionContent,
        String questionType,
        String standardAnswer,
        String studentAnswer,
        Double fullScore,
        List<String> scoringPoints
    );

    /**
     * 批改代码题
     */
    public Map<String, Object> gradeCodeQuestion(
        String problemDescription,
        String templateCode,
        String studentCode,
        List<TestCase> testCases,
        String language
    );

    /**
     * 批改作文/文档
     */
    public Map<String, Object> gradeEssay(
        String essayTopic,
        String requirements,
        String studentEssay,
        List<ScoringCriteria> criteria,
        Double fullScore
    );

    /**
     * 生成学情分析报告
     */
    public LearningAnalysisReport generateLearningAnalysis(
        Long studentId,
        Long courseId,
        List<HomeworkSubmission> submissions
    );

    /**
     * 识别薄弱知识点
     */
    public List<KnowledgePointAnalysis> identifyWeakPoints(
        Long studentId,
        Long courseId
    );
}
```

### 3.2 新增 AIGenerateService.java（AI 命题服务）

```java
@Service
public class AIGenerateService {

    /**
     * 根据知识点生成题目
     */
    public GeneratedQuestion generateQuestion(
        String knowledgePoint,
        String questionType,
        Integer difficulty,
        String courseContext
    );

    /**
     * 批量生成题目
     */
    public List<GeneratedQuestion> batchGenerateQuestions(
        List<String> knowledgePoints,
        Map<String, Integer> typeDistribution,
        Integer totalQuestions
    );

    /**
     * 智能组卷
     */
    public ExamPaper generateExamPaper(
        Long courseId,
        List<String> knowledgePoints,
        Integer totalScore,
        Integer duration,
        DifficultyDistribution difficultyDist
    );

    /**
     * 题目难度评估
     */
    public DifficultyAssessment assessDifficulty(
        String questionContent,
        String answer,
        String targetAudience
    );

    /**
     * 相似题目推荐
     */
    public List<QuestionRecommendation> recommendSimilarQuestions(
        String questionContent,
        Integer limit
    );
}
```

### 3.3 新增 AIAnalysisService.java（学情分析服务）

```java
@Service
public class AIAnalysisService {

    /**
     * 生成班级学情报告
     */
    public ClassReport generateClassReport(
        Long courseId,
        Long homeworkId,
        LocalDate startDate,
        LocalDate endDate
    );

    /**
     * 生成学生个人报告
     */
    public StudentReport generateStudentReport(
        Long studentId,
        Long courseId,
        String reportType  // WEEKLY, MONTHLY, COURSE_SUMMARY
    );

    /**
     * 学习预警
     */
    public List<LearningWarning> checkLearningWarnings(
        Long courseId,
        WarningType warningType  // LOW_SCORE, LATE_SUBMISSION, ABSENCE
    );

    /**
     * 生成讲评建议
     */
    public TeachingSuggestion generateTeachingSuggestion(
        Long homeworkId,
        List<HomeworkSubmission> submissions
    );

    /**
     * 个性化资源推荐
     */
    public List<ResourceRecommendation> recommendResources(
        Long studentId,
        List<String> weakPoints
    );
}
```

---

## 四、Controller 层 API 设计

### 4.1 AI 批改控制器

```java
@Tag(name = "AI 作业批改")
@RestController
@RequestMapping("/api/ai/grade")
public class AIGradeController {

    @PostMapping("/homework")
    public Result<GradeResultVO> gradeHomework(@RequestBody GradeRequest request);

    @PostMapping("/batch")
    public Result<BatchGradeResult> batchGrade(@RequestBody BatchGradeRequest request);

    @GetMapping("/log/{submissionId}")
    public Result<AIGradeLogVO> getGradeLog(@PathVariable Long submissionId);
}
```

### 4.2 AI 命题控制器

```java
@Tag(name = "AI 命题")
@RestController
@RequestMapping("/api/ai/generate")
public class AIGenerateController {

    @PostMapping("/question")
    public Result<GeneratedQuestionVO> generateQuestion(@RequestBody QuestionGenerateRequest request);

    @PostMapping("/questions/batch")
    public Result<List<GeneratedQuestionVO>> batchGenerate(@RequestBody BatchGenerateRequest request);

    @PostMapping("/exam-paper")
    public Result<ExamPaperVO> generateExamPaper(@RequestBody ExamPaperRequest request);

    @PostMapping("/question/assess-difficulty")
    public Result<DifficultyAssessmentVO> assessDifficulty(@RequestBody DifficultyAssessRequest request);

    @GetMapping("/question/similar")
    public Result<List<QuestionRecommendationVO>> getSimilarQuestions(
        @RequestParam Long questionId,
        @RequestParam(defaultValue = "5") Integer limit
    );
}
```

### 4.3 AI 学情分析控制器

```java
@Tag(name = "AI 学情分析")
@RestController
@RequestMapping("/api/ai/analysis")
public class AIAnalysisController {

    @GetMapping("/class/{courseId}")
    public Result<ClassReportVO> getClassReport(
        @PathVariable Long courseId,
        @RequestParam(required = false) Long homeworkId
    );

    @GetMapping("/student/{studentId}")
    public Result<StudentReportVO> getStudentReport(
        @PathVariable Long studentId,
        @RequestParam Long courseId,
        @RequestParam String reportType
    );

    @GetMapping("/warnings")
    public Result<List<LearningWarningVO>> getWarnings(
        @RequestParam Long courseId,
        @RequestParam(required = false) WarningType type
    );

    @PostMapping("/teaching-suggestion")
    public Result<TeachingSuggestionVO> getTeachingSuggestion(
        @RequestBody TeachingSuggestionRequest request
    );

    @GetMapping("/resource-recommend")
    public Result<List<ResourceRecommendationVO>> getResourceRecommend(
        @RequestParam Long studentId
    );
}
```

---

## 五、前端 UI 设计

### 5.1 教师端新增功能模块

```
教师端首页
├── 作业管理（已有，增强）
│   ├── 发布作业
│   ├── AI 智能批改（新增一键批改进度展示）
│   └── 批改详情（新增 AI 评分对比、AI 评语展示）
│
├── AI 命题中心（新增）
│   ├── 智能出题
│   ├── 组卷工具
│   └── 我的题库
│
├── 学情分析（新增）
│   ├── 班级报告
│   ├── 学生画像
│   ├── 薄弱点分析
│   └── 学习预警
│
└── 教学辅助（新增）
    ├── 讲评建议
    ├── 资源推荐
    └── 家长报告
```

### 5.2 AI 批改界面增强

在现有 `HomeworkManage.vue` 基础上增强：

```vue
<!-- 新增 AI 批改结果展示 -->
<el-dialog v-model="showAIDetailDialog" title="AI 批改详情">
  <el-descriptions :column="2">
    <el-descriptions-item label="AI 评分">{{ submission.aiScore }}</el-descriptions-item>
    <el-descriptions-item label="教师评分">{{ submission.score }}</el-descriptions-item>
    <el-descriptions-item label="AI 评语" :span="2">{{ submission.aiFeedback }}</el-descriptions-item>
  </el-descriptions>

  <!-- 知识点掌握度雷达图 -->
  <div ref="knowledgeChartRef" style="height: 300px"></div>

  <!-- 错误分析 -->
  <el-collapse>
    <el-collapse-item title="错误分析" name="1">
      <div v-for="error in errorAnalysis" :key="error.point">
        <el-tag :type="error.severity === 'high' ? 'danger' : 'warning'">
          {{ error.point }}
        </el-tag>
        <span>{{ error.description }}</span>
      </div>
    </el-collapse-item>
  </el-collapse>
</el-dialog>

<!-- 批改进度展示 -->
<el-progress
  :percentage="gradeProgress"
  :status="gradeStatus"
  :format="format => `${gradedCount}/${totalCount} 已批改`"
/>
```

### 5.3 AI 命题中心界面

```vue
<template>
  <div class="ai-question-center page-container">
    <el-card>
      <template #header>
        <span>AI 命题中心</span>
      </template>

      <!-- 题目生成表单 -->
      <el-form :model="generateForm" label-width="120px">
        <el-form-item label="所属课程">
          <el-select v-model="generateForm.courseId" placeholder="选择课程">
            <el-option v-for="course in courses" :key="course.id" :label="course.name" :value="course.id"/>
          </el-select>
        </el-form-item>

        <el-form-item label="知识点">
          <el-select v-model="generateForm.knowledgePoints" multiple placeholder="选择知识点">
            <el-option v-for="kp in knowledgePoints" :key="kp.id" :label="kp.name" :value="kp.id"/>
          </el-select>
        </el-form-item>

        <el-form-item label="题目类型">
          <el-checkbox-group v-model="generateForm.questionTypes">
            <el-checkbox label="SINGLE_CHOICE">单选题</el-checkbox>
            <el-checkbox label="MULTIPLE_CHOICE">多选题</el-checkbox>
            <el-checkbox label="JUDGMENT">判断题</el-checkbox>
            <el-checkbox label="FILL_BLANK">填空题</el-checkbox>
            <el-checkbox label="ESSAY">简答题</el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="难度分布">
          <div style="display: flex; gap: 20px">
            <el-radio-group v-model="generateForm.difficulty">
              <el-radio :label="1">简单</el-radio>
              <el-radio :label="2">中等</el-radio>
              <el-radio :label="3">困难</el-radio>
            </el-radio-group>
          </div>
        </el-form-item>

        <el-form-item label="题目数量">
          <el-input-number v-model="generateForm.quantity" :min="1" :max="50"/>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleGenerate" :loading="generating">
            <el-icon><MagicStick/></el-icon>
            开始生成
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 生成结果展示 -->
      <el-table :data="generatedQuestions" v-if="generatedQuestions.length > 0">
        <el-table-column prop="questionType" label="类型" width="100"/>
        <el-table-column prop="content" label="题目内容" min-width="300"/>
        <el-table-column prop="difficulty" label="难度" width="80">
          <template #default="{row}">
            <el-tag :type="getDifficultyType(row.difficulty)">
              {{ getDifficultyText(row.difficulty) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{row}">
            <el-button size="small" @click="previewQuestion(row)">预览</el-button>
            <el-button size="small" type="primary" @click="addToBank(row)">加入题库</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>
```

### 5.4 学情分析界面

```vue
<template>
  <div class="learning-analysis page-container">
    <!-- 班级整体分析 -->
    <el-row :gutter="20">
      <el-col :span="6">
        <el-statistic title="参考人数" :value="stats.studentCount"/>
      </el-col>
      <el-col :span="6">
        <el-statistic title="平均分" :value="stats.avgScore"/>
      </el-col>
      <el-col :span="6">
        <el-statistic title="及格率" :value="stats.passRate" suffix="%"/>
      </el-col>
      <el-col :span="6">
        <el-statistic title="优秀率" :value="stats.excellentRate" suffix="%"/>
      </el-col>
    </el-row>

    <!-- 分数分布图 -->
    <el-card>
      <template #header>分数分布</template>
      <div ref="scoreDistChartRef" style="height: 300px"></div>
    </el-card>

    <!-- 知识点掌握度热力图 -->
    <el-card>
      <template #header>知识点掌握度</template>
      <div ref="knowledgeHeatmapRef" style="height: 400px"></div>
    </el-card>

    <!-- 薄弱知识点列表 -->
    <el-card>
      <template #header>班级薄弱知识点 TOP 5</template>
      <el-table :data="weakKnowledgePoints">
        <el-table-column prop="rank" label="排名" width="60"/>
        <el-table-column prop="name" label="知识点" min-width="200"/>
        <el-table-column prop="avgMastery" label="平均掌握度" width="120">
          <template #default="{row}">
            <el-progress :percentage="row.avgMastery" :color="getMasteryColor(row.avgMastery)"/>
          </template>
        </el-table-column>
        <el-table-column prop="wrongRate" label="错误率" width="100"/>
      </el-table>
    </el-card>

    <!-- 学习预警学生 -->
    <el-card>
      <template #header>学习预警学生</template>
      <el-table :data="warningStudents">
        <el-table-column prop="studentName" label="姓名" width="100"/>
        <el-table-column prop="warningType" label="预警类型" width="120">
          <template #default="{row}">
            <el-tag :type="getWarningTypeColor(row.warningType)">
              {{ getWarningTypeText(row.warningType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="详情" min-width="200"/>
        <el-table-column label="操作" width="100">
          <template #default="{row}">
            <el-button size="small" @click="viewStudentDetail(row.studentId)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>
```

---

## 六、核心提示词设计

### 6.1 作业批改提示词

```
你是一位经验丰富的{{courseName}}课程教师，请批改学生的作业。

【作业信息】
- 标题：{{homeworkTitle}}
- 要求：{{homeworkDescription}}
- 总分：{{totalScore}}分
- 题目类型：{{questionType}}

【参考答案及评分标准】
{{standardAnswer}}
{{scoringCriteria}}

【学生答案】
{{studentAnswer}}

【批改要求】
1. 请根据评分标准客观公正地进行评分
2. 指出学生答案中的亮点和不足
3. 给出具体、有针对性的改进建议
4. 输出 JSON 格式：
{
  "score": 得分（数字）,
  "feedback": "总体评语",
  "strengths": ["亮点 1", "亮点 2"],
  "weaknesses": ["不足 1", "不足 2"],
  "suggestions": ["建议 1", "建议 2"],
  "knowledgePointMastery": [
    {"point": "知识点名称", "mastery": 80},
    ...
  ]
}
```

### 6.2 题目生成提示词

```
你是一位专业的{{courseName}}课程命题专家。

【命题要求】
- 课程：{{courseName}}
- 知识点：{{knowledgePoint}}
- 题型：{{questionType}}
- 难度：{{difficulty}}
- 适用对象：{{targetAudience}}

【题目要求】
1. 题目内容准确、清晰、无歧义
2. 符合{{difficulty}}难度等级
3. 选择题选项要有干扰性但只有一个正确答案
4. 答案和解析要详细准确

请生成{{count}}道题目，输出 JSON 格式：
[
  {
    "content": "题目内容",
    "options": {"A": "选项 A", "B": "选项 B", ...},
    "answer": "正确答案",
    "analysis": "详细解析",
    "difficulty": 难度等级,
    "knowledgePoint": "关联知识点"
  },
  ...
]
```

### 6.3 学情分析提示词

```
你是一位学情分析专家，请根据学生的作业提交数据分析学习情况。

【学生信息】
- 姓名：{{studentName}}
- 课程：{{courseName}}
- 统计周期：{{dateRange}}

【作业提交数据】
{{submissionData}}

【分析要求】
1. 分析学生的整体学习表现
2. 识别薄弱知识点
3. 给出个性化的学习建议
4. 判断是否存在学习风险

输出 JSON 格式：
{
  "overallPerformance": "整体表现描述",
  "avgScore": 平均分,
  "trend": "上升/下降/稳定",
  "weakPoints": [
    {"point": "知识点", "mastery": 掌握度, "reason": "薄弱原因"}
  ],
  "strengths": ["优势 1", "优势 2"],
  "suggestions": ["建议 1", "建议 2"],
  "riskLevel": "low/medium/high",
  "riskDescription": "风险描述"
}
```

---

## 七、实施步骤

### 阶段一：基础功能增强（1-2 周）
1. 完善 AIGradeService，支持多种题型批改
2. 优化批改结果展示 UI
3. 添加批改日志记录

### 阶段二：AI 命题功能（2-3 周）
1. 实现 AIGenerateService
2. 开发命题中心前端界面
3. 建设 AI 题库

### 阶段三：学情分析（2-3 周）
1. 实现 AIAnalysisService
2. 开发学情分析报表
3. 实现学习预警系统

### 阶段四：教学辅助（1-2 周）
1. 讲评建议生成
2. 个性化资源推荐
3. 家长沟通报告

---

## 八、配置文件更新

```yaml
# application.yml 新增配置
ai:
  bailian:
    api-key: ${AI_BAILIAN_API_KEY:sk-xxxxx}
    model: qwen-coder-plus
    endpoint: https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation

  # AI 批改配置
  grading:
    # 是否启用 AI 批改
    enabled: true
    # 批改超时时间（秒）
    timeout: 30
    # 最大并发批改数
    max-concurrent: 5
    # 是否记录详细日志
    log-detail: true

  # AI 命题配置
  generation:
    # 默认模型
    model: qwen-plus
    # 单次最大生成数量
    max-batch-size: 20
```

---

## 九、预期效果

1. **批改效率提升**：AI 批量批改可使教师批改时间减少 70%
2. **命题效率提升**：AI 辅助命题可使组卷时间减少 50%
3. **学情掌握精准**：实时掌握班级和个人的学习情况
4. **教学质量提升**：基于数据的精准教学干预
