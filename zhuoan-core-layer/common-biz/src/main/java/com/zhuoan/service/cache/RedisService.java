package com.zhuoan.service.cache;

import java.util.Map;
import java.util.Set;

/**
 * RedisService  todo  String Map Set List 封装可以参考实现类
 *
 * @author weixiang.wu
 * @date 2018-04-16 10:38
 **/
public interface RedisService {

    /**
     * 向Redis中添加缓存一定时间的key-value
     *
     * @param key   the key
     * @param value the value
     * @param time  单位s       若为空，则一直存在
     */
    void insertKey(String key, String value, Integer time);

    /**
     * 从redis中删除指定的值
     *
     * @param key the key
     */
    void deleteByKey(String key);

    /**
     * 查询指定的key对应的value
     *
     * @param key the key
     * @return string
     */
    Object queryValueByKey(String key);

    /**
     * redis中对指定的key设置时间
     *
     * @param key     the key
     * @param seconds the seconds
     */
    boolean expire(String key, long seconds);


    long incr(String key, long delta);

    /**
     * 向一张hash表中放入数据,如果不存在将创建
     *
     * @param key   键
     * @param item  项
     * @param value 值
     * @return true 成功 false失败
     */
    boolean hset(String key, String item, Object value);

    /**
     * 向一张hash表中放入数据,如果不存在将创建
     *
     * @param key   键
     * @param item  项
     * @param value 值
     * @param time  时间(秒)  注意:如果已存在的hash表有时间,这里将会替换原有的时间
     * @return true 成功 false失败
     */
    boolean hset(String key, String item, Object value, long time);

    /**
     * 获取hashKey对应的所有键值
     *
     * @param key 键
     * @return 对应的多个键值
     */
    Map<Object, Object> hmget(String key);

    /**
     * HashGet
     *
     * @param key  键 不能为null
     * @param item 项 不能为null
     * @return 值
     */
    Object hget(String key, String item);

    /**
     * 删除hash表中的值
     *
     * @param key  键 不能为null
     * @param item 项 可以使多个 不能为null
     */
    void hdel(String key, Object... item);

    /**
     * 根据value从一个set中查询,是否存在
     *
     * @param key   键
     * @param value 值
     * @return true 存在 false不存在
     */
    boolean sHasKey(String key, Object value);

    /**
     * 将数据放入set缓存
     *
     * @param key    键
     * @param values 值 可以是多个
     * @return 成功个数
     */
    long sSet(String key, Object... values);

    /**
     * 将set数据放入缓存
     *
     * @param key    键
     * @param time   时间(秒)
     * @param values 值 可以是多个
     * @return 成功个数
     */
    long sSetAndTime(String key, long time, Object... values);

    /**
     * 移除值为value的
     *
     * @param key    键
     * @param values 值 可以是多个
     * @return 移除的个数
     */
    long setRemove(String key, Object... values);

    /**
     * 根据key获取Set中的所有值
     *
     * @param key 键
     * @return
     */
    Set<Object> sGet(String key);

}
