package com.aurora.example.controller;

import com.aurora.example.entity.User;
import com.aurora.example.query.UserQuery;
import com.aurora.example.service.UserService;
import com.aurora.starter.common.core.page.PageParam;
import com.aurora.starter.webmvc.domain.response.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户 Controller.
 *
 * <p>所有方法返回 {@link Result} 包装，由 webmvc 通用件统一响应体。</p>
 *
 * @author whb
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public Result<List<User>> list(UserQuery query) {
        return Result.data(userService.list(query));
    }

    @GetMapping("/page")
    public Result<IPage<User>> page(@RequestBody UserQuery query,
                                    @RequestParam(defaultValue = "1") long pageNo,
                                    @RequestParam(defaultValue = "10") long pageSize) {
        return Result.data(userService.page(query, pageNo, pageSize));
    }

    @PostMapping
    public Result<Long> save(@RequestBody User user) {
        return Result.data(userService.save(user));
    }

    @PutMapping
    public Result<Integer> update(@RequestBody User user) {
        return Result.data(userService.update(user));
    }

    @GetMapping("/sorted")
    public Result<List<User>> listSorted(UserQuery query, PageParam pageParam) {
        return Result.data(userService.listWithSort(query, pageParam.getSort()));
    }
}
