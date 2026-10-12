-- ============================================================
-- SmartEdu-Platform 真实数据库结构
-- 说明：此文件包含生产环境的真实数据库结构
-- ============================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 创建数据库
-- ============================================================
CREATE DATABASE IF NOT EXISTS `smartedu_platform`
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE `smartedu_platform`;

-- ============================================================
-- 1. AI 批改记录表
-- ============================================================
create table ai_grade_log
(
    id             bigint auto_increment comment '记录 ID'
        primary key,
    submission_id  bigint                                not null comment '提交 ID',
    homework_id    bigint                                not null comment '作业 ID',
    student_id     bigint                                not null comment '学生 ID',
    ai_model       varchar(50) default 'qwen-coder-plus' null comment '使用的 AI 模型',
    prompt_content text                                  null comment 'AI 提示词',
    ai_response    text                                  null comment 'AI 原始响应',
    grade_result   json                                  null comment '批改结果',
    grade_time     datetime    default CURRENT_TIMESTAMP null comment '批改时间',
    status         tinyint     default 1                 null comment '状态：0-失败，1-成功',
    error_message  varchar(500)                          null comment '错误信息'
)
    comment 'AI 批改记录表' collate = utf8mb4_unicode_ci;

create index idx_grade_time on ai_grade_log (grade_time);
create index idx_homework_id on ai_grade_log (homework_id);
create index idx_student_id on ai_grade_log (student_id);
create index idx_submission_id on ai_grade_log (submission_id);

