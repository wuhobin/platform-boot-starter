package com.aurora.starter.mybatisplus.mybatis;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.aurora.starter.common.core.model.SortBy;
import com.aurora.starter.common.core.page.PageParam;
import com.aurora.starter.common.utils.bean.ReflectUtils;
import com.aurora.starter.mybatisplus.annotation.QueryField;
import com.aurora.starter.mybatisplus.model.BaseQuery;
import com.aurora.starter.mybatisplus.enums.BetweenType;
import com.aurora.starter.mybatisplus.model.BetweenQueryAttribute;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 动态条件.
 *
 * @author Luo
 * @version 1.0
 * @date 2021-9-7 09:58
 */
@Slf4j
public class DynamicCondition {

    /**
     * 转换Wrapper条件.
     *
     * @param query 查询参数
     * @param <T>   类型
     * @return Wrapper
     */
    public static <T> Wrapper<T> toWrapper(final Object query) {
        return toWrapper(query, null);
    }

    /**
     * 转换Wrapper条件.
     *
     * @param query 查询参数
     * @param sort  排序
     * @param <T>   类型
     * @return Wrapper
     */
    public static <T> Wrapper<T> toWrapper(final Object query, final SortBy sort) {
        QueryWrapper<T> wrapper = Wrappers.query();
        if (ObjectUtil.isEmpty(query)) {
            return wrapper;
        }

        List<Field> fields = ReflectUtils.getDeclaredFields(query.getClass(), PageParam.class);

        Object value;
        QueryCondition cond;
        for (Field field : fields) {
            value = ReflectUtils.invokeGetterMethod(query, field.getName());
            if (value == null) {
                continue;
            }
            // 根据属性上的注解获取查询条件
            cond = field.isAnnotationPresent(QueryField.class) ? QueryCondition.of(field.getAnnotation(QueryField.class)) : QueryCondition.defaultCondition();

            // 属性值为 空字符串 判断
            boolean isBlank = field.getType() == String.class && StringUtils.isBlank((String) value);
            if (cond.isIgnore() || (!cond.isQueryEmpty() && isBlank)) {
                continue;
            }
            if (StringUtils.isBlank(cond.getFiledName())) {
                cond.setFiledName(field.getName());
            }
            // mybatis-plus 将查询字段 属性名转为数据库列名（此处为驼峰转下划线）
            cond.setFiledName(StringUtils.camelToUnderline(cond.getFiledName()));

            // 多字段组 OR 查询组合
            if (CollUtil.isNotEmpty(cond.getOrFiledNames())) {
                Object finalValue = value;
                QueryCondition finalCond = cond;
                wrapper.and(x -> {
                    for (String filedName : finalCond.getOrFiledNames()) {
                        QueryCondition orCond = finalCond.copy();
                        orCond.setFiledName(StringUtils.camelToUnderline(filedName));
                        appendCondition(x.or(), orCond, field, finalValue);
                    }
                });
            } else {
                appendCondition(wrapper, cond, field, value);
            }
        }

        // 排序
        if (Objects.nonNull(sort) && CollUtil.isNotEmpty(sort.getOrders())) {
            sort.getOrders().forEach(x -> wrapper.orderBy(StringUtils.isNotBlank(x.getProperty()), SortBy.Direction.ASC == x.getDirection(), x.getProperty()));
        }

        return wrapper;
    }

