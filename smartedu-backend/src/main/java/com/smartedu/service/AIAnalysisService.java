package com.smartedu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartedu.entity.Homework;
import com.smartedu.entity.HomeworkSubmission;
import com.smartedu.entity.User;
import com.smartedu.mapper.HomeworkMapper;
import com.smartedu.mapper.HomeworkSubmissionMapper;
import com.smartedu.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 学情分析服务
 * 为教师提供班级和学生的学情分析功能
 *
 * @author SmartEdu Team
 */
@Slf4j
@Service
public class AIAnalysisService {

    private final HomeworkMapper homeworkMapper;
    private final HomeworkSubmissionMapper submissionMapper;
    private final UserMapper userMapper;
    private final AIGradeService aiGradeService;

    @Value("${ai.bailian.api-key:}")
    private String apiKey;

    @Value("${ai.bailian.model:qwen-coder-plus}")
    private String model;

    @Value("${ai.bailian.endpoint:https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation}")
    private String endpoint;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public AIAnalysisService(HomeworkMapper homeworkMapper,
                             HomeworkSubmissionMapper submissionMapper,
                             UserMapper userMapper,
                             AIGradeService aiGradeService) {
        this.homeworkMapper = homeworkMapper;
        this.submissionMapper = submissionMapper;
        this.userMapper = userMapper;
        this.aiGradeService = aiGradeService;
    }

