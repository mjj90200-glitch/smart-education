package com.smartedu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartedu.entity.News;
import com.smartedu.mapper.NewsMapper;
import com.smartedu.service.NewsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 新闻服务实现
 * <p>
 * 多源异构数据融合实现：
 * 1. 支持从多个外部 API 获取新闻数据
 * 2. 统一数据格式并存入本地数据库
 * 3. 实现去重逻辑，避免重复数据
 * </p>
 *
 * @author SmartEdu Team
 */
@Service
public class NewsServiceImpl extends ServiceImpl<NewsMapper, News> implements NewsService {

    private static final Logger log = LoggerFactory.getLogger(NewsServiceImpl.class);

    @Autowired
    private RestTemplate restTemplate;

    // JSON 解析器
    private final ObjectMapper objectMapper;

    /**
     * IT 之家 RSS 订阅地址
     */
    @Value("${news.api.url:https://www.ithome.com/rss/}")
    private String rssUrl;

    /**
     * 轮播图数量配置
     */
    @Value("${news.api.carousel-count:3}")
    private int carouselCount;

    public NewsServiceImpl() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<News> getCarouselNews() {
        return lambdaQuery()
            .eq(News::getNewsType, 1)
            .orderByAsc(News::getOrderIndex)
            .list();
    }

    @Override
    public List<News> getListNews(Integer limit) {
        return lambdaQuery()
            .eq(News::getNewsType, 2)
            .orderByDesc(News::getIsTop)
            .orderByDesc(News::getPublishTime)
            .last("LIMIT " + limit)
            .list();
    }

    /**
     * 从外部 API 抓取并保存科技新闻
     * <p>
     * 实现步骤：
     * 1. 构建请求参数，调用 NewsAPI
     * 2. 解析返回的 JSON 数据
     * 3. 遍历新闻列表，进行去重判断
     * 4. 将新新闻存入数据库
     * </p>
     *
     * @return 新增的新闻数量
     */
    @Override
    public int fetchAndSaveTechNews() {
        log.info("开始从 IT 之家 RSS 抓取科技新闻：{}", rssUrl);

        int savedCount = 0;
        List<News> newsList = new ArrayList<>();

        try {
            // ========== 获取 RSS 内容 ==========
            String rssContent = restTemplate.getForObject(rssUrl, String.class);

            if (rssContent == null || rssContent.isEmpty()) {
                log.error("RSS 返回空内容");
                return 0;
            }

            log.info("获取到 RSS 内容，长度：{} 字节", rssContent.length());

            // ========== 解析 RSS ==========
            newsList = parseRss(rssContent);

            log.info("成功解析 {} 条新闻数据", newsList.size());

            // ========== 更新已有 IT 之家新闻的轮播图状态 ==========
            // 将已有的 IT 之家新闻（is_manual=0）按发布时间排序，更新前 carouselCount 条为轮播图
            updateCarouselNewsStatus();

            // ========== 去重并保存 ==========
            int carouselCounter = 0; // 轮播图新闻计数器
            for (News news : newsList) {
                if (!isNewsExists(news)) {
                    // 设置默认值
                    if (news.getNewsType() == null) {
                        // 前 3 条新闻设为轮播图，其余为列表新闻
                        if (carouselCounter < carouselCount) {
                            news.setNewsType(1); // 轮播图新闻
                            news.setIsTop(1); // 轮播图新闻默认置顶
                            carouselCounter++;
                        } else {
                            news.setNewsType(2); // 列表新闻
                        }
                    }
                    if (news.getIsTop() == null) {
                        news.setIsTop(0); // 默认不置顶
                    }
                    if (news.getIsManual() == null) {
                        news.setIsManual(0); // 标记为自动抓取
                    }
                    if (news.getPublishTime() == null) {
                        news.setPublishTime(LocalDateTime.now());
                    }

                    // 保存到数据库
                    save(news);
                    savedCount++;
                    log.debug("新增新闻：{} (类型：{})", news.getTitle(), news.getNewsType() == 1 ? "轮播图" : "列表");
                } else {
                    log.debug("新闻已存在，跳过：{}", news.getTitle());
                }
            }

            log.info("本次抓取新增 {} 条新闻", savedCount);

        } catch (Exception e) {
            log.error("抓取新闻失败", e);
            throw new RuntimeException("抓取新闻失败：" + e.getMessage());
        }

        return savedCount;
    }

