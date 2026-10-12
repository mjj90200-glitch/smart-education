package com.smartedu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

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
     * 读取文件内容
     */
    private String readFileContent(String filePath) {
        try {
            // 获取项目根目录
            String baseDir = System.getProperty("user.dir");
            String fullPath = baseDir + filePath;

            File file = new File(fullPath);
            if (file.exists()) {
                byte[] bytes = Files.readAllBytes(Paths.get(fullPath));
                String content = new String(bytes, "UTF-8");

                // 限制内容长度
                if (content.length() > 5000) {
                    content = content.substring(0, 5000) + "\n\n[内容过长，已截断]";
                }
                return content;
            }

            // 尝试直接从路径读取（可能是绝对路径）
            file = new File(filePath);
            if (file.exists()) {
                byte[] bytes = Files.readAllBytes(Paths.get(filePath));
                return new String(bytes, "UTF-8");
            }

            return "[文件不存在，无法读取内容]";

        } catch (IOException e) {
            logger.error("读取文件失败：{}", e.getMessage());
            return "[读取文件失败：" + e.getMessage() + "]";
        }
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