    /**
     * 获取班级学情报告
     */
    public Map<String, Object> getClassReport(Long courseId, Long homeworkId,
                                               String startDate, String endDate) {
        Map<String, Object> report = new HashMap<>();

        try {
            // 1. 统计数据
            LambdaQueryWrapper<Homework> homeworkWrapper = new LambdaQueryWrapper<>();
            homeworkWrapper.eq(Homework::getCourseId, courseId);
            if (homeworkId != null) {
                homeworkWrapper.eq(Homework::getId, homeworkId);
            }
            List<Homework> homeworkList = homeworkMapper.selectList(homeworkWrapper);

            if (homeworkList.isEmpty()) {
                report.put("message", "暂无作业数据");
                return report;
            }

            List<Long> homeworkIds = homeworkList.stream()
                    .map(Homework::getId)
                    .collect(Collectors.toList());

            // 2. 获取所有提交
            LambdaQueryWrapper<HomeworkSubmission> submissionWrapper = new LambdaQueryWrapper<>();
            submissionWrapper.in(HomeworkSubmission::getHomeworkId, homeworkIds);
            if (startDate != null) {
                submissionWrapper.ge(HomeworkSubmission::getSubmitTime,
                        LocalDateTime.parse(startDate + " 00:00:00",
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
            if (endDate != null) {
                submissionWrapper.le(HomeworkSubmission::getSubmitTime,
                        LocalDateTime.parse(endDate + " 23:59:59",
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
            List<HomeworkSubmission> submissions = submissionMapper.selectList(submissionWrapper);

            // 3. 计算统计指标
            long totalStudents = submissions.stream()
                    .map(HomeworkSubmission::getUserId)
                    .distinct()
                    .count();

            long submittedCount = submissions.size();
            long gradedCount = submissions.stream()
                    .filter(s -> s.getGradeStatus() == 2)
                    .count();

            double avgScore = submissions.stream()
                    .filter(s -> s.getScore() != null)
                    .mapToDouble(s -> s.getScore().doubleValue())
                    .average()
                    .orElse(0.0);

            // 分数段分布
            Map<String, Integer> scoreDistribution = new LinkedHashMap<>();
            scoreDistribution.put("90-100", 0);
            scoreDistribution.put("80-89", 0);
            scoreDistribution.put("70-79", 0);
            scoreDistribution.put("60-69", 0);
            scoreDistribution.put("0-59", 0);

            submissions.stream()
                    .filter(s -> s.getScore() != null)
                    .forEach(s -> {
                        int score = s.getScore().intValue();
                        if (score >= 90) scoreDistribution.merge("90-100", 1, Integer::sum);
                        else if (score >= 80) scoreDistribution.merge("80-89", 1, Integer::sum);
                        else if (score >= 70) scoreDistribution.merge("70-79", 1, Integer::sum);
                        else if (score >= 60) scoreDistribution.merge("60-69", 1, Integer::sum);
                        else scoreDistribution.merge("0-59", 1, Integer::sum);
                    });

            // 迟交情况
            long lateCount = submissions.stream()
                    .filter(s -> s.getIsLate() != null && s.getIsLate() == 1)
                    .count();

            // 4. 构建报告
            report.put("courseId", courseId);
            report.put("homeworkCount", homeworkList.size());
            report.put("studentCount", totalStudents);
            report.put("submittedCount", submittedCount);
            report.put("gradedCount", gradedCount);
            report.put("avgScore", String.format("%.1f", avgScore));
            report.put("maxScore", submissions.stream()
                    .filter(s -> s.getScore() != null)
                    .map(HomeworkSubmission::getScore)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO).doubleValue());
            report.put("minScore", submissions.stream()
                    .filter(s -> s.getScore() != null)
                    .map(HomeworkSubmission::getScore)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO).doubleValue());
            report.put("scoreDistribution", scoreDistribution);
            report.put("lateCount", lateCount);
            report.put("lateRate", String.format("%.1f", (double) lateCount / submittedCount * 100));

            // 5. 使用 AI 生成分析建议（可选）
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                try {
                    String aiAnalysis = generateClassAnalysisAI(homeworkList, submissions, avgScore, lateCount);
                    report.put("aiAnalysis", aiAnalysis);
                } catch (Exception e) {
                    log.warn("AI 分析生成失败：{}", e.getMessage());
                }
            }

            // 6. 排名信息
            Map<String, Object> ranking = new HashMap<>();
            List<StudentScoreRanking> studentRankings = submissions.stream()
                    .filter(s -> s.getScore() != null)
                    .collect(Collectors.groupingBy(HomeworkSubmission::getUserId))
                    .entrySet().stream()
                    .map(entry -> {
                        double studentAvg = entry.getValue().stream()
                                .mapToDouble(s -> s.getScore().doubleValue())
                                .average()
                                .orElse(0.0);
                        User student = userMapper.selectById(entry.getKey());
                        return new StudentScoreRanking(
                                entry.getKey(),
                                student != null ? student.getRealName() : "未知",
                                studentAvg,
                                entry.getValue().size()
                        );
                    })
                    .sorted(Comparator.comparingDouble(StudentScoreRanking::getAvgScore).reversed())
                    .limit(10)
                    .collect(Collectors.toList());

            ranking.put("top10", studentRankings);
            report.put("ranking", ranking);

        } catch (Exception e) {
            log.error("生成班级学情报告失败：{}", e.getMessage(), e);
            report.put("error", e.getMessage());
        }

        return report;
    }

    /**
     * 获取学生个人学情报告
     */
    public Map<String, Object> getStudentReport(Long studentId, Long courseId, String reportType) {
        Map<String, Object> report = new HashMap<>();

        try {
            // 1. 获取学生信息
            User student = userMapper.selectById(studentId);
            if (student == null) {
                report.put("error", "学生不存在");
                return report;
            }

            report.put("studentId", studentId);
            report.put("studentName", student.getRealName());
            report.put("role", student.getRole());
            if (student.getRole().equals("STUDENT")) {
                report.put("grade", student.getGrade());
                report.put("major", student.getMajor());
                report.put("className", student.getClassName());
            }

            // 2. 获取作业提交
            LambdaQueryWrapper<HomeworkSubmission> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(HomeworkSubmission::getUserId, studentId);

            if (courseId != null) {
                List<Long> courseHomeworkIds = homeworkMapper.selectList(
                        new LambdaQueryWrapper<Homework>().eq(Homework::getCourseId, courseId)
                ).stream().map(Homework::getId).collect(Collectors.toList());

                if (!courseHomeworkIds.isEmpty()) {
                    wrapper.in(HomeworkSubmission::getHomeworkId, courseHomeworkIds);
                }
            }

            List<HomeworkSubmission> submissions = submissionMapper.selectList(wrapper);

            // 3. 统计指标
            int submittedCount = submissions.size();
            int gradedCount = (int) submissions.stream()
                    .filter(s -> s.getGradeStatus() == 2)
                    .count();
            int lateCount = (int) submissions.stream()
                    .filter(s -> s.getIsLate() != null && s.getIsLate() == 1)
                    .count();

            double avgScore = submissions.stream()
                    .filter(s -> s.getScore() != null)
                    .mapToDouble(s -> s.getScore().doubleValue())
                    .average()
                    .orElse(0.0);

            report.put("submittedCount", submittedCount);
            report.put("gradedCount", gradedCount);
            report.put("lateCount", lateCount);
            report.put("avgScore", String.format("%.1f", avgScore));

            // 4. 学习趋势（按时间）
            List<Map<String, Object>> trend = submissions.stream()
                    .filter(s -> s.getScore() != null)
                    .sorted(Comparator.comparing(HomeworkSubmission::getSubmitTime))
                    .map(s -> {
                        Map<String, Object> point = new HashMap<>();
                        point.put("time", s.getSubmitTime() != null ?
                                s.getSubmitTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "未知");
                        point.put("score", s.getScore().doubleValue());
                        point.put("homeworkId", s.getHomeworkId());
                        return point;
                    })
                    .collect(Collectors.toList());

            report.put("learningTrend", trend);

            // 5. 薄弱知识点（使用 AI 服务）
            List<AIGradeService.KnowledgePointAnalysis> weakPoints =
                    aiGradeService.identifyWeakPoints(studentId, courseId);
            report.put("weakPoints", weakPoints);

            // 6. AI 生成学习建议
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                try {
                    String aiSuggestion = generateStudentSuggestionAI(student, submissions, avgScore, weakPoints);
                    report.put("aiSuggestion", aiSuggestion);
                } catch (Exception e) {
                    log.warn("AI 建议生成失败：{}", e.getMessage());
                }
            }

            // 7. 预警状态
            String riskLevel = "low";
            if (avgScore < 60 || lateCount > submittedCount * 0.3) {
                riskLevel = "high";
            } else if (avgScore < 75 || lateCount > submittedCount * 0.1) {
                riskLevel = "medium";
            }
            report.put("riskLevel", riskLevel);

        } catch (Exception e) {
            log.error("生成学生学情报告失败：{}", e.getMessage(), e);
            report.put("error", e.getMessage());
        }

        return report;
    }

    /**
     * 获取学习预警列表
     */
    public List<Map<String, Object>> getLearningWarnings(Long courseId, String warningType) {
        List<Map<String, Object>> warnings = new ArrayList<>();

        try {
            // 获取课程所有作业
            List<Long> homeworkIds = homeworkMapper.selectList(
                    new LambdaQueryWrapper<Homework>().eq(Homework::getCourseId, courseId)
            ).stream().map(Homework::getId).collect(Collectors.toList());

            if (homeworkIds.isEmpty()) {
                return warnings;
            }

            // 获取所有提交
            List<HomeworkSubmission> submissions = submissionMapper.selectList(
                    new LambdaQueryWrapper<HomeworkSubmission>()
                            .in(HomeworkSubmission::getHomeworkId, homeworkIds)
            );

            // 按学生分组统计
            Map<Long, List<HomeworkSubmission>> studentSubmissions = submissions.stream()
                    .collect(Collectors.groupingBy(HomeworkSubmission::getUserId));

            for (Map.Entry<Long, List<HomeworkSubmission>> entry : studentSubmissions.entrySet()) {
                Long studentId = entry.getKey();
                List<HomeworkSubmission> studentSubs = entry.getValue();

                double avgScore = studentSubs.stream()
                        .filter(s -> s.getScore() != null)
                        .mapToDouble(s -> s.getScore().doubleValue())
                        .average()
                        .orElse(100.0);

                long lateCount = studentSubs.stream()
                        .filter(s -> s.getIsLate() != null && s.getIsLate() == 1)
                        .count();

                User student = userMapper.selectById(studentId);
                if (student == null) continue;

                // 检查预警条件
                if (avgScore < 60) {
                    Map<String, Object> warning = new HashMap<>();
                    warning.put("studentId", studentId);
                    warning.put("studentName", student.getRealName());
                    warning.put("warningType", "LOW_SCORE");
                    warning.put("warningLevel", "high");
                    warning.put("description", String.format("平均分%.1f 分，低于及格线", avgScore));
                    warning.put("value", avgScore);
                    warnings.add(warning);
                }

                if (lateCount > studentSubs.size() * 0.3) {
                    Map<String, Object> warning = new HashMap<>();
                    warning.put("studentId", studentId);
                    warning.put("studentName", student.getRealName());
                    warning.put("warningType", "LATE_SUBMISSION");
                    warning.put("warningLevel", "medium");
                    warning.put("description", String.format("迟交%d次，占总提交%.0f%%",
                            lateCount, (double) lateCount / studentSubs.size() * 100));
                    warning.put("value", lateCount);
                    warnings.add(warning);
                }
            }

        } catch (Exception e) {
            log.error("获取学习预警失败：{}", e.getMessage(), e);
        }

        return warnings;
    }

    /**
     * 生成教学建议
     */
    public Map<String, Object> generateTeachingSuggestion(Long homeworkId, Long courseId) {
        Map<String, Object> suggestion = new HashMap<>();

        try {
            // 获取作业提交
            List<HomeworkSubmission> submissions = submissionMapper.selectList(
                    new LambdaQueryWrapper<HomeworkSubmission>()
                            .eq(HomeworkSubmission::getHomeworkId, homeworkId)
            );

            if (submissions.isEmpty()) {
                suggestion.put("message", "暂无提交数据");
                return suggestion;
            }

            // 统计
            double avgScore = submissions.stream()
                    .filter(s -> s.getScore() != null)
                    .mapToDouble(s -> s.getScore().doubleValue())
                    .average()
                    .orElse(0.0);

            int passCount = (int) submissions.stream()
                    .filter(s -> s.getScore() != null && s.getScore().doubleValue() >= 60)
                    .count();

            double passRate = (double) passCount / submissions.size() * 100;

            suggestion.put("homeworkId", homeworkId);
            suggestion.put("submittedCount", submissions.size());
            suggestion.put("avgScore", String.format("%.1f", avgScore));
            suggestion.put("passRate", String.format("%.1f", passRate));

            // 生成建议
            List<String> suggestions = new ArrayList<>();

            if (avgScore < 60) {
                suggestions.add("班级整体成绩不理想，建议安排习题讲解课");
                suggestions.add("关注后进生，可进行一对一辅导");
            } else if (avgScore < 75) {
                suggestions.add("班级整体表现良好，但仍有提升空间");
                suggestions.add("针对共性错误进行集中讲解");
            } else {
                suggestions.add("班级整体表现优秀，可适当提高难度");
                suggestions.add("鼓励优秀学生进行拓展学习");
            }

            if (passRate < 70) {
                suggestions.add("及格率偏低，需重点关注学习困难学生");
            }

            // 使用 AI 生成更详细的建议
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                try {
                    String aiSuggestion = generateTeachingSuggestionAI(homeworkId, submissions, avgScore, passRate);
                    suggestion.put("aiSuggestion", aiSuggestion);
                } catch (Exception e) {
                    log.warn("AI 教学建议生成失败：{}", e.getMessage());
                }
            }

            suggestion.put("suggestions", suggestions);

        } catch (Exception e) {
            log.error("生成教学建议失败：{}", e.getMessage(), e);
            suggestion.put("error", e.getMessage());
        }

        return suggestion;
    }

    /**
     * 推荐学习资源
     */
    public List<Map<String, Object>> recommendResources(Long studentId, Long courseId) {
        List<Map<String, Object>> resources = new ArrayList<>();

        try {
            // 获取学生的薄弱知识点
            List<AIGradeService.KnowledgePointAnalysis> weakPoints =
                    aiGradeService.identifyWeakPoints(studentId, courseId);

            if (weakPoints.isEmpty()) {
                resources.add(createResource("继续保持良好学习状态", "巩固练习", "medium"));
                return resources;
            }

            // 根据薄弱点推荐资源
            for (AIGradeService.KnowledgePointAnalysis weakPoint : weakPoints) {
                Map<String, Object> resource = new HashMap<>();
                resource.put("knowledgePointId", weakPoint.getKnowledgePointId());
                resource.put("recommendation", String.format("针对知识点 %d 进行专项练习", weakPoint.getKnowledgePointId()));
                resource.put("reason", weakPoint.getWeakReason());
                resource.put("priority", weakPoint.getAvgScore() < 40 ? "high" : "medium");
                resource.put("suggestedAction", "观看相关教学视频并完成练习题");
                resources.add(resource);
            }

        } catch (Exception e) {
            log.error("推荐资源失败：{}", e.getMessage(), e);
        }

        return resources;
    }

    /**
     * 获取知识点掌握图谱
     */
    public Map<String, Object> getKnowledgeMap(Long studentId, Long courseId) {
        Map<String, Object> knowledgeMap = new HashMap<>();

        try {
            List<AIGradeService.KnowledgePointAnalysis> weakPoints =
                    aiGradeService.identifyWeakPoints(studentId, courseId);

            knowledgeMap.put("studentId", studentId);
            knowledgeMap.put("courseId", courseId);
            knowledgeMap.put("weakPoints", weakPoints);

            // 构建图谱数据（用于前端可视化）
            List<Map<String, Object>> nodes = new ArrayList<>();
            List<Map<String, Object>> links = new ArrayList<>();

            // 中心节点（学生）
            Map<String, Object> centerNode = new HashMap<>();
            centerNode.put("id", "student");
            centerNode.put("name", "知识点掌握");
            centerNode.put("value", 100);
            centerNode.put("category", 0);
            nodes.add(centerNode);

            // 知识点节点
            for (int i = 0; i < weakPoints.size(); i++) {
                AIGradeService.KnowledgePointAnalysis point = weakPoints.get(i);
                Map<String, Object> kpNode = new HashMap<>();
                kpNode.put("id", "kp_" + i);
                kpNode.put("name", "知识点-" + point.getKnowledgePointId());
                kpNode.put("value", point.getAvgScore());
                kpNode.put("category", 1);
                nodes.add(kpNode);

                // 连接关系
                Map<String, Object> link = new HashMap<>();
                link.put("source", "student");
                link.put("target", "kp_" + i);
                link.put("value", point.getAvgScore());
                links.add(link);
            }

            knowledgeMap.put("nodes", nodes);
            knowledgeMap.put("links", links);

        } catch (Exception e) {
            log.error("获取知识点图谱失败：{}", e.getMessage(), e);
            knowledgeMap.put("error", e.getMessage());
        }

        return knowledgeMap;
    }

    // ==================== AI 相关方法 ====================

    private String generateClassAnalysisAI(List<Homework> homeworkList,
                                            List<HomeworkSubmission> submissions,
                                            double avgScore, long lateCount) {
        // 构建 AI 提示词生成分析
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位教学经验丰富的教师，请根据以下班级作业情况生成学情分析报告。\n\n");
        prompt.append("【作业数量】").append(homeworkList.size()).append("\n");
        prompt.append("【提交数量】").append(submissions.size()).append("\n");
        prompt.append("【平均分】").append(String.format("%.1f", avgScore)).append("\n");
        prompt.append("【迟交数量】").append(lateCount).append("\n\n");
        prompt.append("请生成 100 字以内的分析报告，包括整体表现、存在的问题和教学建议。");

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            
            // 使用OpenAI兼容格式 - messages字段
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一位教学经验丰富的教师，擅长分析学情数据并提供专业建议。");
            messages.add(systemMessage);
            
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt.toString());
            messages.add(userMessage);
            
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 512);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(endpoint + "/chat/completions", HttpMethod.POST, entity, String.class);

