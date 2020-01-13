package com.qb.utils;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author Liufeng
 * Created on 2018年9月6日 下午11:00:32
 */
public class JedisPoolFactory implements JedisService {
	
	private static final Object lock = new Object();
	
	private static volatile JedisPool pool = null;
	
	private static JedisPoolConfig config = new JedisPoolConfig();

	static {
		config.setMaxTotal(200);
		config.setMaxIdle(50);
		config.setMinIdle(8);
		config.setMaxWaitMillis(10000);
		config.setTestOnBorrow(true);
		config.setTestOnReturn(true);
		//Idle时进行连接扫描
		config.setTestWhileIdle(true);
		//表示idle object evitor两次扫描之间要sleep的毫秒数
		config.setTimeBetweenEvictionRunsMillis(30000);
		//表示idle object evitor每次扫描的最多的对象数
		config.setNumTestsPerEvictionRun(10);
		//表示一个对象至少停留在idle状态的最短时间，然后才能被idle object evitor扫描并驱逐；这一项只有在timeBetweenEvictionRunsMillis大于0时才有意义
		config.setMinEvictableIdleTimeMillis(60000);
	}
	
	private JedisPoolFactory() {}
	
	public static JedisPool getPool() {
		return getPool(config, DEFAULT_HOST, DEFAULT_PORT, 
				DEFAULT_TIMEOUT, DEFAULT_PASSWORD);
	}
	
	public static JedisPool getPool(String host, int port, String password) {
		return getPool(config, host, port, DEFAULT_TIMEOUT, password);
	}
	
	public static JedisPool getPool(String host, String password) {
		return getPool(config, host, DEFAULT_PORT, DEFAULT_TIMEOUT, password);
	}

	public static JedisPool getPool(JedisPoolConfig config, String host, 
			int port, int timeout, String password) {
		if(pool == null) {
			synchronized(lock) {
				if(pool == null) {
					pool = new JedisPool(config, host, port, timeout, password);
				}
			}
		}
		return pool;
	}
	
	public static void setConfig(JedisPoolConfig config) {
		JedisPoolFactory.config = config;
	}

}
