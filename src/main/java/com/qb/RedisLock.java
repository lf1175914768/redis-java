package com.qb;

import com.qb.utils.JedisUtil;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Liufeng on 2020/1/13.
 */
public class RedisLock {

    private Jedis jedis = JedisUtil.getJedis("127.0.0.1", "xyyyhtl12");

    // -----------------------------------------------------
    //  下面所写的都是用在 单个 redis 服务器上的，如果用在Redis
    //  服务器集群的话，会有相关的一些问题
    // -----------------------------------------------------

    //  由于 setnx 和 expire 的非原子性，可能会有问题
    @Deprecated
    public boolean wrongGetLock(String key, String value, int timeout) {
        Long result = jedis.setnx(key, value);
        // result = 1时，设置成功
        if(result == 1L) {
            return jedis.expire(key, timeout) == 1L;
        } else {
            return false;
        }
    }

    // 用Lua 脚本保证原子性
    public boolean tryLockWithLua(String key, String value, int timeout) {
        String luaScript = "if redis.call('setnx', KEYS[1], ARGV[1]) == 1 then " +
                "redis.call('expire', KEYS[1], ARGV[2]) return 1 else return 0 end";
        List<String> keys = new ArrayList<>();
        List<String> values = new ArrayList<>();
        keys.add(key);
        values.add(value);
        values.add(String.valueOf(timeout));
        Object result = jedis.eval(luaScript, keys, values);
        return result.equals(1L);
    }

    // 用set 的参数形式来保证原子性
    public boolean tryLockWithSet(String key, String value, int timeout) {
        return "OK".equals(jedis.set(key, value, "NX", "EX", timeout));
    }

    // 用LUA 脚本的形式保证 释放锁的原子性
    public boolean releaseLockWithLua(String key, String value) {
        String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "return redis.call('del', KEYS[1]) else return 0 end";
        return jedis.eval(luaScript, Collections.singletonList(key), Collections.singletonList(value)).equals(1L);
    }

    // -----------------------------------------------------
    // 分布式锁 redis 集群 To be continued...
    // -----------------------------------------------------
}
