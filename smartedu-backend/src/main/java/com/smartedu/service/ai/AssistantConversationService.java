package com.smartedu.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AssistantConversationService {

    private static final Logger logger = LoggerFactory.getLogger(AssistantConversationService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.bailian.api-key:}")
    private String apiKey;

    @Value("${ai.bailian.model:doubao-seed-2.0-pro}")
    private String model;

    @Value("${ai.bailian.endpoint:}")
    private String endpoint;

    public AssistantConversationService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public String generateReply(String userRole, String latestMessage, List<Map<String, String>> conversationHistory) {
        if (apiKey == null || apiKey.isBlank() || endpoint == null || endpoint.isBlank()) {
            return fallbackReply(userRole, latestMessage);
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.6);
            requestBody.put("max_tokens", 900);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", buildSystemPrompt(userRole)));

            if (conversationHistory != null) {
                for (Map<String, String> item : conversationHistory) {
                    String role = item.get("role");
                    String content = item.get("content");
                    if (role == null || content == null || content.isBlank()) {
                        continue;
                    }
                    if ("user".equals(role) || "assistant".equals(role)) {
                        messages.add(Map.of("role", role, "content", content));
                    }
                }
            }

            messages.add(Map.of("role", "user", "content", latestMessage == null ? "" : latestMessage));
            requestBody.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String apiUri = (endpoint.endsWith("/v1") || endpoint.endsWith("/v3"))
                ? endpoint + "/chat/completions"
                : endpoint;

            ResponseEntity<String> response = restTemplate.exchange(
                apiUri,
                HttpMethod.POST,
                new HttpEntity<>(requestBody, headers),
                String.class
            );

            return parseResponse(response.getBody(), userRole, latestMessage);
        } catch (Exception e) {
            logger.warn("智学助手通用问答调用失败：{}", e.getMessage());
            return fallbackReply(userRole, latestMessage);
        }
    }

    private String buildSystemPrompt(String userRole) {
        String rolePrompt = "STUDENT".equalsIgnoreCase(userRole)
            ? "你当前面对的是学生。回答要清晰、鼓励、偏学习辅导。"
            : "你当前面对的是教师。回答要专业、简洁、偏教学与作业管理。";

        return "你是“智学助手”，服务于智慧教育平台。"
            + "你既能回答常见学习/教学问题，也会在涉及平台操作时给出清晰下一步。"
            + rolePrompt
            + "如果用户是在提问常见知识、平台使用、学习建议、教学建议，请直接自然回答。"
            + "不要强行要求用户提交作业ID或课程ID，除非确实要执行提交作业或发布作业操作。"
            + "回答用中文，语气自然，不要过度营销，不要编造平台里不存在的按钮或页面。";
    }

    private String parseResponse(String responseBody, String userRole, String latestMessage) {
        if (responseBody == null || responseBody.isBlank()) {
            return fallbackReply(userRole, latestMessage);
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                String content = choices.get(0).path("message").path("content").asText();
                if (content != null && !content.isBlank()) {
                    return content.trim();
                }
            }

            JsonNode outputText = root.path("output").path("text");
            if (outputText.isTextual() && !outputText.asText().isBlank()) {
                return outputText.asText().trim();
            }
        } catch (Exception e) {
            logger.warn("解析智学助手通用问答响应失败：{}", e.getMessage());
        }

        return fallbackReply(userRole, latestMessage);
    }

    private String fallbackReply(String userRole, String latestMessage) {
        String message = latestMessage == null ? "" : latestMessage.trim();
        if (message.contains("JWT") || message.toLowerCase().contains("jwt")) {
            return "JWT 通常用来在前后端之间传递登录身份。前端在路由守卫里读取 token 后，可以判断用户是否已登录、是否有权限进入某个页面；后端再校验 token，确认请求身份有效。";
        }
        if (message.contains("学情分析")) {
            return "学情分析一般会围绕作业完成情况、成绩分布、GPA、提醒信息这几块来看。老师更关注全班趋势和需要提醒的学生，学生更关注自己的完成率、成绩和老师反馈。";
        }
        if ("STUDENT".equalsIgnoreCase(userRole)) {
            return "我可以正常帮你解答学习问题，也能继续协助你提交作业。你可以直接提问，或者告诉我你想处理哪一份作业。";
        }
        return "我可以正常帮你解答教学或平台使用问题，也能继续协助你发布作业。你可以直接提问，或者告诉我接下来想处理哪门课程的任务。";
    }
}
