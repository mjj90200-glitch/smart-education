package com.smartedu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 新闻实体类
 * @author SmartEdu Team
 */
@Data
@TableName("news")
public class News {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 新闻标题
     */
    private String title;

    /**
     * 摘要
     */
    private String summary;

    /**
     * 详细内容
     */
    private String content;

    /**
     * 封面图片
     */
    private String imageUrl;

    /**
     * 原文链接
     */
    private String sourceUrl;

    /**
     * 来源名称
     */
    private String sourceName;

    /**
     * 新闻类型：1=轮播图 2=列表新闻
     */
    private Integer newsType;

    /**
     * 是否置顶：0=否 1=是
     */
    private Integer isTop;

    /**
     * 是否手动添加：0=自动抓取 1=手动
     */
    private Integer isManual;

    /**
     * 排序序号（轮播图用）
     */
    private Integer orderIndex;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
