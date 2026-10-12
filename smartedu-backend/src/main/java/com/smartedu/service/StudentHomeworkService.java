package com.smartedu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartedu.entity.Course;
import com.smartedu.entity.Homework;
import com.smartedu.entity.HomeworkSubmission;
import com.smartedu.entity.User;
import com.smartedu.mapper.CourseMapper;
import com.smartedu.mapper.HomeworkMapper;
import com.smartedu.mapper.HomeworkSubmissionMapper;
import com.smartedu.mapper.UserMapper;
import com.smartedu.vo.HomeworkDetailVO;
import com.smartedu.vo.StudentHomeworkVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 学生作业服务 - 简化版
 * @author SmartEdu Team
 */
@Service
public class StudentHomeworkService {

    private final HomeworkMapper homeworkMapper;
    private final HomeworkSubmissionMapper submissionMapper;
    private final CourseMapper courseMapper;
    private final UserMapper userMapper;

    public StudentHomeworkService(HomeworkMapper homeworkMapper,
                                  HomeworkSubmissionMapper submissionMapper,
                                  CourseMapper courseMapper,
                                  UserMapper userMapper) {
        this.homeworkMapper = homeworkMapper;
        this.submissionMapper = submissionMapper;
        this.courseMapper = courseMapper;
        this.userMapper = userMapper;
    }

    /**
     * 获取学生可见的作业列表
     */
    public Page<StudentHomeworkVO> getHomeworkList(Long studentId, Long courseId, Integer status, Integer pageNum, Integer pageSize) {
        Page<Homework> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Homework> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Homework::getStatus, 1);
        if (courseId != null) {
            wrapper.eq(Homework::getCourseId, courseId);
        }
        wrapper.orderByDesc(Homework::getCreatedAt);

        Page<Homework> homeworkPage = homeworkMapper.selectPage(page, wrapper);

        Page<StudentHomeworkVO> resultPage = new Page<>(pageNum, pageSize, homeworkPage.getTotal());
        List<StudentHomeworkVO> voList = new ArrayList<>();

        for (Homework homework : homeworkPage.getRecords()) {
            StudentHomeworkVO vo = convertToVO(homework, studentId);
            if (status == null || status.equals(vo.getStatus())) {
                voList.add(vo);
            }
        }

