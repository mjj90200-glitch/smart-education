package com.smartedu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartedu.entity.Course;
import com.smartedu.entity.Homework;
import com.smartedu.entity.HomeworkSubmission;
import com.smartedu.mapper.CourseMapper;
import com.smartedu.mapper.HomeworkMapper;
import com.smartedu.mapper.HomeworkSubmissionMapper;
import com.smartedu.mapper.UserMapper;
import com.smartedu.vo.StudentHomeworkVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 快速作业服务
 * 简化的作业流程：上传文档 → 直接发布
 * @author SmartEdu Team
 */
@Service
public class QuickHomeworkService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(QuickHomeworkService.class);

    private final HomeworkMapper homeworkMapper;
    private final HomeworkSubmissionMapper submissionMapper;
    private final CourseMapper courseMapper;
    private final UserMapper userMapper;
    private final HomeworkAnalysisService homeworkAnalysisService;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    public QuickHomeworkService(HomeworkMapper homeworkMapper,
                                HomeworkSubmissionMapper submissionMapper,
                                CourseMapper courseMapper,
                                UserMapper userMapper,
                                HomeworkAnalysisService homeworkAnalysisService) {
        this.homeworkMapper = homeworkMapper;
        this.submissionMapper = submissionMapper;
        this.courseMapper = courseMapper;
        this.userMapper = userMapper;
        this.homeworkAnalysisService = homeworkAnalysisService;
    }

    /**
     * 快速发布作业（一步完成：上传文档 + 创建 + 发布）
     */
    @Transactional
    public Map<String, Object> quickPublish(MultipartFile file, String title, Long courseId,
                                             String description, String startTime, String endTime,
                                             Long teacherId) throws IOException {
        // 保存文件
        String attachmentUrl = saveFile(file, "homework");
        String attachmentName = file.getOriginalFilename();

        // 创建作业
        Homework homework = new Homework();
        homework.setTitle(title);
        homework.setDescription(description != null ? description : "");
        homework.setCourseId(courseId);
        homework.setTeacherId(teacherId);
        homework.setQuestionIds(""); // 快速作业模式不需要题目ID
        homework.setAttachmentUrl(attachmentUrl);
        homework.setAttachmentName(attachmentName);
        homework.setTotalScore(new BigDecimal("100"));
        homework.setPassScore(new BigDecimal("60"));

        // 设置时间
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (startTime != null && !startTime.isEmpty()) {
            homework.setStartTime(LocalDateTime.parse(startTime, formatter));
        } else {
            homework.setStartTime(LocalDateTime.now());
        }
        if (endTime != null && !endTime.isEmpty()) {
            homework.setEndTime(LocalDateTime.parse(endTime, formatter));
        } else {
            homework.setEndTime(LocalDateTime.now().plusDays(7));
        }

        homework.setSubmitLimit(0);
        homework.setTimeLimitMinutes(0);
        homework.setAutoGrade(0);
        homework.setStatus(1); // 直接发布

        homeworkMapper.insert(homework);

        // ========== 触发 AI 解析生成 ==========
        Long homeworkId = homework.getId();
        logger.info("======== 作业发布成功，开始触发 AI 解析 ========");
        logger.info("作业ID: {}, 标题: {}", homeworkId, homework.getTitle());
        logger.info("附件URL: {}", homework.getAttachmentUrl());

        try {
            homeworkAnalysisService.generateAnalysisAsync(homeworkId);
            logger.info("======== AI 解析异步任务已触发 ========");
        } catch (Exception e) {
            // AI 解析失败不影响作业发布，仅记录日志
            logger.error("======== 触发 AI 解析失败：{} ========", e.getMessage(), e);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", homework.getId());
        result.put("title", homework.getTitle());
        result.put("attachmentUrl", homework.getAttachmentUrl());
        result.put("attachmentName", homework.getAttachmentName());
        result.put("message", "作业发布成功");

        return result;
    }

    /**
     * 学生提交作业（支持上传文档）
     */
    @Transactional
    public Map<String, Object> submitWithFile(Long homeworkId, MultipartFile file, String answers, Long studentId) throws IOException {
        Homework homework = homeworkMapper.selectById(homeworkId);
        if (homework == null) {
            throw new RuntimeException("作业不存在");
        }

        // 检查是否已截止
        if (homework.getEndTime() != null && LocalDateTime.now().isAfter(homework.getEndTime())) {
            // 允许迟交但标记
        }

        // 检查提交次数
        LambdaQueryWrapper<HomeworkSubmission> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(HomeworkSubmission::getHomeworkId, homeworkId)
                    .eq(HomeworkSubmission::getUserId, studentId);
        Long submitCount = submissionMapper.selectCount(countWrapper);

        if (homework.getSubmitLimit() != null && homework.getSubmitLimit() > 0 && submitCount >= homework.getSubmitLimit()) {
            throw new RuntimeException("已达到最大提交次数");
        }

        // 保存文件
        String attachmentUrl = null;
        String attachmentName = null;
        if (file != null && !file.isEmpty()) {
            attachmentUrl = saveFile(file, "submission");
            attachmentName = file.getOriginalFilename();
        }

        // 判断是否迟交
        int isLate = 0;
        if (homework.getEndTime() != null && LocalDateTime.now().isAfter(homework.getEndTime())) {
            isLate = 1;
        }

        // 创建提交记录
        HomeworkSubmission submission = new HomeworkSubmission();
        submission.setHomeworkId(homeworkId);
        submission.setUserId(studentId);
        submission.setSubmissionContent(answers);
        submission.setSubmissionType(answers != null ? 2 : 1);
        submission.setAttachmentUrl(attachmentUrl);
        submission.setAttachmentName(attachmentName);
        submission.setGradeStatus(1);
        submission.setSubmitTime(LocalDateTime.now());
        submission.setIsLate(isLate);

        submissionMapper.insert(submission);

        Map<String, Object> result = new HashMap<>();
        result.put("id", submission.getId());
        result.put("message", "提交成功");
        return result;
    }

    /**
     * 获取学生作业列表
     */
    public Map<String, Object> getStudentHomeworkList(Long studentId, Long courseId, Integer status, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Homework> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Homework::getStatus, 1);
        if (courseId != null) {
            wrapper.eq(Homework::getCourseId, courseId);
        }
        wrapper.orderByDesc(Homework::getCreatedAt);

        List<Homework> allHomework = homeworkMapper.selectList(wrapper);

        List<Map<String, Object>> list = new ArrayList<>();
        for (Homework h : allHomework) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", h.getId());
            item.put("title", h.getTitle());
            item.put("description", h.getDescription());
            item.put("courseId", h.getCourseId());
            item.put("totalScore", h.getTotalScore());
            item.put("startTime", h.getStartTime());
            item.put("endTime", h.getEndTime());
            item.put("attachmentUrl", h.getAttachmentUrl());
            item.put("attachmentName", h.getAttachmentName());
            item.put("content", h.getContent());

            // 获取课程信息
            Course course = courseMapper.selectById(h.getCourseId());
            if (course != null) {
                item.put("courseName", course.getCourseName());
            }

            // 计算状态
            int hwStatus = 1;
            if (h.getStartTime() != null && LocalDateTime.now().isBefore(h.getStartTime())) {
                hwStatus = 0;
            } else if (h.getEndTime() != null && LocalDateTime.now().isAfter(h.getEndTime())) {
                hwStatus = 2;
            }
            item.put("status", hwStatus);

            // 查询提交状态
            LambdaQueryWrapper<HomeworkSubmission> subWrapper = new LambdaQueryWrapper<>();
            subWrapper.eq(HomeworkSubmission::getHomeworkId, h.getId())
                     .eq(HomeworkSubmission::getUserId, studentId)
                     .orderByDesc(HomeworkSubmission::getSubmitTime)
                     .last("LIMIT 1");
            HomeworkSubmission submission = submissionMapper.selectOne(subWrapper);

            if (submission == null) {
                item.put("submitStatus", 0);
                item.put("score", null);
            } else {
                item.put("submitStatus", submission.getGradeStatus() + 1);
                item.put("score", submission.getScore());
                item.put("submissionId", submission.getId());
            }

            // 状态过滤
            if (status != null && status != hwStatus) {
                continue;
            }

            list.add(item);
        }

        // 简单分页
        int total = list.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<Map<String, Object>> pagedList = fromIndex < total ? list.subList(fromIndex, toIndex) : new ArrayList<>();

        Map<String, Object> result = new HashMap<>();
        result.put("list", pagedList);
        result.put("total", total);
        return result;
    }

    /**
     * 获取作业详情
     */
    public Map<String, Object> getHomeworkDetail(Long homeworkId, Long studentId) {
        Homework h = homeworkMapper.selectById(homeworkId);
        if (h == null) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", h.getId());
        result.put("title", h.getTitle());
        result.put("description", h.getDescription());
        result.put("courseId", h.getCourseId());
        result.put("totalScore", h.getTotalScore());
        result.put("passScore", h.getPassScore());
        result.put("startTime", h.getStartTime());
        result.put("endTime", h.getEndTime());
        result.put("attachmentUrl", h.getAttachmentUrl());
        result.put("attachmentName", h.getAttachmentName());
        result.put("content", h.getContent());
        result.put("submitLimit", h.getSubmitLimit());

        // 获取课程信息
        Course course = courseMapper.selectById(h.getCourseId());
        if (course != null) {
            result.put("courseName", course.getCourseName());
            result.put("teacherId", course.getTeacherId());

            // 获取教师姓名
            var teacher = userMapper.selectById(course.getTeacherId());
            if (teacher != null) {
                result.put("teacherName", teacher.getRealName());
            }
        }

        // 查询学生提交
        if (studentId != null) {
            LambdaQueryWrapper<HomeworkSubmission> subWrapper = new LambdaQueryWrapper<>();
            subWrapper.eq(HomeworkSubmission::getHomeworkId, homeworkId)
                     .eq(HomeworkSubmission::getUserId, studentId)
                     .orderByDesc(HomeworkSubmission::getSubmitTime)
                     .last("LIMIT 1");
            HomeworkSubmission submission = submissionMapper.selectOne(subWrapper);

            if (submission != null) {
                result.put("mySubmission", submission);
            }
        }

        return result;
    }

    /**
     * 保存文件
     */
    private String saveFile(MultipartFile file, String type) throws IOException {
        // 获取项目根目录的绝对路径
        String projectDir = System.getProperty("user.dir");

        // 创建上传目录
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path dirPath = Paths.get(projectDir, uploadDir, type, datePath);

        // 确保目录存在
        Files.createDirectories(dirPath);

        // 生成文件名
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
            ? originalFilename.substring(originalFilename.lastIndexOf("."))
            : "";
        String newFilename = UUID.randomUUID().toString() + extension;

        // 保存文件
        Path filePath = dirPath.resolve(newFilename);
        file.transferTo(filePath.toAbsolutePath().toFile());

        // 返回相对路径（用于URL访问）
        return "/uploads/" + type + "/" + datePath + "/" + newFilename;
    }
}