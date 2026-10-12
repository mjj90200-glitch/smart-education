package com.smartedu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartedu.entity.Homework;
import com.smartedu.mapper.HomeworkMapper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 作业 AI 解析服务
 * 负责异步生成作业的 AI 解析内容
 *
 * AI 解析状态说明：
 * - 0: 未生成
 * - 1: 生成中
 * - 2: 待审核（生成成功）
 * - 3: 生成失败
 * - 4: 已发布
 *
 * @author SmartEdu Team
 */
@Service
public class HomeworkAnalysisService {

    // 状态常量
    private static final int STATUS_NOT_GENERATED = 0;  // 未生成
    private static final int STATUS_GENERATING = 1;     // 生成中
    private static final int STATUS_PENDING_REVIEW = 2; // 待审核
    private static final int STATUS_FAILED = 3;         // 生成失败
    private static final int STATUS_PUBLISHED = 4;      // 已发布

    private static final Logger logger = LoggerFactory.getLogger(HomeworkAnalysisService.class);

    private final HomeworkMapper homeworkMapper;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${ai.bailian.api-key:}")
    private String apiKey;

    @Value("${ai.bailian.model:qwen3.5-plus}")
    private String model;

    // OpenAI 兼容端点（阿里云百炼 Coding Plan）
    @Value("${ai.bailian.endpoint:https://coding.dashscope.aliyuncs.com/v1/chat/completions}")
    private String endpoint;

    public HomeworkAnalysisService(HomeworkMapper homeworkMapper) {
        this.homeworkMapper = homeworkMapper;
        this.objectMapper = new ObjectMapper();
        this.restTemplate = new RestTemplate();
    }

    /**
     * 异步生成作业解析
     * @param homeworkId 作业 ID
     */
    @Async
    public void generateAnalysisAsync(Long homeworkId) {
        logger.info("======== 开始异步生成作业解析 ========");
        logger.info("homeworkId: {}", homeworkId);

        // 【关键】立即更新状态为"生成中"，让前端能看到状态变化
        updateAnalysisStatus(homeworkId, STATUS_GENERATING, "正在生成中...");

        try {
            // 获取作业信息
            Homework homework = homeworkMapper.selectById(homeworkId);
            if (homework == null) {
                logger.error("======== 作业不存在，homeworkId={} ========", homeworkId);
                updateAnalysisError(homeworkId, "作业不存在");
                return;
            }

            logger.info("作业标题: {}", homework.getTitle());
            logger.info("附件URL: {}", homework.getAttachmentUrl());
            logger.info("API Key 前4位: {}",
                apiKey != null && apiKey.length() > 4 ? apiKey.substring(0, 4) + "***" : "未配置");
            logger.info("模型名称: {}", model);
            logger.info("端点地址: {}", endpoint);

            // 构建解析 Prompt
            String prompt = buildAnalysisPrompt(homework);
            logger.info("Prompt 长度: {} 字符", prompt.length());
            logger.info("Prompt 内容: {}", prompt);

            // 调用 AI API
            logger.info("======== 开始调用阿里云百炼 API ========");
            logger.info("准备调用百炼 API，读取到文件：{}", homework.getAttachmentName());
            String analysis = callAIForAnalysis(prompt);
            logger.info("AI 返回内容长度: {} 字符", analysis != null ? analysis.length() : 0);

            // 保存结果，状态设为"待审核"
            saveAnalysisResult(homeworkId, analysis);

            logger.info("======== 作业解析生成完成，homeworkId={}, status=2（待审核） ========", homeworkId);

        } catch (Exception e) {
            logger.error("======== 生成作业解析失败 ========");
            logger.error("homeworkId: {}", homeworkId);
            logger.error("错误信息: {}", e.getMessage(), e);
            // 更新状态为生成失败
            updateAnalysisError(homeworkId, e.getMessage());
        }
    }

