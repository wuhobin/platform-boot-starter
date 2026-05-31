package com.aurora.starter.mybatisplus.mybatis;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.aurora.starter.mybatisplus.annotation.Operator;
import com.aurora.starter.mybatisplus.annotation.QueryField;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 动态查询条件.
 *
 * @author Luo
 * @date 2021-9-7 09:58
 */
@Data
@Accessors(chain = true)
public class QueryCondition {

    private Operator operator;

    private String filedName;

    private List<String> orFiledNames;

    private List<String> filedNames;

    private boolean queryInner;

    private String innerName;

    private boolean queryEmpty;

    private boolean ignore;

    private boolean toMaster;

    /**
     * 复制.
     *
     * @return QueryCondition
     */
    public QueryCondition copy() {
        QueryCondition c = new QueryCondition();
        c.setOperator(this.operator);
        c.setFiledName(this.filedName);
        c.setOrFiledNames(this.orFiledNames);
        c.setFiledNames(this.filedNames);
        c.setQueryInner(this.queryInner);
        c.setInnerName(this.innerName);
        c.setQueryEmpty(this.queryEmpty);
        c.setIgnore(this.ignore);
        c.setToMaster(this.toMaster);
        return c;
    }

    /**
     * 默认条件.
     *
     * @return QueryCondition
     */
    public static QueryCondition defaultCondition() {
        QueryCondition condition = new QueryCondition();
        condition.setOperator(Operator.EQ).setQueryEmpty(false).setIgnore(false).setToMaster(false);
        return condition;
    }

    /**
     * 构建条件.
     *
     * @param queryField 查询字段
     * @return QueryCondition
     */
    public static QueryCondition of(final QueryField queryField) {
        QueryCondition condition = new QueryCondition();
        condition.setOperator(queryField.operator()).setFiledName(queryField.field());
        condition.setQueryInner(queryField.queryInner()).setInnerName(queryField.innerName());
        condition.setQueryEmpty(queryField.queryEmpty()).setIgnore(queryField.ignore());
        // or 字段查询组
        if (ArrayUtil.isNotEmpty(queryField.orFields())) {
            List<String> orFields = Arrays.stream(queryField.orFields()).collect(Collectors.toCollection(ArrayList::new));
            // 兼容之前的写法@何振辉（本身 + or 条件）
            if (StrUtil.isNotBlank(queryField.field())) {
                orFields.add(queryField.field());
            }
            condition.setOrFiledNames(orFields);
        }
        // 多字段组
        List<String> filedNames = new ArrayList<>();
        // 默认字段
        if (StrUtil.isNotBlank(queryField.field())) {
            filedNames.add(queryField.field());
        }
        if (ArrayUtil.isNotEmpty(queryField.fields())) {
            Collections.addAll(filedNames, queryField.fields());
        }
        condition.setFiledNames(filedNames);
        return condition;
    }

}
