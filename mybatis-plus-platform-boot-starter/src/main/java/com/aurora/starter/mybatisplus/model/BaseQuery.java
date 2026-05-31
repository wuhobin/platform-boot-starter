package com.aurora.starter.mybatisplus.model;

import com.aurora.starter.mybatisplus.annotation.Operator;
import com.aurora.starter.mybatisplus.annotation.QueryField;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

/**
 * 基础查询对象.
 *
 * @author Luo
 * @date 2024-06-07 13:49
 */
@Data
@Accessors(chain = true)
public class BaseQuery implements Serializable {

    private static final long serialVersionUID = -1L;

    /**
     * id  IN 查询.
     */
    @QueryField(field = "id", operator = Operator.IN)
    private Set<Long> ids;

    /**
     * 创建时间  开始 查询.
     */
    @QueryField(field = "createTime", operator = Operator.GTE)
    private Date createStartTime;

    /**
     * 创建时间  结束 查询.
     */
    @QueryField(field = "createTime", operator = Operator.LT)
    private Date createEndTime;

    /**
     * 修改时间  开始 查询.
     */
    @QueryField(field = "updateTime", operator = Operator.GTE)
    private Date updateStartTime;

    /**
     * 修改时间  结束 查询.
     */
    @QueryField(field = "updateTime", operator = Operator.LT)
    private Date updateEndTime;

    /**
     * 限制数量.
     */
    @QueryField(field = "limitSize", operator = Operator.LIMIT)
    private Integer limitSize;


}
