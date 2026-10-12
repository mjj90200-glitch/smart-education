package com.smartedu.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartedu.common.result.Result;
import com.smartedu.entity.Question;
import com.smartedu.mapper.QuestionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 题目控制器
 * @author SmartEdu Team
 */
@Tag(name = "题目管理", description = "题库查询、管理相关接口")
@RestController
@RequestMapping("/question")
public class QuestionController {

    private final QuestionMapper questionMapper;

    public QuestionController(QuestionMapper questionMapper) {
        this.questionMapper = questionMapper;
    }

    @GetMapping("/list")
    @Operation(summary = "获取题目列表", description = "分页查询题库，支持按课程、类型、难度筛选")
    public Result<Object> getQuestionList(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String questionType,
            @RequestParam(required = false) Integer difficultyLevel,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Page<Question> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Question::getStatus, 1); // 已发布状态
        queryWrapper.eq(courseId != null, Question::getCourseId, courseId);
        queryWrapper.eq(questionType != null && !questionType.isEmpty(), Question::getQuestionType, questionType);
        queryWrapper.eq(difficultyLevel != null, Question::getDifficultyLevel, difficultyLevel);
        queryWrapper.orderByDesc(Question::getCreatedAt);

        Page<Question> questionPage = questionMapper.selectPage(page, queryWrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("list", questionPage.getRecords());
        result.put("total", questionPage.getTotal());

        return Result.success("获取成功", result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取题目详情", description = "获取单个题目的详细信息")
    public Result<Question> getQuestionDetail(@PathVariable Long id) {
        Question question = questionMapper.selectById(id);
        if (question == null) {
            return Result.error("题目不存在");
        }
        return Result.success("获取成功", question);
    }

    @GetMapping("/by-ids")
    @Operation(summary = "批量获取题目", description = "根据 ID 列表批量获取题目信息")
    public Result<List<Question>> getQuestionsByIds(@RequestParam String ids) {
        if (ids == null || ids.trim().isEmpty()) {
            return Result.success("获取成功", List.of());
        }

        List<Long> idList = List.of(ids.split(","))
                .stream()
                .map(String::trim)
                .map(Long::parseLong)
                .toList();

        List<Question> questions = questionMapper.selectBatchIds(idList);
        return Result.success("获取成功", questions);
    }
}