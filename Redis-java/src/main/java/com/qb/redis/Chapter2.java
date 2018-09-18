package com.qb.redis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.security.auth.callback.Callback;

import org.junit.Test;

import com.google.gson.Gson;
import com.qb.utils.JedisUtil;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Tuple;

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
		Thread.sleep(2000);
	}
	
	@Test
	public void testCacheRows() throws InterruptedException {
		out("\n------------------ testCacheRows -------------------");
		out("first, Let's schedule caching of itemX every 5 seconds.");
		conn.select(15);
		scheduleRowCache(conn, "itemX", 5);
		out("Our schedule looks like: ");
		Set<Tuple> s = conn.zrangeWithScores("schedule:", 0, -1);
		for(Tuple tuple : s) {
			out(" " + tuple.getElement() + ", " + tuple.getScore());
		}
		assert s.size() != 0;
		out("We'll start a caching thread that will cache the data...");
		CacheRowsThread thread = new CacheRowsThread();
		thread.start();
		Thread.sleep(1000);
		out("Our cached data looks like ");
		String r = conn.get("inv:itemX");
		out(r);
		assert r != null;
		out();
		
		out("We will check again in 5 seconds...");
		Thread.sleep(5000);
		out("Notice that the data has changed...");
		String r2 = conn.get("inv:itemX");
		out(r2);
		out();
		assert r2 != null;
		assert !r.equals(r2);
		
		out("Let's force un-caching..");
		scheduleRowCache(conn, "itemX", -1);
		Thread.sleep(1000);
		r = conn.get("inv:itemX");
		out("The Cache was cleared? " + (r == null));
		assert r == null;
		
		thread.quit();
		Thread.sleep(2000);
		if(thread.isAlive()) {
			throw new RuntimeException("The database caching thread is still alive?");
		}
	}
	
	@Test
	public void testCacheRequest() {
		out("\n----------------------- testCacheRequest --------------------");
		String token = UUID.randomUUID().toString();
		Callback callback = new Callback() {
			public String call(String request) {
				return "content for" + request;
			}
		};
		updateToken(conn, token, "username", "itemX");
	}

	private void scheduleRowCache(Jedis conn, String rowId, int delay) {
		conn.zadd("delay:", delay, rowId);
		conn.zadd("schedule:", System.currentTimeMillis() / 1000, rowId);
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
	
	public class CacheRowsThread extends Thread {
		private Jedis conn;
		private boolean quit;
		public CacheRowsThread() {
			this.conn = JedisUtil.getJedis("192.168.231.128", "xyyyhtl12");
			conn.select(15);
		}
		
		public void quit() {
			quit = true;
		}
		
		@Override
		public void run() {
			Gson gson = new Gson();
			while(!quit) {
				Set<Tuple> range = conn.zrangeWithScores("schedule:", 0, 0);
				Tuple next = range.size() > 0 ? range.iterator().next() : null;
				long now = System.currentTimeMillis() / 1000;
				if(next == null || next.getScore() > now) {
					try {
						sleep(50);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					continue;
				}
				String rowId = next.getElement();
				double delay = conn.zscore("delay:", rowId);
				if(delay <= 0) {
					conn.zrem("delay:", rowId);
					conn.zrem("schedule:", rowId);
					conn.del("inv:" + rowId);
					continue;
				} 
				Inventory row = Inventory.get(rowId);
				conn.zadd("schedule:", now + delay, rowId);
				conn.set("inv:" + rowId, gson.toJson(row));
			}
		}
	}
	
	public static class Inventory {
		private String id;
		private String data;
		private long time;
		private Inventory(String id) {
			this.id = id;
			this.data = "data to cache..";
			this.time = System.currentTimeMillis() / 1000;
		} 
		public static Inventory get(String id) {
			return new Inventory(id);
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
