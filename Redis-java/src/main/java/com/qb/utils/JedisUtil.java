package com.qb.utils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * 对Jedis常用的帮助方法进行封装
 * @author Liufeng
 * Created on 2018年9月6日 下午10:55:28
 */
public class JedisUtil {
	
	public static JedisPool getDefaultJedisPool() {
		return JedisPoolFactory.getPool();
	}
	
	public static JedisPool getJedisPool(String host, int port, String password) {
		return JedisPoolFactory.getPool(host, port, password);
	}
	
	public static JedisPool getJedisPool(String host, String password) {
		return JedisPoolFactory.getPool(host, password);
	}
	
	public static Jedis getDefaultJedis() {
		return getDefaultJedisPool().getResource();
	}
	
	public static Jedis getJedis(String host, String password) {
		return getJedisPool(host, password).getResource();
	}
	
	public static Jedis getJedis(String host, int port, String password) {
		return getJedisPool(host, port, password).getResource();
	}

}
