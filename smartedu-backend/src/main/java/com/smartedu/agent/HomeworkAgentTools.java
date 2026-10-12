package com.smartedu.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartedu.entity.Course;
import com.smartedu.entity.Homework;
import com.smartedu.entity.HomeworkSubmission;
import com.smartedu.mapper.CourseMapper;
import com.smartedu.mapper.HomeworkMapper;
import com.smartedu.mapper.HomeworkSubmissionMapper;
import com.smartedu.service.QuickHomeworkService;
import com.smartedu.service.StudentHomeworkService;
import com.smartedu.vo.StudentHomeworkVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 作业 Agent 工具箱
 * 提供 AI 可调用的工具方法，用于学生提交作业和教师发布作业
 *
 * @author SmartEdu Team
 */
@Component
public class HomeworkAgentTools {

    private static final Logger logger = LoggerFactory.getLogger(HomeworkAgentTools.class);

    private final StudentHomeworkService studentHomeworkService;
    private final QuickHomeworkService quickHomeworkService;
    private final HomeworkMapper homeworkMapper;
    private final HomeworkSubmissionMapper submissionMapper;
    private final CourseMapper courseMapper;

    public HomeworkAgentTools(StudentHomeworkService studentHomeworkService,
                              QuickHomeworkService quickHomeworkService,
                              HomeworkMapper homeworkMapper,
                              HomeworkSubmissionMapper submissionMapper,
                              CourseMapper courseMapper) {
        this.studentHomeworkService = studentHomeworkService;
        this.quickHomeworkService = quickHomeworkService;
        this.homeworkMapper = homeworkMapper;
        this.submissionMapper = submissionMapper;
        this.courseMapper = courseMapper;
    }

    /**
     * 学生提交作业的实际执行方法
     * 注意：此方法设计为从 AI Function Calling 直接调用，不依赖 SecurityContext
     * 用户信息需要从外部显式传递
     */
    public Map<String, Object> doSubmitHomework(SubmitHomeworkRequest request) {
        logger.info("AI Agent 调用 submitHomework: homeworkId={}, fileUrl={}, userId={}",
            request.homeworkId, request.fileUrl, request.userId);

        Map<String, Object> result = new HashMap<>();
        try {
            // 使用显式传递的用户 ID
            Long studentId = request.userId;
            if (studentId == null) {
                result.put("success", false);
                result.put("error", "未登录或无法获取用户信息");
                return result;
            }

            // 验证用户角色（确保是学生）
            if (!"STUDENT".equals(request.userRole)) {
                result.put("success", false);
                result.put("error", "只有学生角色才能提交作业");
                return result;
            }

            // 调用服务层提交作业
            studentHomeworkService.submitHomeworkByUrl(request.homeworkId, studentId, request.fileUrl, request.content);

            result.put("success", true);
            result.put("message", "作业提交成功");
            result.put("homeworkId", request.homeworkId);
            result.put("submissionTime", LocalDateTime.now().toString());

            return result;
        } catch (Exception e) {
            logger.error("提交作业失败：homeworkId={}, error={}", request.homeworkId, e.getMessage());
            result.put("success", false);
            result.put("error", "提交失败：" + e.getMessage());
            return result;
        }
    }

    /**
     * 教师发布作业的实际执行方法
     * 注意：此方法设计为从 AI Function Calling 直接调用，不依赖 SecurityContext
     * 用户信息需要从外部显式传递
     */
    public Map<String, Object> doPublishHomework(PublishHomeworkRequest request) {
        logger.info("AI Agent 调用 publishHomework: courseId={}, title={}, fileUrl={}, userId={}",
            request.courseId, request.title, request.fileUrl, request.userId);

        Map<String, Object> result = new HashMap<>();
        try {
            // 使用显式传递的用户 ID
            Long teacherId = request.userId;
            if (teacherId == null) {
                result.put("success", false);
                result.put("error", "未登录或无法获取用户信息");
                return result;
            }

            // 验证用户角色（确保是教师）
            if (!"TEACHER".equals(request.userRole)) {
                result.put("success", false);
                result.put("error", "只有教师角色才能发布作业");
                return result;
            }

            MultipartFile docFile = new UrlBackedMultipartFile(request.fileUrl);
            Map<String, Object> publishResult = quickHomeworkService.quickPublish(
                docFile,
                request.title,
                request.courseId,
                request.description != null ? request.description : "由智学助手发布的作业",
                null,
                request.endTime,
                teacherId
            );

            result.put("success", true);
            result.put("message", "作业发布成功");
            result.put("homeworkId", publishResult.get("id"));
            result.put("title", publishResult.getOrDefault("title", request.title));

            return result;
        } catch (Exception e) {
            logger.error("发布作业失败：courseId={}, title={}, error={}", request.courseId, request.title, e.getMessage());
            result.put("success", false);
            result.put("error", "发布失败：" + e.getMessage());
            return result;
        }
    }

    /**
     * 学生提交作业请求 DTO
     */
    public static class SubmitHomeworkRequest {
        public Long homeworkId;
        public String fileUrl;
        public String content;
        // 显式传递的用户信息（从 Controller 层传递，避免依赖 SecurityContext）
        public Long userId;
        public String userRole;
    }

    /**
     * 教师发布作业请求 DTO
     */
    public static class PublishHomeworkRequest {
        public Long courseId;
        public String title;
        public String fileUrl;
        public String description;
        public String endTime;
        // 显式传递的用户信息（从 Controller 层传递，避免依赖 SecurityContext）
        public Long userId;
        public String userRole;
    }

