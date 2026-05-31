package com.aurora.example.mapper;

import com.aurora.example.entity.Log;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 日志 Mapper.
 *
 * @author whb
 */
@Mapper
public interface LogMapper extends BaseMapper<Log> {
}