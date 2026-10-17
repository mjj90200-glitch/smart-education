-- ============================================================
-- SmartEdu 演示数据脚本（demo_data.sql）
-- 视角: student001(张三, id=7) + teacher001(王老师, id=9)
-- 说明: 日期全部使用相对时间（NOW()/CURDATE() ± INTERVAL），
--       任何时候导入都是"最近发生"的新鲜数据；重复执行前会先清理
--       同 ID 数据，可安全重复导入。
-- ============================================================

USE `smartedu_platform`;

-- ---------- 0. 基础关系修正：课程归属王老师 ----------
UPDATE `courses` SET `teacher_id` = 9 WHERE `teacher_id` IS NULL OR `teacher_id` <> 9;

-- ---------- 1. 清理旧测试作业及其派生数据 ----------
DELETE FROM `ai_grade_log` WHERE `homework_id` <= 100 OR `homework_id` BETWEEN 101 AND 106;
DELETE FROM `homework_submissions` WHERE `homework_id` <= 100 OR `homework_id` BETWEEN 101 AND 106;
DELETE FROM `homework` WHERE `teacher_id` = 5 OR `id` BETWEEN 101 AND 106;

-- ---------- 2. 王老师布置的作业（覆盖 3 门课程、多种状态） ----------
INSERT INTO `homework` (`id`, `title`, `description`, `course_id`, `teacher_id`, `total_score`, `pass_score`, `start_time`, `end_time`, `auto_grade`, `status`, `created_at`) VALUES
(101, '线性表基础练习', '完成顺序表与链表的插入、删除、查找操作实现，提交源码与运行截图。', 1, 9, 100.00, 60.00, NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 3 DAY, 1, 1, NOW() - INTERVAL 20 DAY),
(102, '栈与队列应用：括号匹配与表达式求值', '使用栈实现括号匹配检查与中缀表达式求值，附测试用例。', 1, 9, 100.00, 60.00, NOW() - INTERVAL 5 DAY, NOW() + INTERVAL 5 DAY, 1, 1, NOW() - INTERVAL 5 DAY),
(103, '实验一：Spring Boot 快速入门', '搭建 Spring Boot 工程，实现用户查询 REST 接口并完成单元测试。', 2, 9, 100.00, 60.00, NOW() - INTERVAL 18 DAY, NOW() - INTERVAL 8 DAY, 0, 1, NOW() - INTERVAL 18 DAY),
(104, '实验二：MyBatis-Plus 实战', '使用 MyBatis-Plus 完成课程表的 CRUD 与分页查询。', 2, 9, 100.00, 60.00, NOW() - INTERVAL 2 DAY, NOW() + INTERVAL 6 DAY, 0, 1, NOW() - INTERVAL 2 DAY),
(105, 'SQL 查询设计课堂练习', '针对学生选课库编写多表连接与分组统计查询。', 3, 9, 50.00, 30.00, NOW() - INTERVAL 8 DAY, NOW() - INTERVAL 1 DAY, 1, 1, NOW() - INTERVAL 8 DAY),
(106, '期中复习任务清单', '按知识点清单完成复习笔记并提交思维导图。', 3, 9, 100.00, 60.00, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 8 DAY, 0, 1, NOW() - INTERVAL 1 DAY);