        resultPage.setRecords(voList);
        return resultPage;
    }

    /**
     * 获取作业详情
     */
    public HomeworkDetailVO getHomeworkDetail(Long homeworkId, Long studentId) {
        try {
            Homework homework = homeworkMapper.selectById(homeworkId);
            if (homework == null || homework.getStatus() != 1) {
                return null;
            }

            HomeworkDetailVO vo = new HomeworkDetailVO();
            vo.setId(homework.getId());
            vo.setTitle(homework.getTitle());
            vo.setDescription(homework.getDescription());
            vo.setCourseId(homework.getCourseId());
            vo.setTotalScore(homework.getTotalScore());
            vo.setPassScore(homework.getPassScore());
            vo.setStartTime(homework.getStartTime());
            vo.setEndTime(homework.getEndTime());
            vo.setAttachmentUrl(homework.getAttachmentUrl());
            vo.setAttachmentName(homework.getAttachmentName());

            Course course = courseMapper.selectById(homework.getCourseId());
            if (course != null) {
                vo.setCourseName(course.getCourseName());
                vo.setTeacherId(course.getTeacherId());
                User teacher = userMapper.selectById(course.getTeacherId());
                if (teacher != null) {
                    vo.setTeacherName(teacher.getRealName());
                }
            }

            LambdaQueryWrapper<HomeworkSubmission> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(HomeworkSubmission::getHomeworkId, homeworkId)
                   .eq(HomeworkSubmission::getUserId, studentId)
                   .orderByDesc(HomeworkSubmission::getSubmitTime)
                   .last("LIMIT 1");
            HomeworkSubmission submission = submissionMapper.selectOne(wrapper);

            if (submission != null) {
                vo.setMySubmission(submission);
            }

            // 解析内容返回控制：只有在学生已提交且解析状态为 4（已发布）时才返回解析
            // 添加 null 检查，避免字段不存在时出错
            try {
                if (submission != null && homework.getAiAnalysisStatus() != null
                    && homework.getAiAnalysisStatus() == 4) { // 状态 4 = 已发布
                    // 优先返回老师修改后的解析，否则返回 AI 生成的原始解析
                    String analysis = homework.getTeacherEditedAnalysis() != null
                        ? homework.getTeacherEditedAnalysis()
                        : homework.getAiAnalysisContent();
                    vo.setAnalysis(analysis);
                }
            } catch (Exception e) {
                // AI 解析字段可能不存在，忽略错误
                org.slf4j.LoggerFactory.getLogger(StudentHomeworkService.class)
                    .warn("获取 AI 解析字段失败，可能数据库未升级：{}", e.getMessage());
            }

            return vo;
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(StudentHomeworkService.class)
                .error("获取作业详情失败：homeworkId={}, studentId={}, error={}", homeworkId, studentId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 提交作业 - 简化版（支持文件或文字）
     */
    @Transactional
    public void submitHomework(Long homeworkId, Long studentId, MultipartFile file, String content) {
        if (file == null && (content == null || content.trim().isEmpty())) {
            throw new RuntimeException("请上传文件或填写文字答案");
        }

        Homework homework = homeworkMapper.selectById(homeworkId);
        if (homework == null) {
            throw new RuntimeException("作业不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        if (homework.getStartTime() != null && now.isBefore(homework.getStartTime())) {
            throw new RuntimeException("作业尚未开始");
        }

        LambdaQueryWrapper<HomeworkSubmission> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(HomeworkSubmission::getHomeworkId, homeworkId)
                     .eq(HomeworkSubmission::getUserId, studentId);
        Long submitCount = submissionMapper.selectCount(countWrapper);

        // submitLimit 为 0 或 null 表示无限制
        if (homework.getSubmitLimit() != null && homework.getSubmitLimit() > 0 && submitCount >= homework.getSubmitLimit()) {
            throw new RuntimeException("已达到最大提交次数");
        }

        int isLate = (homework.getEndTime() != null && now.isAfter(homework.getEndTime())) ? 1 : 0;

        HomeworkSubmission submission = new HomeworkSubmission();
        submission.setHomeworkId(homeworkId);
        submission.setUserId(studentId);
        submission.setGradeStatus(1);
        submission.setSubmitTime(now);
        submission.setIsLate(isLate);

        // 处理文件上传
        if (file != null && !file.isEmpty()) {
            String attachmentUrl = saveFile(file);
            submission.setAttachmentUrl(attachmentUrl);
            submission.setAttachmentName(file.getOriginalFilename());
            submission.setSubmissionType(1);
            submission.setSubmissionContent(attachmentUrl);
        } else {
            submission.setSubmissionType(2);
            submission.setSubmissionContent(content);
        }

        submissionMapper.insert(submission);
    }

    /**
     * 保存上传的文件
     */
    private String saveFile(MultipartFile file) {
        try {
            String uploadDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "submissions";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            file.transferTo(filePath.toFile());

            // 返回 Web 访问路径（以 /uploads/ 开头，用于浏览器访问）
            return "/uploads/submissions/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 通过文件 URL 提交作业（用于 AI Agent 调用）
     * 适用于文件已经上传到服务器，只需要关联到提交记录的场景
     */
    @Transactional
    public void submitHomeworkByUrl(Long homeworkId, Long studentId, String fileUrl, String content) {
        if (fileUrl == null && (content == null || content.trim().isEmpty())) {
            throw new RuntimeException("请上传文件或填写文字答案");
        }

        Homework homework = homeworkMapper.selectById(homeworkId);
        if (homework == null) {
            throw new RuntimeException("作业不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        if (homework.getStartTime() != null && now.isBefore(homework.getStartTime())) {
            throw new RuntimeException("作业尚未开始");
        }

        LambdaQueryWrapper<HomeworkSubmission> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(HomeworkSubmission::getHomeworkId, homeworkId)
                     .eq(HomeworkSubmission::getUserId, studentId);
        Long submitCount = submissionMapper.selectCount(countWrapper);

        // submitLimit 为 0 或 null 表示无限制
        if (homework.getSubmitLimit() != null && homework.getSubmitLimit() > 0 && submitCount >= homework.getSubmitLimit()) {
            throw new RuntimeException("已达到最大提交次数");
        }

        int isLate = (homework.getEndTime() != null && now.isAfter(homework.getEndTime())) ? 1 : 0;

        HomeworkSubmission submission = new HomeworkSubmission();
        submission.setHomeworkId(homeworkId);
        submission.setUserId(studentId);
        submission.setGradeStatus(1);
        submission.setSubmitTime(now);
        submission.setIsLate(isLate);

        // 处理文件 URL
        if (fileUrl != null && !fileUrl.isEmpty()) {
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            submission.setAttachmentUrl(fileUrl);
            submission.setAttachmentName(fileName);
            submission.setSubmissionType(1);
            submission.setSubmissionContent(fileUrl);
        } else {
            submission.setSubmissionType(2);
            submission.setSubmissionContent(content);
        }

        submissionMapper.insert(submission);
    }

    /**
     * 获取学生提交记录
     */
    public HomeworkSubmission getMySubmission(Long homeworkId, Long studentId) {
        LambdaQueryWrapper<HomeworkSubmission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HomeworkSubmission::getHomeworkId, homeworkId)
               .eq(HomeworkSubmission::getUserId, studentId)
               .orderByDesc(HomeworkSubmission::getSubmitTime)
               .last("LIMIT 1");
        return submissionMapper.selectOne(wrapper);
    }

    /**
     * 转换为 VO
     */
    private StudentHomeworkVO convertToVO(Homework homework, Long studentId) {
        StudentHomeworkVO vo = new StudentHomeworkVO();
        vo.setId(homework.getId());
        vo.setTitle(homework.getTitle());
        vo.setDescription(homework.getDescription());
        vo.setCourseId(homework.getCourseId());
        vo.setTotalScore(homework.getTotalScore());
        vo.setStartTime(homework.getStartTime());
        vo.setEndTime(homework.getEndTime());
        vo.setAttachmentUrl(homework.getAttachmentUrl());
        vo.setAttachmentName(homework.getAttachmentName());

        Course course = courseMapper.selectById(homework.getCourseId());
        if (course != null) {
            vo.setCourseName(course.getCourseName());
            User teacher = userMapper.selectById(course.getTeacherId());
            if (teacher != null) {
                vo.setTeacherName(teacher.getRealName());
            }
        }

        LocalDateTime now = LocalDateTime.now();
        int status = 1;
        if (homework.getStartTime() != null && now.isBefore(homework.getStartTime())) {
            status = 0;
        } else if (homework.getEndTime() != null && now.isAfter(homework.getEndTime())) {
            status = 2;
        }
        vo.setStatus(status);

        LambdaQueryWrapper<HomeworkSubmission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HomeworkSubmission::getHomeworkId, homework.getId())
               .eq(HomeworkSubmission::getUserId, studentId);
        Long submitCount = submissionMapper.selectCount(wrapper);
        vo.setSubmittedCount(submitCount.intValue());

        LambdaQueryWrapper<HomeworkSubmission> lastWrapper = new LambdaQueryWrapper<>();
        lastWrapper.eq(HomeworkSubmission::getHomeworkId, homework.getId())
                   .eq(HomeworkSubmission::getUserId, studentId)
                   .orderByDesc(HomeworkSubmission::getSubmitTime)
                   .last("LIMIT 1");
        HomeworkSubmission lastSubmission = submissionMapper.selectOne(lastWrapper);

        if (lastSubmission == null) {
            vo.setSubmitStatus(0);
        } else {
            vo.setSubmitStatus(lastSubmission.getGradeStatus() + 1);
            vo.setScore(lastSubmission.getScore());
            vo.setIsLate(lastSubmission.getIsLate());
        }

        return vo;
    }
}
