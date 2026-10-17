package com.smartedu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartedu.entity.Homework;
import com.smartedu.entity.HomeworkSubmission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 阿里云百炼 AI 作业批改服务 - 简化版
 * 支持文件内容分析和文字答案批改
 * @author SmartEdu Team
 */
@Service
public class AIGradeService {

    private static final Logger logger = LoggerFactory.getLogger(AIGradeService.class);

    @Value("${ai.bailian.api-key:}")
    private String apiKey;

    @Value("${ai.bailian.model:qwen-coder-plus}")
    private String model;

    @Value("${ai.bailian.endpoint:https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation}")
    private String endpoint;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 批改作业 - 简化版
     * @param homeworkTitle 作业标题
     * @param homeworkDescription 作业描述
     * @param submissionContent 提交内容（文字或文件路径）
     * @param submissionType 提交类型：1-文件，2-文字
     * @param totalScore 总分
     * @return 批改结果 {score: 得分，feedback: 评语}
     */
    public Map<String, Object> gradeHomework(
            String homeworkTitle,
            String homeworkDescription,
            String submissionContent,
            Integer submissionType,
            double totalScore) {

        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("AI API Key 未配置，返回满分");
            Map<String, Object> result = new HashMap<>();
            result.put("score", totalScore);
            result.put("feedback", "AI 批改功能未配置 API Key");
            return result;
        }

        // 构建提示词
        String prompt = buildGradingPrompt(homeworkTitle, homeworkDescription, submissionContent, submissionType, totalScore);

        // 调用 AI API
        String response = callBailianAPI(prompt);

