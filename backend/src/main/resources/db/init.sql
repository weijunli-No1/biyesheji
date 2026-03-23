CREATE DATABASE IF NOT EXISTS biyesheji DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE biyesheji;

-- ===================== 学院表 =====================
CREATE TABLE IF NOT EXISTS `college` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT,
    `name`        VARCHAR(64) NOT NULL COMMENT '学院名称',
    `code`        VARCHAR(32) COMMENT '学院代码',
    `deleted`     TINYINT NOT NULL DEFAULT 0,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学院表';

-- ===================== 专业表 =====================
CREATE TABLE IF NOT EXISTS `major` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT,
    `name`        VARCHAR(64) NOT NULL COMMENT '专业名称',
    `code`        VARCHAR(32) COMMENT '专业代码',
    `college_id`  BIGINT NOT NULL COMMENT '所属学院',
    `deleted`     TINYINT NOT NULL DEFAULT 0,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name_college` (`name`, `college_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专业表';

-- ===================== 班级表 =====================
CREATE TABLE IF NOT EXISTS `class_group` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT,
    `name`        VARCHAR(64) NOT NULL COMMENT '班级名称',
    `major_id`    BIGINT NOT NULL COMMENT '所属专业',
    `college_id`  BIGINT NOT NULL COMMENT '所属学院（冗余）',
    `enroll_year` SMALLINT COMMENT '入学年份',
    `deleted`     TINYINT NOT NULL DEFAULT 0,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级表';

-- ===================== 用户表 =====================
-- 角色: 1学生 2指导教师 3评阅教师 4答辩委员 5专业管理员 6学院管理员 7教务管理员
CREATE TABLE IF NOT EXISTS `user` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT,
    `username`    VARCHAR(64) NOT NULL COMMENT '用户名（学号/工号）',
    `password`    VARCHAR(255) NOT NULL COMMENT '密码（BCrypt）',
    `real_name`   VARCHAR(32) NOT NULL COMMENT '真实姓名',
    `role`        TINYINT NOT NULL COMMENT '角色: 1学生 2指导教师 3评阅教师 4答辩委员 5专业管理员 6学院管理员 7教务管理员',
    `college_id`  BIGINT COMMENT '所属学院ID',
    `major_id`    BIGINT COMMENT '所属专业ID（学生/专业管理员）',
    `class_id`    BIGINT COMMENT '所属班级ID（学生）',
    `email`       VARCHAR(128) COMMENT '邮箱',
    `phone`       VARCHAR(20) COMMENT '手机号',
    `avatar`      VARCHAR(255) COMMENT '头像URL',
    `status`      TINYINT NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
    `deleted`     TINYINT NOT NULL DEFAULT 0,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ===================== 工作流时间节点配置表 =====================
