package com.aurora.starter.common.core.page;



import com.aurora.starter.common.core.model.SortBy;
import lombok.Data;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

/**
 * 分页参数.
 *
 * @author zenghao
 * @date 2022/6/1
 */
@Data
public class PageParam implements Serializable {

    private static final long serialVersionUID = -6115038235863956119L;

    public static final String SPACE = " ";

    public static final String ASC = "asc";

    public static final String DESC = "desc";

    public static final String COMMA = ",";

    public static final String REDUCE = "-";

    public static final String PLUS = "+";

    public static final int DEFAULT_PAGE = 1;

    public static final int DEFAULT_SIZE = 10;

    public static final int MAX_SIZE = 2000;

    private Integer page;

    private Integer size;

    private String orderBy;

    private Boolean reasonable;

    private Boolean count;

    public PageParam() {
    }

    public PageParam(final Integer page, final Integer size) {
        this(page, size, null);
    }

    public PageParam(final Integer page, final Integer size, final String orderBy) {
        this(page, size, orderBy, false);
    }

    public PageParam(final Integer page, final Integer size, final String orderBy, final Boolean reasonable) {
        this.page = Objects.isNull(page) || page < DEFAULT_PAGE ? DEFAULT_PAGE : page;
        if (size == null || size < 1) {
            this.size = DEFAULT_SIZE;
        } else {
            this.size = size > MAX_SIZE ? MAX_SIZE : size;
        }
        this.orderBy = orderBy;
        this.reasonable = reasonable;
        this.count = true;
    }

    /**
     * 获取排序.
     *
     * @return 结果
     */
    public SortBy getSort() {
        // 判断是否为空
        if (this.getOrderBy() == null || this.getOrderBy().trim().isEmpty()) {
            return null;
        }
        // 排序转换
        return SortBy.of(this.getOrderBy());
    }

    public Boolean getCount() {
        return Optional.ofNullable(count).orElse(true);
    }
}
