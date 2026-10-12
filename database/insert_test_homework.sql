-- ============================================================
-- SmartEdu-Platform 插入测试作业数据
-- ============================================================

USE `smartedu_platform`;

-- 插入测试作业（假设课程 ID=1，教师 ID=3）
INSERT INTO `homework` (
    `title`, `description`, `course_id`, `teacher_id`,
    `question_ids`, `attachment_url`, `attachment_name`, `content`,
    `total_score`, `pass_score`,
    `start_time`, `end_time`,
    `submit_limit`, `time_limit_minutes`, `auto_grade`, `status`
) VALUES (
    '测试作业 - 线性表基础练习',
    '请完成第一章线性表的练习题，包括顺序表和链表的基本操作。',
    1, 3,
    NULL,
    '/homework/2026/03/21/test-homework.pdf',
    '线性表练习题.pdf',
    NULL,
    100.00, 60.00,
    NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY),
    3, 0, 0, 1
);

SELECT '测试作业插入成功！' AS message;
SELECT id, title, attachment_url, attachment_name FROM homework ORDER BY id DESC LIMIT 1;
