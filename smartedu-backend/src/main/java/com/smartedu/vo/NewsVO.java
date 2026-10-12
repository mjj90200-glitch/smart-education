package com.smartedu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 新闻视图对象
 * @author SmartEdu Team
 */
@Data
@Schema(description = "新闻 VO")
public class NewsVO {

    @Schema(description = "新闻 ID")
    private Long id;

    @Schema(description = "新闻标题")
    private String title;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "封面图片 URL")
    private String imageUrl;

    @Schema(description = "原文链接")
    private String sourceUrl;

    @Schema(description = "来源名称")
    private String sourceName;

    @Schema(description = "新闻类型：1=轮播图 2=列表新闻")
    private Integer newsType;

    @Schema(description = "是否置顶：0=否 1=是")
    private Integer isTop;

    @Schema(description = "是否手动添加：0=自动抓取 1=手动")
    private Integer isManual;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
