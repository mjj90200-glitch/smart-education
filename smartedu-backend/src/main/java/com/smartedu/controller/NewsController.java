package com.smartedu.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartedu.common.result.Result;
import com.smartedu.entity.News;
import com.smartedu.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 新闻控制器
 * <p>
 * 功能说明：
 * 1. 提供新闻查询接口（轮播图、列表、详情）
 * 2. 提供手动触发更新接口（用于测试）
 * 3. 支持管理员对新闻进行管理
 * </p>
 *
 * @author SmartEdu Team
 */
@Tag(name = "新闻管理", description = "科技资讯相关接口")
@RestController
@RequestMapping("/news")
public class NewsController {

    @Autowired
    private NewsService newsService;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 获取轮播图新闻
     * <p>
     * 返回 newsType=1 的新闻，按置顶和时间排序
     * </p>
     */
    @GetMapping("/carousel")
    @Operation(summary = "获取轮播图新闻", description = "获取首页轮播图展示的科技新闻")
    public Result<List<News>> getCarousel() {
        return Result.success(newsService.getCarouselNews());
    }

    /**
     * 获取列表新闻
     * <p>
     * 返回 newsType=2 的新闻，支持分页
     * </p>
     *
     * @param limit 返回数量限制，默认 10 条
     */
    @GetMapping("/list")
    @Operation(summary = "获取列表新闻", description = "获取新闻列表，支持数量限制")
    public Result<List<News>> getList(
            @Parameter(description = "数量限制", example = "10")
            @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(newsService.getListNews(limit));
    }

    /**
     * 获取新闻详情
     *
     * @param id 新闻 ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取新闻详情", description = "根据 ID 获取新闻详细信息")
    public Result<News> getById(@PathVariable Long id) {
        News news = newsService.getById(id);
        if (news == null) {
            return Result.error("新闻不存在");
        }
        return Result.success(news);
    }

    /**
     * 手动触发新闻更新
     * <p>
     * 功能说明：
     * 1. 立即从外部 API 抓取最新科技新闻
     * 2. 自动去重并存入数据库
     * 3. 返回新增新闻数量
     * </p>
     *
     * <p>
     * 权限说明：
     * - 仅管理员（ADMIN）可调用此接口
     * - 本地开发测试时可临时放开权限
     * </p>
     *
     * @return 返回本次更新的新闻数量
     */
    @PostMapping("/manual-update")
    @Operation(summary = "手动触发新闻更新", description = "立即从外部 API 抓取最新新闻（仅管理员）")
    @PreAuthorize("hasRole('ADMIN')")  // 仅管理员可调用
    public Result<Map<String, Object>> manualUpdate() {
        // 调用服务层方法抓取并保存新闻
        int savedCount = newsService.fetchAndSaveTechNews();

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("savedCount", savedCount);
        result.put("message", savedCount > 0 ? "成功新增 " + savedCount + " 条新闻" : "没有新增新闻");

        return Result.success("新闻更新完成", result);
    }

    /**
     * 手动触发新闻更新（无权限验证版本）
     * <p>
     * 注意：此接口仅供本地开发测试使用，生产环境应删除或加权限验证
     * </p>
     *
     * @return 返回本次更新的新闻数量
     */
    @PostMapping("/manual-update-dev")
    @Operation(summary = "手动触发新闻更新（开发测试）", description = "立即抓取新闻，无需管理员权限（仅限开发环境）")
    public Result<Map<String, Object>> manualUpdateDev() {
        // 调用服务层方法抓取并保存新闻
        int savedCount = newsService.fetchAndSaveTechNews();

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("savedCount", savedCount);
        result.put("message", savedCount > 0 ? "成功新增 " + savedCount + " 条新闻" : "没有新增新闻");

        return Result.success("新闻更新完成", result);
    }

    /**
     * 删除新闻
     *
     * @param id 新闻 ID
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除新闻", description = "根据 ID 删除新闻（仅管理员）")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteNews(@PathVariable Long id) {
        boolean success = newsService.removeById(id);
        if (success) {
            return Result.<Void>success();
        }
        return Result.error("删除失败");
    }

    /**
     * 更新新闻置顶状态
     *
     * @param id    新闻 ID
     * @param isTop 是否置顶：1-置顶，0-取消置顶
     */
    @PutMapping("/{id}/top")
    @Operation(summary = "更新新闻置顶状态", description = "设置或取消新闻置顶（仅管理员）")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateTopStatus(
            @PathVariable Long id,
            @Parameter(description = "是否置顶：1-置顶，0-取消") @RequestBody Map<String, Integer> isTop) {
        News news = newsService.getById(id);
        if (news == null) {
            return Result.error("新闻不存在");
        }
        news.setIsTop(isTop.get("isTop"));
        newsService.updateById(news);
        return Result.<Void>success();
    }

