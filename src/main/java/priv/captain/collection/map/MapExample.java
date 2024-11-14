package priv.captain.collection.map;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description: Map示例
 * @author: yetiKnight
 * @since: 2024-11-14
 **/
public class MapExample {

    /**
     * HashMap：基于哈希表实现，线程不安全，性能较高。
     * TreeMap：基于红黑树实现，按键排序，线程不安全。
     * LinkedHashMap：继承自 HashMap，维护插入顺序（或访问顺序）或用来实现LRU删除最旧的元素（有一个accesOrder参数）。
     * ConcurrentHashMap：线程安全的哈希表实现，适用于并发场景。
     */

    @Test
    public void hashMap() {
        /*
         * 哈希冲突：多个键的哈希值相同，它们会被存储在同一个桶中,这时候就会产生哈希冲突。
         *  在jdk 1.7及之前使用的是链表来解决，1.8及之后使用链表+红黑树
         *  在冲突的数量没有达到8之前，使用链表，时间复杂度是O(n)
         *  在8个之后，使用红黑树，最糟糕的时间复杂度是O(log n)
         *  这么转换的原因是：链表的性能问题，因为其复杂度是O(n), 经过大量实践测试，超过8个之后使用红黑树
         *  为什么不全部使用红黑树呢？因为红黑树比链表占用的内存更多，并且插入和删除操作会比链表更复杂一些
         *
         *  红黑树增加复杂性的主要原因包括：
            结构复杂性：每个节点需要存储颜色信息，并且红黑树有许多平衡性规则。
            操作复杂性：插入和删除操作需要执行旋转和颜色调整，增加了操作的复杂度。
            内存开销：红黑树节点比链表节点占用更多的内存。
            实现复杂性：红黑树的实现代码复杂，需要处理多个边界条件和调整步骤。
         *
         * 注：
         *  桶数量超过64时，不会再转换成红黑树，因为随着桶数量增多，发生哈希冲突的概率就越低，综合对比链表的性能更好
         */

        Map<String, Integer> map = new HashMap<>();
        map.put("key1", 1);
        map.put("key2", 2);
        map.put("key3", 3);
        map.forEach((k,v) -> System.out.println("key: " + k + ",value="+v));
    }


    @Test
    public void concurrentHashMap() {
        /*
         * 1.7及之前 分段锁:
         * 1、Map 会被分成多个段，每个段包含一个子哈希表。
         * 2、每个段拥有一个锁，线程在访问某个段时只会加锁该段，而不会影响其他段的访问。
         * 3、这样，如果一个线程正在访问某个段的元素，其他线程可以访问该 Map 的其他段，避免了对整个 Map 加锁的性能瓶颈。
         *
         * 1.8及之后 桶级锁：
         *  Map 被分为多个桶，每个桶包含一组哈希冲突的元素。在进行插入、删除等操作时，ConcurrentHashMap 会对冲突的桶加锁，而不会对整个数据结构加锁。
         *
         *  这种设计可以让多个线程并发访问不同的桶，而不互相阻塞，从而提高了并发性能。
         * 无锁的读操作：对于 get 操作，ConcurrentHashMap 在没有发生哈希冲突时通常不需要加锁，可以直接返回结果。
         *
         *   主要实现方式：CAS来实现无锁算法
         *
         *     核心方法：
         *      Object o：要操作的对象。
         *      long offset：对象字段的内存偏移量，通常是字段在对象内存中的位置。
         *      Object expected：期望的当前值。
         *      Object x：要设置的新值。
         *     public final native boolean compareAndSetObject(Object o, long offset,Object expected,Object x);
         *
         */

        Map<String, Integer> map = new ConcurrentHashMap<>();
        map.put("key1", 1);
        map.put("key2", 2);
        map.put("key3", 3);
        map.forEach((k,v) -> System.out.println("key: " + k + ",value="+v));
    }
}
