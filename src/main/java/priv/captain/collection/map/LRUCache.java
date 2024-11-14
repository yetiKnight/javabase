package priv.captain.collection.map;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @description: LRU缓存示例
 * @author: yetiKnight
 * @since: 2024-11-14
 **/
public class LRUCache extends LinkedHashMap<String, String> {

    /**
     * 目前来说使用Guava 和Caffeine 实现更简洁
     * Cache<Integer, String> cache = Caffeine.newBuilder()
     * .maximumSize(3)                // 设置最大缓存大小为 3
     * .expireAfterAccess(10, TimeUnit.MINUTES) // 设置过期时间（可选）
     * .build(); // 构建缓存
     */

    private static final int MAX = 3;

    // 当使用 accessOrder=true 时，最近访问的元素会被移动到链表的尾部
    public LRUCache() {
        super(MAX, 1.0f, true);
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
        return size() > MAX;
    }

    public static void main(String[] args) {
        // LRU 算法会根据最近最少使用（Least Recently Used）的原则来移除元素
        LRUCache cache = new LRUCache();
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");

        cache.get("key1"); // 使key1 成为最元素

        cache.put("key4", "value4");
        // 因为容量是3，根据LRU算法，key2会被移除
        System.out.println(cache);
    }
}
