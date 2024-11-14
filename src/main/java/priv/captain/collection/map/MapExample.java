package priv.captain.collection.map;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description: Map示例
 * @author: yetiKnight
 * @since: 2024-11-14
 **/
public class MapExample {

    public void hashMap(){
        // 1.7基于哈希表实现，1.8基于数组+链表+红黑树。
        Map<String,Integer> map = new HashMap<>();
        map.put("key1",1);
        map.put("key2",2);
        map.put("key3",3);
    }

    public void concurrentHashMap(){
        // ConcurrentHashMap是Java 7 引入的，在并发环境下可以提供高并发性和线程安全。采用分段锁保证线程安全
        Map<String,Integer> map = new ConcurrentHashMap<>();
        map.put("key1",1);
        map.put("key2",2);
        map.put("key3",3);
    }
}