    /**
     * 更新已有 IT 之家新闻的轮播图状态
     * <p>
     * 功能说明：
     * 1. 查询所有已存在的 IT 之家新闻（is_manual=0）
     * 2. 按发布时间倒序排序
     * 3. 前 carouselCount 条设为轮播图（newsType=1, isTop=1）
     * 4. 其余设为列表新闻（newsType=2, isTop=0）
     * </p>
     */
    private void updateCarouselNewsStatus() {
        try {
            // 查询所有 IT 之家新闻（is_manual=0），按发布时间倒序
            List<News> allNews = lambdaQuery()
                .eq(News::getIsManual, 0)
                .orderByDesc(News::getPublishTime)
                .list();

            if (allNews.isEmpty()) {
                log.debug("暂无 IT 之家新闻，跳过轮播图更新");
                return;
            }

            log.info("开始更新 {} 条 IT 之家新闻的轮播图状态", allNews.size());

            int updated = 0;
            for (int i = 0; i < allNews.size(); i++) {
                News news = allNews.get(i);
                Integer expectedType = (i < carouselCount) ? 1 : 2;
                Integer expectedTop = (i < carouselCount) ? 1 : 0;

                // 只有当当前状态与期望状态不一致时才更新
                if (!expectedType.equals(news.getNewsType()) || !expectedTop.equals(news.getIsTop())) {
                    LambdaUpdateWrapper<News> updateWrapper = new LambdaUpdateWrapper<>();
                    updateWrapper.eq(News::getId, news.getId())
                        .set(News::getNewsType, expectedType)
                        .set(News::getIsTop, expectedTop);
                    update(updateWrapper);
                    updated++;
                    log.debug("更新新闻 [{}] 为{}，置顶={}", news.getTitle(),
                        (i < carouselCount) ? "轮播图" : "列表", expectedTop);
                }
            }

            log.info("轮播图更新完成，共更新 {} 条新闻的状态", updated);

        } catch (Exception e) {
            log.error("更新轮播图状态失败：{}", e.getMessage(), e);
        }
    }