-- ---------- 3. 张三的作业提交（已批改 x3、待批改 x1，含一次迟交） ----------
INSERT INTO `homework_submissions` (`id`, `homework_id`, `user_id`, `student_name`, `submission_content`, `submission_type`, `score`, `comment`, `grade_status`, `submit_time`, `grade_time`, `grade_user_id`, `is_late`, `ai_score`, `ai_feedback`) VALUES
(201, 101, 7, '张三', '已完成顺序表与链表全部操作，见附件源码 MyList.java，测试全部通过。', 2, 92.00, '整体实现规范，边界处理到位；注意双向链表的前驱指针维护。', 1, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 3 DAY, 9, 0, 90.50, '代码结构清晰，测试覆盖完整，建议补充时间复杂度分析。'),
(202, 103, 7, '张三', 'Spring Boot 工程与接口实现完毕，单元测试 8 个全部通过。', 2, 88.00, '接口设计合理，异常处理可再完善。', 1, NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 8 DAY, 9, 0, 86.00, '分层清晰，建议统一响应结构与参数校验。'),
(203, 105, 7, '张三', '完成全部 6 道 SQL 题目，其中第 5 题使用窗口函数实现。', 2, 75.00, '多表连接正确，分组统计思路对；注意 NULL 值处理。', 1, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY, 9, 1, NULL, NULL),
(204, 102, 7, '张三', '括号匹配已完成，表达式求值部分还差除法优先级处理，先提交初稿。', 2, NULL, NULL, 0, NOW() - INTERVAL 1 DAY, NULL, NULL, 0, NULL, NULL);

-- ---------- 4. AI 批改日志 ----------
DELETE FROM `ai_grade_log` WHERE `id` BETWEEN 901 AND 902;
INSERT INTO `ai_grade_log` (`id`, `submission_id`, `homework_id`, `student_id`, `ai_model`, `prompt_content`, `ai_response`, `grade_result`, `grade_time`, `status`) VALUES
(901, 201, 101, 7, 'doubao-seed-2.0-pro', '请批改学生的线性表作业，从正确性、规范性、测试完整性三个维度评分。', '代码结构清晰，测试覆盖完整，建议补充时间复杂度分析。', '{"score": 90.5, "pass": true}', NOW() - INTERVAL 3 DAY, 1),
(902, 202, 103, 7, 'doubao-seed-2.0-pro', '请批改学生的 Spring Boot 实验，关注分层设计与 RESTful 规范。', '分层清晰，建议统一响应结构与参数校验。', '{"score": 86.0, "pass": true}', NOW() - INTERVAL 8 DAY, 1);

-- ---------- 5. 王老师 -> 张三 的提醒（1 条未读） ----------
DELETE FROM `teacher_reminders` WHERE `id` BETWEEN 301 AND 303;
INSERT INTO `teacher_reminders` (`id`, `course_id`, `teacher_id`, `student_id`, `title`, `content`, `read_status`, `read_time`, `created_at`) VALUES
(301, 1, 9, 7, '栈与队列作业即将截止', '《栈与队列应用：括号匹配与表达式求值》将于 5 天后截止，请尽快完成表达式求值部分。', 0, NULL, NOW() - INTERVAL 1 DAY),
(302, 2, 9, 7, '实验一成绩已发布', '《Spring Boot 快速入门》已批改完成，得分 88，请查看评语并订正。', 1, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 7 DAY),
(303, 3, 9, 7, '期中复习安排', '下周三晚自习进行数据库原理期中答疑，请提前整理问题清单。', 1, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 3 DAY);

-- ---------- 6. 张三的学习笔记（近一周） ----------
DELETE FROM `student_learning_notes` WHERE `id` BETWEEN 401 AND 405;
INSERT INTO `student_learning_notes` (`id`, `user_id`, `note_date`, `completed_summary`, `pending_summary`, `ai_tool_summary`, `reflection`, `created_at`, `updated_at`) VALUES
(401, 7, CURDATE(), '完成线性表作业订正，整理链表反转图解', '栈与队列作业的表达式求值部分', '用智学助手弄懂了前缀/中缀/后缀表达式的区别', '基础操作已经熟练，复杂应用还要多练。', NOW(), NOW()),
(402, 7, CURDATE() - INTERVAL 1 DAY, '看完 Java 集合框架视频 3 集', 'MyBatis-Plus 分页插件实践', '让 AI 出了 10 道集合框架自测题，答对 8 道', '和 AI 对着练效率很高。', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY),
(403, 7, CURDATE() - INTERVAL 2 DAY, '数据库连接查询练习 6 题', '窗口函数还没吃透', '让 AI 把慢 SQL 改写成了索引友好的写法', 'SQL 要多写才有手感。', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY),
(404, 7, CURDATE() - INTERVAL 4 DAY, 'Spring Boot 工程搭建并跑通单元测试', '统一异常处理还没做', 'AI 帮忙排查了 Bean 循环依赖', '踩坑也是学习的一部分。', NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 4 DAY),
(405, 7, CURDATE() - INTERVAL 6 DAY, '二叉树三种遍历手写实现', '层序遍历的队列写法', '让 AI 出了 5 道遍历变式题', '递归思路清晰了。', NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 6 DAY);

