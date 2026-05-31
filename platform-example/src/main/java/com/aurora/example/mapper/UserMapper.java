package com.aurora.example.mapper;

import com.aurora.example.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper.
 *
 * @author whb
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}