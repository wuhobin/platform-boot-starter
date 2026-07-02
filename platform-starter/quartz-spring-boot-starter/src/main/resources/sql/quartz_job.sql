-- 定时任务元数据表(starter 内置,业务方跑一次)
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

-- 定时任务执行日志表
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