    /**
     * 内部类：基于 URL 的 MultipartFile 实现
     * 用于将已上传的文件 URL 转换回 MultipartFile 供服务层使用
     */
    private static class UrlBackedMultipartFile implements MultipartFile {
        private final String fileUrl;

        UrlBackedMultipartFile(String fileUrl) {
            this.fileUrl = fileUrl;
        }

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            // 从 URL 提取文件名
            if (fileUrl != null && fileUrl.contains("/")) {
                return fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            }
            return "uploaded_file";
        }

        @Override
        public String getContentType() {
            if (fileUrl != null) {
                if (fileUrl.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                if (fileUrl.endsWith(".doc")) return "application/msword";
                if (fileUrl.endsWith(".pdf")) return "application/pdf";
            }
            return "application/octet-stream";
        }

        @Override
        public boolean isEmpty() {
            return getSize() <= 0;
        }

        @Override
        public long getSize() {
            return resolveFile().length();
        }

        @Override
        public byte[] getBytes() throws IOException {
            return java.nio.file.Files.readAllBytes(resolveFile().toPath());
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException {
            java.nio.file.Files.copy(resolveFile().toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new FileInputStream(resolveFile());
        }

        private File resolveFile() {
            String normalizedPath = fileUrl.startsWith("/") ? fileUrl : "/" + fileUrl;
            File file = new File(System.getProperty("user.dir") + normalizedPath);
            if (!file.exists()) {
                throw new RuntimeException("找不到已上传的文件：" + fileUrl);
            }
            return file;
        }
    }

    public List<StudentHomeworkVO> getPendingHomework(Long studentId) {
        List<StudentHomeworkVO> pendingList = new ArrayList<>();
        if (studentId == null) {
            return pendingList;
        }

        List<Homework> homeworkList = homeworkMapper.selectList(new LambdaQueryWrapper<Homework>()
            .eq(Homework::getStatus, 1)
            .orderByAsc(Homework::getEndTime)
            .orderByDesc(Homework::getCreatedAt));

        LocalDateTime now = LocalDateTime.now();
        for (Homework homework : homeworkList) {
            if (homework.getStartTime() != null && now.isBefore(homework.getStartTime())) {
                continue;
            }

            HomeworkSubmission submission = submissionMapper.selectOne(new LambdaQueryWrapper<HomeworkSubmission>()
                .eq(HomeworkSubmission::getHomeworkId, homework.getId())
                .eq(HomeworkSubmission::getUserId, studentId)
                .orderByDesc(HomeworkSubmission::getSubmitTime)
                .last("LIMIT 1"));

            if (submission != null) {
                continue;
            }

            StudentHomeworkVO vo = new StudentHomeworkVO();
            vo.setId(homework.getId());
            vo.setTitle(homework.getTitle());
            vo.setDescription(homework.getDescription());
            vo.setCourseId(homework.getCourseId());
            vo.setStartTime(homework.getStartTime());
            vo.setEndTime(homework.getEndTime());
            vo.setAttachmentUrl(homework.getAttachmentUrl());
            vo.setAttachmentName(homework.getAttachmentName());
            vo.setStatus(homework.getEndTime() != null && now.isAfter(homework.getEndTime()) ? 2 : 1);
            vo.setSubmitStatus(0);

            Course course = courseMapper.selectById(homework.getCourseId());
            if (course != null) {
                vo.setCourseName(course.getCourseName());
            }
            pendingList.add(vo);
        }

        pendingList.sort(Comparator
            .comparing((StudentHomeworkVO item) -> item.getStatus() != null && item.getStatus() == 2 ? 1 : 0)
            .thenComparing(StudentHomeworkVO::getEndTime, Comparator.nullsLast(Comparator.naturalOrder())));
        return pendingList;
    }

    public List<Course> getTeacherCourses(Long teacherId) {
        if (teacherId == null) {
            return List.of();
        }
        return courseMapper.selectList(new LambdaQueryWrapper<Course>()
            .eq(Course::getTeacherId, teacherId)
            .eq(Course::getStatus, 1)
            .orderByAsc(Course::getCourseName));
    }

    public String formatPendingHomework(List<StudentHomeworkVO> pendingList) {
        if (pendingList == null || pendingList.isEmpty()) {
            return "当前没有待提交的作业。";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        StringBuilder builder = new StringBuilder("我查到了你当前还没有提交的作业：\n");
        for (StudentHomeworkVO item : pendingList) {
            builder.append("- 作业ID ")
                .append(item.getId())
                .append("｜")
                .append(item.getTitle());
            if (item.getCourseName() != null) {
                builder.append("｜课程：").append(item.getCourseName());
            }
            if (item.getEndTime() != null) {
                builder.append("｜截止：").append(item.getEndTime().format(formatter));
            }
            if (item.getStatus() != null && item.getStatus() == 2) {
                builder.append("｜已截止");
            }
            builder.append('\n');
        }
        return builder.toString().trim();
    }

    public String formatTeacherCourses(List<Course> courseList) {
        if (courseList == null || courseList.isEmpty()) {
            return "当前账号下还没有可用课程。";
        }

        StringBuilder builder = new StringBuilder("你当前可以发布作业的课程有：\n");
        for (Course course : courseList) {
            builder.append("- 课程ID ")
                .append(course.getId())
                .append("｜")
                .append(course.getCourseName());
            if (course.getSemester() != null && !course.getSemester().isBlank()) {
                builder.append("｜学期：").append(course.getSemester());
            }
            builder.append('\n');
        }
        return builder.toString().trim();
    }
}