    /**
     * 判断新闻是否已存在（去重逻辑）
     * <p>
     * 去重策略：
     * 1. 优先根据来源 URL 判断（如果存在）
     * 2. 其次根据标题判断（模糊匹配，避免同一新闻的不同版本）
     * </p>
     *
     * @param news 待检查的新闻
     * @return true 表示已存在，false 表示不存在
     */
    private boolean isNewsExists(News news) {

        // 如果有来源 URL，优先根据 URL 去重
        if (news.getSourceUrl() != null && !news.getSourceUrl().isEmpty()) {
            long count = lambdaQuery()
                .eq(News::getSourceUrl, news.getSourceUrl())
                .count();
            if (count > 0) {
                return true;
            }
        }

        // 根据标题去重（精确匹配）
        if (news.getTitle() != null && !news.getTitle().isEmpty()) {
            long count = lambdaQuery()
                .eq(News::getTitle, news.getTitle())
                .count();
            if (count > 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * 解析 API 返回的 JSON 数据
     * <p>
     * 适配不同 API 的响应格式：
     * 1. NewsAPI 格式：{ "articles": [...] }
     * 2. 36 氪格式：{ "data": { "items": [...] } }
     * 3. 自定义格式：{ "news": [...] }
     * </p>
     *
     * @param jsonResponse API 返回的 JSON 字符串
     * @return 解析后的新闻列表
     */
    private List<News> parseNewsFromApiResponse(String jsonResponse) {
        List<News> newsList = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(jsonResponse);

            // 适配 NewsAPI 格式
            JsonNode articlesNode = root.path("articles");
            if (articlesNode.isArray()) {
                for (JsonNode article : articlesNode) {
                    News news = new News();
                    news.setTitle(article.path("title").asText());
                    news.setSummary(article.path("description").asText());
                    news.setImageUrl(article.path("urlToImage").asText());
                    news.setSourceUrl(article.path("url").asText());
                    news.setSourceName(article.path("source").path("name").asText());
                    news.setNewsType(2); // 列表新闻

                    // 解析发布时间
                    String publishedAt = article.path("publishedAt").asText();
                    if (!publishedAt.isEmpty()) {
                        news.setPublishTime(parseDateTime(publishedAt));
                    }

                    newsList.add(news);
                }
            }

            // 适配其他格式（可根据需要扩展）
            // ...

        } catch (Exception e) {
            log.error("解析 JSON 数据失败", e);
        }

        return newsList;
    }

    /**
     * 解析 RSS 订阅内容
     * <p>
     * 支持 RSS 2.0 格式，解析 IT 之家等科技网站的 RSS Feed
     * </p>
     *
     * @param rssContent RSS 原始内容（XML 格式）
     * @return 解析后的新闻列表
     */
    private List<News> parseRss(String rssContent) {
        List<News> newsList = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setNamespaceAware(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(rssContent.getBytes(StandardCharsets.UTF_8)));
            doc.getDocumentElement().normalize();

            NodeList itemList = doc.getElementsByTagName("item");
            log.info("RSS 中包含 {} 个 item 元素", itemList.getLength());

            for (int i = 0; i < itemList.getLength(); i++) {
                Node itemNode = itemList.item(i);
                if (itemNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element itemElement = (Element) itemNode;

                    News news = new News();

                    // 获取 title
                    String title = getTagValue(itemElement, "title");
                    if (title != null && !title.isEmpty()) {
                        news.setTitle(title.trim());
                        log.debug("解析到新闻标题：{}", title);
                    }

                    // 获取 description/summary
                    String description = getTagValue(itemElement, "description");
                    if (description != null && !description.isEmpty()) {
                        // 去除 HTML 标签
                        String cleanDesc = description.replaceAll("<[^>]*>", "");
                        // 限制长度在 500 字符以内
                        if (cleanDesc.length() > 500) {
                            cleanDesc = cleanDesc.substring(0, 500) + "...";
                        }
                        news.setSummary(cleanDesc.trim());
                    }

                    // 获取 link
                    String link = getTagValue(itemElement, "link");
                    if (link != null && !link.isEmpty()) {
                        news.setSourceUrl(link.trim());
                    }

                    // 获取来源
                    news.setSourceName("IT 之家");

                    // 获取发布时间
                    String pubDate = getTagValue(itemElement, "pubDate");
                    if (pubDate != null && !pubDate.isEmpty()) {
                        news.setPublishTime(parseDateTime(pubDate));
                    } else {
                        news.setPublishTime(LocalDateTime.now());
                    }

                    // 获取图片（从 description 中提取或设置默认）
                    String imageUrl = extractImageUrl(description);
                    news.setImageUrl(imageUrl);

                    // 注意：不在这里设置 newsType，由 fetchAndSaveTechNews() 方法统一分配
                    // 前 carouselCount 条新闻设为轮播图 (newsType=1)，其余为列表新闻 (newsType=2)
                    news.setIsManual(0); // 标记为自动抓取

                    newsList.add(news);
                    log.info("解析新闻 [{}/{}]: {}", i + 1, itemList.getLength(), news.getTitle());
                }
            }

            log.info("RSS 解析完成，共 {} 条新闻", newsList.size());

        } catch (Exception e) {
            log.error("解析 RSS 失败：{}", e.getMessage(), e);
        }

        return newsList;
    }

    /**
     * 从 XML 元素中获取标签值
     */
    private String getTagValue(Element element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            if (node.hasChildNodes()) {
                return node.getFirstChild().getNodeValue();
            }
        }
        return null;
    }

