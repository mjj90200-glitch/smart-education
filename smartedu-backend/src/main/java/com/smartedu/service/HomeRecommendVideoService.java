package com.smartedu.service;

import com.smartedu.dto.HomeRecommendDTO;
import com.smartedu.entity.HomeRecommendVideo;
import com.smartedu.entity.VideoPost;
import com.smartedu.mapper.HomeRecommendVideoMapper;
import com.smartedu.mapper.VideoCollectionMapper;
import com.smartedu.mapper.VideoPostMapper;
import com.smartedu.vo.HomeRecommendVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 首页推荐视频服务
 * @author SmartEdu Team
 */
@Service
@RequiredArgsConstructor
public class HomeRecommendVideoService {

    private final HomeRecommendVideoMapper homeRecommendVideoMapper;
    private final VideoPostMapper videoPostMapper;
    private final VideoCollectionMapper videoCollectionMapper;

    // 轮播位置最多1个
    private static final int MAX_CAROUSEL = 1;
    // 网格位置最多4个
    private static final int MAX_GRID = 4;

    /**
     * 获取首页推荐视频（公开接口）
     * 返回：1个轮播 + 4个网格
     */
    public List<HomeRecommendVO> getHomeRecommendVideos(Long currentUserId) {
        List<HomeRecommendVideo> recommends = homeRecommendVideoMapper.selectAllRecommendVideos();
        return recommends.stream()
                .map(rec -> convertToVO(rec, currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * 获取指定位置的推荐列表（管理员）
     */
    public List<HomeRecommendVO> getRecommendByPosition(int positionType, Long currentUserId) {
        List<HomeRecommendVideo> recommends = homeRecommendVideoMapper.selectByPositionType(positionType);
        return recommends.stream()
                .map(rec -> convertToVO(rec, currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * 添加推荐视频（管理员）
     */
    @Transactional
    public Long addRecommend(HomeRecommendDTO dto) {
        // 验证视频存在且已通过审核
        VideoPost video = videoPostMapper.selectById(dto.getVideoPostId());
        if (video == null || video.getStatus() != VideoPost.STATUS_APPROVED) {
            throw new RuntimeException("视频不存在或未通过审核");
        }

        // 检查是否已推荐
        int exists = homeRecommendVideoMapper.checkVideoInPosition(dto.getVideoPostId(), dto.getPositionType());
        if (exists > 0) {
            throw new RuntimeException("该视频已在当前推荐位置中");
        }

        // 检查位置数量限制
        int count = homeRecommendVideoMapper.countByPositionType(dto.getPositionType());
        int maxCount = dto.getPositionType() == HomeRecommendVideo.POSITION_CAROUSEL ? MAX_CAROUSEL : MAX_GRID;
        if (count >= maxCount) {
            String positionName = dto.getPositionType() == HomeRecommendVideo.POSITION_CAROUSEL ? "轮播" : "网格";
            throw new RuntimeException(positionName + "位置已满，请先移除现有推荐");
        }

        // 设置排序值
        int sortOrder = dto.getSortOrder() != null ? dto.getSortOrder() :
                homeRecommendVideoMapper.getMaxSortOrder(dto.getPositionType()) + 1;

        // 创建推荐记录
        HomeRecommendVideo recommend = new HomeRecommendVideo();
        recommend.setVideoPostId(dto.getVideoPostId());
        recommend.setPositionType(dto.getPositionType());
        recommend.setSortOrder(sortOrder);
        recommend.setCreatedAt(LocalDateTime.now());
        recommend.setUpdatedAt(LocalDateTime.now());

        homeRecommendVideoMapper.insert(recommend);
        return recommend.getId();
    }

    /**
     * 移除推荐（管理员）
     */
    @Transactional
    public void removeRecommend(Long id) {
        HomeRecommendVideo recommend = homeRecommendVideoMapper.selectById(id);
        if (recommend == null) {
            throw new RuntimeException("推荐配置不存在");
        }
        homeRecommendVideoMapper.deleteById(id);
    }

    /**
     * 上移排序（管理员）
     */
    @Transactional
    public void moveUp(Long id) {
        HomeRecommendVideo current = homeRecommendVideoMapper.selectById(id);
        if (current == null) {
            throw new RuntimeException("推荐配置不存在");
        }

        if (current.getSortOrder() <= 1) {
            throw new RuntimeException("已经是第一位，无法上移");
        }

        // 查找前一个
        List<HomeRecommendVideo> list = homeRecommendVideoMapper.selectByPositionType(current.getPositionType());
        HomeRecommendVideo prev = null;
        for (HomeRecommendVideo item : list) {
            if (item.getSortOrder() == current.getSortOrder() - 1) {
                prev = item;
                break;
            }
        }

        if (prev != null) {
            homeRecommendVideoMapper.swapSortOrder(current.getId(), prev.getId(),
                    current.getSortOrder(), prev.getSortOrder());
        }
    }

    /**
     * 下移排序（管理员）
     */
    @Transactional
    public void moveDown(Long id) {
        HomeRecommendVideo current = homeRecommendVideoMapper.selectById(id);
        if (current == null) {
            throw new RuntimeException("推荐配置不存在");
        }

        int maxOrder = homeRecommendVideoMapper.getMaxSortOrder(current.getPositionType());
        if (current.getSortOrder() >= maxOrder) {
            throw new RuntimeException("已经是最后一位，无法下移");
        }

        // 查找后一个
        List<HomeRecommendVideo> list = homeRecommendVideoMapper.selectByPositionType(current.getPositionType());
        HomeRecommendVideo next = null;
        for (HomeRecommendVideo item : list) {
            if (item.getSortOrder() == current.getSortOrder() + 1) {
                next = item;
                break;
            }
        }

        if (next != null) {
            homeRecommendVideoMapper.swapSortOrder(current.getId(), next.getId(),
                    current.getSortOrder(), next.getSortOrder());
        }
    }

    /**
     * 搜索可推荐的视频（管理员）
     * 返回已通过审核的视频列表
     */
    public List<HomeRecommendVO> searchAvailableVideos(String keyword, int page, int size, Long currentUserId) {
        int offset = (page - 1) * size;

        // 查询已通过审核的视频
        List<VideoPost> videos = videoPostMapper.selectApprovedVideos(keyword, offset, size);

        return videos.stream()
                .map(video -> {
                    HomeRecommendVO vo = new HomeRecommendVO();
                    vo.setVideoPostId(video.getId());
                    vo.setTitle(video.getTitle());
                    vo.setCoverUrl(video.getCoverUrl());
                    vo.setViewCount(video.getViewCount());
                    vo.setCollectionCount(video.getCollectionCount());

                    // 检查是否已被推荐
                    int carouselCount = homeRecommendVideoMapper.checkVideoInPosition(
                            video.getId(), HomeRecommendVideo.POSITION_CAROUSEL);
                    int gridCount = homeRecommendVideoMapper.checkVideoInPosition(
                            video.getId(), HomeRecommendVideo.POSITION_GRID);

                    // 用 positionType 字段标识推荐状态（前端用）
                    if (carouselCount > 0) {
                        vo.setPositionType(HomeRecommendVideo.POSITION_CAROUSEL);
                    } else if (gridCount > 0) {
                        vo.setPositionType(HomeRecommendVideo.POSITION_GRID);
                    } else {
                        vo.setPositionType(0); // 未推荐
                    }

                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取未推荐的已审核视频（分页）
     * 排除已在推荐位的视频
     */
    public com.baomidou.mybatisplus.core.metadata.IPage<HomeRecommendVO> getUnrecommendedVideos(
            String keyword, int page, int size, Long currentUserId) {

        // 创建分页对象
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<VideoPost> pageParam =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);

        // 查询未推荐的已审核视频
        com.baomidou.mybatisplus.core.metadata.IPage<VideoPost> videoPage =
                videoPostMapper.selectUnrecommendedVideos(pageParam, keyword);

        // 转换为 VO
        return videoPage.convert(video -> {
            HomeRecommendVO vo = new HomeRecommendVO();
            vo.setId(video.getId());
            vo.setVideoPostId(video.getId());
            vo.setTitle(video.getTitle());
            vo.setCoverUrl(video.getCoverUrl());
            vo.setVideoUrl(video.getVideoUrl());
            vo.setDescription(video.getDescription());
            vo.setViewCount(video.getViewCount());
            vo.setCollectionCount(video.getCollectionCount());
            vo.setPositionType(0); // 未推荐

            // 检查收藏状态
            if (currentUserId != null) {
                int collected = videoCollectionMapper.checkCollected(currentUserId, video.getId());
                vo.setCollected(collected > 0);
            } else {
                vo.setCollected(false);
            }

            return vo;
        });
    }

    /**
     * 实体转VO
     */
    private HomeRecommendVO convertToVO(HomeRecommendVideo recommend, Long currentUserId) {
        HomeRecommendVO vo = new HomeRecommendVO();
        vo.setId(recommend.getId());
        vo.setVideoPostId(recommend.getVideoPostId());
        vo.setPositionType(recommend.getPositionType());
        vo.setSortOrder(recommend.getSortOrder());

        // 从关联查询结果获取视频信息
        VideoPost video = videoPostMapper.selectById(recommend.getVideoPostId());
        if (video != null) {
            vo.setTitle(video.getTitle());
            vo.setCoverUrl(video.getCoverUrl());
            vo.setVideoUrl(video.getVideoUrl());
            vo.setDescription(video.getDescription());
            vo.setViewCount(video.getViewCount());
            vo.setCollectionCount(video.getCollectionCount());

            // 检查收藏状态
            if (currentUserId != null) {
                int collected = videoCollectionMapper.checkCollected(currentUserId, video.getId());
                vo.setCollected(collected > 0);
            } else {
                vo.setCollected(false);
            }
        }

        return vo;
    }
}