    /**
     * 添加条件.
     *
     * @param wrapper  wrapper
     * @param cond     查询条件
     * @param field    字段
     * @param value    字段值
     * @param <T>      类型
     */
    private static <T> void appendCondition(final QueryWrapper<T> wrapper, final QueryCondition cond, final Field field, final Object value) {
        switch (cond.getOperator()) {
            case EQ:
            case ENUM_EQ:
                wrapper.eq(cond.getFiledName(), value);
                break;
            case NE:
                wrapper.ne(cond.getFiledName(), value);
                break;
            case LIKE:
                wrapper.like(cond.getFiledName(), value);
                break;
            case LIKE_LEFT:
                wrapper.likeLeft(cond.getFiledName(), value);
                break;
            case LIKE_RIGHT:
                wrapper.likeRight(cond.getFiledName(), value);
                break;
            case NOT_LIKE:
                wrapper.notLike(cond.getFiledName(), value);
                break;
            case GT:
                wrapper.gt(cond.getFiledName(), value);
                break;
            case LT:
                wrapper.lt(cond.getFiledName(), value);
                break;
            case GTE:
                wrapper.ge(cond.getFiledName(), value);
                break;
            case LTE:
                wrapper.le(cond.getFiledName(), value);
                break;
            case BETWEEN:
                if (ObjectUtil.isNotEmpty(value) && value instanceof BetweenQueryAttribute<?> attribute) {
                    appendBetweenCondition(wrapper, cond, attribute, true);
                }
                break;
            case NOT_BETWEEN:
                if (ObjectUtil.isNotEmpty(value) && value instanceof BetweenQueryAttribute<?> attribute) {
                    appendBetweenCondition(wrapper, cond, attribute, false);
                }
                break;
            case NOT_NULL:
                wrapper.isNotNull(cond.getFiledName());
                break;
            case NOT_EMPTY:
                wrapper.and(w -> w.isNotNull(cond.getFiledName()).or().ne(cond.getFiledName(), StringPool.EMPTY));
                break;
            case NOT_AND_EMPTY:
                wrapper.and(w -> w.isNotNull(cond.getFiledName()).ne(cond.getFiledName(), StringPool.EMPTY));
                break;
            case IS_NULL:
                wrapper.isNull(cond.getFiledName());
                break;
            case IS_EMPTY:
                wrapper.and(w -> w.isNull(cond.getFiledName()).or().eq(cond.getFiledName(), StringPool.EMPTY));
                break;
            case IS_AND_EMPTY:
                wrapper.and(w -> w.isNull(cond.getFiledName()).eq(cond.getFiledName(), StringPool.EMPTY));
                break;
            case GROUP:
                wrapper.groupBy(camelToUnderlines(cond.getFiledNames()));
                break;
            case IN:
                if (ObjectUtil.isNotEmpty(value)) {
                    if (value instanceof Collection) {
                        wrapper.in(cond.getFiledName(), (Collection<?>) value);
                    } else if (value.getClass().isArray()) {
                        wrapper.in(cond.getFiledName(), (Object[]) value);
                    } else {
                        wrapper.in(cond.getFiledName(), value);
                    }
                }
                break;
            case NOT_IN:
                if (ObjectUtil.isNotEmpty(value)) {
                    if (value instanceof Collection) {
                        wrapper.notIn(cond.getFiledName(), (Collection<?>) value);
                    } else if (value.getClass().isArray()) {
                        wrapper.notIn(cond.getFiledName(), (Object[]) value);
                    } else {
                        wrapper.notIn(cond.getFiledName(), value);
                    }
                }
                break;
            case JSON_ARRAY_ALL_MATCH:
                if (ObjectUtil.isNotEmpty(value)) {
                    wrapper.apply(generateJsonArrayFieldToWrapper(field, cond, value, false).getSqlSegment());
                }
                break;
            case JSON_ARRAY_ALL_NOT_MATCH:
                if (ObjectUtil.isNotEmpty(value)) {
                    wrapper.apply(generateJsonArrayFieldNotInToWrapper(field, cond, value, false).getSqlSegment());
                }
                break;
            case JSON_ARRAY_ALL_MATCH_DORIS:
                if (ObjectUtil.isNotEmpty(value)) {
                    wrapper.apply(generateJsonArrayFieldToWrapper(field, cond, value, false, true).getSqlSegment());
                }
                break;
            case JSON_ARRAY_ANY_MATCH:
                if (ObjectUtil.isNotEmpty(value)) {
                    wrapper.apply(generateJsonArrayFieldToWrapper(field, cond, value, true).getSqlSegment());
                }
                break;
            case JSON_ARRAY_ANY_MATCH_DORIS:
                if (ObjectUtil.isNotEmpty(value)) {
                    wrapper.apply(generateJsonArrayFieldToWrapper(field, cond, value, true, true).getSqlSegment());
                }
                break;
            case JSON_ARRAY_ANY_MATCH_WITH_EMPTY:
                if (ObjectUtil.isNotEmpty(value)) {
                    wrapper.apply(generateJsonArrayFieldToWrapperWithEmpty(field, cond, value, true).getSqlSegment());
                }
                break;
            case JSON_ARRAY_ANY_MATCH_WITH_EMPTY_DORIS:
                if (ObjectUtil.isNotEmpty(value)) {
                    wrapper.apply(generateJsonArrayFieldToWrapperWithEmpty(field, cond, value, true, true).getSqlSegment());
                }
                break;
            case FIND_IN_SET:
                wrapper.apply(" FIND_IN_SET({0}," + cond.getFiledName() + ")", value);
                break;
            case LIMIT:
                if (ObjectUtil.isNotEmpty(value) && (value instanceof Integer || value instanceof Long)) {
                    wrapper.last("LIMIT " + value);
                }
                break;
            case DISTINCT:
                List<String> filedNames = camelToUnderlines(cond.getFiledNames());
                wrapper.select("DISTINCT " + String.join(StringPool.COMMA, filedNames));
                break;
            default:
                break;
        }
    }