    /**
     * 更新解析状态
     */
    private void updateAnalysisStatus(Long homeworkId, int status, String content) {
        Homework homework = new Homework();
        homework.setId(homeworkId);
        homework.setAiAnalysisStatus(status);
        homework.setAiAnalysisContent(content);
        homeworkMapper.updateById(homework);
        logger.info("状态已更新：homeworkId={}, status={}, content={}", homeworkId, status,
            content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content);
    }

    /**
     * 同步生成作业解析的入口方法
     * @param homeworkId 作业 ID
     */
    public void generateAnalysisForHomework(Long homeworkId) {
        generateAnalysisAsync(homeworkId);
    }

    /**
     * 构建解析生成提示词
     */
    private String buildAnalysisPrompt(Homework homework) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位资深的教师，请为以下作业生成详细的解析参考答案。\n\n");

        prompt.append("【作业标题】").append(homework.getTitle()).append("\n");

        // 作业描述
        String description = homework.getDescription();
        if (description != null && !description.trim().isEmpty()) {
            prompt.append("【作业描述】").append(description).append("\n");
        }

        // 作业内容（如果有直接输入的内容）
        String content = homework.getContent();
        if (content != null && !content.trim().isEmpty()) {
            prompt.append("【作业内容】\n").append(content).append("\n");
        }

        // 附件内容（如果有）
        if (homework.getAttachmentUrl() != null && !homework.getAttachmentUrl().trim().isEmpty()) {
            String attachmentContent = readAttachmentContent(homework.getAttachmentUrl());
            if (attachmentContent != null && !attachmentContent.trim().isEmpty()) {
                prompt.append("【作业附件内容】\n").append(attachmentContent).append("\n");
            }
        }

        prompt.append("\n【要求】\n");
        prompt.append("1. 请生成完整的解题思路和参考答案\n");
        prompt.append("2. 解析应当详细、准确，便于学生理解\n");
        prompt.append("3. 如果有多种解题方法，请列出主要方法\n");
        prompt.append("4. 对于编程类作业，请提供关键代码片段\n");
        prompt.append("5. 最后用简洁的总结收尾\n\n");

        prompt.append("请直接输出解析内容，格式清晰，使用 Markdown 格式。");

