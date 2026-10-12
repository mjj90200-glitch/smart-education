package com.smartedu.dto;

import java.util.List;
import java.util.Map;

/**
 * AI 聊天请求 DTO
 * 用于接收前端发送的聊天消息
 *
 * @author SmartEdu Team
 */
public class AIChatRequestDTO {

    /**
     * 用户消息内容
     */
    private String message;

    /**
     * 对话历史
     */
    private List<Map<String, String>> history;

    /**
     * 文件 URL（可选）
     */
    private String fileUrl;

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Map<String, String>> getHistory() {
        return history;
    }

    public void setHistory(List<Map<String, String>> history) {
        this.history = history;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
}