            JsonNode rootNode = objectMapper.readTree(response.getBody());
            return rootNode.path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            log.warn("AI 分析生成失败：{}", e.getMessage());
            return String.format("班级整体表现：平均分%.1f 分，迟交%d次。建议关注后进生，加强作业管理。", avgScore, lateCount);
        }
    }

    private String generateStudentSuggestionAI(User student, List<HomeworkSubmission> submissions,
                                                double avgScore, List<AIGradeService.KnowledgePointAnalysis> weakPoints) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位学习指导老师，请根据以下学生情况生成学习建议。\n\n");
        prompt.append("【学生】").append(student.getRealName()).append("\n");
        prompt.append("【提交次数】").append(submissions.size()).append("\n");
        prompt.append("【平均分】").append(String.format("%.1f", avgScore)).append("\n");
        if (!weakPoints.isEmpty()) {
            prompt.append("【薄弱知识点】\n");
            for (AIGradeService.KnowledgePointAnalysis point : weakPoints) {
                prompt.append("- 知识点 ").append(point.getKnowledgePointId())
                        .append(": ").append(point.getWeakReason()).append("\n");
            }
        }
        prompt.append("\n请生成 100 字以内的学习建议。");

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            
            // 使用OpenAI兼容格式 - messages字段
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一位教学经验丰富的教师，擅长分析学情数据并提供专业建议。");
            messages.add(systemMessage);
            
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt.toString());
            messages.add(userMessage);
            
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 512);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(endpoint + "/chat/completions", HttpMethod.POST, entity, String.class);

            JsonNode rootNode = objectMapper.readTree(response.getBody());
            return rootNode.path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            log.warn("AI 建议生成失败：{}", e.getMessage());
            return String.format("该学生平均分%.1f 分，建议继续保持良好学习状态，针对薄弱知识点进行专项练习。", avgScore);
        }
    }

    private String generateTeachingSuggestionAI(Long homeworkId, List<HomeworkSubmission> submissions,
                                                 double avgScore, double passRate) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位教学经验丰富的教师，请根据以下作业批改情况生成教学建议。\n\n");
        prompt.append("【作业 ID】").append(homeworkId).append("\n");
        prompt.append("【提交数量】").append(submissions.size()).append("\n");
        prompt.append("【平均分】").append(String.format("%.1f", avgScore)).append("\n");
        prompt.append("【及格率】").append(String.format("%.1f", passRate)).append("%\n\n");
        prompt.append("请生成 150 字以内的教学建议，包括讲评重点和针对性措施。");

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            
            // 使用OpenAI兼容格式 - messages字段
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一位教学经验丰富的教师，擅长分析学情数据并提供专业建议。");
            messages.add(systemMessage);
            
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt.toString());
            messages.add(userMessage);
            
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 512);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(endpoint + "/chat/completions", HttpMethod.POST, entity, String.class);

            JsonNode rootNode = objectMapper.readTree(response.getBody());
            return rootNode.path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            log.warn("AI 教学建议生成失败：{}", e.getMessage());
            return "建议根据学生完成情况进行针对性讲解，关注后进生，鼓励优秀学生。";
        }
    }

    private Map<String, Object> createResource(String title, String type, String priority) {
        Map<String, Object> resource = new HashMap<>();
        resource.put("title", title);
        resource.put("type", type);
        resource.put("priority", priority);
        return resource;
    }

    // ==================== 数据类 ====================

    static class StudentScoreRanking {
        private final Long studentId;
        private final String studentName;
        private final double avgScore;
        private final int homeworkCount;

        public StudentScoreRanking(Long studentId, String studentName, double avgScore, int homeworkCount) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.avgScore = avgScore;
            this.homeworkCount = homeworkCount;
        }

        public Long getStudentId() { return studentId; }
        public String getStudentName() { return studentName; }
        public double getAvgScore() { return avgScore; }
        public int getHomeworkCount() { return homeworkCount; }
    }
}
