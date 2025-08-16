package priv.captain.juc;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;

public class StampedLockDemo {

    private final Map<String, String> cache = new HashMap<>();
    private final StampedLock lock = new StampedLock();

    // 乐观读（比 ReentrantReadWriteLock 读性能更高）
    public String get(String key) {
        long stamp = lock.tryOptimisticRead(); // 1. 乐观读
        String value = cache.get(key);
        if (!lock.validate(stamp)) { // 2. 校验期间是否被写修改
            stamp = lock.readLock(); // 3. 回退到悲观读
            try {
                value = cache.get(key);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return value;
    }

    // 写缓存
    public void put(String key, String value) {
        long stamp = lock.writeLock();
        try {
            cache.put(key, value);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

}
