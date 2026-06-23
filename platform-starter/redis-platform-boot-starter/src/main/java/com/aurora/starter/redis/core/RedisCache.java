package com.aurora.starter.redis.core;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * spring redis 工具类
 *
 * @author whb
 **/
@Slf4j
@AllArgsConstructor
public class RedisCache {

    private final RedisTemplate redisTemplate;


    /**
     * 获取过期时间.
     *
     * @param key 缓存键值
     * @return 过期时间 单位：秒
     */
    public Long getExpire(final String key) {
        return redisTemplate.opsForValue().getOperations().getExpire(key);
    }

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key   缓存的键值
     * @param value 缓存的值
     */
    public <T> void setCacheObject(final String key, final T value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * key不存在时设置值
     *
     * @param key   缓存的键值
     * @param value 缓存的值
     * @return 是否设置成功
     */
    public <T> Boolean setIfAbsent(final String key, T value) {
        return redisTemplate.opsForValue().setIfAbsent(key, value);
    }

    /**
     * key不存在时设置值
     *
     * @param key      缓存的键值
     * @param value    缓存的值
     * @param time     键过期时间
     * @param timeUnit 过期时间单位
     * @return 是否设置成功
     */
    public <T> Boolean setIfAbsent(final String key, T value, long time, TimeUnit timeUnit) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, time, timeUnit);
    }

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key      缓存的键值
     * @param value    缓存的值
     * @param timeout  时间
     * @param timeUnit 时间颗粒度
     */
    public <T> void setCacheObject(final String key, final T value, final long timeout, final TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    /**
     * 判断key是否存在
     *
     * @param key Redis键
     * @return true=设置成功；false=设置失败
     */
    public boolean exists(final String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 设置有效时间
     *
     * @param key     Redis键
     * @param timeout 超时时间
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout) {
        return expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 设置有效时间
     *
     * @param key     Redis键
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout, final TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 获得缓存的基本对象。
     *
     * @param key 缓存键值
     * @return 缓存键值对应的数据
     */
    public <T> T getCacheObject(final String key) {
        ValueOperations<String, T> operation = redisTemplate.opsForValue();
        return operation.get(key);
    }

    /**
     * 带布隆过滤器防护的缓存查询.
     * <p>
     * Bloom filter 判定不存在则直接返回 null，不查询 Redis，用于防止缓存穿透。
     * 如果 Bloom filter 判定可能存在但 Redis 中实际不存在（误判），返回 null 并记录 warn 日志。
     *
     * @param key         缓存键值
     * @param bloomFilter 布隆过滤器实例
     * @param <T>         缓存值类型
     * @return 缓存对象，不存在返回 null
     * @since 1.0.0
     */
    public <T> T getCacheObject(final String key, final RedisBloomFilter<String> bloomFilter) {
        if (!bloomFilter.contains(key)) {
            return null;
        }
        T result = this.<T>getCacheObject(key);
        if (result == null) {
            log.warn("Bloom filter [{}] false positive for cache key [{}]", bloomFilter.getName(), key);
        }
        return result;
    }

    /**
     * 删除单个对象
     *
     * @param key
     */
    public boolean deleteObject(final String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 删除多个对象.
     *
     * @param collection 多个对象
     * @return
     */
    public long deleteObject(final Collection collection) {
        return redisTemplate.delete(collection);
    }

    /**
     * 基于SCAN批量删除.
     *
     * @param pattern key 通配符
     */
    public void deleteByPattern(final String pattern) {
        if (StrUtil.isBlank(pattern)) {
            return;
        }
        // 默认批量处理数量
        final int batchSize = 1000;
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            try (Cursor<byte[]> cursor = connection.scan(
                ScanOptions.scanOptions().match(pattern).count(batchSize).build())) {

                List<byte[]> batch = new ArrayList<>(batchSize);

                while (cursor.hasNext()) {
                    batch.add(cursor.next());
                    if (batch.size() >= batchSize) {
                        connection.del(batch.toArray(new byte[0][]));
                        batch.clear();
                    }
                }

                // 删除剩余 batch
                if (!batch.isEmpty()) {
                    connection.del(batch.toArray(new byte[0][]));
                }
            }
            return null;
        });
    }

    /**
     * 缓存List数据
     *
     * @param key      缓存的键值
     * @param dataList 待缓存的List数据
     * @return 缓存的对象
     */
    public <T> long setCacheList(final String key, final List<T> dataList) {
        Long count = redisTemplate.boundListOps(key).rightPushAll(dataList.toArray());
        return count == null ? 0 : count;
    }

    /**
     * 缓存List数据
     *
     * @param key  缓存的键值
     * @param data 待缓存的数据
     * @return 缓存的对象
     */
    public <T> long addCacheList(final String key, final T data) {
        Long count = redisTemplate.boundListOps(key).rightPush(data);
        return count == null ? 0 : count;
    }

    /**
     * 获得缓存的list对象
     *
     * @param key 缓存的键值
     * @return 缓存键值对应的数据
     */
    public <T> List<T> getCacheList(final String key) {
        return redisTemplate.boundListOps(key).range(0, -1);
    }

    /**
     * 缓存Set
     *
     * @param key     缓存键值
     * @param dataSet 缓存的数据
     * @return 缓存数据的对象
     */
    public <T> long setCacheSet(final String key, final Set<T> dataSet) {
        Long count = redisTemplate.boundSetOps(key).add(dataSet.toArray());
        return count == null ? 0 : count;
    }

    /**
     * 缓存Set
     *
     * @param key  缓存键值
     * @param data 缓存的数据
     * @return 缓存数据的对象
     */
    public <T> long addCacheSet(final String key, final T... data) {
        Long count = redisTemplate.boundSetOps(key).add(data);
        return count == null ? 0 : count;
    }

    /**
     * 批量插入数据到 Redis 中
     *
     * @param dataMap 包含 key-value 键值对的 Map
     */
    public <T> void setCacheObjects(Map<String, T> dataMap) {
        redisTemplate.opsForValue().multiSet(dataMap);
    }

    /**
     * 获得缓存的set
     *
     * @param key
     * @return
     */
    public <T> Set<T> getCacheSet(final String key) {
        return redisTemplate.boundSetOps(key).members();
    }

    /**
     * 缓存Map
     *
     * @param key
     * @param dataMap
     */
    public <T> void setCacheMap(final String key, final Map<String, T> dataMap) {
        if (dataMap != null) {
            redisTemplate.boundHashOps(key).putAll(dataMap);
        }
    }

    /**
     * 获得缓存的Map
     *
     * @param key
     * @return
     */
    public <T> Map<String, T> getCacheMap(final String key) {
        return redisTemplate.boundHashOps(key).entries();
    }

    /**
     * 往Hash中存入数据
     *
     * @param key   Redis键
     * @param hKey  Hash键
     * @param value 值
     */
    public <T> void setCacheMapValue(final String key, final String hKey, final T value) {
        redisTemplate.boundHashOps(key).put(hKey, value);
    }

    /**
     * 获取Hash中的数据
     *
     * @param key  Redis键
     * @param hKey Hash键
     * @return Hash中的对象
     */
    public <T> T getCacheMapValue(final String key, final String hKey) {
        BoundHashOperations<String, String, T> hashOperations = redisTemplate.boundHashOps(key);
        return hashOperations.get(hKey);
    }

    /**
     * 获取Hash中的key
     *
     * @param key hash表的key
     * @return Hash中的key列表
     */
    public <T> Set<T> getCacheMapKeys(final String key) {
        return redisTemplate.boundHashOps(key).keys();
    }

    /**
     * 删除Hash中的数据
     *
     * @param key
     * @param hKey
     */
    public void delCacheMapValue(final String key, final Object... hKey) {
        redisTemplate.boundHashOps(key).delete(hKey);
    }

    /**
     * 获取多个Hash中的数据
     *
     * @param key   Redis键
     * @param hKeys Hash键集合
     * @return Hash对象集合
     */
    public <T> List<T> getMultiCacheMapValue(final String key, final Collection<Object> hKeys) {
        return redisTemplate.boundHashOps(key).multiGet(hKeys);
    }

    /**
     * 设置多个key-value到Hash表中
     *
     * @param key     Redis键
     * @param dataMap Hash键集合
     * @return Hash对象集合
     */
    public <T> void setMultiCacheMapValue(final String key, final Map<String, T> dataMap) {
        if (dataMap != null) {
            redisTemplate.boundHashOps(key).putAll(dataMap);
        }
    }

    /**
     * 获得缓存的基本对象列表
     * keys的操作会导致数据库暂时被锁住，其他的请求都会被堵塞；业务量大的时候会出问题
     *
     * @param pattern 字符串前缀
     * @return 对象列表
     * @deprecated 缓存数据量较大时，请使用 #{@link #scan(String)} 获取数据
     */
    @Deprecated
    public Collection<String> keys(final String pattern) {
        return redisTemplate.keys(pattern);
    }

    /**
     * 获得缓存的基本对象列表.
     *
     * @param pattern 字符串前缀
     * @return 对象列表
     */
    public Collection<String> scan(final String pattern) {
        return (Collection<String>) redisTemplate.execute((RedisCallback<Collection<String>>) connection -> {
            Set<String> keysTmp = new HashSet<>();
            try (Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().match(pattern).count(1000).build())) {
                while (cursor.hasNext()) {
                    keysTmp.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            }
            return keysTmp;
        });
    }

    /**
     * 批量获取.
     *
     * @param pattern key表达式
     * @param <T>     泛型
     * @return 结果
     */
    public <T> List<T> multiGet(final String pattern) {
        return multiGet((Set<String>) scan(pattern));
    }

    /**
     * 批量获取.
     *
     * @param keys key集合
     * @param <T>  泛型
     * @return 结果
     */
    public <T> List<T> multiGet(final Set<String> keys) {
        return redisTemplate.opsForValue().multiGet(keys);
    }

    /**
     * 增加有序集合
     *
     * @param key
     * @param value
     * @param seqNo
     * @return
     */
    public Boolean addZset(String key, Object value, double seqNo) {
        return redisTemplate.boundZSetOps(key).add(value, seqNo);
    }

    /**
     * 增加有序集合
     *
     * @param key
     * @param value
     * @param seqNo
     * @return
     */
    public Boolean addZset(String key, Object value, double seqNo, int maxSize) {
        Boolean addZset = addZset(key, value, seqNo);
        if (addZset) {
            redisTemplate.boundZSetOps(key).removeRange(0, -maxSize - 1);
        }
        return addZset;
    }

    /**
     * 获得缓存的zSet对象.
     *
     * @param key 缓存的键值
     * @return 缓存键值对应的数据
     */
    public <T> Set<T> getCacheZSet(final String key) {
        return redisTemplate.boundZSetOps(key).range(0, -1);
    }

    /**
     * 获得缓存的反转zSet对象.
     *
     * @param key 缓存的键值
     * @return 缓存键值对应的数据
     */
    public <T> Set<T> getCacheReverseZSet(final String key) {
        return redisTemplate.boundZSetOps(key).reverseRange(0, -1);
    }

    /**
     * 获取zset指定范围内的集合
     *
     * @param key
     * @param min
     * @param max
     * @return
     */
    public Set<Object> rangeZsetByScore(String key, double min, double max) {
        return redisTemplate.boundZSetOps(key).rangeByScore(min, max);
    }

    /**
     * 获取zset分数从高到低排序的指定范围内集合
     *
     * @param key
     * @param start
     * @param end
     * @return
     */
    public <T> List<T> reverseRangeWithScores(String key, Integer start, Integer end) {
        Set<ZSetOperations.TypedTuple<T>> set = redisTemplate.opsForZSet()
            .reverseRangeWithScores(key, start, end);
        if (set.size() > 0) {
            return set.stream().map(ZSetOperations.TypedTuple::getValue).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * 根据key和value移除指定元素
     *
     * @param key
     * @param value
     * @return
     */
    public Long removeZset(String key, Object value) {
        return redisTemplate.boundZSetOps(key).remove(value);
    }

    /**
     * 根据key和value移除指定元素
     *
     * @param key
     * @param value
     * @return
     */
    public Long removeSet(String key, Object value) {
        return redisTemplate.boundSetOps(key).remove(value);
    }

    /**
     * 增加对应的集合中元素的score值，并返回增加后的值.
     * <p>
     * value不存在，直接新增一个元素
     * </p>
     *
     * @param key   缓存键值
     * @param value 缓存的数据
     * @param score 分数
     * @return 分数
     */
    public <T> Double incrementScore(final String key, final T value, final double score) {
        return redisTemplate.boundZSetOps(key).incrementScore(value, score);
    }

    /**
     * 获取对应集合中元素的score值.
     *
     * @param key   缓存键值
     * @param value 缓存的数据
     * @return 分数
     */
    public <T> Double score(final String key, final T value) {
        return redisTemplate.boundZSetOps(key).score(value);
    }

    /**
     * 自增.
     *
     * @param key 缓存key
     * @return 结果
     */
    public Long increment(final String key) {
        return increment(key, 1L);
    }

    /**
     * 自增.
     *
     * @param key  缓存key
     * @param step 步进数
     * @return 结果
     */
    public Long increment(final String key, final long step) {
        return redisTemplate.opsForValue().increment(key, step);
    }

    /**
     * 自增.
     *
     * @param key      缓存key
     * @param step     步进数
     * @param time     过期时间
     * @param timeUnit 时间单位
     * @return 结果
     */
    public Long increment(final String key, final long step, long time, TimeUnit timeUnit) {
        Long result = redisTemplate.opsForValue().increment(key, step);
        expireIfPositive(key, time, timeUnit);
        return result;
    }

    /**
     * 自减.
     *
     * @param key 缓存key
     * @return 结果
     */
    public Long decrement(final String key) {
        return decrement(key, 1L);
    }

    /**
     * 自减.
     *
     * @param key  缓存key
     * @param step 步进数
     * @return 结果
     */
    public Long decrement(final String key, final long step) {
        return redisTemplate.opsForValue().decrement(key, step);
    }

    /**
     * 自减.
     *
     * @param key      缓存key
     * @param step     步进数
     * @param time     过期时间
     * @param timeUnit 时间单位
     * @return 结果
     */
    public Long decrement(final String key, final long step, long time, TimeUnit timeUnit) {
        Long result = redisTemplate.opsForValue().decrement(key, step);
        expireIfPositive(key, time, timeUnit);
        return result;
    }

    private void expireIfPositive(final String key, final long time, final TimeUnit timeUnit) {
        if (time > 0) {
            redisTemplate.expire(key, time, timeUnit);
        }
    }

    /**
     * redis格式全局自增id方法
     * 生成id格式： 前缀+当前日期+id+后缀
     *
     * @param prefix 前缀
     * @param suffix 后缀
     * @param key    redisKey
     * @param date   日期（key过期日期）
     * @return 生成编号
     */
    public String soleId(String prefix, String suffix, String key, Date date) {
        return soleId(prefix, suffix, key, date, 4);
    }

    /**
     * redis格式全局自增id方法
     * 生成id格式： 前缀+当前日期+id+后缀
     *
     * @param prefix 前缀
     * @param suffix 后缀
     * @param key    redisKey
     * @param date   日期（key过期日期）
     * @param len    id长度
     * @return 生成编号
     */
    public String soleId(String prefix, String suffix, String key, Date date, int len) {
        Long id = redisTemplate.opsForValue().increment(key);
        redisTemplate.expireAt(key, DateUtil.endOfDay(date));

        return Stream.of(prefix, DateUtil.format(DateUtil.date(), DatePattern.PURE_DATE_PATTERN),
                StrUtil.fillBefore(String.valueOf(id), '0', len), suffix)
            .filter(StrUtil::isNotBlank).collect(Collectors.joining());
    }
}
