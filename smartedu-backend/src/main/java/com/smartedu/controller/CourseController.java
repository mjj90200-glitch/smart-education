package com.smartedu.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartedu.common.result.Result;
import com.smartedu.entity.Course;
import com.smartedu.entity.Homework;
import com.smartedu.mapper.CourseMapper;
import com.smartedu.mapper.HomeworkMapper;
import com.smartedu.security.JwtAuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

/**
 * 课程控制器
 * @author SmartEdu Team
 */
@Tag(name = "课程管理", description = "课程查询、管理相关接口")
@RestController
@RequestMapping("/course")
public class CourseController {

    private final CourseMapper courseMapper;
    private final HomeworkMapper homeworkMapper;

    public CourseController(CourseMapper courseMapper, HomeworkMapper homeworkMapper) {
        this.courseMapper = courseMapper;
        this.homeworkMapper = homeworkMapper;
    }

    @GetMapping("/list")
    @Operation(summary = "获取课程列表", description = "查询所有启用的课程（所有教师可见）")
    public Result<List<Course>> getCourseList() {
        LambdaQueryWrapper<Course> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Course::getStatus, 1);
        queryWrapper.orderByDesc(Course::getCreatedAt);

        List<Course> courses = courseMapper.selectList(queryWrapper);
        return Result.success("获取成功", courses);
    }

    @GetMapping("/teacher")
    @Operation(summary = "获取教师的课程", description = "获取当前登录教师所教授的课程")
    public Result<List<Course>> getTeacherCourses(
            @AuthenticationPrincipal UserDetails userDetails) {

        Long teacherId = getUserId(userDetails);

        LambdaQueryWrapper<Course> ownedCourseWrapper = new LambdaQueryWrapper<>();
        ownedCourseWrapper.eq(Course::getTeacherId, teacherId);
        ownedCourseWrapper.eq(Course::getStatus, 1);
        ownedCourseWrapper.orderByDesc(Course::getCreatedAt);
        List<Course> ownedCourses = courseMapper.selectList(ownedCourseWrapper);

        LambdaQueryWrapper<Homework> homeworkWrapper = new LambdaQueryWrapper<>();
        homeworkWrapper.eq(Homework::getTeacherId, teacherId);
        homeworkWrapper.select(Homework::getCourseId);
        List<Homework> teacherHomeworks = homeworkMapper.selectList(homeworkWrapper);

        java.util.LinkedHashMap<Long, Course> mergedCourses = new java.util.LinkedHashMap<>();
        for (Course course : ownedCourses) {
            mergedCourses.put(course.getId(), course);
        }

        for (Homework homework : teacherHomeworks) {
            if (homework.getCourseId() == null || mergedCourses.containsKey(homework.getCourseId())) {
                continue;
            }
            Course course = courseMapper.selectById(homework.getCourseId());
            if (course != null && course.getStatus() != null && course.getStatus() == 1) {
                mergedCourses.put(course.getId(), course);
            }
        }

        List<Course> courses = new java.util.ArrayList<>(mergedCourses.values());
        return Result.success("获取成功", courses);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取课程详情")
    public Result<Course> getCourseDetail(@PathVariable Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            return Result.error("课程不存在");
        }
        return Result.success("获取成功", course);
    }

    @PostMapping("/create")
    @Operation(summary = "创建课程", description = "教师创建新课程")
    public Result<Long> createCourse(
            @RequestBody CourseCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long teacherId = getUserId(userDetails);

        // 检查课程代码是否已存在
        LambdaQueryWrapper<Course> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Course::getCourseCode, request.getCourseCode());
        if (courseMapper.selectCount(queryWrapper) > 0) {
            return Result.error("课程代码已存在");
        }

        Course course = new Course();
        course.setCourseName(request.getCourseName());
        course.setCourseCode(request.getCourseCode());
        course.setDescription(request.getDescription());
        course.setCredit(request.getCredit() != null ? request.getCredit() : new java.math.BigDecimal("3.0"));
        course.setTeacherId(teacherId);
        course.setSemester(request.getSemester());
        course.setGrade(request.getGrade());
        course.setMajor(request.getMajor());
        course.setStatus(1);

        courseMapper.insert(course);
        return Result.success("课程创建成功", course.getId());
    }

    @PostMapping("/teacher/assign")
    @Transactional
    @Operation(summary = "绑定教师课程", description = "教师勾选自己教授的课程")
    public Result<Void> assignTeacherCourses(
            @RequestBody TeacherCourseAssignRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long teacherId = getUserId(userDetails);
        List<Long> selectedCourseIds = request.getCourseIds() == null ? java.util.Collections.emptyList() : request.getCourseIds();

        List<Course> currentTeacherCourses = courseMapper.selectList(new LambdaQueryWrapper<Course>()
            .eq(Course::getTeacherId, teacherId));

        for (Course course : currentTeacherCourses) {
            if (!selectedCourseIds.contains(course.getId())) {
                course.setTeacherId(null);
                course.setUpdatedAt(LocalDateTime.now());
                courseMapper.updateById(course);
            }
        }

        if (!selectedCourseIds.isEmpty()) {
            List<Course> selectedCourses = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                .in(Course::getId, selectedCourseIds));

            for (Course course : selectedCourses) {
                course.setTeacherId(teacherId);
                course.setUpdatedAt(LocalDateTime.now());
                courseMapper.updateById(course);
            }
        }

        return Result.success("课程绑定成功");
    }

    private Long getUserId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new RuntimeException("未登录");
        }
        org.springframework.security.core.Authentication authentication =
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken) {
            return ((JwtAuthenticationToken) authentication).getUserId();
        }
        throw new RuntimeException("用户认证信息异常");
    }

    /**
     * 课程创建请求 DTO
     */
    @lombok.Data
    public static class CourseCreateRequest {
        private String courseName;
        private String courseCode;
        private String description;
        private java.math.BigDecimal credit;
        private String semester;
        private String grade;
        private String major;
    }

    @lombok.Data
    public static class TeacherCourseAssignRequest {
        private List<Long> courseIds;
    }
}
