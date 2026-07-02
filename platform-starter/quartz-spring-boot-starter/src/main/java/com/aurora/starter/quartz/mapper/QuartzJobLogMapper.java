package com.aurora.starter.quartz.mapper;

import com.aurora.starter.quartz.domain.QuartzJobLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 定时任务日志 Mapper.
 */
@Mapper
public interface QuartzJobLogMapper extends BaseMapper<QuartzJobLog> {
}
