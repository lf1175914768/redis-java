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