-- ---------- 7. 问答广场（学生提问 + 教师解答 + 点赞） ----------
DELETE FROM `question_reply` WHERE `id` BETWEEN 601 AND 603;
DELETE FROM `question_topic` WHERE `id` BETWEEN 501 AND 503;
DELETE FROM `question_like` WHERE `user_id` = 7;
INSERT INTO `question_topic` (`id`, `user_id`, `title`, `content`, `category`, `view_count`, `reply_count`, `like_count`, `status`, `is_solved`, `accepted_reply_id`, `created_at`, `updated_at`) VALUES
(501, 7, '二叉树的层序遍历用什么数据结构实现？', '只知道递归写法，层序遍历是不是要借助队列？求思路。', 'QUESTION', 45, 2, 3, 1, 1, 601, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 2 DAY),
(502, 7, 'Spring Boot 依赖注入 @Autowired 和 @Resource 怎么选？', '两种注入方式有什么区别？项目里应该统一用哪个？', 'DISCUSSION', 62, 1, 5, 1, 1, 603, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 4 DAY),
(503, 9, '数据结构期中复习重点提示', '本次期中重点：线性表、栈与队列、二叉树遍历。请结合作业错题复习。', 'DISCUSSION', 120, 0, 12, 1, 0, NULL, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY);
INSERT INTO `question_reply` (`id`, `topic_id`, `user_id`, `content`, `user_role`, `user_name`, `like_count`, `is_accepted`, `created_at`) VALUES
(601, 501, 9, '层序遍历借助队列：根节点入队，每次出队一个节点就把它的左右子节点依次入队，队列为空时结束。', 'TEACHER', '王老师', 6, 1, NOW() - INTERVAL 2 DAY),
(602, 501, 7, '明白了！出队时把子节点入队就能保证按层访问，谢谢老师！', 'STUDENT', '张三', 1, 0, NOW() - INTERVAL 2 DAY),
(603, 502, 9, '功能上两者几乎等价。@Resource 按名称注入，@Autowired 按类型注入，团队内统一一种即可，推荐 @Autowired + @Qualifier。', 'TEACHER', '王老师', 4, 1, NOW() - INTERVAL 4 DAY);
INSERT INTO `question_like` (`user_id`, `target_type`, `target_id`, `created_at`) VALUES
(7, 1, 503, NOW() - INTERVAL 1 DAY),
(7, 2, 601, NOW() - INTERVAL 2 DAY);

