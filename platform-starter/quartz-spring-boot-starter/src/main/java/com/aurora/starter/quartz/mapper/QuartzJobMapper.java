package com.aurora.starter.quartz.mapper;

import com.aurora.starter.quartz.domain.QuartzJob;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 定时任务 Mapper.
 */
@Mapper
public interface QuartzJobMapper extends BaseMapper<QuartzJob> {
}
