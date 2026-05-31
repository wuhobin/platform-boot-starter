package com.aurora.starter.mybatisplus.model;

import cn.hutool.core.util.ObjectUtil;
import com.aurora.starter.mybatisplus.enums.BetweenType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 范围查询.
 *
 * @author Luo
 * @date 2023-8-11 10:56
 */
@Getter
@NoArgsConstructor
public class BetweenQueryAttribute<T> implements Serializable {

    private static final long serialVersionUID = 693129368413900029L;

    /**
     * 查询范围类型.
     * 默认：左开右开
     *
     * @see BetweenType
     */
    private BetweenType betweenType = BetweenType.BOTH_NOT_CONTAIN;

    /**
     * 开始.
     */
    private T start;

    /**
     * 结束.
     */
    private T end;

    public BetweenQueryAttribute(final T start, final T end) {
        if (ObjectUtil.isEmpty(start) || ObjectUtil.isEmpty(end)) {
            throw new RuntimeException("start or end is required");
        }
        this.start = start;
        this.end = end;
    }

    public BetweenQueryAttribute(final BetweenType betweenType, final T start, final T end) {
        if (ObjectUtil.isEmpty(betweenType) || ObjectUtil.isEmpty(start) || ObjectUtil.isEmpty(end)) {
            throw new RuntimeException("betweenType or start or end is required");
        }
        this.betweenType = betweenType;
        this.start = start;
        this.end = end;
    }

}
