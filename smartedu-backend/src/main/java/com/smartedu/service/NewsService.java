package com.smartedu.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartedu.entity.News;

import java.util.List;

/**
 * 新闻服务接口
 * @author SmartEdu Team
 */
public interface NewsService extends IService<News> {

    /**
     * 获取轮播图新闻
     */
    List<News> getCarouselNews();

    /**
     * 获取列表新闻
     * @param limit 数量限制
     */
    List<News> getListNews(Integer limit);

    /**
     * 从外部 API 抓取并保存科技新闻
     * <p>
     * 功能说明：
     * 1. 调用外部新闻 API（如 NewsAPI、36氪等）
     * 2. 解析返回的 JSON 数据
     * 3. 根据标题或来源 URL 进行去重判断
     * 4. 将新新闻存入数据库
     * </p>
     *
     * @return 新增的新闻数量
     */
    int fetchAndSaveTechNews();
}
