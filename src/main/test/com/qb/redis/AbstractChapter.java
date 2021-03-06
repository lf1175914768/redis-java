package com.qb.redis;

import com.qb.utils.JedisUtil;
import redis.clients.jedis.Jedis;

public abstract class AbstractChapter {
	
	protected Jedis conn = null;
	
	public AbstractChapter() {
		if(this.conn == null)
			this.conn = JedisUtil.getJedis("127.0.0.1", "xyyyhtl12");
	}
	
	protected void out() {
		out("");
	}
	
	protected void out(String message) {
		System.out.println(message);
	}

}