    /**
     * 添加 BETWEEN / NOT_BETWEEN 条件，根据 {@link BetweenType} 选择左/右开闭。
     */
    private static <T> void appendBetweenCondition(QueryWrapper<T> wrapper, QueryCondition cond,
                                                   BetweenQueryAttribute<?> attr, boolean positive) {
        String column = cond.getFiledName();
        Object start = attr.getStart();
        Object end = attr.getEnd();
        switch (attr.getBetweenType()) {
            case BOTH_EQUAL:       // [start, end]
                if (positive) wrapper.ge(column, start).le(column, end);
                else         wrapper.notIn(column, start, end);
                break;
            case ONLY_MIN_EQUAL:   // [start, end)
                if (positive) wrapper.ge(column, start).lt(column, end);
                else         wrapper.and(w -> w.ne(column, start).or().lt(column, start));
                break;
            case ONLY_MAX_EQUAL:    // (start, end]
                if (positive) wrapper.gt(column, start).le(column, end);
                else         wrapper.and(w -> w.lt(column, end).or().gt(column, end));
                break;
            case BOTH_NOT_CONTAIN: // (start, end)
            default:
                if (positive) wrapper.gt(column, start).lt(column, end);
                else         wrapper.and(w -> w.lt(column, start).or().gt(column, end));
                break;
        }
    }

    /**
     * 多字段集合驼峰转下划线.
     *
     * @param filedNames filedNames
     * @return 结果
     */
    private static List<String> camelToUnderlines(final List<String> filedNames) {
        if (ObjectUtil.isEmpty(filedNames)) {
            return Collections.emptyList();
        }
        return filedNames.stream().map(StringUtils::camelToUnderline).collect(Collectors.toList());
    }

    /**
     * 构建json列表查询wrapper.
     *
     * @param field field
     * @param cond cond
     * @param value value
     * @param isOr isOr
     * @return Wrapper<T>
     * @param <T> 类型
     */
    private static <T> Wrapper<T> generateJsonArrayFieldToWrapper(final Field field, final QueryCondition cond, final Object value, final Boolean isOr) {
        return generateJsonArrayFieldToWrapper(field, cond, value, isOr, false);
    }