    /**
     * 从 RSS 描述中提取图片 URL
     * <p>
     * 支持多种图片来源：
     * 1. img 标签的 src 属性
     * 2. enclosure 标签（RSS 标准图片）
     * 3. media:content 标签
     * 4. 如果都没有，返回默认图片 URL
     * </p>
     */
    private String extractImageUrl(String description) {
        if (description == null || description.isEmpty()) {
            return getDefaultTechImage();
        }

        // 1. 使用正则表达式从 HTML 中提取 img 标签的 src
        String regex = "<img[^>]*src=[\"']([^\"']*)[\"']";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex, java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(description);

        if (matcher.find()) {
            String imageUrl = matcher.group(1).trim();
            // 返回有效的图片 URL（排除过小或无效的图片）
            if (!imageUrl.isEmpty() && !imageUrl.endsWith(".gif")) {
                return imageUrl;
            }
        }

        // 2. 尝试从 description 中提取 background-image
        regex = "background-image:[^url]*url\\([\"']?([^\"')\\s]+)[\"']?\\)";
        pattern = java.util.regex.Pattern.compile(regex, java.util.regex.Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(description);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // 3. 如果都没找到，返回默认科技类图片
        return getDefaultTechImage();
    }

    /**
     * 获取默认科技类图片 URL
     * 使用 Unsplash 源，每次随机返回不同的科技主题图片
     */
    private String getDefaultTechImage() {
        // 使用 Unsplash Source 的随机图片服务（带关键词）
        // 或者使用固定的高质量科技图片
        String[] defaultImages = {
            "https://images.unsplash.com/photo-1518770660439-4636190af475?w=800&h=400&fit=crop",  // 芯片
            "https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=800&h=400&fit=crop",  // 编程
            "https://images.unsplash.com/photo-1504639725590-34d0984388bd?w=800&h=400&fit=crop",  // AI
            "https://images.unsplash.com/photo-1550751827-4bd374c3f58b?w=800&h=400&fit=crop",  // 网络安全
            "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=800&h=400&fit=crop",  // 黑客
            "https://images.unsplash.com/photo-1555255714-732bb582f834?w=800&h=400&fit=crop",  // 数据
            "https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=800&h=400&fit=crop",  // 云服务
            "https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=800&h=400&fit=crop"   // 机器人
        };
        // 使用当前时间戳作为随机种子，保证同一新闻每次获取相同的默认图片
        int index = (int)(System.currentTimeMillis() / 1000) % defaultImages.length;
        return defaultImages[index];
    }

    /**
     * 解析聚合数据 API 返回的 JSON 数据
     * <p>
     * 响应格式：
     * {
     *   "resultcode": "200",
     *   "reason": "正确的返回",
     *   "result": {
     *     "stat": 1,
     *     "data": [
     *       {
     *         "title": "新闻标题",
     *         "description": "新闻摘要",
     *         "thumbnail_pic_s": "图片 URL",
     *         "url": "新闻链接",
     *         "author_name": "来源",
     *         "date": "2024-01-15 10:30"
     *       }
     *     ]
     *   }
     * }
     * </p>
     *
     * @param jsonResponse API 返回的 JSON 字符串
     * @return 解析后的新闻列表
     */
    private List<News> parseNewsFromJuhe(String jsonResponse) {
        List<News> newsList = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(jsonResponse);

            // 检查响应码
            String resultcode = root.path("resultcode").asText();
            if (!"200".equals(resultcode)) {
                String reason = root.path("reason").asText();
                log.error("API 返回错误：{} (resultcode: {})", reason, resultcode);
                return newsList;
            }

            // 解析响应数据
            JsonNode resultNode = root.path("result");
            JsonNode dataNode = resultNode.path("data");

            if (dataNode.isArray()) {
                for (JsonNode item : dataNode) {
                    News news = new News();
                    news.setTitle(item.path("title").asText());
                    news.setSummary(item.path("description").asText());
                    news.setImageUrl(item.path("thumbnail_pic_s").asText());
                    news.setSourceUrl(item.path("url").asText());
                    news.setSourceName(item.path("author_name").asText());
                    news.setNewsType(2); // 列表新闻

                    // 解析发布时间（格式：2024-01-15 10:30）
                    String pubDate = item.path("date").asText();
                    if (!pubDate.isEmpty()) {
                        news.setPublishTime(parseDateTime(pubDate));
                    }

                    newsList.add(news);
                }
            }

        } catch (Exception e) {
            log.error("解析 JSON 数据失败", e);
        }

        return newsList;
    }

