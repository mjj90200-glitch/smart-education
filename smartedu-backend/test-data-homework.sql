-- ============================================================
-- SmartEdu 测试数据 - 学生作业功能测试
-- 请在 MySQL 中执行此脚本
-- ============================================================

-- 1. 查看现有教师 ID
SELECT id, username, real_name, role FROM users WHERE role = 'TEACHER';

-- 2. 如果没有教师，创建一个测试教师（密码是 123456 的 BCrypt 加密）
INSERT INTO users (username, password, real_name, email, phone, role, status, created_at, updated_at)
SELECT 'teacher001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z9qA2Qv.C3p4Xl.j5O7WPBKS', '张教授', 'teacher001@test.com', '13800000001', 'TEACHER', 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'teacher001');

-- 3. 创建测试课程（使用教师 ID）
INSERT INTO courses (course_name, course_code, description, credit, teacher_id, semester, grade, major, status, created_at, updated_at)
SELECT '数据结构', 'CS101', '数据结构是计算机科学中的一门基础课程', 4.0, (SELECT id FROM users WHERE username = 'teacher001'), '2024-2025-1', '2023级', '计算机科学与技术', 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM courses WHERE course_code = 'CS101');

-- 4. 创建测试题目
INSERT INTO questions (question_code, question_type, content, options, answer, analysis, difficulty_level, score, course_id, create_user_id, status, created_at, updated_at)
SELECT 'Q-TEST001', 'ESSAY', '请简述什么是栈？栈的特点是什么？请举例说明栈的应用场景。', '{}', '', '', 2, 20.0, (SELECT id FROM courses WHERE course_code = 'CS101'), (SELECT id FROM users WHERE username = 'teacher001'), 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE question_code = 'Q-TEST001');

INSERT INTO questions (question_code, question_type, content, options, answer, analysis, difficulty_level, score, course_id, create_user_id, status, created_at, updated_at)
SELECT 'Q-TEST002', 'ESSAY', '请解释二叉树的前序、中序、后序遍历，并给出示例。', '{}', '', '', 2, 20.0, (SELECT id FROM courses WHERE course_code = 'CS101'), (SELECT id FROM users WHERE username = 'teacher001'), 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE question_code = 'Q-TEST002');

INSERT INTO questions (question_code, question_type, content, options, answer, analysis, difficulty_level, score, course_id, create_user_id, status, created_at, updated_at)
SELECT 'Q-TEST003', 'ESSAY', '什么是哈希表？请解释哈希冲突的解决方法。', '{}', '', '', 2, 20.0, (SELECT id FROM courses WHERE course_code = 'CS101'), (SELECT id FROM users WHERE username = 'teacher001'), 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE question_code = 'Q-TEST003');

INSERT INTO questions (question_code, question_type, content, options, answer, analysis, difficulty_level, score, course_id, create_user_id, status, created_at, updated_at)
SELECT 'Q-TEST004', 'ESSAY', '请比较数组和链表的优缺点，并说明各自的适用场景。', '{}', '', '', 2, 20.0, (SELECT id FROM courses WHERE course_code = 'CS101'), (SELECT id FROM users WHERE username = 'teacher001'), 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE question_code = 'Q-TEST004');

INSERT INTO questions (question_code, question_type, content, options, answer, analysis, difficulty_level, score, course_id, create_user_id, status, created_at, updated_at)
SELECT 'Q-TEST005', 'ESSAY', '什么是图的遍历？请解释深度优先搜索和广度优先搜索的区别。', '{}', '', '', 3, 20.0, (SELECT id FROM courses WHERE course_code = 'CS101'), (SELECT id FROM users WHERE username = 'teacher001'), 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE question_code = 'Q-TEST005');

-- 5. 创建测试作业（已发布状态）
INSERT INTO homework (title, description, course_id, teacher_id, question_ids, total_score, pass_score, start_time, end_time, submit_limit, time_limit_minutes, auto_grade, status, created_at, updated_at)
SELECT '第一章 数据结构基础作业', '请认真完成以下题目，截止日期前提交。',
       (SELECT id FROM courses WHERE course_code = 'CS101'),
       (SELECT id FROM users WHERE username = 'teacher001'),
       (SELECT GROUP_CONCAT(id ORDER BY id) FROM questions WHERE question_code LIKE 'Q-TEST%'),
       100.0, 60.0,
       DATE_SUB(NOW(), INTERVAL 1 DAY),  -- 昨天开始
       DATE_ADD(NOW(), INTERVAL 7 DAY),  -- 7天后结束
       3, 120, 0, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM homework WHERE title = '第一章 数据结构基础作业');

-- 6. 再创建一个即将开始的作业
INSERT INTO homework (title, description, course_id, teacher_id, question_ids, total_score, pass_score, start_time, end_time, submit_limit, time_limit_minutes, auto_grade, status, created_at, updated_at)
SELECT '第二章 栈与队列作业', '本作业将在明天开始，请做好准备。',
       (SELECT id FROM courses WHERE course_code = 'CS101'),
       (SELECT id FROM users WHERE username = 'teacher001'),
       (SELECT GROUP_CONCAT(id ORDER BY id LIMIT 3) FROM questions WHERE question_code LIKE 'Q-TEST%'),
       60.0, 36.0,
       DATE_ADD(NOW(), INTERVAL 1 DAY),  -- 明天开始
       DATE_ADD(NOW(), INTERVAL 14 DAY), -- 14天后结束
       2, 90, 0, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM homework WHERE title = '第二章 栈与队列作业');

-- 7. 验证数据
SELECT '=== 测试数据验证 ===' AS info;
SELECT '课程:' AS type, COUNT(*) AS count FROM courses WHERE course_code = 'CS101';
SELECT '题目:' AS type, COUNT(*) AS count FROM questions WHERE question_code LIKE 'Q-TEST%';
SELECT '作业:' AS type, COUNT(*) AS count FROM homework WHERE title LIKE '%数据结构%' OR title LIKE '%栈与队列%';

-- 8. 显示作业详情
SELECT h.id, h.title, h.status, h.start_time, h.end_time, c.course_name, u.real_name AS teacher_name
FROM homework h
JOIN courses c ON h.course_id = c.id
JOIN users u ON h.teacher_id = u.id
WHERE h.title LIKE '%数据结构%' OR h.title LIKE '%栈与队列%';