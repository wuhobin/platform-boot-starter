package com.aurora.example.query;

import com.aurora.starter.mybatisplus.annotation.QueryField;
import com.aurora.starter.mybatisplus.annotation.Operator;
import com.aurora.starter.mybatisplus.model.BaseQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户查询对象.
 *
 * @author whb
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserQuery extends BaseQuery {

    @QueryField
    private String name;

    @QueryField(operator = Operator.LIKE)
    private String email;

    @QueryField(operator = Operator.IN, field = "status")
    private Integer[] statusArr;

    @QueryField(operator = Operator.GT, field = "age")
    private Integer ageGt;

    @QueryField(operator = Operator.NE, field = "status")
    private Integer statusNe;
}