    /**
     * 解析万维易源 API 返回的 JSON 数据
     * <p>
     * 响应格式：
     * {
     *   "showapi_error": "",
     *   "showapi_res_body": {
     *     "ret_code": 0,
     *     "pagebean": {
     *       "contentlist": [
     *         {
     *           "title": "新闻标题",
     *           "summary": "新闻摘要",
     *           "imageUrl": "图片 URL",
     *           "source": "来源",
     *           "pubDate": "2024-01-15 10:30:00"
     *         }
     *       ]
     *     }
     *   }
     * }
     * </p>
     *
     * @param jsonResponse API 返回的 JSON 字符串
     * @return 解析后的新闻列表
     */
    private List<News> parseNewsFromShowApi(String jsonResponse) {
        List<News> newsList = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(jsonResponse);

            // 检查是否有错误
            JsonNode errorNode = root.path("showapi_error");
            if (errorNode.isTextual() && !errorNode.asText().isEmpty()) {
                log.error("API 返回错误：{}", errorNode.asText());
                return newsList;
            }

            // 解析响应数据
            JsonNode bodyNode = root.path("showapi_res_body");
            JsonNode pagebeanNode = bodyNode.path("pagebean");
            JsonNode contentlistNode = pagebeanNode.path("contentlist");

            if (contentlistNode.isArray()) {
                for (JsonNode item : contentlistNode) {
                    News news = new News();
                    news.setTitle(item.path("title").asText());
                    news.setSummary(item.path("summary").asText());
                    news.setImageUrl(item.path("imageUrl").asText());
                    news.setSourceUrl(item.path("sourceurl").asText());
                    news.setSourceName(item.path("source").asText());
                    news.setNewsType(2); // 列表新闻

                    // 解析发布时间（格式：2024-01-15 10:30:00）
                    String pubDate = item.path("pubDate").asText();
                    if (!pubDate.isEmpty()) {
                        news.setPublishTime(parseDateTime(pubDate));
                    }

                    newsList.add(news);
                }
            }

        } catch (Exception e) {
            log.error("解析 JSON 数据失败", e);
        }

