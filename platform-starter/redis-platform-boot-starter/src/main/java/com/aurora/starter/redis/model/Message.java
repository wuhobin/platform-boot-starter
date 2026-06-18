package com.aurora.starter.redis.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 队列消息.
 *
 * @author whb
 * @date 2026/6/18
 */
@Data
public class Message<T> implements Serializable {

    private String msgId;

    private T data;
}
