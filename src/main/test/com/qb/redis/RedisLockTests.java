package com.qb.redis;

import com.qb.RedisLock;
import org.junit.Test;

/**
 * Created by Liufeng on 2020/1/13.
 */
public class RedisLockTests extends AbstractChapter {

    @Test
    public void testWrongGetLock() throws InterruptedException {
        RedisLock lock = new RedisLock();
        lock.wrongGetLock("test", "test2", 2);
        Thread.sleep(1000);
        assert conn.get("test").equals("test2");
        Thread.sleep(2000);
        assert conn.get("test") == null;
    }

    @Test
    public void testGetLockWithLua() throws InterruptedException {
        RedisLock lock = new RedisLock();
        lock.tryLockWithLua("test", "test2", 2);
        Thread.sleep(1000);
        assert conn.get("test").equals("test2");
        Thread.sleep(2000);
        assert conn.get("test") == null;
    }

    @Test
    public void testGetLockWithSet() throws InterruptedException {
        RedisLock lock = new RedisLock();
        lock.tryLockWithSet("test", "test2", 2);
        Thread.sleep(1000);
        assert conn.get("test").equals("test2");
        Thread.sleep(2000);
        assert conn.get("test") == null;
    }
}
