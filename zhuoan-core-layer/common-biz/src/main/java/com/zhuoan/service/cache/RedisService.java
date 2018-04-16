package com.zhuoan.service.cache;

/**
 * RedisService
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
     * @param time  单位s
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


}
