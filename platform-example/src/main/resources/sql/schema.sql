-- ============================================================================
-- platform-example 数据库初始化脚本
-- 依据实体: com.aurora.example.entity.User / Log（均继承 BaseEntity）
-- BaseEntity 字段: create_time（INSERT 自动填充）/ update_time（INSERT_UPDATE 自动填充）
-- ============================================================================

CREATE DATABASE IF NOT EXISTS demo DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE demo;

-- ----------------------------------------------------------------------------
-- t_user  用户表
-- 对应实体: com.aurora.example.entity.User
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS t_user;
CREATE TABLE t_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT       COMMENT '主键',
    name        VARCHAR(64)  NOT NULL                      COMMENT '用户名',
    age         INT          DEFAULT NULL                  COMMENT '年龄',
    email       VARCHAR(128) DEFAULT NULL                  COMMENT '邮箱',
    status      TINYINT      NOT NULL DEFAULT 0            COMMENT '状态：0-正常 1-禁用',
    version     INT          NOT NULL DEFAULT 0            COMMENT '乐观锁版本号（@Version）',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                       COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_name   (name),
    KEY idx_status (status),
    KEY idx_email  (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ----------------------------------------------------------------------------
-- t_log  日志表（动态表名基表，按月分表：t_log_yyyyMM）
-- 对应实体: com.aurora.example.entity.Log
-- 触发分表: RequestThread.addParam(Constants.DYNAMIC_TABLE_SUFFIX, "202606")
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS t_log;
CREATE TABLE t_log (
    id          BIGINT      NOT NULL AUTO_INCREMENT       COMMENT '主键',
    user_id     BIGINT      DEFAULT NULL                  COMMENT '用户ID',
    action      VARCHAR(64) DEFAULT NULL                  COMMENT '操作类型',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP                       COMMENT '创建时间',
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_id     (user_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表（按月分表基表）';

-- 月份分表示例（动态表名启用后会路由到 t_log_yyyyMM）
DROP TABLE IF EXISTS t_log_202606;
CREATE TABLE t_log_202606 LIKE t_log;

-- ----------------------------------------------------------------------------
-- 测试数据
-- ----------------------------------------------------------------------------
INSERT INTO t_user (name, age, email, status) VALUES
('张三', 25, 'zhangsan@example.com', 0),
('李四', 30, 'lisi@example.com',     1),
('王五', 28, 'wangwu@example.com',   0);

INSERT INTO t_log (user_id, action) VALUES
(1, 'LOGIN'),
(2, 'LOGIN'),
(1, 'UPDATE_PROFILE');

-- ============================================================================
-- quartz-spring-boot-starter 定时任务表
-- 对应实体: com.aurora.starter.quartz.domain.QuartzJob / QuartzJobLog
-- ============================================================================

DROP TABLE IF EXISTS `quartz_job`;
CREATE TABLE `quartz_job` (
    `job_id`          BIGINT       NOT NULL COMMENT '任务ID',
    `job_name`        VARCHAR(64)  NOT NULL COMMENT '任务名称',
    `job_group`       VARCHAR(64)           DEFAULT 'DEFAULT' COMMENT '任务分组',
    `cron_expression` VARCHAR(255) NOT NULL COMMENT 'Cron表达式',
    `invoke_target`   VARCHAR(500) NOT NULL COMMENT '调用目标字符串,例:beanName.method(''a'',1L)',
    `concurrent`      CHAR(1)               DEFAULT '0' COMMENT '是否并发 0=允许 1=禁止',
    `misfire_policy`  CHAR(1)               DEFAULT '0' COMMENT 'misfire策略 0=默认 1=立即触发 2=触发一次 3=不触发',
    `status`          CHAR(1)               DEFAULT '0' COMMENT '状态 0=正常 1=暂停',
    PRIMARY KEY (`job_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='定时任务表';

DROP TABLE IF EXISTS `quartz_job_log`;
CREATE TABLE `quartz_job_log` (
    `log_id`         BIGINT       NOT NULL COMMENT '日志ID',
    `job_id`         BIGINT                DEFAULT NULL COMMENT '任务ID',
    `job_name`       VARCHAR(64)           DEFAULT NULL COMMENT '任务名称',
    `job_group`      VARCHAR(64)           DEFAULT NULL COMMENT '任务分组',
    `invoke_target`  VARCHAR(500)          DEFAULT NULL COMMENT '调用目标字符串',
    `start_time`     DATETIME              DEFAULT NULL COMMENT '开始时间',
    `stop_time`      DATETIME              DEFAULT NULL COMMENT '结束时间',
    `cost_millis`    BIGINT                DEFAULT NULL COMMENT '耗时(毫秒)',
    `job_message`    VARCHAR(500)          DEFAULT NULL COMMENT '任务消息',
    `status`         CHAR(1)               DEFAULT '0' COMMENT '执行状态 0=成功 1=失败',
    `exception_info` TEXT                  COMMENT '异常信息',
    PRIMARY KEY (`log_id`),
    KEY `idx_job_id` (`job_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='定时任务执行日志表';

-- 测试数据:每隔5秒输出一行日志
INSERT INTO `quartz_job` VALUES
(1, '示例任务', 'DEFAULT', '0/5 * * * * ?', 'exampleTask.doSomething()', '0', '0', '0');
