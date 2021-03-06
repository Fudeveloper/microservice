package com.somnus.microservice.commons.redis.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @author Kevin
 * @packageName com.somnus.microservice.commons.redis.handler
 * @title: RedisHandlerImpl
 * @description: TODO
 * @date 2019/7/5 16:51
 */
public class RedisHandlerImpl implements RedisHandler {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 获取RedisTemplate
     * @return
     */
    @Override
    public RedisTemplate<String, Object> getRedisTemplate() {
        return redisTemplate;
    }

    /**
     * 获取StringRedisTemplate
     * @return
     */
    @Override
    public StringRedisTemplate getStringRedisTemplate() {
        return stringRedisTemplate;
    }
}