        return newsList;
    }

    /**
     * 解析日期时间字符串
     * 支持多种格式：ISO 8601、RFC 822、自定义格式等
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return LocalDateTime.now();
        }

        try {
            // ISO 8601 格式：2024-01-15T10:30:00Z
            if (dateTimeStr.contains("T")) {
                return LocalDateTime.parse(dateTimeStr.replace("Z", ""),
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }

            // RFC 822 格式：Wed, 18 Mar 2026 11:55:45 GMT
            if (dateTimeStr.contains(",")) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                        "EEE, dd MMM yyyy HH:mm:ss 'GMT'", java.util.Locale.ENGLISH);
                    return LocalDateTime.parse(dateTimeStr, formatter);
                } catch (Exception e) {
                    log.debug("RFC 822 格式解析失败：{}", e.getMessage());
                }
            }

            // 其他格式可根据需要添加
            return LocalDateTime.now();
        } catch (Exception e) {
            log.debug("日期解析失败：{}", e.getMessage());
            return LocalDateTime.now();
        }
    }

    /**
     * 获取模拟新闻数据（用于开发和测试，当 API 不可用时作为备选）
     * <p>
     * 模拟多源异构数据：
     * 1. 不同来源的新闻（OpenAI、Google、Microsoft 等）
     * 2. 不同格式的内容（标题、摘要、图片等）
     * 3. 不同类型的新闻（轮播图、列表新闻）
     * </p>
     *
     * @return 模拟的新闻列表
     */
    private List<News> fetchMockNews() {
        List<News> newsList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // ========== 轮播图新闻（newsType=1）==========
        News carousel1 = new News();
        carousel1.setTitle("OpenAI 发布 GPT-5：多模态能力全面升级，推理速度提升 3 倍");
        carousel1.setSummary("OpenAI 今日正式发布 GPT-5，新增实时视频理解、代码自动调试等功能。");
        carousel1.setContent("OpenAI 今日正式发布 GPT-5，这是继 GPT-4 之后又一重大突破。" +
            "新版本支持实时视频理解、代码自动调试、多语言实时翻译等功能，推理速度相比 GPT-4 提升了 3 倍。" +
            "GPT-5 在多项基准测试中取得了超越人类水平的成绩。");
        carousel1.setImageUrl("https://images.unsplash.com/photo-1677442136019-21780ecad995?w=800&h=400&fit=crop");
        carousel1.setSourceUrl("https://openai.com/blog/gpt-5");
        carousel1.setSourceName("OpenAI 官方博客");
        carousel1.setNewsType(1);
        carousel1.setIsTop(1);
        carousel1.setIsManual(0);
        carousel1.setPublishTime(now);
        newsList.add(carousel1);

        News carousel2 = new News();
        carousel2.setTitle("英伟达发布新一代 AI 芯片 Blackwell B300：性能提升 10 倍");
        carousel2.setSummary("NVIDIA Blackwell B300 采用 3nm 工艺，支持 10TB/s 内存带宽。");
        carousel2.setContent("NVIDIA 今日发布新一代 AI 芯片 Blackwell B300，采用台积电 3nm 工艺制程。" +
            "新芯片支持 10TB/s 的超高内存带宽，专为大模型训练设计，性能相比上一代提升 10 倍。");
        carousel2.setImageUrl("https://images.unsplash.com/photo-1555617778-02518510b9fa?w=800&h=400&fit=crop");
        carousel2.setSourceUrl("https://nvidia.com/blog/blackwell-b300");
        carousel2.setSourceName("NVIDIA");
        carousel2.setNewsType(1);
        carousel2.setIsTop(0);
        carousel2.setIsManual(0);
        carousel2.setPublishTime(now.minusHours(1));
        newsList.add(carousel2);

        // ========== 列表新闻（newsType=2）==========
        News list1 = new News();
        list1.setTitle("谷歌 DeepMind 突破：AI 首次独立发现数学定理");
        list1.setSummary("DeepMind 的 AI 系统在没有任何人类干预的情况下发现了一个新的数学定理。");
        list1.setContent("谷歌 DeepMind 研究团队宣布，其开发的 AI 系统在没有任何人类干预的情况下，" +
            "独立发现并证明了一个新的数学定理。这是 AI 在数学研究领域的重大突破。");
        list1.setImageUrl("https://images.unsplash.com/photo-1509228468518-180dd4864904?w=800&h=400&fit=crop");
        list1.setSourceUrl("https://deepmind.com/blog/ai-math-discovery");
        list1.setSourceName("DeepMind");
        list1.setNewsType(2);
        list1.setIsTop(1);
        list1.setIsManual(0);
        list1.setPublishTime(now.minusHours(2));
        newsList.add(list1);

        News list2 = new News();
        list2.setTitle("字节豆包大模型日活突破 5000 万，成国内第一");
        list2.setSummary("字节跳动旗下的豆包大模型日活跃用户数突破 5000 万。");
        list2.setContent("字节跳动官方宣布，旗下 AI 大模型产品「豆包」日活跃用户数已突破 5000 万，" +
            "成为国内用户量最大的 AI 助手应用。豆包在对话、写作、编程等多个场景表现出色。");
        list2.setImageUrl("https://images.unsplash.com/photo-1677442135703-1787eea5ce01?w=800&h=400&fit=crop");
        list2.setSourceUrl("https://www.bytedance.com/news/doubao-50m-users");
        list2.setSourceName("字节跳动");
        list2.setNewsType(2);
        list2.setIsTop(0);
        list2.setIsManual(0);
        list2.setPublishTime(now.minusHours(3));
        newsList.add(list2);

        News list3 = new News();
        list3.setTitle("华为盘古大模型 5.0 发布：支持 100+ 语言");
        list3.setSummary("华为盘古大模型 5.0 正式发布，支持超过 100 种语言的文本处理。");
        list3.setContent("华为在开发者大会上正式发布盘古大模型 5.0 版本，新版本支持超过 100 种语言的文本处理，" +
            "在金融、医疗、制造等行业场景提供了专业化解决方案。");
        list3.setImageUrl("https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=800&h=400&fit=crop");
        list3.setSourceUrl("https://huawei.com/pangu-5");
        list3.setSourceName("华为");
        list3.setNewsType(2);
        list3.setIsTop(0);
        list3.setIsManual(0);
        list3.setPublishTime(now.minusHours(4));
        newsList.add(list3);

        News list4 = new News();
        list4.setTitle("阿里云通义千问开源 Qwen-72B，性能领跑开源模型");
        list4.setSummary("阿里云开源 Qwen-72B 大模型，在多项测试中领跑开源模型。");
        list4.setContent("阿里云宣布开源通义千问 720 亿参数版本 Qwen-72B，该模型在多项基准测试中取得了" +
            "开源模型最佳成绩，为研究者和开发者提供了强大的基础模型。");
        list4.setImageUrl("https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=800&h=400&fit=crop");
        list4.setSourceUrl("https://aliyun.com/qwen-72b");
        list4.setSourceName("阿里云");
        list4.setNewsType(2);
        list4.setIsTop(0);
        list4.setIsManual(0);
        list4.setPublishTime(now.minusHours(5));
        newsList.add(list4);

        return newsList;
    }
}
