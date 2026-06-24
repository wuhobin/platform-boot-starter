package com.aurora.starter.mybatisplus.mybatis;

import cn.hutool.json.JSONUtil;
import com.aurora.starter.common.utils.sql.SqlUtil;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * MyBatis-Plus 分页工具类（纯原生实现，零 PageHelper 依赖）.
 *
 * @author wuhongbin
 * @date 2026-06-11
 */
@Slf4j
public class PageUtils {

    /**
     * 从分页参数构造 MyBatis-Plus 的 {@link Page} 对象，自动处理 orderBy 字符串.
     *
     * @param pageParam 分页参数（page/size/orderBy）
     * @return MyBatis-Plus Page 对象，可直接传给 mapper.selectPage
     */
    public static <T> Page<T> buildPage(PageParam pageParam) {
        log.info("pageParam {}", JSONUtil.toJsonStr(pageParam));
        if (pageParam == null) {
            return new Page<>(PageParam.DEFAULT_PAGE, PageParam.DEFAULT_SIZE);
        }
        Page<T> page = new Page<>(pageParam.getPageNum(), pageParam.getPageSize());

        // 解析 orderBy 字符串并设置排序
        String orderBy = pageParam.getOrderBy();
        if (orderBy != null && !orderBy.trim().isEmpty()) {
            List<OrderItem> orders = parseOrderBy(orderBy);
            page.setOrders(orders);
        }

        return page;
    }

    /**
     * 快速构造分页对象（无排序）.
     */
    public static <T> Page<T> buildPage(long pageNo, long pageSize) {
        return new Page<>(pageNo, pageSize);
    }

    /**
     * 快速构造分页对象（带排序字符串）.
     */
    public static <T> Page<T> buildPage(long pageNo, long pageSize, String orderBy) {
        PageParam param = new PageParam((int) pageNo, (int) pageSize, orderBy);
        return buildPage(param);
    }

    /**
     * 分页结果 VO 转换：将 Page&lt;Entity&gt; 转成 Page&lt;VO&gt;，保留分页元信息.
     *
     * @param sourcePage 原始分页结果
     * @param converter  单条记录转换函数（Entity → VO）
     * @return 转换后的分页结果
     */
    public static <S, T> Page<T> convert(Page<S> sourcePage, Function<S, T> converter) {
        List<T> converted = new ArrayList<>(sourcePage.getRecords().size());
        for (S source : sourcePage.getRecords()) {
            converted.add(converter.apply(source));
        }

        Page<T> targetPage = new Page<>(sourcePage.getCurrent(), sourcePage.getSize(), sourcePage.getTotal());
        targetPage.setRecords(converted);
        return targetPage;
    }

    /**
     * 解析 orderBy 字符串为 MyBatis-Plus 的 {@link OrderItem} 列表.
     * <p>支持两种格式：</p>
     * <ul>
     *   <li>前端格式（SQL 标准）：{@code "create_time desc, id asc"}</li>
     *   <li>后端约定格式：{@code "-create_time,+id"}（- 表示 DESC，+ 表示 ASC）</li>
     * </ul>
     *
     * @param orderBy 排序字符串
     * @return OrderItem 列表
     */
    private static List<OrderItem> parseOrderBy(String orderBy) {
        // 后端约定格式（+/-）统一转换为 SQL 标准格式，复用 SqlUtil 逻辑
        String normalized = SqlUtil.convertSqlSort(orderBy);
        SqlUtil.escapeOrderBySql(normalized);

        List<OrderItem> orders = new ArrayList<>();
        String lowerOrder = normalized.toLowerCase();

        // 解析 SQL 标准格式："field1 desc, field2 asc, field3"
        if (lowerOrder.contains(" asc") || lowerOrder.contains(" desc")) {
            String[] parts = normalized.split(",");
            for (String part : parts) {
                part = part.trim();
                if (part.isEmpty()) continue;

                String lowerPart = part.toLowerCase();
                if (lowerPart.endsWith(" asc")) {
                    orders.add(OrderItem.asc(part.substring(0, part.length() - 4).trim()));
                } else if (lowerPart.endsWith(" desc")) {
                    orders.add(OrderItem.desc(part.substring(0, part.length() - 5).trim()));
                } else {
                    orders.add(OrderItem.asc(part));
                }
            }
        } else {
            orders.add(OrderItem.asc(normalized.trim()));
        }

        return orders;
    }
}
