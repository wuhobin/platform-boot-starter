package com.aurora.starter.mybatisplus.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

/**
 * 分页参数（纯 MyBatis-Plus 原生分页方案）.
 *
 * @author wuhongbin
 * @date 2022/6/1
 */
@Data
public class PageParam implements Serializable {

    private static final long serialVersionUID = -6115038235863956119L;

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 2000;

    /** 当前页码（从 1 开始）. */
    private Integer pageNum;

    /** 每页条数. */
    private Integer pageSize;

    /** 排序字符串，支持两种格式：
     * <ul>
     *   <li>SQL 标准：{@code "create_time desc, id asc"}</li>
     *   <li>后端约定：{@code "-create_time,+id"}（- 表示 DESC，+ 表示 ASC）</li>
     * </ul>
     */
    private String orderBy;

    public PageParam() {
    }

    public PageParam(final Integer page, final Integer size) {
        this(page, size, null);
    }

    public PageParam(final Integer page, final Integer size, final String orderBy) {
        this.pageNum = Objects.isNull(page) || page < DEFAULT_PAGE ? DEFAULT_PAGE : page;
        if (size == null || size < 1) {
            this.pageSize = DEFAULT_SIZE;
        } else {
            this.pageSize = Math.min(size, MAX_SIZE);
        }
        this.orderBy = orderBy;
    }
}