-- ---------- 8. 视频：张三投稿 + 收藏 ----------
DELETE FROM `video_post` WHERE `id` BETWEEN 701 AND 702;
DELETE FROM `video_collection` WHERE `user_id` = 7;
INSERT INTO `video_post` (`id`, `user_id`, `title`, `cover_url`, `video_url`, `description`, `status`, `view_count`, `collection_count`, `created_at`) VALUES
(701, 7, '张三的数据结构学习笔记：手写链表反转', '', 'https://www.bilibili.com/video/BV1xK411S7on', '链表反转与双指针技巧讲解，配合《线性表基础练习》作业整理。', 1, 156, 4, NOW() - INTERVAL 6 DAY),
(702, 7, 'Spring Boot 入门踩坑记录', '', 'https://www.bilibili.com/video/BV1nZ4y1B7Up', '实验一完成过程中遇到的三个坑：端口冲突、循环依赖、时区配置。', 1, 89, 2, NOW() - INTERVAL 3 DAY);
INSERT INTO `video_collection` (`user_id`, `video_id`, `created_at`) VALUES
(7, 1, NOW() - INTERVAL 5 DAY),
(7, 2, NOW() - INTERVAL 4 DAY),
(7, 3, NOW() - INTERVAL 2 DAY);
UPDATE `video_post` SET `collection_count` = `collection_count` + 1 WHERE `id` IN (1, 2, 3);
-- 张三的投稿进入首页推荐列表
DELETE FROM `home_recommend_video` WHERE `video_post_id` IN (701, 702);
INSERT INTO `home_recommend_video` (`video_post_id`, `position_type`, `sort_order`) VALUES (701, 2, 5);

-- ---------- 9. 学情分析报告与知识点掌握度 ----------
DELETE FROM `learning_analytics` WHERE `user_id` = 7;
DELETE FROM `learning_analytics_detail` WHERE `user_id` = 7;
INSERT INTO `learning_analytics` (`user_id`, `course_id`, `report_type`, `report_period`, `study_time_hours`, `completed_tasks`, `correct_rate`, `weak_points`, `suggestions`, `rank_in_class`, `total_in_class`, `generated_at`) VALUES
(7, 1, 'WEEKLY', '本周', 6.50, 3, 82.00, '二叉树层序遍历,表达式求值', '栈的应用要多做变式题，建议每天 2 道队列题目。', 2, 18, NOW() - INTERVAL 1 DAY),
(7, 2, 'WEEKLY', '本周', 4.00, 2, 88.00, 'MyBatis 分页插件', '实验代码质量不错，建议学习统一异常处理。', 3, 18, NOW() - INTERVAL 1 DAY),
(7, 1, 'MONTHLY', '十月', 22.50, 9, 80.50, 'AVL 平衡旋转,哈希冲突处理', '整体稳步提升，弱项集中在树与哈希部分。', 4, 18, NOW() - INTERVAL 2 DAY),
(7, 2, 'MONTHLY', '十月', 15.00, 6, 85.00, '事务传播机制', '保持节奏，期中前完成实验二。', 5, 18, NOW() - INTERVAL 2 DAY);
INSERT INTO `learning_analytics_detail` (`user_id`, `course_id`, `knowledge_point_id`, `mastery_level`, `practice_count`, `correct_count`, `avg_score`, `last_practice_time`, `weak_point`) VALUES
(7, 1, 1, 92.00, 12, 11, 91.00, NOW() - INTERVAL 2 DAY, 0),
(7, 1, 2, 85.00, 9, 8, 86.00, NOW() - INTERVAL 2 DAY, 0),
(7, 1, 3, 62.00, 6, 4, 65.00, NOW() - INTERVAL 1 DAY, 1),
(7, 1, 4, 58.00, 5, 3, 60.00, NOW() - INTERVAL 1 DAY, 1),
(7, 1, 5, 78.00, 7, 6, 80.00, NOW() - INTERVAL 3 DAY, 0);

-- ---------- 10. 学习计划 ----------
DELETE FROM `learning_plans` WHERE `user_id` = 7;
INSERT INTO `learning_plans` (`user_id`, `course_id`, `title`, `description`, `goal_type`, `target_score`, `start_date`, `end_date`, `daily_study_hours`, `knowledge_point_ids`, `plan_items`, `progress`, `status`) VALUES
(7, 1, '期中冲刺：数据结构通关计划', '针对薄弱知识点集中训练，目标期中 90 分。', 'EXAM', 90.00, CURDATE() - INTERVAL 5 DAY, CURDATE() + INTERVAL 10 DAY, 2.00, '[3,4]', '[{"task":"每天 3 道队列题目","done":true},{"task":"二叉树遍历专项","done":false}]', 45.00, 1),
(7, 2, 'Java 实验二攻坚计划', '完成 MyBatis-Plus 实验并整理常见坑。', 'HOMEWORK', 95.00, CURDATE() - INTERVAL 2 DAY, CURDATE() + INTERVAL 5 DAY, 1.50, '[]', '[{"task":"CRUD 与分页","done":true},{"task":"单元测试补全","done":false}]', 60.00, 1);

