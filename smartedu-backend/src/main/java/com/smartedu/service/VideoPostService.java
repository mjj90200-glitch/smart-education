package com.smartedu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartedu.dto.VideoAuditDTO;
import com.smartedu.dto.VideoSubmitDTO;
import com.smartedu.entity.VideoCollection;
import com.smartedu.entity.VideoPost;
import com.smartedu.mapper.VideoCollectionMapper;
import com.smartedu.mapper.VideoPostMapper;
import com.smartedu.vo.VideoPostVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 视频投稿服务
 * @author SmartEdu Team
 */
@Service
@RequiredArgsConstructor
public class VideoPostService {

    private final VideoPostMapper videoPostMapper;
    private final VideoCollectionMapper videoCollectionMapper;

    /**
     * 用户投稿视频
     */
    @Transactional
    public Long submitVideo(Long userId, VideoSubmitDTO dto) {
        VideoPost video = new VideoPost();
        video.setUserId(userId);
        video.setTitle(dto.getTitle());
        video.setCoverUrl(dto.getCoverUrl());
        video.setVideoUrl(dto.getVideoUrl());
        video.setDescription(dto.getDescription());
        video.setStatus(VideoPost.STATUS_PENDING);
        video.setViewCount(0);
        video.setCollectionCount(0);
        video.setSortOrder(0);
        videoPostMapper.insert(video);
        return video.getId();
    }

    /**
     * 管理员审核视频
     */
    @Transactional
    public void auditVideo(VideoAuditDTO dto) {
        VideoPost video = videoPostMapper.selectById(dto.getVideoId());
        if (video == null) {
            throw new RuntimeException("视频不存在");
        }
        video.setStatus(dto.getStatus());
        if (dto.getStatus() == VideoPost.STATUS_REJECTED) {
            video.setRejectReason(dto.getRejectReason());
        }
        videoPostMapper.updateById(video);
    }

    /**
     * 获取已通过的视频列表（分页）
     */
    public IPage<VideoPostVO> getApprovedVideos(int page, int size, String keyword, Long currentUserId) {
        Page<VideoPost> pageParam = new Page<>(page, size);
        IPage<VideoPost> videoPage = videoPostMapper.selectApprovedPage(pageParam, keyword);

        return videoPage.convert(video -> convertToVO(video, currentUserId));
    }

    /**
     * 获取待审核视频列表（分页）
     */
    public IPage<VideoPostVO> getPendingVideos(int page, int size) {
        Page<VideoPost> pageParam = new Page<>(page, size);
        IPage<VideoPost> videoPage = videoPostMapper.selectPendingPage(pageParam);

        return videoPage.convert(video -> convertToVO(video, null));
    }

    /**
     * 获取首页展示视频（fallback用，5个：1个精选 + 4个网格）
     */
    public List<VideoPostVO> getHomeVideos(Long currentUserId) {
        List<VideoPost> videos = videoPostMapper.selectHomeVideos();
        return videos.stream()
                .map(video -> convertToVO(video, currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * 获取视频详情
     */
    public VideoPostVO getVideoDetail(Long id, Long currentUserId) {
        VideoPost video = videoPostMapper.selectById(id);
        if (video == null || video.getDeleted() == 1) {
            throw new RuntimeException("视频不存在");
        }
        // 增加浏览次数
        videoPostMapper.incrementViewCount(id);
        return convertToVO(video, currentUserId);
    }

    /**
     * 收藏/取消收藏视频
     */
    @Transactional
    public boolean toggleCollection(Long userId, Long videoId) {
        // 检查是否已收藏
        int count = videoCollectionMapper.checkCollected(userId, videoId);

        if (count > 0) {
            // 取消收藏
            LambdaQueryWrapper<VideoCollection> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(VideoCollection::getUserId, userId)
                   .eq(VideoCollection::getVideoId, videoId);
            videoCollectionMapper.delete(wrapper);
            videoPostMapper.decrementCollectionCount(videoId);
            return false;
        } else {
            // 添加收藏
            VideoCollection collection = new VideoCollection();
            collection.setUserId(userId);
            collection.setVideoId(videoId);
            collection.setCreatedAt(LocalDateTime.now());
            videoCollectionMapper.insert(collection);
            videoPostMapper.incrementCollectionCount(videoId);
            return true;
        }
    }

    /**
     * 获取用户收藏的视频列表
     */
    public IPage<VideoPostVO> getUserCollections(Long userId, int page, int size) {
        Page<VideoCollection> pageParam = new Page<>(page, size);

        // 查询用户收藏的视频ID
        LambdaQueryWrapper<VideoCollection> cWrapper = new LambdaQueryWrapper<>();
        cWrapper.eq(VideoCollection::getUserId, userId)
                .orderByDesc(VideoCollection::getCreatedAt);

        IPage<VideoCollection> collectionPage = videoCollectionMapper.selectPage(pageParam, cWrapper);

        // 创建新的分页结果
        Page<VideoPostVO> resultPage = new Page<>(page, size);
        resultPage.setTotal(collectionPage.getTotal());

        List<VideoPostVO> voList = collectionPage.getRecords().stream()
                .map(collection -> {
                    VideoPost video = videoPostMapper.selectById(collection.getVideoId());
                    if (video != null && video.getStatus() == VideoPost.STATUS_APPROVED) {
                        VideoPostVO vo = convertToVO(video, userId);
                        vo.setCollected(true);
                        return vo;
                    }
                    return null;
                })
                .filter(vo -> vo != null)
                .collect(Collectors.toList());

        resultPage.setRecords(voList);
        return resultPage;
    }

    /**
     * 删除视频（管理员）
     */
    @Transactional
    public void deleteVideo(Long id) {
        // 使用自定义的 updateDeleted 方法来更新 deleted 字段
        videoPostMapper.updateDeleted(id);
    }

    /**
     * 实体转VO
     */
    private VideoPostVO convertToVO(VideoPost video, Long currentUserId) {
        VideoPostVO vo = new VideoPostVO();
        vo.setId(video.getId());
        vo.setUserId(video.getUserId());
        vo.setTitle(video.getTitle());
        vo.setCoverUrl(video.getCoverUrl());
        vo.setVideoUrl(video.getVideoUrl());
        vo.setDescription(video.getDescription());
        vo.setStatus(video.getStatus());
        vo.setRejectReason(video.getRejectReason());
        vo.setViewCount(video.getViewCount());
        vo.setCollectionCount(video.getCollectionCount());

        // 设置用户名（从Mapper查询的join结果）
        // 注意：需要在entity中添加@TableField(exist=false)的userName字段，或使用Map
        // 这里简化处理，实际可以使用userName字段

        // 检查是否已收藏
        if (currentUserId != null) {
            int collected = videoCollectionMapper.checkCollected(currentUserId, video.getId());
            vo.setCollected(collected > 0);
        } else {
            vo.setCollected(false);
        }

        // 格式化时间
        if (video.getCreatedAt() != null) {
            vo.setCreatedAt(video.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        }

        return vo;
    }
}