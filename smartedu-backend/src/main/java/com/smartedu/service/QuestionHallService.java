package com.smartedu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartedu.common.exception.BusinessException;
import com.smartedu.entity.QuestionLike;
import com.smartedu.entity.QuestionReply;
import com.smartedu.entity.QuestionTopic;
import com.smartedu.entity.User;
import com.smartedu.mapper.QuestionLikeMapper;
import com.smartedu.mapper.QuestionReplyMapper;
import com.smartedu.mapper.QuestionTopicMapper;
import com.smartedu.mapper.UserMapper;
import com.smartedu.vo.ReplyVO;
import com.smartedu.vo.TopicDetailVO;
import com.smartedu.vo.TopicListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 答疑大厅服务
 * @author SmartEdu Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionHallService extends ServiceImpl<QuestionTopicMapper, QuestionTopic> {

    private final QuestionTopicMapper topicMapper;
    private final QuestionReplyMapper replyMapper;
    private final QuestionLikeMapper likeMapper;
    private final UserMapper userMapper;

    // ==================== 主帖相关 ====================

    /**
     * 分页查询帖子列表
     */
    public Page<TopicListVO> getTopicList(String category, String keyword, Integer status,
                                           Integer pageNum, Integer pageSize, Long currentUserId) {
        Page<QuestionTopic> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<QuestionTopic> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(category != null && !category.isEmpty(), QuestionTopic::getCategory, category);
        wrapper.eq(status != null, QuestionTopic::getStatus, status);
        wrapper.and(keyword != null && !keyword.isEmpty(), w ->
            w.like(QuestionTopic::getTitle, keyword)
             .or()
             .like(QuestionTopic::getContent, keyword)
        );
        wrapper.orderByDesc(QuestionTopic::getStatus); // 置顶优先
        wrapper.orderByDesc(QuestionTopic::getCreatedAt);

        Page<QuestionTopic> topicPage = topicMapper.selectPage(page, wrapper);

        // 转换为 VO
        Page<TopicListVO> voPage = new Page<>(pageNum, pageSize, topicPage.getTotal());
        List<TopicListVO> voList = topicPage.getRecords().stream()
            .map(topic -> convertToListVO(topic, currentUserId))
            .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    /**
     * 获取帖子详情
     */
    @Transactional
    public TopicDetailVO getTopicDetail(Long id, Long currentUserId) {
        QuestionTopic topic = topicMapper.selectById(id);
        if (topic == null) {
            throw new BusinessException("帖子不存在");
        }

        // 增加浏览次数
        topicMapper.incrementViewCount(id);

        // 获取发帖人信息
        User author = userMapper.selectById(topic.getUserId());

        // 获取回复列表
        List<ReplyVO> replies = getRepliesByTopicId(id, currentUserId);

        // 转换为 VO
        TopicDetailVO vo = new TopicDetailVO();
        vo.setId(topic.getId());
        vo.setTitle(topic.getTitle());
        vo.setContent(topic.getContent());
        vo.setCategory(topic.getCategory());
        vo.setUserId(topic.getUserId());
        vo.setUserName(author != null ? author.getRealName() : "未知用户");
        vo.setUserAvatar(author != null ? author.getAvatar() : null);
        vo.setUserRole(author != null ? author.getRole() : "STUDENT");
        vo.setViewCount(topic.getViewCount() + 1); // 加上本次浏览
        vo.setReplyCount(topic.getReplyCount());
        vo.setLikeCount(topic.getLikeCount());
        vo.setIsSolved(topic.getIsSolved());
        vo.setStatus(topic.getStatus());
        vo.setAttachmentUrl(topic.getAttachmentUrl());
        vo.setAcceptedReplyId(topic.getAcceptedReplyId());
        vo.setCreatedAt(topic.getCreatedAt());
        vo.setUpdatedAt(topic.getUpdatedAt());
        vo.setIsLiked(checkLiked(currentUserId, 1, id));
        vo.setReplies(replies);

        return vo;
    }

    /**
     * 创建帖子
     */
    @Transactional
    public Long createTopic(Long userId, String title, String content, String category, String attachmentUrl) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        QuestionTopic topic = new QuestionTopic();
        topic.setUserId(userId);
        topic.setTitle(title);
        topic.setContent(content);
        topic.setCategory(category != null ? category : "QUESTION");
        topic.setViewCount(0);
        topic.setReplyCount(0);
        topic.setLikeCount(0);
        topic.setStatus(1);
        topic.setIsSolved(0);
        topic.setAttachmentUrl(attachmentUrl);

        topicMapper.insert(topic);
        return topic.getId();
    }

    /**
     * 删除帖子（仅作者可删除）
     */
    @Transactional
    public void deleteTopic(Long topicId, Long userId) {
        QuestionTopic topic = topicMapper.selectById(topicId);
        if (topic == null) {
            throw new BusinessException("帖子不存在");
        }
        if (!topic.getUserId().equals(userId)) {
            throw new BusinessException("无权删除此帖子");
        }
        topicMapper.deleteById(topicId);
    }

    /**
     * 管理员删除帖子（逻辑删除）
     * 双重权限检查：Controller 层已通过 @PreAuthorize 拦截，此处再次确认
     * 注：@TableLogic 注解会自动将 deleteById 转换为逻辑删除
     */
    @Transactional
    public void adminDeleteTopic(Long topicId, Long adminId) {
        // 1. 验证帖子是否存在
        QuestionTopic topic = topicMapper.selectById(topicId);
        if (topic == null) {
            throw new BusinessException("帖子不存在");
        }

        // 2. 双重权限检查：确认操作者是管理员
        User admin = userMapper.selectById(adminId);
        if (admin == null || !"ADMIN".equals(admin.getRole())) {
            log.warn("[安全警告] 非管理员用户 ID: {} 尝试调用管理员删除接口", adminId);
            throw new BusinessException("无权执行此操作");
        }

        // 3. 执行逻辑删除（@TableLogic 会自动转为 UPDATE ... SET deleted = 1）
        topicMapper.deleteById(topicId);

        log.info("[审计日志] 管理员 {} ({}) 成功删除帖子 ID: {}", admin.getRealName(), adminId, topicId);
    }

    /**
     * 管理员删除回复（逻辑删除）
     * 双重权限检查：Controller 层已通过 @PreAuthorize 拦截，此处再次确认
     * 注：@TableLogic 注解会自动将 deleteById 转换为逻辑删除
     */
    @Transactional
    public void adminDeleteReply(Long replyId, Long adminId) {
        // 1. 验证回复是否存在
        QuestionReply reply = replyMapper.selectById(replyId);
        if (reply == null) {
            throw new BusinessException("回复不存在");
        }

        // 2. 双重权限检查：确认操作者是管理员
        User admin = userMapper.selectById(adminId);
        if (admin == null || !"ADMIN".equals(admin.getRole())) {
            log.warn("[安全警告] 非管理员用户 ID: {} 尝试调用管理员删除接口", adminId);
            throw new BusinessException("无权执行此操作");
        }

        // 3. 执行逻辑删除（@TableLogic 会自动转为 UPDATE ... SET deleted = 1）
        replyMapper.deleteById(replyId);

        // 4. 更新帖子的回复计数
        topicMapper.decrementReplyCount(reply.getTopicId());

        log.info("[审计日志] 管理员 {} ({}) 成功删除回复 ID: {}, 所属帖子 ID: {}",
                admin.getRealName(), adminId, replyId, reply.getTopicId());
    }

    // ==================== 回复相关 ====================

    /**
     * 创建回复
     */
    @Transactional
    public Long createReply(Long topicId, Long userId, String content, Long parentReplyId, String attachmentUrl) {
        QuestionTopic topic = topicMapper.selectById(topicId);
        if (topic == null) {
            throw new BusinessException("帖子不存在");
        }
        if (topic.getStatus() == 0) {
            throw new BusinessException("帖子已关闭，无法回复");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        QuestionReply reply = new QuestionReply();
        reply.setTopicId(topicId);
        reply.setUserId(userId);
        reply.setContent(content);
        reply.setParentReplyId(parentReplyId);
        reply.setUserRole(user.getRole());
        reply.setUserName(user.getRealName());
        reply.setUserAvatar(user.getAvatar());
        reply.setLikeCount(0);
        reply.setIsAccepted(0);
        reply.setStatus(1);
        reply.setAttachmentUrl(attachmentUrl);

        replyMapper.insert(reply);
        topicMapper.incrementReplyCount(topicId);

        return reply.getId();
    }

    /**
     * 删除回复（仅作者可删除）
     */
    @Transactional
    public void deleteReply(Long replyId, Long userId) {
        QuestionReply reply = replyMapper.selectById(replyId);
        if (reply == null) {
            throw new BusinessException("回复不存在");
        }
        if (!reply.getUserId().equals(userId)) {
            throw new BusinessException("无权删除此回复");
        }

        replyMapper.deleteById(replyId);
        topicMapper.decrementReplyCount(reply.getTopicId());
    }

    /**
     * 采纳答案（仅帖子作者可采纳）
     */
    @Transactional
    public void acceptReply(Long topicId, Long replyId, Long userId) {
        QuestionTopic topic = topicMapper.selectById(topicId);
        if (topic == null) {
            throw new BusinessException("帖子不存在");
        }
        if (!topic.getUserId().equals(userId)) {
            throw new BusinessException("只有帖子作者才能采纳答案");
        }

        QuestionReply reply = replyMapper.selectById(replyId);
        if (reply == null || !reply.getTopicId().equals(topicId)) {
            throw new BusinessException("回复不存在");
        }

        // 取消之前的采纳
        replyMapper.clearAcceptedByTopicId(topicId);

        // 设置新的采纳
        reply.setIsAccepted(1);
        replyMapper.updateById(reply);

        // 更新帖子状态
        topic.setIsSolved(1);
        topic.setAcceptedReplyId(replyId);
        topicMapper.updateById(topic);
    }

    // ==================== 点赞相关 ====================

    /**
     * 点赞主帖
     */
    @Transactional
    public boolean likeTopic(Long topicId, Long userId) {
        if (checkLiked(userId, 1, topicId)) {
            // 已点赞，取消点赞
            likeMapper.delete(new LambdaQueryWrapper<QuestionLike>()
                .eq(QuestionLike::getUserId, userId)
                .eq(QuestionLike::getTargetType, 1)
                .eq(QuestionLike::getTargetId, topicId));
            topicMapper.decrementLikeCount(topicId);
            return false;
        } else {
            // 未点赞，添加点赞
            QuestionLike like = new QuestionLike();
            like.setUserId(userId);
            like.setTargetType(1);
            like.setTargetId(topicId);
            likeMapper.insert(like);
            topicMapper.incrementLikeCount(topicId);
            return true;
        }
    }

    /**
     * 点赞回复
     */
    @Transactional
    public boolean likeReply(Long replyId, Long userId) {
        if (checkLiked(userId, 2, replyId)) {
            // 已点赞，取消点赞
            likeMapper.delete(new LambdaQueryWrapper<QuestionLike>()
                .eq(QuestionLike::getUserId, userId)
                .eq(QuestionLike::getTargetType, 2)
                .eq(QuestionLike::getTargetId, replyId));
            replyMapper.decrementLikeCount(replyId);
            return false;
        } else {
            // 未点赞，添加点赞
            QuestionLike like = new QuestionLike();
            like.setUserId(userId);
            like.setTargetType(2);
            like.setTargetId(replyId);
            likeMapper.insert(like);
            replyMapper.incrementLikeCount(replyId);
            return true;
        }
    }

    // ==================== 私有方法 ====================

    private TopicListVO convertToListVO(QuestionTopic topic, Long currentUserId) {
        User author = userMapper.selectById(topic.getUserId());

        TopicListVO vo = new TopicListVO();
        vo.setId(topic.getId());
        vo.setTitle(topic.getTitle());
        vo.setContentPreview(truncate(topic.getContent(), 100));
        vo.setCategory(topic.getCategory());
        vo.setUserId(topic.getUserId());
        vo.setUserName(author != null ? author.getRealName() : "未知用户");
        vo.setUserAvatar(author != null ? author.getAvatar() : null);
        vo.setUserRole(author != null ? author.getRole() : "STUDENT");
        vo.setViewCount(topic.getViewCount());
        vo.setReplyCount(topic.getReplyCount());
        vo.setLikeCount(topic.getLikeCount());
        vo.setIsSolved(topic.getIsSolved());
        vo.setStatus(topic.getStatus());
        vo.setCreatedAt(topic.getCreatedAt());
        vo.setIsLiked(checkLiked(currentUserId, 1, topic.getId()));

        return vo;
    }

    private List<ReplyVO> getRepliesByTopicId(Long topicId, Long currentUserId) {
        LambdaQueryWrapper<QuestionReply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionReply::getTopicId, topicId);
        wrapper.eq(QuestionReply::getStatus, 1);
        wrapper.orderByAsc(QuestionReply::getCreatedAt);

        List<QuestionReply> replies = replyMapper.selectList(wrapper);

        // 构建树形结构
        Map<Long, ReplyVO> replyMap = new HashMap<>();
        List<ReplyVO> rootReplies = new ArrayList<>();

        for (QuestionReply reply : replies) {
            ReplyVO vo = convertToReplyVO(reply, currentUserId);
            replyMap.put(vo.getId(), vo);
        }

        for (ReplyVO vo : replyMap.values()) {
            if (vo.getParentReplyId() == null) {
                rootReplies.add(vo);
            } else {
                ReplyVO parent = replyMap.get(vo.getParentReplyId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(vo);
                }
            }
        }

        return rootReplies;
    }

    private ReplyVO convertToReplyVO(QuestionReply reply, Long currentUserId) {
        ReplyVO vo = new ReplyVO();
        vo.setId(reply.getId());
        vo.setTopicId(reply.getTopicId());
        vo.setUserId(reply.getUserId());
        vo.setUserName(reply.getUserName());
        vo.setUserAvatar(reply.getUserAvatar());
        vo.setUserRole(reply.getUserRole());
        vo.setIsTeacher("TEACHER".equals(reply.getUserRole()));
        vo.setContent(reply.getContent());
        vo.setLikeCount(reply.getLikeCount());
        vo.setIsAccepted(reply.getIsAccepted());
        vo.setParentReplyId(reply.getParentReplyId());
        vo.setAttachmentUrl(reply.getAttachmentUrl());
        vo.setCreatedAt(reply.getCreatedAt());
        vo.setIsLiked(checkLiked(currentUserId, 2, reply.getId()));

        return vo;
    }

    private boolean checkLiked(Long userId, Integer targetType, Long targetId) {
        if (userId == null) {
            return false;
        }
        return likeMapper.checkLiked(userId, targetType, targetId) > 0;
    }

    private String truncate(String str, int maxLength) {
        if (str == null) {
            return "";
        }
        return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }
}