    /**
     * 测试 RSS 连接
     */
    @GetMapping("/test-api")
    @Operation(summary = "测试 RSS 连接", description = "测试 IT 之家 RSS 是否可访问")
    public Result<Map<String, Object>> testApi() {
        Map<String, Object> result = new HashMap<>();
        try {
            String rssUrl = "https://www.ithome.com/rss/";
            long start = System.currentTimeMillis();
            String response = restTemplate.getForObject(rssUrl, String.class);
            long elapsed = System.currentTimeMillis() - start;
            result.put("status", "success");
            result.put("elapsed", elapsed);
            result.put("responseLength", response != null ? response.length() : 0);
            result.put("response", response != null ? response.substring(0, Math.min(500, response.length())) : "");
            return Result.success(result);
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
            return Result.success(result);
        }
    }

    /**
     * 测试 RSS 解析
     */
    @GetMapping("/test-parse")
    @Operation(summary = "测试 RSS 解析", description = "测试 IT 之家 RSS 解析功能")
    public Result<Map<String, Object>> testParse() {
        Map<String, Object> result = new HashMap<>();
        try {
            String rssUrl = "https://www.ithome.com/rss/";
            String rssContent = restTemplate.getForObject(rssUrl, String.class);

            // 调用服务层解析方法
            int count = newsService.fetchAndSaveTechNews();

            result.put("status", "success");
            result.put("message", "解析成功");
            result.put("savedCount", count);
            return Result.success(result);
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
            result.put("trace", e.toString());
            return Result.success(result);
        }
    }