        return prompt.toString();
    }

    /**
     * 读取附件内容
     * 支持：.txt, .md, .docx, .pdf 等格式
     */
    private String readAttachmentContent(String attachmentUrl) {
        try {
            String baseDir = System.getProperty("user.dir");

            // 统一路径格式：处理可能的路径分隔符问题
            String normalizedUrl = attachmentUrl.replace("/", File.separator).replace("\\", File.separator);

            // 去除开头的路径分隔符（手动处理，避免正则表达式问题）
            String urlWithoutLeadingSep = normalizedUrl;
            while (urlWithoutLeadingSep.startsWith(File.separator)) {
                urlWithoutLeadingSep = urlWithoutLeadingSep.substring(1);
            }

            // 尝试多种路径组合
            String[] possiblePaths = {
                baseDir + File.separator + "uploads" + File.separator + urlWithoutLeadingSep,
                baseDir + File.separator + "uploads" + normalizedUrl,
                baseDir + normalizedUrl,
                normalizedUrl
            };

            for (String path : possiblePaths) {
                File file = new File(path);
                if (file.exists() && file.isFile()) {
                    logger.info("======== 找到附件文件：{} ========", path);

                    String fileName = file.getName().toLowerCase();
                    String content = null;

                    // 根据文件类型选择解析方式
                    if (fileName.endsWith(".docx")) {
                        logger.info("检测到 .docx 文件，使用 Apache POI 解析...");
                        content = extractDocxContent(file);
                    } else if (fileName.endsWith(".doc")) {
                        logger.info("检测到 .doc 文件（旧格式），暂不支持");
                        return "[.doc 格式文件暂不支持，请转换为 .docx 格式后重新上传]";
                    } else if (fileName.endsWith(".pdf")) {
                        logger.info("检测到 .pdf 文件，使用 Apache PDFBox 解析...");
                        content = extractPdfContent(file);
                    } else if (fileName.endsWith(".txt") || fileName.endsWith(".md")) {
                        logger.info("检测到文本文件，直接读取...");
                        content = extractTextContent(file);
                    } else if (isBinaryFile(fileName)) {
                        logger.warn("附件是不支持的二进制文件：{}", fileName);
                        return "[附件格式不支持解析，请上传 .docx、.pdf 或 .txt 格式的文件]";
                    } else {
                        // 尝试作为文本文件读取
                        logger.info("尝试作为文本文件读取...");
                        content = extractTextContent(file);
                    }

                    if (content != null && !content.trim().isEmpty()) {
                        // 限制内容长度
                        if (content.length() > 8000) {
                            content = content.substring(0, 8000) + "\n\n[内容过长，已截断]";
                        }
                        logger.info("======== 成功读取附件内容，长度: {} 字符 ========", content.length());
                        return content;
                    }

                    return "[附件内容为空或无法解析]";
                }
            }

            logger.warn("附件文件不存在，尝试的路径：{}", String.join("; ", possiblePaths));
            return null;

        } catch (Exception e) {
            logger.error("读取附件内容失败：{}", e.getMessage(), e);
            return "[读取附件失败：" + e.getMessage() + "]";
        }
    }

    /**
     * 提取 .docx 文件内容
     */
    private String extractDocxContent(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {

            String text = extractor.getText();
            logger.info(".docx 解析成功，提取文本长度: {} 字符", text != null ? text.length() : 0);
            return text;

        } catch (Exception e) {
            logger.error("解析 .docx 文件失败：{}", e.getMessage(), e);
            return "[解析 Word 文档失败：" + e.getMessage() + "]";
        }
    }

    /**
     * 提取 .pdf 文件内容
     */
    private String extractPdfContent(File file) {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            logger.info(".pdf 解析成功，提取文本长度: {} 字符", text != null ? text.length() : 0);
            return text;

        } catch (Exception e) {
            logger.error("解析 .pdf 文件失败：{}", e.getMessage(), e);
            return "[解析 PDF 文件失败：" + e.getMessage() + "]";
        }
    }

    /**
     * 提取纯文本文件内容
     */
    private String extractTextContent(File file) {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(file.getAbsolutePath()));

            // 检测是否为有效的 UTF-8 文本
            if (!isValidUtf8Text(bytes)) {
                logger.warn("文件内容不是有效的 UTF-8 文本");
                return "[文件编码无法识别，请确保文件为 UTF-8 编码的文本文件]";
            }

            return new String(bytes, "UTF-8");

        } catch (Exception e) {
            logger.error("读取文本文件失败：{}", e.getMessage());
            return "[读取文本文件失败：" + e.getMessage() + "]";
        }
    }

    /**
     * 判断是否为不支持的二进制文件
     * 注意：.docx 和 .pdf 现在支持解析，不视为二进制文件
     */
    private boolean isBinaryFile(String fileName) {
        // 不支持的二进制文件扩展名
        String[] binaryExtensions = {
            // 文档类（已支持的不在此列）
            ".doc",  // 旧版 Word，暂不支持
            ".pptx", ".ppt", ".xlsx", ".xls",
            // 压缩包
            ".zip", ".rar", ".7z", ".tar", ".gz",
            // 图片
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".ico", ".svg",
            // 音视频
            ".mp3", ".mp4", ".avi", ".mov", ".wav", ".flv",
            // 可执行文件
            ".exe", ".dll", ".so", ".dylib",
            // Java
            ".class", ".jar", ".war"
        };

        for (String ext : binaryExtensions) {
            if (fileName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检测字节数组是否为有效的 UTF-8 文本
     */
    private boolean isValidUtf8Text(byte[] bytes) {
        try {
            // 尝试用 UTF-8 解码
            String text = new String(bytes, "UTF-8");

            // 检查是否有过多乱码字符（替换字符）
            int replacementCount = 0;
            for (char c : text.toCharArray()) {
                if (c == '\ufffd') { // Unicode 替换字符
                    replacementCount++;
                }
            }

            // 如果替换字符超过 5%， 认为不是有效文本
            double ratio = (double) replacementCount / text.length();
            return ratio < 0.05;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 调用 AI API 生成解析
     * OpenAI 兼容格式（阿里云百炼 Coding Plan）
     */
    private String callAIForAnalysis(String prompt) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("AI API Key 未配置，返回默认解析");
            return "AI 解析功能暂未配置，请联系管理员配置 AI 服务。\n\n" +
                   "**提示**：作业解析生成功能需要配置阿里云百炼 API Key。";
        }

        try {
            // OpenAI 兼容格式请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);

            // messages 数组格式（OpenAI 兼容）
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);
            requestBody.put("messages", messages);

            String requestJson = objectMapper.writeValueAsString(requestBody);
            logger.info("======== AI API 请求 ========");
            logger.info("端点: {}", endpoint);
            logger.info("模型: {}", model);
            logger.info("请求体: {}", requestJson);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 拼接完整的 API 路径（如果 endpoint 以 /v1 或 /v3 结尾，则添加 /chat/completions）
            String apiUri = (endpoint.endsWith("/v1") || endpoint.endsWith("/v3")) ? (endpoint + "/chat/completions") : endpoint;

            // ====== 健康检查日志 ======
            System.out.println("====== [AI 请求] 完整 URL: " + apiUri + " ======");
            logger.info("完整 API URI: {}", apiUri);

            ResponseEntity<String> response = restTemplate.exchange(
                apiUri,
                HttpMethod.POST,
                entity,
                String.class
            );

            // ====== 健康检查日志 ======
            System.out.println("      [AI 响应] 状态码：" + response.getStatusCode());
            logger.info("======== AI API 响应 ========");
            logger.info("状态码: {}", response.getStatusCode());
            logger.info("响应内容: {}", response.getBody());
            return parseAnalysisResponse(response.getBody());

        } catch (Exception e) {
            logger.error("======== 调用 AI API 失败 ========");
            logger.error("错误信息: {}", e.getMessage(), e);
            return "解析生成失败：" + e.getMessage() + "\n\n请稍后重试或联系管理员。";
        }
    }

    /**
     * 解析 AI 响应
     * 支持 OpenAI 兼容格式和阿里云百炼原生格式
     */
    private String parseAnalysisResponse(String response) {
        if (response == null || response.isEmpty()) {
            return "AI 未返回有效内容";
        }

        try {
            JsonNode rootNode = objectMapper.readTree(response);

            // 格式1: OpenAI 兼容格式 - choices[0].message.content
            JsonNode choicesNode = rootNode.path("choices");
            if (choicesNode.isArray() && choicesNode.size() > 0) {
                JsonNode contentNode = choicesNode.get(0).path("message").path("content");
                if (contentNode.isTextual()) {
                    logger.info("解析成功（OpenAI 格式），内容长度: {}", contentNode.asText().length());
                    return contentNode.asText();
                }
            }

            // 格式2: 阿里云百炼原生格式 - output.text
            JsonNode outputNode = rootNode.path("output").path("text");
            if (outputNode.isTextual()) {
                logger.info("解析成功（百炼 text 格式），内容长度: {}", outputNode.asText().length());
                return outputNode.asText();
            }

            // 格式3: 阿里云百炼 message 格式 - output.choices[0].message.content
            JsonNode outputChoicesNode = rootNode.path("output").path("choices");
            if (outputChoicesNode.isArray() && outputChoicesNode.size() > 0) {
                JsonNode contentNode = outputChoicesNode.get(0).path("message").path("content");
                if (contentNode.isTextual()) {
                    logger.info("解析成功（百炼 message 格式），内容长度: {}", contentNode.asText().length());
                    return contentNode.asText();
                }
            }

            // 格式4: result 字段（某些模型）
            JsonNode resultNode = rootNode.path("result");
            if (resultNode.isTextual()) {
                logger.info("解析成功（result 格式），内容长度: {}", resultNode.asText().length());
                return resultNode.asText();
            }

            logger.warn("======== AI 响应格式无法解析 ========");
            logger.warn("完整响应: {}", response);
            return "解析格式解析失败，原始响应：" + response.substring(0, Math.min(500, response.length()));

        } catch (Exception e) {
            logger.error("解析 AI 响应失败：{}", e.getMessage());
            return "解析内容解析失败：" + e.getMessage();
        }
    }

    /**
     * 保存解析结果
     */
    private void saveAnalysisResult(Long homeworkId, String analysis) {
        Homework homework = new Homework();
        homework.setId(homeworkId);
        homework.setAiAnalysisContent(analysis);
        homework.setAiAnalysisStatus(STATUS_PENDING_REVIEW); // 状态设为待审核
        homeworkMapper.updateById(homework);

        logger.info("解析结果已保存，homeworkId={}, status=2（待审核）", homeworkId);
    }

    /**
     * 更新解析错误状态
     * 状态 3 表示生成失败
     */
    private void updateAnalysisError(Long homeworkId, String errorMessage) {
        Homework homework = new Homework();
        homework.setId(homeworkId);
        homework.setAiAnalysisContent("❌ 解析生成失败：" + errorMessage + "\n\n您可以点击「刷新状态」重新尝试，或联系管理员。");
        homework.setAiAnalysisStatus(STATUS_FAILED); // 状态 3 表示生成失败
        homeworkMapper.updateById(homework);
        logger.error("解析生成失败，已更新状态为 3，homeworkId={}, error={}", homeworkId, errorMessage);
    }

    /**
     * 重新生成解析（用于失败后重试）
     */
    public void retryGenerateAnalysis(Long homeworkId) {
        // 重置状态为 0，然后触发新的生成
        Homework homework = new Homework();
        homework.setId(homeworkId);
        homework.setAiAnalysisStatus(0);
        homework.setAiAnalysisContent(null);
        homeworkMapper.updateById(homework);

        // 异步生成
        generateAnalysisAsync(homeworkId);
    }

    /**
     * 审核并发布解析
     * @param homeworkId 作业 ID
     * @param editedContent 教师修改后的内容（可选）
     * @param teacherId 教师 ID
     */
    public void approveAnalysis(Long homeworkId, String editedContent, Long teacherId) {
        Homework homework = homeworkMapper.selectById(homeworkId);
        if (homework == null) {
            throw new RuntimeException("作业不存在");
        }

        if (homework.getAiAnalysisStatus() == null || homework.getAiAnalysisStatus() < STATUS_PENDING_REVIEW) {
            throw new RuntimeException("解析尚未生成完成，无法发布");
        }

        Homework updateHomework = new Homework();
        updateHomework.setId(homeworkId);
        updateHomework.setAiAnalysisStatus(STATUS_PUBLISHED); // 状态设为已发布

        // 如果教师修改了内容，保存修改后的内容
        if (editedContent != null && !editedContent.trim().isEmpty()) {
            updateHomework.setTeacherEditedAnalysis(editedContent);
        }

        homeworkMapper.updateById(updateHomework);
        logger.info("解析已发布，homeworkId={}, teacherId={}, status=4（已发布）", homeworkId, teacherId);
    }

    /**
     * 获取作业解析内容
     * @param homeworkId 作业 ID
     * @return 解析内容信息
     */
    public Map<String, Object> getAnalysis(Long homeworkId) {
        Homework homework = homeworkMapper.selectById(homeworkId);
        if (homework == null) {
            throw new RuntimeException("作业不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("homeworkId", homeworkId);
        result.put("title", homework.getTitle());
        result.put("aiAnalysisStatus", homework.getAiAnalysisStatus());
        result.put("aiAnalysisContent", homework.getAiAnalysisContent());
        result.put("teacherEditedAnalysis", homework.getTeacherEditedAnalysis());

        return result;
    }
}