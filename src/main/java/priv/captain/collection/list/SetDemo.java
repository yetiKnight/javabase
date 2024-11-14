package priv.captain.collection.list;

import org.junit.Test;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.TreeSet;

/**
 * @description:
 * @author: yetiKnight
 * @since: 2024-11-12
 **/
public class SetDemo {


    @Test
    public void hashSet() {
        /*
         * 一、使用场景：
         * 1、去重
         * 2、快速查找：判断某个元素是否存在
         *
         * 二、原理：基于哈希表实现，底层是一个hashMap，值通过哈希计算后存储，值为key,value是一个常量对象
         *    哈希表存储：HashSet 是通过哈希表来存储元素的。它根据每个元素的 hashCode() 生成一个哈希值，然后使用这个哈希值确定元素的存储位置。
              当你调用 add() 方法时，HashSet 会根据元素的哈希值找到一个位置。如果该位置没有其他元素，它就直接存放该元素。
              如果该位置已经有元素（可能是同样的元素或者哈希冲突），则会进行比较（通过 equals() 方法）来确定元素是否已经存在。

              哈希冲突的处理：
              当两个不同的元素具有相同的哈希值时，发生哈希冲突。HashSet 会通过链表（或树形结构，在 JDK 1.8 之后，链表长度超过阈值时转换为红黑树）来解决哈希冲突。

           注意：当hashSet需要扩容时，需要重新计算元素位置，这时候性能会有所下降，如果已经容量大小，则初始化时设定好。
         */
        HashSet<String> set = new HashSet<>();

        // 添加元素
        set.add("Apple");
        set.add("Banana");
        set.add("Orange");

        // 尝试添加重复元素
        System.out.println(set.add("Apple")); // 输出 false，Apple 已经存在

        // 遍历 HashSet
        for (String fruit : set) {
            System.out.println(fruit);
        }

        // 检查是否包含元素
        System.out.println(set.contains("Banana")); // 输出 true

        // 删除元素
        set.remove("Orange");
        System.out.println(set.contains("Orange")); // 输出 false

        // 获取集合大小
        System.out.println(set.size()); // 输出 2

    }

    @Test
    public void linkedHashSet() {
        /*
         * LinkedHashSet 继承自HashSet，最大的特点就是它维护了插入顺序，基于哈希表+双向链表。
         * 适用场景：当你需要去重且需要按插入顺序遍历元素时，LinkedHashSet 是一个非常好的选择。而如果顺序无关紧要，HashSet 会更为高效。
         */
        LinkedHashSet<Object> linkedHashSet = new LinkedHashSet<>();
        linkedHashSet.add("apple");
    }

    @Test
    public void treeSet() {
        /*
            基于红黑树实现：TreeSet 底层使用红黑树（平衡二叉搜索树）来存储元素，这确保了操作的 对数时间复杂度（O(log n)），包括插入、删除、查找等。

            TreeSet 的应用场景
              需要排序的集合：TreeSet 适用于需要对元素自然顺序 或 自定义比较器顺序时，例如处理一组有序的数据、求最大值或最小值等。
              去重并排序：如果你需要一个既去重又排序的集合，TreeSet 是一个理想选择。例如，获取一组数中的不重复的最小或最大值。
              范围查询：由于 TreeSet 内部使用红黑树，范围查询（如查找某个范围内的元素）会非常高效。
         */
        TreeSet<Integer> treeSet = new TreeSet<>();
        // 添加元素
        treeSet.add(10);
        treeSet.add(20);
        treeSet.add(30);
        treeSet.add(20);  // 不会添加重复元素
        treeSet.add(5);

        // 打印 TreeSet，元素将按升序排序
        System.out.println("treeSet elements= " + treeSet);

        // 查找最小和最大元素
        System.out.println("First element: " + treeSet.first());  // 输出: 5
        System.out.println("Last element: " + treeSet.last());    // 输出: 30

        // 删除元素
        treeSet.remove(10);
        System.out.println("After removing 10: " + treeSet);  // 输出: [5, 20, 30]

        // 检查元素是否存在
        System.out.println("Contains 20: " + treeSet.contains(20));  // 输出: true
        System.out.println("Contains 100: " + treeSet.contains(100)); // 输出: false
    }

}
