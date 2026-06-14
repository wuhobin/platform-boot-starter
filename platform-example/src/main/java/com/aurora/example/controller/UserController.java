package com.aurora.example.controller;

import com.aurora.example.entity.User;
import com.aurora.example.query.UserQuery;
import com.aurora.example.service.UserService;
import com.aurora.example.vo.UserVO;
import com.aurora.starter.mybatisplus.model.PageParam;

import com.aurora.starter.mybatisplus.mybatis.PageUtils;
import com.aurora.starter.webmvc.domain.response.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
 * 用户 Controller - 演示 MyBatis-Plus 分页工具用法.
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
    public Result<List<User>> list(@RequestBody UserQuery query) {
        return Result.data(userService.list(query));
    }

    /**
     * 示例 1：基础分页（最常用，推荐）.
     * <p>前端传参：/users/page?page=1&size=10&orderBy=create_time desc,id asc</p>
     */
    @PostMapping("/page")
    public Result<IPage<User>> page(@RequestBody UserQuery query,
                                    @RequestParam(defaultValue = "1") long pageNo,
                                    @RequestParam(defaultValue = "10") long pageSize) {
        return Result.data(userService.page(query, pageNo, pageSize));
    }

    /**
     * 示例 2：使用 PageParam（自动处理参数校验和排序）.
     * <p>前端传参：/users/page-with-param?page=2&size=20&orderBy=-create_time,+id</p>
     * <p>支持两种 orderBy 格式：</p>
     * <ul>
     *   <li>SQL 标准：{@code create_time desc, id asc}</li>
     *   <li>后端约定：{@code -create_time,+id}（- 表示 DESC，+ 表示 ASC）</li>
     * </ul>
     */
    @PostMapping("/page-with-param")
    public Result<IPage<User>> pageWithParam(@RequestBody UserQuery query,final  PageParam pageParam) {
        Page<User> page = PageUtils.buildPage(pageParam);
        IPage<User> result = userService.page(query, page);
        return Result.data(result);
    }

    /**
     * 示例 3：分页结果 VO 转换（Entity → VO）.
     * <p>演示如何将分页查询的 Entity 结果转换为 VO，保留分页元信息。</p>
     */
    @GetMapping("/page-vo")
    public Result<Page<UserVO>> pageWithVO(UserQuery query, PageParam pageParam) {
        Page<User> page = PageUtils.buildPage(pageParam);
        Page<User> entityPage = (Page<User>) userService.page(query, page);

        // 使用 PageUtils.convert 进行 VO 转换
        Page<UserVO> voPage = PageUtils.convert(entityPage, user -> {
            UserVO vo = new UserVO();
            vo.setId(user.getId());
            vo.setName(user.getName());
            vo.setAge(user.getAge());
            vo.setEmail(user.getEmail());
            vo.setCreateTime(user.getCreateTime());
            vo.setStatusDesc(user.getStatus() == 1 ? "正常" : "禁用");
            return vo;
        });

        return Result.data(voPage);
    }

    /**
     * 示例 4：简单分页（不带动态条件，固定 pageNo 和 pageSize）.
     */
    @GetMapping("/page-simple")
    public Result<IPage<User>> pageSimple() {
        Page<User> page = PageUtils.buildPage(1, 10);
        IPage<User> result = userService.page(null, page);
        return Result.data(result);
    }

    @PostMapping
    public Result<Long> save(@RequestBody User user) {
        return Result.data(userService.save(user));
    }

    @PutMapping
    public Result<Integer> update(@RequestBody User user) {
        return Result.data(userService.update(user));
    }
}