    /**
     * 管理后台 - 获取新闻列表（分页）
     * <p>
     * 功能说明：
     * 1. 支持分页查询
     * 2. 支持按类型筛选（轮播图/列表）
     * 3. 支持关键词搜索（标题）
     * </p>
     *
     * @param pageNum  页码，默认 1
     * @param pageSize 每页数量，默认 10
     * @param keyword  搜索关键词（标题）
     * @param newsType 新闻类型：1=轮播图，2=列表
     */
    @GetMapping("/admin/list")
    @Operation(summary = "管理后台 - 新闻列表", description = "分页查询新闻列表，支持筛选（仅管理员）")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getAdminList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer newsType) {

        Page<News> page = new Page<>(pageNum, pageSize);
        var query = newsService.lambdaQuery();

        // 关键词搜索
        if (keyword != null && !keyword.isEmpty()) {
            query.like(News::getTitle, keyword);
        }

        // 类型筛选
        if (newsType != null) {
            query.eq(News::getNewsType, newsType);
        }

        // 按发布时间倒序
        query.orderByDesc(News::getPublishTime);

        Page<News> resultPage = query.page(page);

        Map<String, Object> response = new HashMap<>();
        response.put("records", resultPage.getRecords());
        response.put("total", resultPage.getTotal());
        response.put("pageNum", pageNum);
        response.put("pageSize", pageSize);

        return Result.success(response);
    }

    /**
     * 管理后台 - 保存新闻（新增或更新）
     * <p>
     * 功能说明：
     * 1. ID 为空时新增新闻
     * 2. ID 不为空时更新新闻
     * 3. 如果 imageUrl 为空，自动设置默认科技图片
     * </p>
     *
     * @param news 新闻对象
     */
    @PostMapping("/save")
    @Operation(summary = "管理后台 - 保存新闻", description = "新增或更新新闻（仅管理员）")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> saveNews(@RequestBody News news) {
        boolean success;
        Map<String, Object> result = new HashMap<>();

        // 如果 imageUrl 为空或空白，设置默认图片
        if (news.getImageUrl() == null || news.getImageUrl().trim().isEmpty()) {
            news.setImageUrl(getDefaultTechImage());
        }

        if (news.getId() == null) {
            // 新增
            news.setCreateTime(java.time.LocalDateTime.now());
            news.setUpdateTime(java.time.LocalDateTime.now());
            if (news.getIsManual() == null) {
                news.setIsManual(1); // 手动添加
            }
            success = newsService.save(news);
            result.put("id", news.getId());
        } else {
            // 更新
            news.setUpdateTime(java.time.LocalDateTime.now());
            success = newsService.updateById(news);
        }

        if (success) {
            return Result.success("保存成功", result);
        } else {
            return Result.error("保存失败");
        }
    }

    /**
     * 获取默认科技类图片 URL
     */
    private String getDefaultTechImage() {
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
        int index = (int)(System.currentTimeMillis() / 1000) % defaultImages.length;
        return defaultImages[index];
    }

    /**
     * 管理后台 - 保存轮播图排序
     * <p>
     * 功能说明：
     * 1. 根据传入的新闻 ID 列表更新轮播图顺序
     * 2. 自动将传入的新闻类型设为 1（轮播图）
     * 3. 不在列表中的轮播图新闻类型设为 2（列表新闻）
     * 4. orderIndex 从 1 开始递增
     * </p>
     *
     * @param params 包含 newsIds 列表
     */
    @PostMapping("/admin/carousel/order")
    @Operation(summary = "管理后台 - 保存轮播图排序", description = "保存轮播图的显示顺序（仅管理员）")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> saveCarouselOrder(@RequestBody Map<String, List<Integer>> params) {
        List<Integer> newsIds = params.get("newsIds");
        if (newsIds == null || newsIds.isEmpty()) {
            return Result.error("新闻 ID 列表不能为空");
        }

        try {
            // 1. 将传入的 newsIds 对应的新闻设为轮播图，并设置 orderIndex
            for (int i = 0; i < newsIds.size(); i++) {
                Long id = newsIds.get(i).longValue();
                News news = newsService.getById(id);
                if (news != null) {
                    news.setNewsType(1); // 设为轮播图
                    news.setOrderIndex(i + 1); // 设置顺序
                    newsService.updateById(news);
                }
            }

            // 2. 查询所有当前是轮播图的新闻
            List<News> allCarouselNews = newsService.lambdaQuery()
                .eq(News::getNewsType, 1)
                .list();

            // 3. 将不在传入列表中的轮播图新闻改为列表新闻
            for (News news : allCarouselNews) {
                if (!newsIds.contains(news.getId().intValue())) {
                    news.setNewsType(2); // 改为列表新闻
                    news.setOrderIndex(0); // 清空排序序号
                    newsService.updateById(news);
                }
            }

            return Result.success();
        } catch (Exception e) {
            return Result.error("保存失败：" + e.getMessage());
        }
    }

    /**
     * 管理后台 - 批量更新新闻类型
     * <p>
     * 功能说明：
     * 1. 批量修改新闻类型（轮播图/列表）
     * 2. ids: 新闻 ID 列表
     * 3. newsType: 1=轮播图，2=列表
     * </p>
     *
     * @param params 包含 ids 列表和 newsType
     */
    @PutMapping("/admin/batch-update-type")
    @Operation(summary = "管理后台 - 批量更新新闻类型", description = "批量修改新闻类型（仅管理员）")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> batchUpdateNewsType(@RequestBody Map<String, Object> params) {
        List<Long> ids = (List<Long>) params.get("ids");
        Integer newsType = (Integer) params.get("newsType");

        if (ids == null || ids.isEmpty()) {
            return Result.error("新闻 ID 列表不能为空");
        }
        if (newsType == null || (newsType != 1 && newsType != 2)) {
            return Result.error("新闻类型必须为 1 或 2");
        }

        for (Long id : ids) {
            News news = newsService.getById(id);
            if (news != null) {
                news.setNewsType(newsType);
                newsService.updateById(news);
            }
        }

        return Result.success();
    }

    /**
     * 管理后台 - 上传新闻图片
     * <p>
     * 功能说明：
     * 1. 支持上传图片文件（JPG、PNG、GIF 等格式）
     * 2. 图片最大 10MB
     * 3. 保存到服务器 uploads/news 目录
     * 4. 返回图片访问 URL
     * </p>
     *
     * @param file 图片文件
     * @return 图片 URL
     */
    @PostMapping("/admin/upload-image")
    @Operation(summary = "管理后台 - 上传新闻图片", description = "上传图片作为新闻封面，返回图片 URL（仅管理员）")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, String>> uploadNewsImage(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }

        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("请上传图片文件（支持 JPG、PNG、GIF 等格式）");
        }

        // 验证文件大小（最大 10MB）
        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            return Result.error("图片体积过大，请选择小于 10MB 的图片");
        }

        try {
            // 生成文件名
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                originalFilename = "image.jpg";
            }
            // 安全获取文件扩展名
            String extension = "";
            int lastDotIndex = originalFilename.lastIndexOf(".");
            if (lastDotIndex > 0 && lastDotIndex < originalFilename.length() - 1) {
                extension = originalFilename.substring(lastDotIndex).toLowerCase();
            } else {
                // 默认使用.jpg 扩展名
                extension = ".jpg";
            }
            String filename = UUID.randomUUID().toString() + extension;

            // 保存到本地目录
            String uploadDir = "uploads/news";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(filename);
            file.transferTo(filePath);

            // 返回访问 URL（相对路径，前端会加上 baseURL）
            String imageUrl = "/news/image/" + filename;

            Map<String, String> result = new HashMap<>();
            result.put("imageUrl", imageUrl);
            result.put("filename", filename);
            result.put("originalFilename", originalFilename);

            return Result.success("上传成功", result);
        } catch (IOException e) {
            return Result.error("上传失败：" + e.getMessage());
        }
    }

    /**
     * 获取新闻图片
     */
    @GetMapping("/image/{filename}")
    @Operation(summary = "获取新闻图片", description = "根据文件名返回图片内容")
    public ResponseEntity<byte[]> getNewsImage(@PathVariable String filename) throws IOException {
        String uploadDir = "uploads/news";
        Path filePath = Paths.get(uploadDir).resolve(filename);
        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }

        // 根据文件扩展名设置 Content-Type
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setCacheControl("public, max-age=31536000"); // 缓存 1 年

        return ResponseEntity.ok()
                .headers(headers)
                .body(Files.readAllBytes(filePath));
    }
}