-- ---------- 11. 错题本 ----------
DELETE FROM `error_logs` WHERE `user_id` = 7;
INSERT INTO `error_logs` (`user_id`, `question_id`, `course_id`, `knowledge_point_ids`, `user_answer`, `correct_answer`, `error_type`, `error_reason`, `score_obtained`, `score_total`, `source_type`, `source_id`, `review_status`, `review_count`, `last_review_time`, `next_review_time`, `created_at`) VALUES
(7, 0, 1, '3', '用两个栈交替存储中间结果', '用一个栈保存运算符，注意出栈时机按优先级比较', 'CONCEPT', '混淆了中缀转后缀时运算符的出栈时机', 4.00, 10.00, 'HOMEWORK', 102, 0, 0, NULL, NOW() + INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY),
(7, 0, 1, '4', '层序遍历用栈实现', '层序遍历用队列，先入先出才能保证按层访问', 'CONCEPT', '底层数据结构选择错误', 0.00, 5.00, 'HOMEWORK', 101, 1, 2, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 2 DAY, NOW() - INTERVAL 3 DAY),
(7, 0, 2, '6', '接口里直接 try/catch 返回 Result', '应使用全局异常处理器统一包装返回', 'CARELESS', '遗漏统一响应包装', 8.00, 10.00, 'HOMEWORK', 103, 0, 1, NOW() - INTERVAL 2 DAY, NOW() + INTERVAL 1 DAY, NOW() - INTERVAL 6 DAY);

-- ---------- 12. AI 使用记录（看板"AI 使用"与时间线） ----------
DELETE FROM `student_ai_usage_logs` WHERE `user_id` = 7;
INSERT INTO `student_ai_usage_logs` (`user_id`, `feature`, `model_name`, `action_summary`, `prompt_excerpt`, `result_excerpt`, `created_at`) VALUES
(7, 'AI 问答', 'doubao-seed-2.0-pro', '讲解二叉树层序遍历思路', '二叉树的层序遍历用什么数据结构实现？', '借助队列：根节点入队，出队时将子节点入队……', NOW() - INTERVAL 3 DAY),
(7, '作业解析', 'doubao-seed-2.0-pro', '生成表达式求值解题思路', '中缀表达式转后缀的规则是什么？', '运算符出栈时机按优先级比较……', NOW() - INTERVAL 2 DAY),
(7, 'AI 问答', 'doubao-seed-2.0-pro', '排查 Spring Bean 循环依赖', '启动报错 BeanCurrentlyInCreationException', '使用 @Lazy 打破循环依赖……', NOW() - INTERVAL 4 DAY),
(7, '题目推荐', 'doubao-seed-2.0-pro', '根据薄弱点推荐 5 道练习', '我的薄弱点是二叉树与哈希', '已生成 5 道变式训练题……', NOW() - INTERVAL 1 DAY),
(7, 'AI 问答', 'doubao-seed-2.0-pro', '优化多表连接慢 SQL', '多表连接查询很慢怎么优化？', '为连接字段建立索引并避免 SELECT *……', NOW() - INTERVAL 2 DAY);