-- college_id=0 且 major_id=0 表示全局默认
-- college_id>0 且 major_id=0 表示学院级配置
-- college_id>0 且 major_id>0 表示专业级配置（优先级最高）
CREATE TABLE IF NOT EXISTS `workflow_config` (
    `id`           BIGINT NOT NULL AUTO_INCREMENT,
    `stage`        VARCHAR(32) NOT NULL COMMENT '阶段键',
    `stage_name`   VARCHAR(64) NOT NULL COMMENT '阶段名称',
    `start_time`   DATETIME NOT NULL,
    `end_time`     DATETIME NOT NULL,
    `year`         SMALLINT NOT NULL COMMENT '届次',
    `college_id`   BIGINT NOT NULL DEFAULT 0 COMMENT '0=全局',
    `major_id`     BIGINT NOT NULL DEFAULT 0 COMMENT '0=全局或学院级',
    `description`  VARCHAR(255),
    `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stage_scope` (`stage`, `year`, `college_id`, `major_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流时间节点';

-- ===================== 课题表 =====================
CREATE TABLE IF NOT EXISTS `topic` (
    `id`             BIGINT NOT NULL AUTO_INCREMENT,
    `title`          VARCHAR(255) NOT NULL,
    `type`           TINYINT NOT NULL COMMENT '1设计类 2论文类 3软件类 4实验类',
    `description`    TEXT,
    `requirement`    TEXT,
    `max_students`   TINYINT NOT NULL DEFAULT 1,
    `teacher_id`     BIGINT NOT NULL,
    `college_id`     BIGINT NOT NULL DEFAULT 0 COMMENT '所属学院',
    `major_id`       BIGINT NOT NULL DEFAULT 0 COMMENT '所属专业',
    `year`           SMALLINT NOT NULL,
    `status`         TINYINT NOT NULL DEFAULT 0 COMMENT '0待审批 1已发布 2已关闭 3已驳回',
    `reject_reason`  VARCHAR(255),
    `deleted`        TINYINT NOT NULL DEFAULT 0,
    `create_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课题表';

-- ===================== 选题关系表 =====================
CREATE TABLE IF NOT EXISTS `topic_selection` (
    `id`           BIGINT NOT NULL AUTO_INCREMENT,
    `student_id`   BIGINT NOT NULL,
    `topic_id`     BIGINT NOT NULL,
    `teacher_id`   BIGINT NOT NULL,
    `college_id`   BIGINT NOT NULL DEFAULT 0,
    `major_id`     BIGINT NOT NULL DEFAULT 0,
    `year`         SMALLINT NOT NULL,
    `status`       TINYINT NOT NULL DEFAULT 0 COMMENT '0待确认 1已确认 2已拒绝 3已解除',
    `apply_time`   DATETIME,
    `confirm_time` DATETIME,
    `deleted`      TINYINT NOT NULL DEFAULT 0,
    `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_year` (`student_id`, `year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='选题关系表';

-- ===================== 任务书表 =====================
CREATE TABLE IF NOT EXISTS `task_book` (
    `id`              BIGINT NOT NULL AUTO_INCREMENT,
    `selection_id`    BIGINT NOT NULL,
    `student_id`      BIGINT NOT NULL,
    `teacher_id`      BIGINT NOT NULL,
    `content`         TEXT,
    `references`      TEXT,
    `schedule`        TEXT,
    `file_url`        VARCHAR(255),
    `status`          TINYINT NOT NULL DEFAULT 0 COMMENT '0草稿 1已下达 2学生已确认',
    `issue_time`      DATETIME,
    `confirm_time`    DATETIME,
    `deleted`         TINYINT NOT NULL DEFAULT 0,
    `create_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_selection` (`selection_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务书';

-- ===================== 开题报告表 =====================
CREATE TABLE IF NOT EXISTS `proposal` (
    `id`               BIGINT NOT NULL AUTO_INCREMENT,
    `selection_id`     BIGINT NOT NULL,
    `student_id`       BIGINT NOT NULL,
    `teacher_id`       BIGINT NOT NULL,
    `background`       TEXT,
    `literature`       TEXT,
    `method`           TEXT,
    `plan`             TEXT,
    `expected_result`  TEXT,
    `file_url`         VARCHAR(255),
    `translation_url`  VARCHAR(255),
    `status`           TINYINT NOT NULL DEFAULT 0 COMMENT '0草稿 1待导师审核 2待评审 3已通过 4已退回',
    `teacher_comment`  TEXT,
    `review_comment`   TEXT,
    `review_score`     TINYINT,
    `reject_reason`    VARCHAR(255),
    `submit_time`      DATETIME,
    `deleted`          TINYINT NOT NULL DEFAULT 0,
    `create_time`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_selection` (`selection_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='开题报告';

-- ===================== 指导日志表 =====================
CREATE TABLE IF NOT EXISTS `guidance_log` (
    `id`            BIGINT NOT NULL AUTO_INCREMENT,
    `selection_id`  BIGINT NOT NULL,
    `student_id`    BIGINT NOT NULL,
    `teacher_id`    BIGINT NOT NULL,
    `guide_time`    DATETIME NOT NULL,
    `content`       TEXT NOT NULL,
    `student_work`  TEXT,
    `next_plan`     TEXT,
    `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指导日志';

-- ===================== 中期检查表 =====================
CREATE TABLE IF NOT EXISTS `mid_check` (
    `id`              BIGINT NOT NULL AUTO_INCREMENT,
    `selection_id`    BIGINT NOT NULL,
    `student_id`      BIGINT NOT NULL,
    `teacher_id`      BIGINT NOT NULL,
    `completed_work`  TEXT,
    `problems`        TEXT,
    `next_plan`       TEXT,
    `progress`        TINYINT COMMENT '1正常 2滞后 3严重滞后',
    `status`          TINYINT NOT NULL DEFAULT 0 COMMENT '0未提交 1待导师审核 2已通过 3已退回',
    `teacher_comment` TEXT,
    `submit_time`     DATETIME,
    `deleted`         TINYINT NOT NULL DEFAULT 0,
    `create_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_selection` (`selection_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='中期检查';

-- ===================== 论文版本表 =====================
CREATE TABLE IF NOT EXISTS `thesis_version` (
    `id`            BIGINT NOT NULL AUTO_INCREMENT,
    `selection_id`  BIGINT NOT NULL,
    `student_id`    BIGINT NOT NULL,
    `teacher_id`    BIGINT NOT NULL,
    `version`       TINYINT NOT NULL DEFAULT 1,
    `version_type`  TINYINT NOT NULL COMMENT '1初稿 2修改稿 3送审稿 4定稿',
    `file_url`      VARCHAR(255) NOT NULL,
    `file_name`     VARCHAR(255),
    `file_size`     BIGINT,
    `comment`       TEXT,
    `status`        TINYINT NOT NULL DEFAULT 0 COMMENT '0待审阅 1已退回 2已通过',
    `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='论文版本';

-- ===================== 查重记录表 =====================
CREATE TABLE IF NOT EXISTS `check_record` (
    `id`             BIGINT NOT NULL AUTO_INCREMENT,
    `selection_id`   BIGINT NOT NULL,
    `student_id`     BIGINT NOT NULL,
    `file_url`       VARCHAR(255) NOT NULL,
    `check_time`     DATETIME,
    `similarity`     DECIMAL(5,2),
    `report_url`     VARCHAR(255),
    `engine`         VARCHAR(32) COMMENT 'CNKI/VIP',
    `status`         TINYINT NOT NULL DEFAULT 0 COMMENT '0待检测 1已通过 2未通过',
    `threshold`      DECIMAL(5,2),
    `create_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='查重记录';

-- ===================== 评阅表 =====================
CREATE TABLE IF NOT EXISTS `review` (
    `id`               BIGINT NOT NULL AUTO_INCREMENT,
    `selection_id`     BIGINT NOT NULL,
    `student_id`       BIGINT NOT NULL,
    `reviewer_id`      BIGINT NOT NULL,
    `thesis_url`       VARCHAR(255),
    `content_score`    TINYINT,
    `method_score`     TINYINT,
    `innovation_score` TINYINT,
    `writing_score`    TINYINT,
    `total_score`      TINYINT,
    `comment`          TEXT,
    `status`           TINYINT NOT NULL DEFAULT 0 COMMENT '0待评阅 1已完成 2已退回',
    `can_defense`      TINYINT COMMENT '1是 0否',
    `review_time`      DATETIME,
    `deleted`          TINYINT NOT NULL DEFAULT 0,
    `create_time`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评阅表';

-- ===================== 答辩小组表 =====================
CREATE TABLE IF NOT EXISTS `defense_group` (
    `id`           BIGINT NOT NULL AUTO_INCREMENT,
    `name`         VARCHAR(64) NOT NULL,
    `year`         SMALLINT NOT NULL,
    `college_id`   BIGINT NOT NULL DEFAULT 0,
    `major_id`     BIGINT NOT NULL DEFAULT 0,
    `location`     VARCHAR(128),
    `defense_time` DATETIME,
    `leader_id`    BIGINT,
    `secretary_id` BIGINT,
    `deleted`      TINYINT NOT NULL DEFAULT 0,
    `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答辩小组';

CREATE TABLE IF NOT EXISTS `defense_group_member` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT,
    `group_id`    BIGINT NOT NULL,
    `teacher_id`  BIGINT NOT NULL,
    `role`        TINYINT NOT NULL COMMENT '1组长 2成员 3秘书',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_teacher` (`group_id`, `teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答辩小组成员';

-- ===================== 答辩记录表 =====================
CREATE TABLE IF NOT EXISTS `defense_record` (
    `id`              BIGINT NOT NULL AUTO_INCREMENT,
    `selection_id`    BIGINT NOT NULL,
    `student_id`      BIGINT NOT NULL,
    `group_id`        BIGINT,
    `defense_time`    DATETIME,
    `defense_round`   TINYINT NOT NULL DEFAULT 1,
    `present_score`   TINYINT,
    `qa_score`        TINYINT,
    `total_score`     TINYINT,
    `result`          TINYINT COMMENT '1通过 2修改后通过 3不通过',
    `questions`       TEXT,
    `comment`         TEXT,
    `secretary_note`  TEXT,
    `deleted`         TINYINT NOT NULL DEFAULT 0,
    `create_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答辩记录';

-- ===================== 综合成绩表 =====================
CREATE TABLE IF NOT EXISTS `score` (
    `id`                BIGINT NOT NULL AUTO_INCREMENT,
    `selection_id`      BIGINT NOT NULL,
    `student_id`        BIGINT NOT NULL,
    `teacher_id`        BIGINT NOT NULL,
    `proposal_score`    TINYINT,
    `teacher_score`     TINYINT,
    `review_score`      TINYINT,
    `defense_score`     TINYINT,
    `total_score`       DECIMAL(5,2),
    `grade`             VARCHAR(8),
    `is_excellent`      TINYINT DEFAULT 0,
    `is_locked`         TINYINT DEFAULT 0,
    `teacher_comment`   TEXT,
    `create_time`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_selection` (`selection_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='综合成绩';

-- ===================== 通知公告表 =====================
CREATE TABLE IF NOT EXISTS `notice` (
    `id`           BIGINT NOT NULL AUTO_INCREMENT,
    `title`        VARCHAR(255) NOT NULL,
    `content`      TEXT NOT NULL,
    `type`         TINYINT NOT NULL DEFAULT 1 COMMENT '1公告 2提醒',
    `target_role`  TINYINT,
    `college_id`   BIGINT DEFAULT 0 COMMENT '0=全校',
    `publisher_id` BIGINT NOT NULL,
    `is_top`       TINYINT DEFAULT 0,
    `deleted`      TINYINT NOT NULL DEFAULT 0,
    `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知公告';


-- =====================================================================
-- 初始数据
-- 密码均为 password123 的 BCrypt 哈希
-- $2a$10$Upz6Lpch4vuWqO/oGRwNDuAOAvoZQqFeI7G4jTNihlL8oL/BC8po6
-- =====================================================================

-- 学院
INSERT INTO `college` (`id`, `name`, `code`) VALUES
(1, '计算机学院', 'CS'),
(2, '软件学院',   'SE');

-- 专业
INSERT INTO `major` (`id`, `name`, `code`, `college_id`) VALUES
(1, '计算机科学与技术', 'CST', 1),
(2, '人工智能',        'AI',  1),
(3, '软件工程',        'SWE', 2);

-- 班级
INSERT INTO `class_group` (`id`, `name`, `major_id`, `college_id`, `enroll_year`) VALUES
(1, '计科2022-1班', 1, 1, 2022),
(2, '计科2022-2班', 1, 1, 2022),
(3, '人工智能2022-1班', 2, 1, 2022),
(4, '软工2022-1班', 3, 2, 2022);

-- 用户（角色: 1学生 2指导教师 3评阅教师 4答辩委员 5专业管理员 6学院管理员 7教务管理员）
INSERT INTO `user` (`username`, `password`, `real_name`, `role`, `college_id`, `major_id`, `class_id`, `email`, `status`) VALUES
-- 教务管理员
('admin',      '$2a$10$Upz6Lpch4vuWqO/oGRwNDuAOAvoZQqFeI7G4jTNihlL8oL/BC8po6', '教务管理员',    7, NULL, NULL, NULL, 'admin@uni.edu.cn', 1),
-- 学院管理员
('college001', '$2a$10$Upz6Lpch4vuWqO/oGRwNDuAOAvoZQqFeI7G4jTNihlL8oL/BC8po6', '计算机学院管理员', 6, 1, NULL, NULL, 'cs-admin@uni.edu.cn', 1),
('college002', '$2a$10$Upz6Lpch4vuWqO/oGRwNDuAOAvoZQqFeI7G4jTNihlL8oL/BC8po6', '软件学院管理员',   6, 2, NULL, NULL, 'se-admin@uni.edu.cn', 1),
-- 专业管理员
('major001',   '$2a$10$Upz6Lpch4vuWqO/oGRwNDuAOAvoZQqFeI7G4jTNihlL8oL/BC8po6', '计科专业负责人', 5, 1, 1, NULL, 'cst-admin@uni.edu.cn', 1),
('major002',   '$2a$10$Upz6Lpch4vuWqO/oGRwNDuAOAvoZQqFeI7G4jTNihlL8oL/BC8po6', 'AI专业负责人',  5, 1, 2, NULL, 'ai-admin@uni.edu.cn', 1),
-- 指导教师
('teacher001', '$2a$10$Upz6Lpch4vuWqO/oGRwNDuAOAvoZQqFeI7G4jTNihlL8oL/BC8po6', '张教授',   2, 1, 1, NULL, 'zhang@uni.edu.cn', 1),
('teacher002', '$2a$10$Upz6Lpch4vuWqO/oGRwNDuAOAvoZQqFeI7G4jTNihlL8oL/BC8po6', '李副教授', 2, 1, 2, NULL, 'li@uni.edu.cn', 1),
-- 评阅教师
('reviewer001','$2a$10$Upz6Lpch4vuWqO/oGRwNDuAOAvoZQqFeI7G4jTNihlL8oL/BC8po6', '王教授',   3, 1, NULL, NULL, 'wang@uni.edu.cn', 1),
-- 答辩委员
('defense001', '$2a$10$Upz6Lpch4vuWqO/oGRwNDuAOAvoZQqFeI7G4jTNihlL8oL/BC8po6', '赵教授',   4, 1, NULL, NULL, 'zhao@uni.edu.cn', 1),
-- 学生
('student001', '$2a$10$Upz6Lpch4vuWqO/oGRwNDuAOAvoZQqFeI7G4jTNihlL8oL/BC8po6', '王小明', 1, 1, 1, 1, 'wangxm@student.edu.cn', 1),
('student002', '$2a$10$Upz6Lpch4vuWqO/oGRwNDuAOAvoZQqFeI7G4jTNihlL8oL/BC8po6', '李小红', 1, 1, 1, 1, 'lixh@student.edu.cn', 1),
('student003', '$2a$10$Upz6Lpch4vuWqO/oGRwNDuAOAvoZQqFeI7G4jTNihlL8oL/BC8po6', '陈小军', 1, 1, 2, 3, 'chenxj@student.edu.cn', 1),
('student004', '$2a$10$Upz6Lpch4vuWqO/oGRwNDuAOAvoZQqFeI7G4jTNihlL8oL/BC8po6', '刘小芳', 1, 2, 3, 4, 'liuxf@student.edu.cn', 1);
-- 初始密码均为: password123

-- 全局工作流配置（college_id=0, major_id=0 表示全局默认）
INSERT INTO `workflow_config` (`stage`, `stage_name`, `start_time`, `end_time`, `year`, `college_id`, `major_id`) VALUES
('TOPIC_APPLY',  '课题申报',   '2025-09-01 00:00:00', '2025-10-10 23:59:59', 2026, 0, 0),
('TOPIC_SELECT', '学生选题',   '2025-10-11 00:00:00', '2025-10-31 23:59:59', 2026, 0, 0),
('TASK_BOOK',    '任务书下达', '2025-11-01 00:00:00', '2025-11-15 23:59:59', 2026, 0, 0),
('PROPOSAL',     '开题报告',   '2025-11-16 00:00:00', '2025-12-20 23:59:59', 2026, 0, 0),
('MID_CHECK',    '中期检查',   '2026-03-01 00:00:00', '2026-03-31 23:59:59', 2026, 0, 0),
('THESIS',       '论文提交',   '2026-04-01 00:00:00', '2026-05-10 23:59:59', 2026, 0, 0),
('CHECK',        '查重检测',   '2026-04-20 00:00:00', '2026-05-05 23:59:59', 2026, 0, 0),
('REVIEW',       '论文评阅',   '2026-05-06 00:00:00', '2026-05-20 23:59:59', 2026, 0, 0),
('DEFENSE',      '毕业答辩',   '2026-05-21 00:00:00', '2026-06-10 23:59:59', 2026, 0, 0);
