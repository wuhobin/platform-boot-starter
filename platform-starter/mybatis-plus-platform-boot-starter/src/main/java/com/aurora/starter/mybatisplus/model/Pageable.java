package com.aurora.starter.mybatisplus.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页数据 VO.
 *
 * <p>数据访问层无 OpenAPI 注解（保持模块隔离）；如需在接口文档中描述本类，
 * 由 Web 层在自己的 DTO 上 {@code @Schema} 引用即可。</p>
 *
 * @author zenghao
 * @date 2022/5/17
 */
@Data
public class Pageable<T> implements Serializable {

    @SuppressWarnings("rawtypes")
    private static final Pageable EMPTY_PAGE = new Pageable<>(0, Collections.emptyList(), null);

    /** 总记录数. */
    private long total;

    /** 列表数据. */
    private List<T> rows;

    /** 每页记录条数. */
    private Integer size;

    public Pageable() {
    }

    public Pageable(final long total, final List<T> rows, final Integer size) {
        this.total = total;
        this.rows = rows;
        this.size = size;
    }

    public static <T> Pageable<T> of(final long total, final List<T> rows) {
        return of(total, rows, null);
    }

    public static <T> Pageable<T> of(final long total, final List<T> rows, final Integer size) {
        return new Pageable<>(total, rows, size);
    }

    @SuppressWarnings("unchecked")
    public static <T> Pageable<T> empty() {
        return EMPTY_PAGE;
    }
}