-- ---------- 13. 课堂教学评估（王老师的两次课） ----------
DELETE FROM `class_evaluation_records` WHERE `session_id` BETWEEN 801 AND 802;
DELETE FROM `class_evaluation_sessions` WHERE `id` BETWEEN 801 AND 802;
INSERT INTO `class_evaluation_sessions` (`id`, `teacher_id`, `course_id`, `week_number`, `session_date`, `session_title`, `teaching_topic`, `source_file_name`, `start_time`, `end_time`, `total_students`, `present_count`, `absent_count`, `leave_count`, `late_count`, `answer_count`, `question_count`, `interaction_count`, `high_interaction_count`, `average_participation_score`, `analysis_summary`, `created_at`) VALUES
(801, 9, 1, 6, CURDATE() - INTERVAL 7 DAY, '第 6 讲：栈与队列', '栈的应用：括号匹配与表达式求值', '第6周课堂互动数据.xlsx', '08:00:00', '09:40:00', 40, 37, 1, 2, 0, 26, 9, 84, 31, 82.50, '课堂互动活跃，答题正确率 81%；学生对表达式求值的优先级处理普遍薄弱，建议下次课增加 2 道变式练习。', NOW() - INTERVAL 7 DAY),
(802, 9, 1, 7, CURDATE() - INTERVAL 2 DAY, '第 7 讲：二叉树基础', '二叉树的存储与三种遍历', '第7周课堂互动数据.xlsx', '08:00:00', '09:40:00', 40, 39, 0, 1, 0, 31, 12, 96, 40, 86.30, '到课率 97.5%，互动率创近期新高；递归遍历掌握良好，层序遍历仍需巩固。', NOW() - INTERVAL 2 DAY);
INSERT INTO `class_evaluation_records` (`session_id`, `student_id`, `student_no`, `student_name`, `class_name`, `performance_level`, `attendance_status`, `answer_count`, `question_count`, `participation_score`, `teacher_comment`, `absent_reason`, `created_at`) VALUES
(801, 7, '20230101', '张三', '计算机 2301 班', 'A', '出勤', 3, 1, 92, '回答积极，思路清晰，是本节课互动最多的学生之一。', NULL, NOW() - INTERVAL 7 DAY),
(801, 8, '20230102', '李四', '计算机 2301 班', 'B', '出勤', 1, 0, 76, '能跟上节奏，主动发言偏少。', NULL, NOW() - INTERVAL 7 DAY),
(802, 7, '20230101', '张三', '计算机 2301 班', 'A', '出勤', 4, 2, 95, '层序遍历回答正确，进步明显。', NULL, NOW() - INTERVAL 2 DAY),
(802, 8, '20230102', '李四', '计算机 2301 班', 'A-', '迟到', 2, 1, 85, '迟到 5 分钟，其余表现良好。', NULL, NOW() - INTERVAL 2 DAY);

-- ---------- 14. 学习资源（王老师上传） ----------
DELETE FROM `learning_resources` WHERE `upload_user_id` = 9;
INSERT INTO `learning_resources` (`title`, `description`, `resource_type`, `url`, `course_id`, `tags`, `difficulty_level`, `view_count`, `download_count`, `rating`, `upload_user_id`, `status`) VALUES
('二叉树遍历可视化讲义', '前序/中序/后序/层序遍历的图解与动演示例。', 'DOCUMENT', 'https://example.com/docs/tree-traversal.pdf', 1, '二叉树,遍历', 2, 320, 96, 4.8, 9, 1),
('栈与队列经典 20 题', '括号匹配、单调栈、循环队列等高频题型解析。', 'EXERCISE', 'https://example.com/docs/stack-queue-20.pdf', 1, '栈,队列', 3, 265, 81, 4.6, 9, 1),
('Spring Boot 官方入门指南（中文导读）', '从工程搭建到 Web 开发的官方文档导读。', 'LINK', 'https://spring.io/guides/gs/spring-boot', 2, 'Spring Boot,入门', 1, 412, 0, 4.9, 9, 1),
('MySQL 索引与查询优化笔记', '结合课堂例题整理的索引原理与优化清单。', 'DOCUMENT', 'https://example.com/docs/mysql-index.pdf', 3, 'MySQL,索引', 3, 198, 55, 4.5, 9, 1);

SELECT '演示数据导入完成' AS message;
