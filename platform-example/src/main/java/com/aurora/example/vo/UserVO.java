package com.aurora.example.vo;

import lombok.Data;

import java.util.Date;

/**
 * 用户 VO（用于演示分页结果转换）.
 *
 * @author whb
 */
@Data
public class UserVO {

    private Long id;

    private String name;

    private Integer age;

    private String email;

    private Date createTime;

    /** 状态描述（演示字段转换） */
    private String statusDesc;
}
