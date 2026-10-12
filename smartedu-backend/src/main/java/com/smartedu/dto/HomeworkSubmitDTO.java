package com.smartedu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 作业提交 DTO
 * @author SmartEdu Team
 */
@Data
@Schema(description = "作业提交请求")
public class HomeworkSubmitDTO {

    @Schema(description = "作业 ID", example = "1")
    @NotNull(message = "作业 ID 不能为空")
    private Long homeworkId;

    @Schema(description = "答案列表")
    @NotNull(message = "答案不能为空")
    private List<AnswerItem> answers;

    /**
     * 单个题目答案
     */
    @Data
    @Schema(description = "单个题目答案")
    public static class AnswerItem {
        @Schema(description = "题目 ID", example = "1")
        @NotNull(message = "题目 ID 不能为空")
        private Long questionId;

        @Schema(description = "学生答案")
        private String answer;
    }
}