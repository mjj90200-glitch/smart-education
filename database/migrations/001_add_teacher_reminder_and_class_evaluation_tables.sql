-- ============================================================
-- 001: 补建教师提醒与课堂教学评估模块缺失的数据表
-- 背景: 这三个模块的 Java 代码先行合入，但建表 SQL 一直缺失，
--       导致 /student/learning/reminders、/dashboard、/analysis 等接口 500。
-- 依据: entity/TeacherReminder.java、ClassEvaluationSession.java、ClassEvaluationRecord.java
-- ============================================================

USE `smartedu_platform`;

CREATE TABLE IF NOT EXISTS `teacher_reminders` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `course_id`   BIGINT       DEFAULT NULL COMMENT '课程ID',
  `teacher_id`  BIGINT       DEFAULT NULL COMMENT '教师ID',
  `student_id`  BIGINT       DEFAULT NULL COMMENT '学生ID',
  `title`       VARCHAR(200) DEFAULT NULL COMMENT '提醒标题',
  `content`     TEXT         COMMENT '提醒内容',
  `read_status` TINYINT      DEFAULT 0 COMMENT '已读状态 0-未读 1-已读',
  `read_time`   DATETIME     DEFAULT NULL COMMENT '阅读时间',
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT      DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
  PRIMARY KEY (`id`),
  KEY `idx_tr_student` (`student_id`, `deleted`),
  KEY `idx_tr_teacher` (`teacher_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师提醒表';

CREATE TABLE IF NOT EXISTS `class_evaluation_sessions` (
  `id`                          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `teacher_id`                  BIGINT        DEFAULT NULL COMMENT '教师ID',
  `course_id`                   BIGINT        DEFAULT NULL COMMENT '课程ID',
  `week_number`                 INT           DEFAULT NULL COMMENT '教学周次',
  `session_date`                DATE          DEFAULT NULL COMMENT '上课日期',
  `session_title`               VARCHAR(200)  DEFAULT NULL COMMENT '课程标题',
  `teaching_topic`              VARCHAR(500)  DEFAULT NULL COMMENT '教学主题',
  `source_file_name`            VARCHAR(255)  DEFAULT NULL COMMENT '课堂数据来源文件名',
  `start_time`                  TIME          DEFAULT NULL COMMENT '开始时间',
  `end_time`                    TIME          DEFAULT NULL COMMENT '结束时间',
  `total_students`              INT           DEFAULT 0 COMMENT '应到学生数',
  `present_count`               INT           DEFAULT 0 COMMENT '出勤人数',
  `absent_count`                INT           DEFAULT 0 COMMENT '缺勤人数',
  `leave_count`                 INT           DEFAULT 0 COMMENT '请假人数',
  `late_count`                  INT           DEFAULT 0 COMMENT '迟到人数',
  `answer_count`                INT           DEFAULT 0 COMMENT '课堂答题次数',
  `question_count`              INT           DEFAULT 0 COMMENT '课堂提问次数',
  `interaction_count`           INT           DEFAULT 0 COMMENT '互动总次数',
  `high_interaction_count`      INT           DEFAULT 0 COMMENT '高互动次数',
  `average_participation_score` DECIMAL(5,2)  DEFAULT NULL COMMENT '平均参与度得分',
  `analysis_summary`            TEXT          COMMENT 'AI 课堂分析摘要',
  `created_at`                  DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`                  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`                     TINYINT       DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
  PRIMARY KEY (`id`),
  KEY `idx_ces_teacher_course` (`teacher_id`, `course_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课堂教学评估场次表';

CREATE TABLE IF NOT EXISTS `class_evaluation_records` (
  `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_id`         BIGINT       DEFAULT NULL COMMENT '课堂场次ID',
  `student_id`         BIGINT       DEFAULT NULL COMMENT '学生ID',
  `student_no`         VARCHAR(50)  DEFAULT NULL COMMENT '学号',
  `student_name`       VARCHAR(100) DEFAULT NULL COMMENT '学生姓名',
  `class_name`         VARCHAR(100) DEFAULT NULL COMMENT '班级名称',
  `performance_level`  VARCHAR(20)  DEFAULT NULL COMMENT '课堂表现等级',
  `attendance_status`  VARCHAR(20)  DEFAULT NULL COMMENT '出勤状态',
  `answer_count`       INT          DEFAULT 0 COMMENT '答题次数',
  `question_count`     INT          DEFAULT 0 COMMENT '提问次数',
  `participation_score` INT         DEFAULT 0 COMMENT '参与度得分',
  `teacher_comment`    VARCHAR(500) DEFAULT NULL COMMENT '教师评语',
  `absent_reason`      VARCHAR(255) DEFAULT NULL COMMENT '缺勤原因',
  `created_at`         DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`            TINYINT      DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
  PRIMARY KEY (`id`),
  KEY `idx_cer_session` (`session_id`, `deleted`),
  KEY `idx_cer_student` (`student_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课堂学生表现记录表';
