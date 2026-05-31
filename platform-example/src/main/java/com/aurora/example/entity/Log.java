package com.aurora.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.aurora.starter.mybatisplus.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 日志实体（用于测试动态表名）.
 *
 * @author whb
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_log")
public class Log extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String action;

    private Date createTime;
}