-- ============================================================
-- 2. AI 题库表
-- ============================================================
create table ai_question_bank
(
    id                  bigint auto_increment comment '题目 ID'
        primary key,
    course_id           bigint                                not null comment '课程 ID',
    knowledge_point_ids varchar(500)                          null comment '关联知识点 ID 列表（逗号分隔）',
    question_type       varchar(20)                           not null comment '题目类型',
    content             text                                  not null comment '题目内容',
    options             json                                  null comment '选项（JSON 格式）',
    answer              text                                  not null comment '答案',
    analysis            text                                  null comment '解析',
    difficulty_level    tinyint     default 2                 null comment '难度：1-简单，2-中等，3-困难',
    source              varchar(50) default 'AI_GENERATED'    null comment '来源',
    usage_count         int         default 0                 null comment '使用次数',
    avg_correct_rate    decimal(5, 2)                         null comment '平均正确率',
    status              tinyint     default 1                 null comment '状态：0-停用，1-启用',
    created_at          datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    updated_at          datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment 'AI 题库表' collate = utf8mb4_unicode_ci;

create index idx_course_id on ai_question_bank (course_id);
create index idx_difficulty on ai_question_bank (difficulty_level);
create index idx_type on ai_question_bank (question_type);

-- ============================================================
-- 3. 课程表
-- ============================================================
create table courses
(
    id          bigint auto_increment comment '课程 ID'
        primary key,
    course_name varchar(100)                            not null comment '课程名称',
    course_code varchar(50)                             not null comment '课程代码',
    description text                                    null comment '课程描述',
    credit      decimal(3, 1) default 0.0               null comment '学分',
    teacher_id  bigint                                  not null comment '授课教师 ID',
    semester    varchar(50)                             null comment '学期',
    grade       varchar(50)                             null comment '适用年级',
    major       varchar(100)                            null comment '适用专业',
    status      tinyint       default 1                 null comment '状态：0-停用，1-启用',
    created_at  datetime      default CURRENT_TIMESTAMP null comment '创建时间',
    updated_at  datetime      default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     tinyint       default 0                 null comment '逻辑删除',
    constraint uk_course_code unique (course_code)
)
    comment '课程表' collate = utf8mb4_unicode_ci;

create index idx_grade on courses (grade);
create index idx_semester on courses (semester);
create index idx_teacher_id on courses (teacher_id);

-- ============================================================
-- 4. 错题表
-- ============================================================
create table error_logs
(
    id                  bigint auto_increment comment '错题记录 ID'
        primary key,
    user_id             bigint                                  not null comment '学生 ID',
    question_id         bigint                                  not null comment '题目 ID',
    course_id           bigint                                  null comment '所属课程 ID',
    knowledge_point_ids varchar(500)                            null comment '涉及知识点 ID 列表',
    user_answer         text                                    null comment '学生答案',
    correct_answer      text                                    null comment '正确答案',
    error_type          varchar(50)                             null comment '错误类型',
    error_reason        text                                    null comment '错误原因分析',
    score_obtained      decimal(5, 2) default 0.00              null comment '得分',
    score_total         decimal(5, 2) default 0.00              null comment '总分',
    source_type         varchar(50)                             null comment '来源类型',
    source_id           bigint                                  null comment '来源 ID',
    review_status       tinyint       default 0                 null comment '复习状态',
    review_count        int           default 0                 null comment '复习次数',
    last_review_time    datetime                                null comment '最后复习时间',
    next_review_time    datetime                                null comment '下次复习时间',
    created_at          datetime      default CURRENT_TIMESTAMP null comment '创建时间',
    updated_at          datetime      default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '错题记录表' collate = utf8mb4_unicode_ci;

create index idx_course_id on error_logs (course_id);
create index idx_next_review on error_logs (next_review_time);
create index idx_question_id on error_logs (question_id);
create index idx_review_status on error_logs (review_status);
create index idx_user_id on error_logs (user_id);

-- ============================================================
-- 5. 答疑大厅帖子表
-- ============================================================
create table forum_posts
(
    id                 bigint auto_increment comment '帖子 ID'
        primary key,
    user_id            bigint                             not null comment '发帖人 ID',
    course_id          bigint                             null comment '关联课程 ID',
    knowledge_point_id bigint                             null comment '关联知识点 ID',
    title              varchar(200)                       not null comment '帖子标题',
    content            text                               not null comment '帖子内容',
    attachment_url     varchar(500)                       null comment '附件路径',
    attachment_name    varchar(255)                       null comment '附件名称',
    bounty_score       int      default 0                 null comment '悬赏分数',
    category           varchar(50)                        null comment '分类',
    status             tinyint  default 0                 null comment '状态',
    view_count         int      default 0                 null comment '浏览次数',
    like_count         int      default 0                 null comment '点赞数',
    reply_count        int      default 0                 null comment '回复数',
    is_top             tinyint  default 0                 null comment '是否置顶',
    is_anonymous       tinyint  default 0                 null comment '是否匿名',
    created_at         datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updated_at         datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted            tinyint  default 0                 null comment '逻辑删除'
)
    comment '答疑大厅帖子表' collate = utf8mb4_unicode_ci;

create index idx_bounty on forum_posts (bounty_score);
create index idx_category on forum_posts (category);
create index idx_course_id on forum_posts (course_id);
create index idx_created_at on forum_posts (created_at);
create index idx_status on forum_posts (status);
create index idx_user_id on forum_posts (user_id);

-- ============================================================
-- 6. 答疑大厅回复表
-- ============================================================
create table forum_replies
(
    id              bigint auto_increment comment '回复 ID'
        primary key,
    post_id         bigint                             not null comment '帖子 ID',
    user_id         bigint                             not null comment '回复人 ID',
    parent_id       bigint                             null comment '父回复 ID',
    content         text                               not null comment '回复内容',
    attachment_url  varchar(500)                       null comment '附件路径',
    attachment_name varchar(255)                       null comment '附件名称',
    like_count      int      default 0                 null comment '点赞数',
    is_accepted     tinyint  default 0                 null comment '是否被采纳',
    is_ai_generated tinyint  default 0                 null comment '是否 AI 生成',
    created_at      datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updated_at      datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted         tinyint  default 0                 null comment '逻辑删除'
)
    comment '答疑大厅回复表' collate = utf8mb4_unicode_ci;

create index idx_accepted on forum_replies (is_accepted);
create index idx_parent_id on forum_replies (parent_id);
create index idx_post_id on forum_replies (post_id);
create index idx_user_id on forum_replies (user_id);

-- ============================================================
-- 7. 首页推荐视频表
-- ============================================================
create table home_recommend_video
(
    id            bigint auto_increment comment '主键 ID'
        primary key,
    video_post_id bigint                             not null comment '关联的视频 ID',
    position_type tinyint  default 1                 not null comment '位置类型',
    sort_order    int      default 0                 not null comment '排序值',
    created_at    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_video_position unique (video_post_id, position_type)
)
    comment '首页推荐视频配置表' collate = utf8mb4_unicode_ci;

create index idx_position_sort on home_recommend_video (position_type, sort_order);
create index idx_video_id on home_recommend_video (video_post_id);

-- ============================================================
-- 8. 作业表
-- ============================================================
create table homework
(
    id                      bigint auto_increment comment '作业 ID'
        primary key,
    title                   varchar(200)                             not null comment '作业标题',
    description             text                                     null comment '作业描述',
    course_id               bigint                                   null comment '课程 ID',
    teacher_id              bigint                                   null comment '布置教师 ID',
    question_ids            varchar(500)                             null comment '题目 ID 列表',
    attachment_url          varchar(500)                             null comment '作业文档路径',
    attachment_name         varchar(200)                             null comment '作业文档名称',
    content                 text                                     null comment '作业内容',
    total_score             decimal(10, 2) default 100.00            null comment '总分',
    pass_score              decimal(10, 2) default 60.00             null comment '及格分',
    start_time              datetime                                 null comment '开始时间',
    end_time                datetime                                 null comment '截止时间',
    submit_limit            int            default 0                 null comment '提交次数限制',
    time_limit_minutes      int                                      null comment '答题时长限制',
    auto_grade              tinyint        default 1                 null comment '是否自动批改',
    status                  tinyint        default 0                 null comment '状态',
    created_at              datetime       default CURRENT_TIMESTAMP null comment '创建时间',
    updated_at              datetime       default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted                 tinyint        default 0                 null comment '逻辑删除',
    ai_analysis_content     text                                     null comment 'AI 解析内容',
    ai_analysis_status      tinyint        default 0                 null comment 'AI 解析状态',
    teacher_edited_analysis text                                     null comment '老师修改后的最终解析'
)
    comment '作业表' collate = utf8mb4_unicode_ci;

create index idx_ai_analysis_status on homework (ai_analysis_status);
create index idx_course_id on homework (course_id);
create index idx_created_at on homework (created_at);
create index idx_status on homework (status);
create index idx_teacher_id on homework (teacher_id);

-- ============================================================
-- 9. 作业提交表
-- ============================================================
create table homework_submissions
(
    id                      bigint auto_increment comment '提交 ID'
        primary key,
    homework_id             bigint                             not null comment '作业 ID',
    user_id                 bigint                             not null comment '学生 ID',
    student_name            varchar(50)                        null,
    submission_content      text                               null,
    submission_type         tinyint  default 1                 null comment '提交类型',
    answers                 json                               null comment '答案内容',
    attachment_url          varchar(500)                       null comment '提交附件 URL',
    attachment_name         varchar(255)                       null comment '提交附件名称',
    score                   decimal(5, 2)                      null comment '得分',
    comment                 text                               null comment '教师评语',
    grade_status            tinyint  default 0                 null comment '批改状态',
    submit_time             datetime default CURRENT_TIMESTAMP null comment '提交时间',
    grade_time              datetime                           null comment '批改时间',
    grade_user_id           bigint                             null comment '批改人 ID',
    is_late                 tinyint  default 0                 null comment '是否迟交',
    created_at              datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updated_at              datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    ai_score                decimal(5, 2)                      null comment 'AI 评分',
    ai_feedback             text                               null comment 'AI 评语',
    knowledge_point_mastery json                               null comment '知识点掌握度',
    error_analysis          json                               null comment '错误分析',
    constraint uk_homework_user unique (homework_id, user_id)
)
    comment '作业提交表' collate = utf8mb4_unicode_ci;

create index idx_status on homework_submissions (grade_status);
create index idx_user_id on homework_submissions (user_id);

-- ============================================================
-- 10. 知识点表
-- ============================================================
create table knowledge_points
(
    id                  bigint auto_increment comment '知识点 ID'
        primary key,
    course_id           bigint                                  not null comment '所属课程 ID',
    parent_id           bigint                                  null comment '父知识点 ID',
    name                varchar(100)                            not null comment '知识点名称',
    code                varchar(50)                             not null comment '知识点编码',
    description         text                                    null comment '知识点描述',
    difficulty_level    tinyint       default 1                 null comment '难度等级',
    importance_level    tinyint       default 1                 null comment '重要程度',
    prerequisite_ids    varchar(500)                            null comment '前置知识点 ID 列表',
    learning_objectives text                                    null comment '学习目标',
    estimated_hours     decimal(5, 2) default 0.00              null comment '预计学习时长',
    order_num           int           default 0                 null comment '排序号',
    created_at          datetime      default CURRENT_TIMESTAMP null comment '创建时间',
    updated_at          datetime      default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted             tinyint       default 0                 null comment '逻辑删除',
    constraint uk_code unique (code)
)
    comment '知识点表' collate = utf8mb4_unicode_ci;

create index idx_course_id on knowledge_points (course_id);
create index idx_parent_id on knowledge_points (parent_id);

-- ============================================================
-- 11. 知识点关联表
-- ============================================================
create table knowledge_relations
(
    id            bigint auto_increment comment '关系 ID'
        primary key,
    source_kp_id  bigint                                  not null comment '源知识点 ID',
    target_kp_id  bigint                                  not null comment '目标知识点 ID',
    relation_type varchar(50)                             not null comment '关系类型',
    weight        decimal(3, 2) default 1.00              null comment '关系权重',
    description   varchar(255)                            null comment '关系描述',
    created_at    datetime      default CURRENT_TIMESTAMP null comment '创建时间',
    constraint uk_relation unique (source_kp_id, target_kp_id, relation_type)
)
    comment '知识点关联表' collate = utf8mb4_unicode_ci;

create index idx_source on knowledge_relations (source_kp_id);
create index idx_target on knowledge_relations (target_kp_id);

-- ============================================================
-- 12. 学情分析表
-- ============================================================
create table learning_analytics
(
    id                bigint auto_increment comment '分析记录 ID'
        primary key,
    user_id           bigint                                  not null comment '学生 ID',
    course_id         bigint                                  null comment '课程 ID',
    report_type       varchar(50)                             not null comment '报告类型',
    report_period     varchar(50)                             null comment '报告周期',
    study_time_hours  decimal(5, 2) default 0.00              null comment '学习时长',
    completed_tasks   int           default 0                 null comment '完成任务数',
    correct_rate      decimal(5, 2) default 0.00              null comment '正确率',
    knowledge_mastery json                                    null comment '知识点掌握度',
    weak_points       varchar(500)                            null comment '薄弱知识点',
    suggestions       text                                    null comment '学习建议',
    rank_in_class     int                                     null comment '班级排名',
    total_in_class    int                                     null comment '班级总人数',
    report_data       json                                    null comment '完整报告数据',
    generated_at      datetime      default CURRENT_TIMESTAMP null comment '生成时间'
)
    comment '学情分析表' collate = utf8mb4_unicode_ci;

create index idx_course_id on learning_analytics (course_id);
create index idx_period on learning_analytics (report_period);
create index idx_report_type on learning_analytics (report_type);
create index idx_user_id on learning_analytics (user_id);

-- ============================================================
-- 13. 学情分析详情表
-- ============================================================
create table learning_analytics_detail
(
    id                 bigint auto_increment comment '记录 ID'
        primary key,
    user_id            bigint                                  not null comment '学生 ID',
    course_id          bigint                                  not null comment '课程 ID',
    knowledge_point_id bigint                                  null comment '知识点 ID',
    mastery_level      decimal(5, 2) default 0.00              null comment '掌握度',
    practice_count     int           default 0                 null comment '练习次数',
    correct_count      int           default 0                 null comment '正确次数',
    avg_score          decimal(5, 2) default 0.00              null comment '平均得分',
    last_practice_time datetime                                null comment '最后练习时间',
    weak_point         tinyint       default 0                 null comment '是否薄弱点',
    created_at         datetime      default CURRENT_TIMESTAMP null comment '创建时间',
    updated_at         datetime      default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_user_kp unique (user_id, knowledge_point_id)
)
    comment '学情分析详情表' collate = utf8mb4_unicode_ci;

create index idx_course_id on learning_analytics_detail (course_id);
create index idx_user_id on learning_analytics_detail (user_id);
create index idx_weak_point on learning_analytics_detail (weak_point);

-- ============================================================
-- 14. 学习计划表
-- ============================================================
create table learning_plans
(
    id                  bigint auto_increment comment '学习计划 ID'
        primary key,
    user_id             bigint                                  not null comment '学生 ID',
    course_id           bigint                                  null comment '课程 ID',
    title               varchar(100)                            not null comment '计划标题',
    description         text                                    null comment '计划描述',
    goal_type           varchar(50)                             null comment '目标类型',
    target_score        decimal(5, 2)                           null comment '目标分数',
    start_date          date                                    not null comment '开始日期',
    end_date            date                                    not null comment '结束日期',
    daily_study_hours   decimal(3, 2) default 2.00              null comment '每日学习时长',
    knowledge_point_ids varchar(1000)                           null comment '目标知识点 ID 列表',
    plan_items          json                                    null comment '详细计划项',
    progress            decimal(5, 2) default 0.00              null comment '完成进度',
    status              tinyint       default 1                 null comment '状态',
    created_at          datetime      default CURRENT_TIMESTAMP null comment '创建时间',
    updated_at          datetime      default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '学习计划表' collate = utf8mb4_unicode_ci;

create index idx_course_id on learning_plans (course_id);
create index idx_status on learning_plans (status);
create index idx_user_id on learning_plans (user_id);

-- ============================================================
-- 15. 学习资源表
-- ============================================================
create table learning_resources
(
    id                  bigint auto_increment comment '资源 ID'
        primary key,
    title               varchar(200)                            not null comment '资源标题',
    description         text                                    null comment '资源描述',
    resource_type       varchar(50)                             not null comment '资源类型',
    url                 varchar(500)                            null comment '资源 URL',
    file_path           varchar(255)                            null comment '文件路径',
    file_size           bigint                                  null comment '文件大小',
    course_id           bigint                                  null comment '所属课程 ID',
    knowledge_point_ids varchar(500)                            null comment '关联知识点 ID 列表',
    tags                varchar(255)                            null comment '标签',
    difficulty_level    tinyint       default 2                 null comment '难度等级',
    view_count          int           default 0                 null comment '浏览次数',
    download_count      int           default 0                 null comment '下载次数',
    rating              decimal(2, 1) default 0.0               null comment '评分',
    upload_user_id      bigint                                  null comment '上传人 ID',
    status              tinyint       default 1                 null comment '状态',
    created_at          datetime      default CURRENT_TIMESTAMP null comment '创建时间',
    updated_at          datetime      default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted             tinyint       default 0                 null comment '逻辑删除'
)
    comment '学习资源表' collate = utf8mb4_unicode_ci;

create index idx_course_id on learning_resources (course_id);
create index idx_tags on learning_resources (tags);
create index idx_type on learning_resources (resource_type);

-- ============================================================
-- 16. 学习预警表
-- ============================================================
create table learning_warning
(
    id              bigint auto_increment comment '预警 ID'
        primary key,
    user_id         bigint                             not null comment '学生 ID',
    course_id       bigint                             not null comment '课程 ID',
    warning_type    varchar(50)                        not null comment '预警类型',
    warning_level   tinyint  default 2                 null comment '预警级别',
    trigger_value   varchar(200)                       null comment '触发值',
    description     text                               null comment '预警描述',
    status          tinyint  default 0                 null comment '状态',
    handled_by      bigint                             null comment '处理人 ID',
    handled_at      datetime                           null comment '处理时间',
    handled_comment text                               null comment '处理意见',
    created_at      datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updated_at      datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '学习预警表' collate = utf8mb4_unicode_ci;

create index idx_course_id on learning_warning (course_id);
create index idx_status on learning_warning (status);
create index idx_type on learning_warning (warning_type);
create index idx_user_id on learning_warning (user_id);

-- ============================================================
-- 17. 新闻表
-- ============================================================
create table news
(
    id           bigint auto_increment comment '新闻 ID'
        primary key,
    title        varchar(255)                       not null comment '新闻标题',
    summary      varchar(1000)                      null comment '摘要',
    content      text                               null comment '详细内容',
    image_url    varchar(500)                       null comment '封面图片 URL',
    source_url   varchar(500)                       null comment '原文链接',
    source_name  varchar(50)                        null comment '来源名称',
    news_type    tinyint  default 2                 null comment '新闻类型',
    is_top       tinyint  default 0                 null comment '是否置顶',
    is_manual    tinyint  default 0                 null comment '是否手动添加',
    order_index  int      default 0                 null comment '排序序号',
    publish_time datetime                           null comment '发布时间',
    create_time  datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time  datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '新闻表';

create index idx_create_time on news (create_time);
create index idx_type_time on news (news_type, is_top, publish_time);

-- ============================================================
-- 18. 问答表
-- ============================================================
create table qa_items
(
    id                 bigint auto_increment comment '问答 ID'
        primary key,
    user_id            bigint                             not null comment '提问者 ID',
    course_id          bigint                             null comment '课程 ID',
    knowledge_point_id bigint                             null comment '知识点 ID',
    title              varchar(200)                       not null comment '问题标题',
    content            text                               not null comment '问题内容',
    answer             text                               null comment '回答内容',
    answer_user_id     bigint                             null comment '回答者 ID',
    answer_type        varchar(50)                        null comment '回答类型',
    category           varchar(50)                        null comment '问题分类',
    status             tinyint  default 0                 null comment '状态',
    view_count         int      default 0                 null comment '浏览次数',
    like_count         int      default 0                 null comment '点赞数',
    is_anonymous       tinyint  default 0                 null comment '是否匿名',
    created_at         datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updated_at         datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '问答表' collate = utf8mb4_unicode_ci;

create index idx_course_id on qa_items (course_id);
create index idx_knowledge_point on qa_items (knowledge_point_id);
create index idx_status on qa_items (status);
create index idx_user_id on qa_items (user_id);

-- ============================================================
-- 19. 点赞表
-- ============================================================
create table question_like
(
    id          bigint auto_increment
        primary key,
    user_id     bigint                             not null,
    target_type tinyint                            not null,
    target_id   bigint                             not null,
    created_at  datetime default CURRENT_TIMESTAMP null,
    constraint uk_user_target unique (user_id, target_type, target_id)
);

-- ============================================================
-- 20. 答疑主帖表
-- ============================================================
create table question_topic
(
    id                bigint auto_increment comment '帖子 ID'
        primary key,
    user_id           bigint                             not null comment '发帖人 ID',
    title             varchar(200)                       not null comment '帖子标题',
    content           text                               not null comment '帖子内容',
    category          varchar(50)  default 'QUESTION'    null comment '帖子分类',
    view_count        int          default 0             null comment '浏览次数',
    reply_count       int          default 0             null comment '回复数量',
    like_count        int          default 0             null comment '点赞数量',
    status            tinyint      default 1             null comment '状态',
    is_solved         tinyint      default 0             null comment '是否已解决',
    accepted_reply_id bigint                               null comment '采纳的答案 ID',
    attachment_url    varchar(500)                         null comment '附件 URL',
    created_at        datetime     default CURRENT_TIMESTAMP null comment '创建时间',
    updated_at        datetime     default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted           tinyint      default 0             null comment '逻辑删除'
)
    comment '答疑大厅 - 主帖表' collate = utf8mb4_unicode_ci;

create index idx_category on question_topic (category);
create index idx_created_at on question_topic (created_at desc);
create index idx_is_solved on question_topic (is_solved);
create index idx_status on question_topic (status);
create index idx_user_id on question_topic (user_id);

-- ============================================================
-- 21. 答疑回复表
-- ============================================================
create table question_reply
(
    id              bigint auto_increment comment '回复 ID'
        primary key,
    topic_id        bigint                             not null comment '主帖 ID',
    user_id         bigint                             not null comment '回复人 ID',
    parent_reply_id bigint                             null comment '父回复 ID',
    content         text                               not null comment '回复内容',
    user_role       varchar(20)                        not null comment '回复人角色',
    user_name       varchar(100)                       not null comment '回复人姓名',
    user_avatar     varchar(500)                       null comment '回复人头像',
    like_count      int      default 0                 null comment '点赞数量',
    is_accepted     tinyint  default 0                 null comment '是否被采纳',
    status          tinyint  default 1                 null comment '状态',
    attachment_url  varchar(500)                       null comment '附件 URL',
    created_at      datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updated_at      datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted         tinyint  default 0                 null comment '逻辑删除',
    constraint fk_reply_topic foreign key (topic_id) references question_topic (id) on delete cascade
)
    comment '答疑大厅 - 回帖表' collate = utf8mb4_unicode_ci;

create index idx_created_at on question_reply (created_at);
create index idx_parent_reply_id on question_reply (parent_reply_id);
create index idx_topic_id on question_reply (topic_id);
create index idx_user_id on question_reply (user_id);
create index idx_user_role on question_reply (user_role);

-- ============================================================
-- 22. 题目表
-- ============================================================
create table questions
(
    id                  bigint auto_increment comment '题目 ID'
        primary key,
    question_code       varchar(50)                            not null comment '题目编码',
    question_type       varchar(20)                            not null comment '题目类型',
    content             text                                   not null comment '题目内容',
    options             json                                   null comment '选项内容',
    answer              text                                   not null comment '答案',
    analysis            text                                   null comment '解析',
    difficulty_level    tinyint      default 2                 null comment '难度等级',
    score               decimal(5, 2)  default 0.00            null comment '题目分值',
    course_id           bigint                                 null comment '所属课程 ID',
    knowledge_point_ids varchar(500)                           null comment '关联知识点 ID 列表',
    tags                varchar(255)                           null comment '标签',
    source              varchar(100)                           null comment '题目来源',
    create_user_id      bigint                                 null comment '创建人 ID',
    status              tinyint      default 1                 null comment '状态',
    created_at          datetime     default CURRENT_TIMESTAMP null comment '创建时间',
    updated_at          datetime     default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted             tinyint      default 0                 null comment '逻辑删除',
    constraint uk_question_code unique (question_code)
)
    comment '题目标' collate = utf8mb4_unicode_ci;

create index idx_course_id on questions (course_id);
create index idx_difficulty on questions (difficulty_level);
create index idx_status on questions (status);
create index idx_type on questions (question_type);

-- ============================================================
-- 23. 用户表
-- ============================================================
create table users
(
    id              bigint auto_increment comment '用户 ID'
        primary key,
    username        varchar(50)                        not null comment '用户名',
    password        varchar(255)                       not null comment '密码',
    real_name       varchar(50)                        not null comment '真实姓名',
    email           varchar(100)                       null comment '邮箱',
    phone           varchar(20)                        null comment '手机',
    avatar          varchar(255)                       null comment '头像 URL',
    role            varchar(20)                        not null comment '角色',
    grade           varchar(50)                        null comment '年级',
    major           varchar(100)                       null comment '专业',
    class_name      varchar(50)                        null comment '班级',
    department      varchar(100)                       null comment '院系',
    title           varchar(50)                        null comment '职称',
    status          tinyint  default 1                 null comment '状态',
    last_login_time datetime                           null comment '最后登录时间',
    created_at      datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updated_at      datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted         tinyint  default 0                 null comment '逻辑删除',
    constraint uk_username unique (username)
)
    comment '用户表' collate = utf8mb4_unicode_ci;

create index idx_department on users (department);
create index idx_grade on users (grade);
create index idx_major on users (major);
create index idx_role on users (role);

-- ============================================================
-- 24. 视频收藏表
-- ============================================================
create table video_collection
(
    id         bigint auto_increment comment '收藏 ID'
        primary key,
    user_id    bigint                             not null comment '用户 ID',
    video_id   bigint                             not null comment '视频 ID',
    created_at datetime default CURRENT_TIMESTAMP null comment '收藏时间',
    constraint uk_user_video unique (user_id, video_id)
)
    comment '视频收藏表' collate = utf8mb4_unicode_ci;

create index idx_user_id on video_collection (user_id);
create index idx_video_id on video_collection (video_id);

-- ============================================================
-- 25. 视频投稿表
-- ============================================================
create table video_post
(
    id               bigint auto_increment comment '视频 ID'
        primary key,
    user_id          bigint                             not null comment '投稿人 ID',
    title            varchar(200)                       not null comment '视频标题',
    cover_url        varchar(500)                       null comment '封面图 URL',
    video_url        varchar(500)                       not null comment '视频链接',
    description      varchar(500)                       null comment '视频简介',
    status           tinyint  default 0                 null comment '状态',
    reject_reason    varchar(500)                       null comment '拒绝理由',
    view_count       int      default 0                 null comment '浏览次数',
    collection_count int      default 0                 null comment '收藏次数',
    sort_order       int      default 0                 null comment '排序序号',
    deleted          tinyint  default 0                 null comment '逻辑删除',
    created_at       datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updated_at       datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '视频投稿表' collate = utf8mb4_unicode_ci;

create index idx_created_at on video_post (created_at desc);
create index idx_deleted on video_post (deleted);
create index idx_status on video_post (status);
create index idx_user_id on video_post (user_id);

-- ============================================================
-- 初始化数据
-- ============================================================

-- 插入测试用户（密码为 123456 的 BCrypt 加密）
-- 插入测试用户
INSERT INTO `users` (`username`, `password`, `real_name`, `email`, `role`, `grade`, `major`, `class_name`, `department`) VALUES
('student001', '$2a$10$rOEX7s.jW7Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5K', '张三', 'zhangsan@example.com', 'STUDENT', '2023 级', '计算机科学 with 技术', '计算机 1 班', NULL),
('student002', '$2a$10$rOEX7s.jW7Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5K', '李四', 'lisi@example.com', 'STUDENT', '2023 级', '计算机科学与技术', '计算机 1 班', NULL),
('teacher001', '$2a$10$rOEX7s.jW7Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5K', '王老师', 'wang@example.com', 'TEACHER', NULL, NULL, NULL, '计算机学院'),
('admin001', '$2a$10$rOEX7s.jW7Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5K', '管理员', 'admin@example.com', 'ADMIN', NULL, NULL, NULL, NULL);
-- 插入测试课程
INSERT INTO `courses` (`course_name`, `course_code`, `description`, `credit`, `teacher_id`, `semester`, `grade`, `major`) VALUES
('数据结构', 'CS101', '计算机专业核心基础课程', 4.0, 3, '2024-2025-1', '2023 级', '计算机科学与技术'),
('Java 程序设计', 'CS102', 'Java 语言基础与面向对象编程', 3.5, 3, '2024-2025-1', '2023 级', '计算机科学与技术'),
('数据库原理', 'CS201', '数据库系统基本原理与应用', 4.0, 3, '2024-2025-2', '2023 级', '计算机科学与技术');

-- 插入测试新闻
INSERT INTO `news` (`title`, `summary`, `image_url`, `source_url`, `source_name`, `news_type`, `is_top`, `is_manual`, `publish_time`) VALUES
('OpenAI 发布 GPT-5：多模态能力全面升级', 'OpenAI 今日正式发布 GPT-5，新增实时视频理解、代码自动调试等功能', 'https://images.unsplash.com/photo-1677442136019-21780ecad995?w=800&h=400&fit=crop', '#', 'OpenAI 官方博客', 1, 1, 1, NOW()),
('英伟达发布新一代 AI 芯片 Blackwell B300', 'NVIDIA Blackwell B300 采用 3nm 工艺，支持 10TB/s 内存带宽', 'https://images.unsplash.com/photo-1555617778-02518510b9fa?w=800&h=400&fit=crop', '#', 'NVIDIA', 1, 0, 1, NOW()),
('智谱 AI 完成 5 亿美元融资，估值突破 30 亿美元', '智谱 AI 宣布完成 D 轮融资，将加速 GLM 大模型研发', 'https://images.unsplash.com/photo-1552664730-d307ca884978?w=800&h=400&fit=crop', '#', '智谱 AI', 1, 0, 1, NOW());

-- 插入测试视频
INSERT INTO `video_post` (`user_id`, `title`, `cover_url`, `video_url`, `description`, `status`, `view_count`, `collection_count`) VALUES
(1, 'Python 零基础入门教程', 'https://i0.hdslb.com/bfs/archive/python1.jpg', 'https://www.bilibili.com/video/BV1xx411c7mD', '适合零基础学员', 1, 12580, 856),
(1, 'Java 企业级开发实战', 'https://i0.hdslb.com/bfs/archive/java1.jpg', 'https://www.bilibili.com/video/BV1yy411c7mE', '企业级 Java 开发', 1, 8960, 623),
(1, 'MySQL 数据库优化技巧', 'https://i0.hdslb.com/bfs/archive/mysql1.jpg', 'https://www.bilibili.com/video/BV1zz411c7mF', '数据库性能优化', 1, 6750, 412),
(2, 'Vue3 前端开发进阶', 'https://i0.hdslb.com/bfs/archive/vue1.jpg', 'https://www.bilibili.com/video/BV1bb411c7mH', 'Vue3 新特性深入讲解', 1, 5430, 298),
(2, 'Docker 容器化部署指南', 'https://i0.hdslb.com/bfs/archive/docker1.jpg', 'https://www.bilibili.com/video/BV1cc411c7mI', 'Docker 基础与实战', 1, 4280, 256),
(2, 'Redis 缓存实战应用', 'https://i0.hdslb.com/bfs/archive/redis1.jpg', 'https://www.bilibili.com/video/BV1dd411c7mJ', 'Redis 数据结构与缓存策略', 1, 3890, 189);

-- 插入首页推荐视频
INSERT INTO `home_recommend_video` (`video_post_id`, `position_type`, `sort_order`) VALUES
(1, 1, 1),
(2, 2, 1),
(3, 2, 2),
(4, 2, 3),
(5, 2, 4);

-- 插入测试帖子
INSERT INTO `question_topic` (`user_id`, `title`, `content`, `category`, `status`, `is_solved`, `view_count`, `reply_count`, `like_count`) VALUES
(1, '如何理解数据结构中的栈和队列？', '我对栈和队列的区别总是分不清，谁能帮我解释一下？', 'QUESTION', 1, 1, 125, 3, 8),
(2, '二叉树遍历的实现方法', '求二叉树的前序、中序、后序遍历的代码实现', 'QUESTION', 1, 0, 89, 1, 5),
(1, 'Java 接口和抽象类的区别', '面试经常被问到这个问题，有没有通俗易懂的解释？', 'DISCUSSION', 1, 0, 156, 2, 12);

-- 插入测试回复
INSERT INTO `question_reply` (`topic_id`, `user_id`, `content`, `user_role`, `user_name`, `is_accepted`, `like_count`) VALUES
(1, 3, '栈是后进先出（LIFO），队列是先进先出（FIFO）。', 'TEACHER', '王老师', 1, 15),
(1, 4, '我画个图给你解释吧...', 'STUDENT', '赵六', 0, 3),
(2, 3, '前序遍历：根左右；中序遍历：左根右；后序遍历：左右根。', 'TEACHER', '王老师', 0, 8);

SELECT '====================================' AS message;
SELECT 'SmartEdu Platform 数据库初始化完成！' AS message;
SELECT '====================================' AS message;
SELECT CONCAT('数据库：', DATABASE()) AS info;
SELECT CONCAT('表数量：', (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE())) AS info;
