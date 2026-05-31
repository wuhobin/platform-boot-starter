package com.aurora.example.service;

import com.aurora.example.entity.User;
import com.aurora.example.mapper.UserMapper;
import com.aurora.example.query.UserQuery;
import com.aurora.starter.common.core.model.SortBy;
import com.aurora.starter.mybatisplus.mybatis.DynamicCondition;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户 Service.
 *
 * @author whb
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    public List<User> list(UserQuery query) {
        Wrapper<User> wrapper = DynamicCondition.toWrapper(query);
        return userMapper.selectList(wrapper);
    }

    public List<User> listWithSort(UserQuery query, SortBy sort) {
        Wrapper<User> wrapper = DynamicCondition.toWrapper(query, sort);
        return userMapper.selectList(wrapper);
    }

    public IPage<User> page(UserQuery query, long pageNo, long pageSize) {
        Wrapper<User> wrapper = DynamicCondition.toWrapper(query);
        return userMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
    }

    public Long save(User user) {
        userMapper.insert(user);
        return user.getId();
    }

    public int update(User user) {
        return userMapper.updateById(user);
    }
}