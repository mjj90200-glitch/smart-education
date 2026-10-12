package com.smartedu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartedu.entity.QaItem;
import com.smartedu.entity.User;
import com.smartedu.entity.Course;
import com.smartedu.mapper.QaItemMapper;
import com.smartedu.mapper.UserMapper;
import com.smartedu.mapper.CourseMapper;
import com.smartedu.vo.QADetailVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 问答服务类
 * @author SmartEdu Team
 */
@Service
public class QaItemService {

    private final QaItemMapper qaItemMapper;
    private final UserMapper userMapper;
    private final CourseMapper courseMapper;

    public QaItemService(QaItemMapper qaItemMapper, UserMapper userMapper, CourseMapper courseMapper) {
        this.qaItemMapper = qaItemMapper;
        this.userMapper = userMapper;
        this.courseMapper = courseMapper;
    }

    /**
     * 提问
     * @param qaItem 问题信息
     * @return 问题 ID
     */
    public Long askQuestion(QaItem qaItem) {
        qaItemMapper.insert(qaItem);
        return qaItem.getId();
    }

    /**
     * 获取问答列表（分页）
     * @param courseId 课程 ID（可选）
     * @param status 状态（可选）
     * @param category 分类（可选）
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 问答列表
     */
    public Page<QADetailVO> getQAList(Long courseId, Integer status, String category, Integer pageNum, Integer pageSize) {
        Page<QaItem> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<QaItem> queryWrapper = new LambdaQueryWrapper<>();

        // 添加查询条件
        if (courseId != null) {
            queryWrapper.eq(QaItem::getCourseId, courseId);
        }
        if (status != null) {
            queryWrapper.eq(QaItem::getStatus, status);
        }
        if (category != null) {
            queryWrapper.eq(QaItem::getCategory, category);
        }

        // 按创建时间倒序
        queryWrapper.orderByDesc(QaItem::getCreatedAt);

        Page<QaItem> qaPage = qaItemMapper.selectPage(page, queryWrapper);

        // 转换为 VO
        Page<QADetailVO> resultPage = new Page<>(pageNum, pageSize);
        resultPage.setTotal(qaPage.getTotal());
        resultPage.setRecords(qaPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList()));
        return resultPage;
    }

    /**
     * 获取问答详情
     * @param id 问答 ID
     * @return 问答详情
     */
    public QADetailVO getQADetail(Long id) {
        QaItem qaItem = qaItemMapper.selectById(id);
        if (qaItem == null) {
            return null;
        }

        // 增加浏览次数
        qaItem.setViewCount(qaItem.getViewCount() != null ? qaItem.getViewCount() + 1 : 1);
        qaItemMapper.updateById(qaItem);

        return convertToVO(qaItem);
    }

    /**
     * 回答问题
     * @param id 问题 ID
     * @param answerUserId 回答者 ID
     * @param content 回答内容
     * @param answerType 回答类型
     * @return 是否成功
     */
    public boolean answerQuestion(Long id, Long answerUserId, String content, String answerType) {
        QaItem qaItem = qaItemMapper.selectById(id);
        if (qaItem == null) {
            return false;
        }

        qaItem.setAnswer(content);
        qaItem.setAnswerUserId(answerUserId);
        qaItem.setAnswerType(answerType);
        qaItem.setStatus(1); // 已回答
        qaItemMapper.updateById(qaItem);

        return true;
    }

    /**
     * 采纳答案
     * @param id 问题 ID
     * @return 是否成功
     */
    public boolean adoptAnswer(Long id) {
        QaItem qaItem = qaItemMapper.selectById(id);
        if (qaItem == null || qaItem.getAnswer() == null) {
            return false;
        }

        qaItem.setStatus(2); // 已采纳
        qaItemMapper.updateById(qaItem);

        return true;
    }

    /**
     * 获取我的提问列表
     * @param userId 用户 ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 问答列表
     */
    public Page<QADetailVO> getMyQuestions(Long userId, Integer pageNum, Integer pageSize) {
        Page<QaItem> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<QaItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(QaItem::getUserId, userId);
        queryWrapper.orderByDesc(QaItem::getCreatedAt);

        Page<QaItem> qaPage = qaItemMapper.selectPage(page, queryWrapper);

        Page<QADetailVO> resultPage = new Page<>(pageNum, pageSize);
        resultPage.setTotal(qaPage.getTotal());
        resultPage.setRecords(qaPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList()));
        return resultPage;
    }

    /**
     * 获取热门问题
     * @param courseId 课程 ID（可选）
     * @param limit 数量限制
     * @return 热门问题列表
     */
    public List<QADetailVO> getHotQuestions(Long courseId, Integer limit) {
        LambdaQueryWrapper<QaItem> queryWrapper = new LambdaQueryWrapper<>();

        if (courseId != null) {
            queryWrapper.eq(QaItem::getCourseId, courseId);
        }

        // 按浏览量和点赞数排序
        queryWrapper.orderByDesc(QaItem::getViewCount)
                    .orderByDesc(QaItem::getLikeCount)
                    .last("LIMIT " + limit);

        List<QaItem> qaItems = qaItemMapper.selectList(queryWrapper);

        return qaItems.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 点赞问题
     * @param id 问题 ID
     * @return 是否成功
     */
    public boolean likeQuestion(Long id) {
        QaItem qaItem = qaItemMapper.selectById(id);
        if (qaItem == null) {
            return false;
        }

        qaItem.setLikeCount(qaItem.getLikeCount() != null ? qaItem.getLikeCount() + 1 : 1);
        qaItemMapper.updateById(qaItem);

        return true;
    }

    /**
     * 转换为 VO
     */
    private QADetailVO convertToVO(QaItem qaItem) {
        QADetailVO vo = new QADetailVO();
        vo.setId(qaItem.getId());
        vo.setUserId(qaItem.getUserId());
        vo.setCourseId(qaItem.getCourseId());
        vo.setKnowledgePointId(qaItem.getKnowledgePointId());
        vo.setTitle(qaItem.getTitle());
        vo.setContent(qaItem.getContent());
        vo.setAnswer(qaItem.getAnswer());
        vo.setAnswerUserId(qaItem.getAnswerUserId());
        vo.setAnswerType(qaItem.getAnswerType());
        vo.setCategory(qaItem.getCategory());
        vo.setStatus(qaItem.getStatus());
        vo.setViewCount(qaItem.getViewCount());
        vo.setLikeCount(qaItem.getLikeCount());
        vo.setIsAnonymous(qaItem.getIsAnonymous() != null && qaItem.getIsAnonymous() == 1);
        vo.setCreatedAt(qaItem.getCreatedAt());
        vo.setUpdatedAt(qaItem.getUpdatedAt());

        // 查询提问者信息
        if (qaItem.getUserId() != null) {
            User user = userMapper.selectById(qaItem.getUserId());
            if (user != null) {
                vo.setUserName(user.getRealName());
                vo.setUserAvatar(user.getAvatar());
            }
        }

        // 查询回答者信息
        if (qaItem.getAnswerUserId() != null && qaItem.getAnswerUserId() > 0) {
            User answerUser = userMapper.selectById(qaItem.getAnswerUserId());
            if (answerUser != null) {
                vo.setAnswerUserName(answerUser.getRealName());
                vo.setAnswerUserAvatar(answerUser.getAvatar());
            }
        }

        // 查询课程信息
        if (qaItem.getCourseId() != null) {
            Course course = courseMapper.selectById(qaItem.getCourseId());
            if (course != null) {
                vo.setCourseName(course.getCourseName());
            }
        }

        return vo;
    }
}
