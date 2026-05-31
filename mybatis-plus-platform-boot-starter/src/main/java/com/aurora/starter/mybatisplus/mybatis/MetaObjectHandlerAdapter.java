package com.aurora.starter.mybatisplus.mybatis;

import com.aurora.starter.common.utils.DateUtils;
import com.aurora.starter.mybatisplus.model.BaseEntity;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.handlers.StrictFill;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * mybatis-plus自定义填充.
 *
 * @author whb
 */
@Slf4j
public class MetaObjectHandlerAdapter implements MetaObjectHandler {

    /**
     * 新增时自动填充默认数据.
     *
     * @param metaObject metaObject
     */
    @Override
    public void insertFill(final MetaObject metaObject) {
        if (metaObject.getOriginalObject() instanceof BaseEntity) {
            Date now = DateUtils.getNowDate();
            List<StrictFill<?, ?>> list = new ArrayList<>(4);
            list.add(StrictFill.of("createTime", Date.class, now));
            list.add(StrictFill.of("updateTime", Date.class, now));
            this.strictInsertFill(findTableInfo(metaObject), metaObject, list);
        }
    }

    /**
     * 修改时自动填充默认数据.
     *
     * @param metaObject metaObject
     */
    @Override
    public void updateFill(final MetaObject metaObject) {
        if (metaObject.getOriginalObject() instanceof BaseEntity) {
            this.setFieldValByName("updateTime", DateUtils.getNowDate(), metaObject);
        }
    }

}