    /**
     * 构建json列表查询wrapper.
     *
     * @param field field
     * @param cond cond
     * @param value value
     * @param isOr isOr
     * @param isDoris isDoris
     * @return Wrapper<T>
     * @param <T> 类型
     */
    private static <T> Wrapper<T> generateJsonArrayFieldToWrapper(final Field field, final QueryCondition cond, final Object value,
                                                                  final Boolean isOr, final Boolean isDoris) {
        QueryWrapper<T> wrapper = Wrappers.query();

        Class<?> elementType = null;
        if (value.getClass().isArray()) {
            elementType = value.getClass().getComponentType();
        } else if (value instanceof Collection) {
            Type genericType = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
            if (genericType instanceof Class) {
                elementType = (Class<?>) genericType;
            }
        }

        boolean isStringType = elementType == String.class;
        String columnSqlTemplate = isDoris
                ? (isStringType ? "JSON_CONTAINS({0}, CAST({1} AS JSON))" : "JSON_CONTAINS({0}, CAST({1} AS JSON))")
                : (isStringType ? "JSON_CONTAINS({0}, {1})" : "JSON_CONTAINS({0}, {1})");

        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                applyJsonArrayCondition(wrapper, cond, Array.get(value, i), columnSqlTemplate, isStringType, isOr);
            }
        } else if (value instanceof Collection) {
            for (Object element : (Collection<?>) value) {
                applyJsonArrayCondition(wrapper, cond, element, columnSqlTemplate, isStringType, isOr);
            }
        }

        return wrapper;
    }

    private static <T> Wrapper<T> generateJsonArrayFieldNotInToWrapper(final Field field, final QueryCondition cond, final Object value,
                                                                  final Boolean isOr) {
        QueryWrapper<T> wrapper = Wrappers.query();

        Class<?> elementType = null;
        if (value.getClass().isArray()) {
            elementType = value.getClass().getComponentType();
        } else if (value instanceof Collection) {
            Type genericType = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
            if (genericType instanceof Class) {
                elementType = (Class<?>) genericType;
            }
        }

        boolean isStringType = elementType == String.class;
        String columnSqlTemplate = "NOT JSON_CONTAINS({0}, CAST({1} AS JSON))";

        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                applyJsonArrayCondition(wrapper, cond, Array.get(value, i), columnSqlTemplate, isStringType, isOr);
            }
        } else if (value instanceof Collection) {
            for (Object element : (Collection<?>) value) {
                applyJsonArrayCondition(wrapper, cond, element, columnSqlTemplate, isStringType, isOr);
            }
        }

        return wrapper;
    }

    /**
     * 构建json列表查询wrapper.
     */
    private static <T> Wrapper<T> generateJsonArrayFieldToWrapperWithEmpty(final Field field, final QueryCondition cond, final Object value, final Boolean isOr) {
        return generateJsonArrayFieldToWrapperWithEmpty(field, cond, value, isOr, false);
    }

    /**
     * 构建json列表查询wrapper.
     */
    private static <T> Wrapper<T> generateJsonArrayFieldToWrapperWithEmpty(final Field field, final QueryCondition cond, final Object value, final Boolean isOr, final Boolean isDoris) {
        Wrapper<T> baseWrapper = generateJsonArrayFieldToWrapper(field, cond, value, isOr, isDoris);
        QueryWrapper<T> wrapper = (QueryWrapper<T>) baseWrapper;
        String column = cond.getFiledName();
        wrapper.or();
        wrapper.apply(" JSON_LENGTH({0}) = 0", column);
        wrapper.or();
        wrapper.apply("{0} IS NULL", column);
        return wrapper;
    }

    /**
     * 拼接json列表查询条件.
     * <p>SQL 模板使用 {0}=列名（字符串拼接，仅允许来自 {@link QueryField}）、{1}=值（走 MyBatis 参数绑定，防注入）。</p>
     *
     * @param wrapper wrapper
     * @param cond cond
     * @param element element
     * @param columnSqlTemplate 列 SQL 模板，必须含 {0} 与 {1} 两个占位
     * @param isStringType json 元素是否为字符串
     * @param isOr isOr
     * @param <T> 类型
     */
    private static <T> void applyJsonArrayCondition(QueryWrapper<T> wrapper, QueryCondition cond, Object element,
                                                    String columnSqlTemplate, boolean isStringType, Boolean isOr) {
        if (isOr) {
            wrapper.or();
        }
        // 列名是字段元数据，安全；元素值必须通过参数绑定。MyBatis-Plus 的 apply 使用 {0} 占位符做参数绑定。
        String sqlWithColumn = StrUtil.format(columnSqlTemplate, cond.getFiledName(), "{0}");
        Object boundValue = isStringType ? StrUtil.format("\"{}\"", element) : element;
        wrapper.apply(sqlWithColumn, boundValue);
    }

}