        // 解析结果
        return parseGradingResult(response, totalScore);
    }

    /**
     * 构建批改提示词
     */
    private String buildGradingPrompt(
            String homeworkTitle,
            String homeworkDescription,
            String submissionContent,
            Integer submissionType,
            double totalScore) {

        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位专业的教师，请批改学生的作业。\n\n");

        prompt.append("【作业标题】").append(homeworkTitle).append("\n");
        prompt.append("【作业要求】").append(homeworkDescription != null ? homeworkDescription : "无").append("\n");
        prompt.append("【总分】").append(totalScore).append("分\n\n");

        if (submissionType == 1) {
            prompt.append("【学生提交】学生提交了文件作业，文件内容如下：\n");
            // 读取文件内容
            String fileContent = readFileContent(submissionContent);
            prompt.append(fileContent != null ? fileContent : "无法读取文件内容");
        } else {
            prompt.append("【学生答案】\n");
            prompt.append(submissionContent != null ? submissionContent : "未作答");
        }

        prompt.append("\n\n请按照以下要求评分：\n");
        prompt.append("1. 根据作业要求和学生完成情况进行评分\n");
        prompt.append("2. 给出 0-").append((int)totalScore).append(" 之间的分数\n");
        prompt.append("3. 输出 JSON 格式：{\"score\": 得分，\"feedback\": \"评语\"}\n");
        prompt.append("4. 得分必须是数字，评语简洁明了（50 字以内）\n");
        prompt.append("5. 只输出 JSON，不要其他内容\n\n");

        prompt.append("输出格式示例：{\"score\": 85, \"feedback\": \"作业完成良好，但需要改进论述深度\"}");

        return prompt.toString();
    }

    /**
     * 读取文件内容（路径经白名单校验，仅限项目 uploads/ 目录内，防止路径穿越）
     */
    private String readFileContent(String filePath) {
        File file = resolveWithin(filePath);
        if (file == null) {
            logger.warn("拒绝非法或越界的文件路径：{}", filePath);
            return "[非法文件路径]";
        }
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String content = new String(bytes, "UTF-8");

            // 限制内容长度
            if (content.length() > 5000) {
                content = content.substring(0, 5000) + "\n\n[内容过长，已截断]";
            }
            return content;
        } catch (IOException e) {
            logger.error("读取文件失败：{}", e.getMessage());
            return "[读取文件失败：" + e.getMessage() + "]";
        }
    }

    /** 提交文件的合法相对路径：仅允许 uploads/ 目录下由常规文件名字符组成的路径 */
    private static final Pattern SAFE_FILE_PATH = Pattern.compile(
        "^/?uploads/[A-Za-z0-9_\\-][A-Za-z0-9_\\-./]{0,200}$"
    );

    /**
     * 将请求路径限制在项目根目录内，防止路径穿越：
     * 1) 白名单正则校验（仅 uploads/ 下常规文件名，显式拒绝 ".."）
     * 2) 按路径分段解析，逐段拼接到真实基准目录
     * 3) toRealPath + startsWith 最终边界校验
     */
    private File resolveWithin(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        String cleaned = relativePath.replace('\\', '/');
        if (cleaned.contains("..") || !SAFE_FILE_PATH.matcher(cleaned).matches()) {
            return null;
        }
        try {
            Path base = Paths.get(System.getProperty("user.dir")).toRealPath();
            Path target = base;
            for (String part : cleaned.split("/")) {
                if (part.isEmpty() || ".".equals(part)) {
                    continue;
                }
                if ("..".equals(part) || !part.matches("[A-Za-z0-9_.\\-]+")) {
                    return null;
                }
                target = target.resolve(part);
            }
            if (!target.startsWith(base) || !Files.isRegularFile(target)) {
                return null;
            }
            return target.toFile();
        } catch (IOException e) {
            return null;
        }
    }

    private static final Pattern STUDENT_ANSWER_PATTERN = Pattern.compile(
        "学生作答\\s*[:：]\\s*([^\\n\\r]*)",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern NUMBERED_ANSWER_PATTERN = Pattern.compile(
        "^\\s*(?:第\\s*)?(\\d+)\\s*(?:题)?\\s*[、.．):：]\\s*(?:答案\\s*[:：])?\\s*(.+)$"
    );

    /**
     * 判断作业是否满足客观题自动批改条件：
     * 教师开启自动批改，且解析字段中带有 AI 批改答案键（AI_GRADE_OBJECTIVE_V1）。
     */
    public boolean supportsObjectiveAutoGrade(Homework homework) {
        return homework != null
            && Integer.valueOf(1).equals(homework.getAutoGrade())
            && homework.getTeacherEditedAnalysis() != null
            && homework.getTeacherEditedAnalysis().startsWith(AiGradeHomeworkPreparationService.ANSWER_KEY_MARKER);
    }

    /**
     * 按答案键对学生提交的文本作答做客观题判分。
     * @param submissionContent 学生提交的文本内容（每题以"学生作答：X"形式作答）
     */
    public Map<String, Object> gradeObjectiveContent(Homework homework, String submissionContent, double totalScore) {
        List<ObjectiveAnswer> answerKey = parseObjectiveAnswerKey(homework.getTeacherEditedAnalysis());
        if (answerKey.isEmpty()) {
            throw new IllegalArgumentException("AI 批改答案键为空，无法自动评分");
        }

        Map<Integer, String> submittedAnswers = extractSubmittedAnswers(submissionContent);

        int correctCount = 0;
        List<String> wrongItems = new ArrayList<>();
        List<String> missingItems = new ArrayList<>();

        for (int i = 0; i < answerKey.size(); i++) {
            ObjectiveAnswer expected = answerKey.get(i);
            String studentAnswer = submittedAnswers.get(expected.number);
            if (studentAnswer == null && i + 1 <= submittedAnswers.size()) {
                studentAnswer = submittedAnswers.get(i + 1);
            }

            if (studentAnswer == null || studentAnswer.isBlank() || studentAnswer.replace("_", "").isBlank()) {
                missingItems.add(String.valueOf(expected.number));
                continue;
            }

            if (isAnswerMatched(expected, studentAnswer)) {
                correctCount++;
            } else {
                wrongItems.add(expected.number + "题");
            }
        }

        BigDecimal score = BigDecimal.valueOf(totalScore)
            .multiply(BigDecimal.valueOf(correctCount))
            .divide(BigDecimal.valueOf(answerKey.size()), 2, RoundingMode.HALF_UP);

        StringBuilder feedback = new StringBuilder();
        feedback.append("AI 客观题批改：答对 ")
            .append(correctCount)
            .append("/")
            .append(answerKey.size())
            .append(" 题。");
        if (!wrongItems.isEmpty()) {
            feedback.append(" 错题：").append(String.join("、", wrongItems)).append("。");
        }
        if (!missingItems.isEmpty()) {
            feedback.append(" 未作答：").append(String.join("、", missingItems)).append("。");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("score", score.doubleValue());
        result.put("feedback", feedback.toString());
        return result;
    }

    private List<ObjectiveAnswer> parseObjectiveAnswerKey(String answerKeyContent) {
        if (answerKeyContent == null || !answerKeyContent.startsWith(AiGradeHomeworkPreparationService.ANSWER_KEY_MARKER)) {
            return List.of();
        }

        String json = answerKeyContent.substring(AiGradeHomeworkPreparationService.ANSWER_KEY_MARKER.length()).trim();
        try {
            JsonNode root = objectMapper.readTree(json);
            List<ObjectiveAnswer> answers = new ArrayList<>();
            if (root.isArray()) {
                for (JsonNode item : root) {
                    ObjectiveAnswer answer = new ObjectiveAnswer();
                    answer.number = item.path("number").asInt(answers.size() + 1);
                    answer.type = item.path("type").asText("fill_blank");
                    answer.answer = item.path("answer").asText("");
                    answers.add(answer);
                }
            }
            return answers;
        } catch (Exception e) {
            logger.error("解析 AI 批改答案键失败：{}", e.getMessage());
            return List.of();
        }
    }

    private Map<Integer, String> extractSubmittedAnswers(String text) {
        Map<Integer, String> answers = new LinkedHashMap<>();
        if (text == null || text.isBlank()) {
            return answers;
        }

        Matcher studentAnswerMatcher = STUDENT_ANSWER_PATTERN.matcher(text);
        int sequence = 1;
        while (studentAnswerMatcher.find()) {
            answers.put(sequence, studentAnswerMatcher.group(1).trim());
            sequence++;
        }

        if (!answers.isEmpty()) {
            return answers;
        }

        String[] lines = text.split("\\R");
        for (String line : lines) {
            Matcher numberedMatcher = NUMBERED_ANSWER_PATTERN.matcher(line);
            if (numberedMatcher.find()) {
                answers.put(Integer.parseInt(numberedMatcher.group(1)), numberedMatcher.group(2).trim());
            }
        }
        return answers;
    }

    private boolean isAnswerMatched(ObjectiveAnswer expected, String submittedAnswer) {
        if ("choice".equalsIgnoreCase(expected.type)) {
            String expectedChoice = normalizeChoice(expected.answer);
            String submittedChoice = normalizeChoice(submittedAnswer);
            return !expectedChoice.isBlank() && expectedChoice.equals(submittedChoice);
        }

        String normalizedSubmitted = normalizeFillBlank(submittedAnswer);
        String[] candidates = expected.answer.split("[/|｜或]");
        for (String candidate : candidates) {
            if (normalizeFillBlank(candidate).equals(normalizedSubmitted)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeChoice(String answer) {
        if (answer == null) {
            return "";
        }
        Matcher matcher = Pattern.compile("[A-Da-d]").matcher(answer);
        return matcher.find() ? matcher.group().toUpperCase(Locale.ROOT) : normalizeFillBlank(answer).toUpperCase(Locale.ROOT);
    }

    private String normalizeFillBlank(String answer) {
        if (answer == null) {
            return "";
        }
        return answer
            .replaceAll("[\\s，,。.；;：:、（）()【】\\[\\]＿_]+", "")
            .trim()
            .toLowerCase(Locale.ROOT);
    }

    private static class ObjectiveAnswer {
        int number;
        String type;
        String answer;
    }

    /**
     * 调用阿里云百炼 API
     */
    private String callBailianAPI(String prompt) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);

            Map<String, String> input = new HashMap<>();
            input.put("prompt", prompt);
            requestBody.put("input", input);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("temperature", 0.3);
            parameters.put("max_tokens", 500);
            requestBody.put("parameters", parameters);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                endpoint,
                HttpMethod.POST,
                entity,
                String.class
            );

            logger.info("AI API 响应状态码：{}", response.getStatusCode());
            return response.getBody();

        } catch (Exception e) {
            logger.error("调用阿里云百炼 AI 失败：{}", e.getMessage());
            throw new RuntimeException("AI 批改服务调用失败：" + e.getMessage(), e);
        }
    }

    /**
     * 解析 AI 批改结果
     */
    private Map<String, Object> parseGradingResult(String response, double totalScore) {
        Map<String, Object> result = new HashMap<>();
        result.put("score", 0.0);
        result.put("feedback", "批改失败");

        if (response == null || response.isEmpty()) {
            logger.warn("AI 返回为空");
            return result;
        }

        try {
            logger.info("AI 原始响应：{}", response);

            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode outputNode = rootNode.path("output").path("text");

            if (!outputNode.isTextual()) {
                logger.warn("AI 返回格式异常");
                return result;
            }

            String aiResponseText = outputNode.asText().trim();
            logger.info("AI 批改内容：{}", aiResponseText);

            // 提取 JSON 部分
            int jsonStart = aiResponseText.indexOf("{");
            int jsonEnd = aiResponseText.lastIndexOf("}");
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String jsonStr = aiResponseText.substring(jsonStart, jsonEnd + 1);
                JsonNode gradingResult = objectMapper.readTree(jsonStr);

                JsonNode scoreNode = gradingResult.path("score");
                if (scoreNode.isNumber()) {
                    double score = scoreNode.asDouble();
                    result.put("score", Math.min(score, totalScore));
                }

                JsonNode feedbackNode = gradingResult.path("feedback");
                if (feedbackNode.isTextual()) {
                    result.put("feedback", feedbackNode.asText());
                }
            }

        } catch (Exception e) {
            logger.error("解析 AI 批改结果失败：{}", e.getMessage());
            result.put("score", totalScore * 0.8);
            result.put("feedback", "解析失败，给 80% 分数：" + e.getMessage());
        }

        return result;
    }

    /**
     * 识别学生薄弱知识点
     * @param studentId 学生 ID
     * @param courseId 课程 ID（可选）
     * @return 薄弱知识点列表
     */
    public List<KnowledgePointAnalysis> identifyWeakPoints(Long studentId, Long courseId) {
        List<KnowledgePointAnalysis> weakPoints = new ArrayList<>();

        // 注意：这里简化实现，实际应该查询数据库获取知识点掌握情况
        // 由于缺少数据库表依赖，返回空列表
        // 实际使用时需要注入 HomeworkSubmissionMapper 和 QuestionMapper

        // TODO: 实现完整的知识点掌握度计算逻辑
        // 1. 获取学生所有作业提交
        // 2. 分析每道题目的得分情况
        // 3. 按知识点统计正确率
        // 4. 识别正确率低于 60% 的知识点

        return weakPoints;
    }

    /**
     * 知识点分析数据类
     */
    public static class KnowledgePointAnalysis {
        private final Long knowledgePointId;
        private final String knowledgePointName;
        private final double avgScore;
        private final int practiceCount;
        private final String weakReason;

        public KnowledgePointAnalysis(Long knowledgePointId, String knowledgePointName,
                                       double avgScore, int practiceCount, String weakReason) {
            this.knowledgePointId = knowledgePointId;
            this.knowledgePointName = knowledgePointName;
            this.avgScore = avgScore;
            this.practiceCount = practiceCount;
            this.weakReason = weakReason;
        }

        public Long getKnowledgePointId() { return knowledgePointId; }
        public String getKnowledgePointName() { return knowledgePointName; }
        public double getAvgScore() { return avgScore; }
        public int getPracticeCount() { return practiceCount; }
        public String getWeakReason() { return weakReason; }
    }
}
