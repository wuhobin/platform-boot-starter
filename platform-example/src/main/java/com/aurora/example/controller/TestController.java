package com.aurora.example.controller;

import com.aurora.example.entity.Log;
import com.aurora.example.entity.User;
import com.aurora.example.mapper.LogMapper;
import com.aurora.example.mapper.UserMapper;
import com.aurora.starter.common.constant.Constants;
import com.aurora.starter.common.utils.threads.RequestThread;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 功能测试 Controller.
 *
 * @author whb
 */
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final UserMapper userMapper;
    private final LogMapper logMapper;

    // ==================== 全表扫描拦截测试 ====================

    /**
     * 无条件全表扫描 - 测试 FullScanInterceptor 是否拦截
     * GET /test/full-scan-blocked
     */
    @GetMapping("/full-scan-blocked")
    public List<User> fullScanBlocked() {
        // t_user 在 disable-full-scan-table 中，应抛 BadSqlGrammarException
        return userMapper.selectList(null);
    }

    /**
     * 带 limit 的全表扫描 - 应该放行
     * GET /test/full-scan-with-limit
     */
    @GetMapping("/full-scan-with-limit")
    public List<User> fullScanWithLimit() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.last("LIMIT 10");
        return userMapper.selectList(wrapper);
    }

    /**
     * 带 where 条件的查询 - 应该放行
     * GET /test/full-scan-with-condition?name=张三
     */
    @GetMapping("/full-scan-with-condition")
    public List<User> fullScanWithCondition(String name) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isBlank()) {
            wrapper.eq(User::getName, name);
        }
        return userMapper.selectList(wrapper);
    }

    // ==================== 动态表名测试 ====================

    /**
     * 查询 t_log（动态表名）- 使用当前月份后缀
     * GET /test/dynamic-table/current
     */
    @GetMapping("/dynamic-table/current")
    public List<Log> dynamicTableCurrent() {
        // 未设置后缀，fallback-to-current-month=false，原表名 t_log 不存在当月 suffix
        return logMapper.selectList(null);
    }

    /**
     * 查询 t_log（动态表名）- 手动指定后缀 202601
     * GET /test/dynamic-table/suffix
     */
    @GetMapping("/dynamic-table/suffix")
    public List<Log> dynamicTableWithSuffix() {
        try {
            RequestThread.addParam(Constants.DYNAMIC_TABLE_SUFFIX, "202601");
            return logMapper.selectList(null);
        } finally {
            RequestThread.clear();
        }
    }

    /**
     * 查询 t_log（动态表名）- 使用 DEFAULT，不追加后缀
     * GET /test/dynamic-table/default
     */
    @GetMapping("/dynamic-table/default")
    public List<Log> dynamicTableDefault() {
        try {
            RequestThread.addParam(Constants.DYNAMIC_TABLE_SUFFIX, "DEFAULT");
            return logMapper.selectList(null);
        } finally {
            RequestThread.clear();
        }
    }

    /**
     * 恢复全局默认表名模式（不追加任何后缀）
     * GET /test/dynamic-table/reset
     */
    @GetMapping("/dynamic-table/reset")
    public List<Log> dynamicTableReset() {
        try {
            // 显式设为 DEFAULT = 不追加后缀，查原表 t_log
            RequestThread.addParam(Constants.DYNAMIC_TABLE_SUFFIX, Constants.DYNAMIC_TABLE_DEFAULT_NAME);
            return logMapper.selectList(null);
        } finally {
            RequestThread.clear();
        }
    }
}