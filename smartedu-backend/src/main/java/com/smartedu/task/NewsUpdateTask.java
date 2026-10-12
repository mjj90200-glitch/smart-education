package com.smartedu.task;

import com.smartedu.service.NewsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 新闻自动更新定时任务
 * <p>
 * 功能说明：
 * 1. 每天凌晨 2 点自动从外部 API 抓取科技新闻
 * 2. 支持手动触发更新（用于测试）
 * 3. 实现多源异构数据融合的初步需求
 * </p>
 *
 * Cron 表达式说明：0 0 2 * * ?
 * - 秒：0
 * - 分：0
 * - 时：2（凌晨 2 点）
 * - 日：*（每天）
 * - 月：*（每月）
 * - 周：?（不指定）
 *
 * @author SmartEdu Team
 */
@Component
public class NewsUpdateTask {

    private static final Logger log = LoggerFactory.getLogger(NewsUpdateTask.class);

    @Autowired
    private NewsService newsService;

    /**
     * 定时任务：每天凌晨 2 点自动抓取科技新闻
     * <p>
     * 执行流程：
     * 1. 调用外部新闻 API 获取最新科技资讯
     * 2. 解析 JSON 数据并去重
     * 3. 将新新闻存入数据库
     * </p>
     */
    @Scheduled(cron = "${news.schedule.cron:0 0 2 * * ?}")
    public void autoFetchNews() {
        log.info("========== 开始执行新闻自动更新任务 ==========");
        long startTime = System.currentTimeMillis();

        try {
            // 调用服务层方法抓取并保存新闻
            int savedCount = newsService.fetchAndSaveTechNews();

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("========== 新闻自动更新完成：新增 {} 条新闻，耗时 {} ms ==========", savedCount, elapsed);

        } catch (Exception e) {
            log.error("========== 新闻自动更新失败 ==========", e);
        }
    }
}