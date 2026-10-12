package com.smartedu.service.ai;

import com.smartedu.agent.HomeworkAgentTools;
import com.smartedu.agent.HomeworkAgentTools.PublishHomeworkRequest;
import com.smartedu.agent.HomeworkAgentTools.SubmitHomeworkRequest;
import com.smartedu.entity.Course;
import com.smartedu.vo.StudentHomeworkVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AgentOrchestratorService {

    private static final Logger logger = LoggerFactory.getLogger(AgentOrchestratorService.class);

    private static final Pattern HOMEWORK_ID_PATTERN = Pattern.compile("(?:作业\\s*ID|homework\\s*id|作业)\\s*[:：#]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COURSE_ID_PATTERN = Pattern.compile("(?:课程\\s*ID|course\\s*id|课程)\\s*[:：#]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COURSE_NAME_PATTERN = Pattern.compile("(?:课程名称|课程名|course\\s*name)\\s*[:：]\\s*([^\\n，。,；;]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TITLE_PATTERN = Pattern.compile("(?:标题|作业标题|title)\\s*[:：]\\s*([^\\n，。,；;]+)");
    private static final Pattern DATE_PATTERN = Pattern.compile("(20\\d{2}[-/年]\\d{1,2}[-/月]\\d{1,2}(?:[日\\sT]\\d{1,2}:\\d{2}(?::\\d{2})?)?)");
    private static final Pattern DAYS_PATTERN = Pattern.compile("(\\d{1,3})\\s*天");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("（[^：）]+：\\s*）|\\([^:)]+:\\s*\\)");

    private final HomeworkAgentTools homeworkAgentTools;
    private final AssistantConversationService assistantConversationService;

    public AgentOrchestratorService(HomeworkAgentTools homeworkAgentTools,
                                    AssistantConversationService assistantConversationService) {
        this.homeworkAgentTools = homeworkAgentTools;
        this.assistantConversationService = assistantConversationService;
    }

    public String chat(String userMessage, List<Map<String, String>> conversationHistory, Long userId, String userRole) {
        return handleConversation(userMessage, conversationHistory, userId, userRole, null);
    }

    public String chatWithFile(String userMessage, String fileUrl, List<Map<String, String>> conversationHistory,
                               Long userId, String userRole) {
        return handleConversation(userMessage, conversationHistory, userId, userRole, fileUrl);
    }

    public Flux<String> chatStream(String userMessage, List<Map<String, String>> conversationHistory,
                                   Long userId, String userRole, String username) {
        return buildStream(handleConversation(userMessage, conversationHistory, userId, userRole, null), username);
    }

    public Flux<String> chatWithFileStream(String userMessage, String fileUrl, List<Map<String, String>> conversationHistory,
                                           Long userId, String userRole, String username) {
        return buildStream(handleConversation(userMessage, conversationHistory, userId, userRole, fileUrl), username);
    }

    public Flux<String> streamText(String text, String username) {
        return buildStream(text, username);
    }

    private Flux<String> buildStream(String text, String username) {
        List<String> chunks = splitIntoChunks(text);
        return Flux.fromIterable(chunks)
            .delayElements(Duration.ofMillis(25))
            .doOnSubscribe(subscription -> logger.info("AI 助手开始响应，用户={}", username == null ? "unknown" : username))
            .doOnComplete(() -> logger.info("AI 助手响应完成，用户={}", username == null ? "unknown" : username));
    }

    private String handleConversation(String userMessage, List<Map<String, String>> conversationHistory,
                                      Long userId, String userRole, String fileUrl) {
        if (userId == null || userRole == null || userRole.isBlank()) {
            return "当前登录信息不可用，请刷新页面后重新登录。";
        }

        String message = userMessage == null ? "" : userMessage.trim();
        String combinedContext = buildContext(message, conversationHistory);
        String normalized = combinedContext.toLowerCase(Locale.ROOT);

        if (containsEmptyPlaceholders(message)) {
            return buildPlaceholderGuidance(userRole);
        }

        if (isStudentSubmitIntent(normalized, userRole, fileUrl)) {
            return handleStudentSubmit(message, combinedContext, userId, userRole, fileUrl);
        }

        if (isTeacherPublishIntent(normalized, userRole, fileUrl)) {
            return handleTeacherPublish(message, combinedContext, userId, userRole, fileUrl);
        }

        return buildGeneralReply(message, userRole, fileUrl, conversationHistory);
    }

    private String handleStudentSubmit(String message, String context, Long userId, String userRole, String fileUrl) {
        if (!"STUDENT".equalsIgnoreCase(userRole)) {
            return "当前账号不是学生身份，我只能帮学生提交作业。你可以切换学生账号后再提交。";
        }

        Long homeworkId = extractLong(context, HOMEWORK_ID_PATTERN);
        List<StudentHomeworkVO> pendingHomework = homeworkAgentTools.getPendingHomework(userId);

        if (homeworkId == null) {
            if (pendingHomework.isEmpty()) {
                return "我已经帮你查过了，你当前没有待提交的作业。如果你想确认历史提交情况，也可以告诉我具体作业。";
            }

            if (pendingHomework.size() == 1) {
                StudentHomeworkVO onlyHomework = pendingHomework.get(0);
                if ((fileUrl != null && !fileUrl.isBlank()) || (extractAnswerContent(message) != null && !extractAnswerContent(message).isBlank())) {
                    homeworkId = onlyHomework.getId();
                } else {
                    return homeworkAgentTools.formatPendingHomework(pendingHomework)
                        + "\n\n我已经帮你定位到唯一一份待提交作业。现在把文件上传给我，或者直接发送：\n"
                        + "帮我提交作业（作业ID：" + onlyHomework.getId() + "）（答案内容：可选）";
                }
            } else {
                return homeworkAgentTools.formatPendingHomework(pendingHomework)
                    + "\n\n请直接回复：帮我提交作业（作业ID：）"
                    + "\n如果你已经上传文件，我拿到作业 ID 后就能继续处理。";
            }
        }

        String content = extractAnswerContent(message);
        if ((fileUrl == null || fileUrl.isBlank()) && (content == null || content.isBlank())) {
            return "我已经识别到你要提交作业，但还缺少答案内容。请上传文件，或者直接在消息里写“答案：你的内容”。";
        }

        SubmitHomeworkRequest request = new SubmitHomeworkRequest();
        request.homeworkId = homeworkId;
        request.fileUrl = fileUrl;
        request.content = content;
        request.userId = userId;
        request.userRole = userRole;

        Map<String, Object> result = homeworkAgentTools.doSubmitHomework(request);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return String.format(
                "作业已经帮你提交好了。\n\n作业ID：%s\n提交时间：%s\n\n接下来你可以去“我的作业”里查看提交状态和成绩。",
                result.get("homeworkId"),
                result.get("submissionTime")
            );
        }

        return "提交没有成功，原因是：" + result.getOrDefault("error", "未知错误") + "。你可以把作业 ID 和附件再发我一次，我继续帮你处理。";
    }

    private String handleTeacherPublish(String message, String context, Long userId, String userRole, String fileUrl) {
        if (!"TEACHER".equalsIgnoreCase(userRole) && !"ADMIN".equalsIgnoreCase(userRole)) {
            return "当前账号不是教师身份，我只能帮教师发布作业。";
        }

        List<Course> courseList = homeworkAgentTools.getTeacherCourses(userId);
        Long courseId = extractLong(context, COURSE_ID_PATTERN);
        if (courseId == null) {
            courseId = extractCourseIdByName(context, courseList);
        }

        if (courseId == null) {
            if (courseList.isEmpty()) {
                return "我没有查到你名下可发布作业的课程，建议先确认课程是否已经创建并分配到当前教师账号。";
            }
            return homeworkAgentTools.formatTeacherCourses(courseList)
                + "\n\n你可以直接填写这个模板发给我：\n"
                + "帮我发布作业（课程名称：）（作业标题：）（截止时间：7天）";
        }

        if (fileUrl == null || fileUrl.isBlank()) {
            return "课程我已经识别到了。接下来请先上传作业文件，然后再发一次：\n"
                + "帮我发布作业（课程名称：" + resolveCourseName(courseId, courseList) + "）（作业标题：）（截止时间：7天）";
        }

        String title = extractTitle(context, fileUrl);
        String endTime = extractDeadline(context);
        String description = buildDescription(message, title);

        PublishHomeworkRequest request = new PublishHomeworkRequest();
        request.courseId = courseId;
        request.title = title;
        request.fileUrl = fileUrl;
        request.description = description;
        request.endTime = endTime;
        request.userId = userId;
        request.userRole = "ADMIN".equalsIgnoreCase(userRole) ? "TEACHER" : userRole;

        Map<String, Object> result = homeworkAgentTools.doPublishHomework(request);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return String.format(
                "作业已经发布完成。\n\n作业ID：%s\n标题：%s\n课程ID：%s\n\n学生现在可以在作业列表里看到并提交这份作业了。",
                result.get("homeworkId"),
                result.get("title"),
                courseId
            );
        }

        return "发布作业失败，原因是：" + result.getOrDefault("error", "未知错误") + "。你可以把课程 ID、标题和文件重新发我一次。";
    }

    private String buildGeneralReply(String message, String userRole, String fileUrl, List<Map<String, String>> conversationHistory) {
        String normalized = message == null ? "" : message.toLowerCase(Locale.ROOT);

        if (normalized.contains("你好") || normalized.contains("hello") || normalized.contains("hi")) {
            if ("TEACHER".equalsIgnoreCase(userRole) || "ADMIN".equalsIgnoreCase(userRole)) {
                return "你好，我是智学助手。我可以帮你发布作业、整理教学说明，也可以直接回答教学和平台使用上的常见问题。";
            }
            return "你好，我是智学助手。我可以帮你提交作业、梳理学习任务，也可以直接回答课程学习和平台使用上的常见问题。";
        }

        if (normalized.contains("能做什么") || normalized.contains("你可以做什么") || normalized.contains("help")) {
            if ("TEACHER".equalsIgnoreCase(userRole) || "ADMIN".equalsIgnoreCase(userRole)) {
                return "我现在可以直接帮你做三类事：\n1. 上传文档后发布作业。\n2. 先帮你列出课程，再按模板一步步完成发布。\n3. 解答教学安排、平台操作、作业说明整理这类常见问题。";
            }
            return "我现在可以直接帮你做三类事：\n1. 自动检查你还没提交的作业并继续帮你提交。\n2. 上传文件或直接填写文字答案来交作业。\n3. 解答常见学习问题和平台使用问题。";
        }

        if (fileUrl != null && !fileUrl.isBlank()) {
            if ("TEACHER".equalsIgnoreCase(userRole) || "ADMIN".equalsIgnoreCase(userRole)) {
                return "文件我已经收到了。接下来告诉我课程 ID 和作业标题，我就能继续帮你发布。";
            }
            return "文件我已经收到了。如果你不确定作业 ID，也可以直接说“帮我检查未提交作业”，我会先帮你查。";
        }

        return assistantConversationService.generateReply(userRole, message, conversationHistory);
    }

    private boolean isStudentSubmitIntent(String normalized, String userRole, String fileUrl) {
        if ("STUDENT".equalsIgnoreCase(userRole) && fileUrl != null && !fileUrl.isBlank()) {
            return true;
        }
        return normalized.contains("提交作业")
            || normalized.contains("上传作业")
            || normalized.contains("交作业")
            || normalized.contains("submit homework");
    }

    private boolean isTeacherPublishIntent(String normalized, String userRole, String fileUrl) {
        if (("TEACHER".equalsIgnoreCase(userRole) || "ADMIN".equalsIgnoreCase(userRole)) && fileUrl != null && !fileUrl.isBlank()) {
            return normalized.contains("发布")
                || normalized.contains("布置")
                || normalized.contains("作业")
                || normalized.contains("publish");
        }
        return normalized.contains("发布作业")
            || normalized.contains("布置作业")
            || normalized.contains("创建作业")
            || normalized.contains("publish homework");
    }

    private String buildContext(String message, List<Map<String, String>> conversationHistory) {
        StringBuilder builder = new StringBuilder();
        if (conversationHistory != null) {
            for (Map<String, String> item : conversationHistory) {
                if (!"user".equals(item.get("role"))) {
                    continue;
                }
                String content = item.get("content");
                if (content != null && !content.isBlank()) {
                    builder.append(content).append('\n');
                }
            }
        }
        if (message != null && !message.isBlank()) {
            builder.append(message);
        }
        return builder.toString();
    }

    private Long extractLong(String text, Pattern pattern) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Matcher matcher = pattern.matcher(text);
        Long value = null;
        while (matcher.find()) {
            value = Long.parseLong(matcher.group(1));
        }
        return value;
    }

    private String extractTitle(String text, String fileUrl) {
        if (text != null) {
            Matcher matcher = TITLE_PATTERN.matcher(text);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }

        if (fileUrl != null && fileUrl.contains("/")) {
            String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
            int dotIndex = fileName.lastIndexOf('.');
            return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        }

        return "AI 助手发布的作业";
    }

    private String extractDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        Matcher matcher = DATE_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }

        String raw = matcher.group(1)
            .replace("年", "-")
            .replace("月", "-")
            .replace("日", "")
            .replace("/", "-")
            .replace("T", " ")
            .trim();

        if (raw.matches("\\d{4}-\\d{1,2}-\\d{1,2}$")) {
            raw = raw + " 23:59:59";
        } else if (raw.matches("\\d{4}-\\d{1,2}-\\d{1,2}\\s+\\d{1,2}:\\d{2}$")) {
            raw = raw + ":00";
        }

        try {
            DateTimeFormatter parser = DateTimeFormatter.ofPattern("yyyy-M-d HH:mm:ss");
            LocalDateTime dateTime = LocalDateTime.parse(raw, parser);
            return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            logger.warn("解析日期失败：{}", raw);
            return null;
        }
    }

    private String extractAnswerContent(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        int index = message.indexOf("答案：");
        if (index >= 0) {
            return message.substring(index + 3).trim();
        }
        index = message.indexOf("答案:");
        if (index >= 0) {
            return message.substring(index + 3).trim();
        }
        return null;
    }

    private String buildDescription(String message, String title) {
        if (message == null || message.isBlank()) {
            return "通过智学助手快速发布的作业";
        }
        String cleaned = message.replace(title, "").trim();
        return cleaned.isBlank() ? "通过智学助手快速发布的作业" : cleaned;
    }

    private boolean containsEmptyPlaceholders(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        return PLACEHOLDER_PATTERN.matcher(message).find();
    }

    private String buildPlaceholderGuidance(String userRole) {
        if ("TEACHER".equalsIgnoreCase(userRole) || "ADMIN".equalsIgnoreCase(userRole)) {
            return "我看到了你正在使用发布模板。请把括号里的信息补全后再发我一次，例如：\n"
                + "帮我发布作业（课程名称：数据结构）（作业标题：第3章练习）（截止时间：7天）";
        }
        return "我看到了你正在使用提交模板。请把括号里的信息补全后再发我一次；如果你不清楚作业 ID，也可以直接回复“帮我检查未提交作业”，我会先帮你查。";
    }

    private Long extractCourseIdByName(String text, List<Course> courseList) {
        if (text == null || text.isBlank() || courseList == null || courseList.isEmpty()) {
            return null;
        }

        Matcher namedMatcher = COURSE_NAME_PATTERN.matcher(text);
        if (namedMatcher.find()) {
            String courseName = namedMatcher.group(1).trim();
            Long matchedId = findCourseIdByName(courseName, courseList);
            if (matchedId != null) {
                return matchedId;
            }
        }

        for (Course course : courseList) {
            if (course.getCourseName() != null && text.contains(course.getCourseName())) {
                return course.getId();
            }
        }
        return null;
    }

    private Long findCourseIdByName(String courseName, List<Course> courseList) {
        if (courseName == null || courseName.isBlank()) {
            return null;
        }
        String normalized = courseName.trim();
        for (Course course : courseList) {
            if (course.getCourseName() != null && course.getCourseName().equalsIgnoreCase(normalized)) {
                return course.getId();
            }
        }
        for (Course course : courseList) {
            if (course.getCourseName() != null && course.getCourseName().contains(normalized)) {
                return course.getId();
            }
        }
        return null;
    }

    private String resolveCourseName(Long courseId, List<Course> courseList) {
        if (courseId == null || courseList == null) {
            return "";
        }
        for (Course course : courseList) {
            if (courseId.equals(course.getId())) {
                return course.getCourseName();
            }
        }
        return "";
    }

    private String extractDeadline(String text) {
        String absoluteDate = extractDate(text);
        if (absoluteDate != null) {
            return absoluteDate;
        }

        if (text == null || text.isBlank()) {
            return null;
        }

        Matcher matcher = DAYS_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }

        int days = Integer.parseInt(matcher.group(1));
        LocalDateTime deadline = LocalDateTime.now().plusDays(days).withHour(23).withMinute(59).withSecond(59);
        return deadline.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private List<String> splitIntoChunks(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            chunks.add("暂时没有可返回的内容。");
            return chunks;
        }

        int index = 0;
        while (index < text.length()) {
            int next = Math.min(index + 24, text.length());
            chunks.add(text.substring(index, next));
            index = next;
        }
        return chunks;
    }
}
