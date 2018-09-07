package com.qb.redis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import com.qb.utils.JedisUtil;

import redis.clients.jedis.Jedis;

/**
 * 在这里对第二章节的内容进行分析
 * @author Liufeng
 * Created on 2018年9月6日 下午10:53:45
 */
public class Chapter2 extends AbstractChapter {
	
	@Test
	public void testLoginCookie() throws InterruptedException {
		conn.select(15);
		System.out.println("\n--------------- testLoginCookie ----------------");
		String token = UUID.randomUUID().toString();
		updateToken(conn, token, "username-liufeng", "itemX");
		out("We just logged-in / updated token: " + token);
		out("For user: 'username'");
		out();
		out("What username do we get when we look up that token?");
		String r = checkToken(conn, token);
		out(r);
		out();
		assert r != null;
		
		out("Let's drop the maximum number of the cookies to 0 to clean them out");
		out("we will start a thread to do the cleaning, while we stop it later.");
		CleanSessionsThread thread = new CleanSessionsThread(0);
		thread.start();
		Thread.sleep(1000);
		thread.quit();
		Thread.sleep(2000);
		if(thread.isAlive()) {
			throw new RuntimeException("The clean sessions thread is still alive?!");
		} 
		long s = conn.hlen("login:");
		out("The current number of sessions still available is " + s);
		assert s == 0;
	}
	
	@Test
	public void testShoppingCartCookies() throws InterruptedException {
		out("\n-----------------testShoppingCartCookies-----------------------");
		String token = UUID.randomUUID().toString();
		out("We will refresh out sessions.");
		updateToken(conn, token, "username-liufeng", "itemX");
		out("And add an item to the shopping cart.");
		addToCart(conn, token, "itemY", 3);
		Map<String, String> r = conn.hgetAll("cart:" + token);
		out("our shopping cart currently has : ");
		for(Map.Entry<String, String> entry : r.entrySet()) {
			out(" " + entry.getKey() + ": " + entry.getValue());
		}
		out();
		assert r.size() >= 1;
		out("Let's clean out our sessions and carts.");
		CleanFullSessionsThread cleanThread = null;
		Thread thread = new Thread(cleanThread = new CleanFullSessionsThread(0));
		thread.start();
		Thread.sleep(1000);
		cleanThread.quit();
	}

	private void addToCart(Jedis conn, String session, String item, int count) {
		if(count <= 0) {
			conn.hdel("cart:" + session, item);
		} else {
			conn.hset("cart:" + session, item, String.valueOf(count));
		}
	}

	private String checkToken(Jedis conn, String token) {
		return conn.hget("login:", token);
	}

	private void updateToken(Jedis conn, String token, String user, String item) {
		long timeStamp = System.currentTimeMillis() / 1000;
		conn.hset("login:", token, user);
		conn.zadd("recent:", timeStamp, token);
		if(item != null) {
			conn.zadd("viewed:" + token, timeStamp, item);
			conn.zremrangeByRank("viewed:" + token, 0, -26);
			conn.zincrby("viewed:", -1, item);
		}
	}
	
	public class CleanSessionsThread extends Thread {
		private Jedis conn;
		private int limit;
		private boolean quit;
		public CleanSessionsThread(int limit) {
			this.limit = limit;
			this.conn = JedisUtil.getJedis("192.168.231.128", "xyyyhtl12");
			conn.select(15);
		}
		public void quit() {
			this.quit = true;
		}
		
		@Override
		public void run() {
			while(!quit) {
				long size = conn.zcard("recent:");
				if(size <= limit) {
					try {
						sleep(1000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					continue;
				}
				long endIndex = Math.min(size - limit, 100);
				Set<String> tokenSet = conn.zrange("recent:", 0, endIndex - 1);
				String[] tokens = tokenSet.toArray(new String[tokenSet.size()]);
				List<String> sessionKeys = new ArrayList<String>();
				for(String token : tokens) {
					sessionKeys.add(token);
				}
				conn.del(sessionKeys.toArray(new String[sessionKeys.size()]));
				conn.hdel("login:", tokens);
				conn.zrem("recent:", tokens);
			}
		}
	}
	
	public class CleanFullSessionsThread implements Runnable {
		
		private Jedis conn;
		private int limit;
		private boolean quit;
		
		public CleanFullSessionsThread(int limit) {
			this.limit = limit;
			this.conn = JedisUtil.getJedis("192.168.231.128", "xyyyhtl12");
			this.conn.select(15);
		}
		public void quit() {
			quit = true;
		}

		@Override
		public void run() {
			while(!quit) {
				long size = conn.zcard("recent:");
				if(size <= limit) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					continue;
				}
				long endIndex = Math.min(size - limit, 100);
				Set<String> sessionSet = conn.zrange("recent:", 0, endIndex - 1);
				String[] sessions = sessionSet.toArray(new String[sessionSet.size()]);
				List<String> sessionKeys = new ArrayList<String>();
				for(String session : sessions) {
					sessionKeys.add("viewed:" + session);
					sessionKeys.add("cart:" + session);
				}
				conn.del(sessionKeys.toArray(new String[sessionKeys.size()]));
				conn.hdel("login:", sessions);
				conn.zrem("recent:", sessions);
			}
		}
		
	}

}
