package com.aurora.example.controller;

import com.aurora.example.entity.User;
import com.aurora.example.query.UserQuery;
import com.aurora.example.service.UserService;
import com.aurora.starter.common.core.model.SortBy;
import com.aurora.starter.common.core.page.PageParam;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户 Controller.
 *
 * @author whb
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<User> list(UserQuery query) {
        return userService.list(query);
    }

    @GetMapping("/page")
    public IPage<User> page(@RequestBody UserQuery query,
                            @RequestParam(defaultValue = "1") long pageNo,
                            @RequestParam(defaultValue = "10") long pageSize) {
        return userService.page(query, pageNo, pageSize);
    }

    @PostMapping
    public Long save(@RequestBody User user) {
        return userService.save(user);
    }

    @PutMapping
    public int update(@RequestBody User user) {
        return userService.update(user);
    }

    @GetMapping("/sorted")
    public List<User> listSorted(UserQuery query, PageParam pageParam) {
        return userService.listWithSort(query, pageParam.getSort());
    }
}