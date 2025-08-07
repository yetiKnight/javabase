package priv.captain.collection.list;

// 📌 考点：ArrayList 底层原理、扩容机制、线程安全与 fail-fast
// 🎯 面试常问：
//   1. ArrayList 的底层实现原理是什么？
//   2. ArrayList 和 LinkedList 有什么区别？
//   3. ArrayList 的扩容机制？默认容量？
//   4. 为什么 ArrayList 线程不安全？如何保证安全？
//   5. JDK8/11/17 不同版本底层有变化吗？
// 🧠 关键点：
//   - ArrayList 本质是基于动态数组实现的，底层用 transient Object[] elementData 存储元素。
//   - 默认容量（JDK8+）：首次 add 时才分配 10 个空间（懒加载）。
//   - 扩容机制：容量不够时，扩容为原容量的 1.5 倍（newCapacity = oldCapacity + (oldCapacity >> 1)）。
//   - 线程不安全，适合单线程场景；多线程需用 Collections.synchronizedList 或 CopyOnWriteArrayList。
// ⚠️ 易错点：
//   - remove(int index) 和 remove(Object o) 容易混淆。
//   - 扩容时数组复制，频繁 add 可能影响性能。
//   - 不能用于高并发场景，否则可能数据错乱。
// 💬 衍生思考：
//   - 源码中的 modCount 机制用于 fail-fast，面试常考。
//   - ArrayList 的扩容策略和 HashMap 的区别。
//   - JDK 9+ 支持 List.of() 创建不可变 List。

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ArrayListSourceAnalysis {
    public static void main(String[] args) {
        // 创建 ArrayList，初始容量为默认（JDK8+懒加载，首次 add 时才分配空间）
        List<String> list = new ArrayList<>();

        // 添加元素
        list.add("Java");
        list.add("面试");
        list.add("ArrayList");

        // 遍历元素
        for (String s : list) {
            System.out.println(s); // 输出每个元素
        }

        // 指定位置插入
        list.add(1, "学习笔记"); // 在索引1插入

        // 删除元素（注意：remove(int) 和 remove(Object) 区别）
        list.remove("Java"); // 按值删除
        list.remove(0);      // 按索引删除

        // 获取元素
        String value = list.get(0);

        // 修改元素
        list.set(0, "Java基础");

        // 获取大小
        int size = list.size();

        // 判断是否包含
        boolean hasNote = list.contains("学习笔记");

        // 扩容机制演示：不断添加元素，触发扩容
        for (int i = 0; i < 20; i++) {
            list.add("lang" + i);
        }
        // 当元素数量超过当前容量时，ArrayList 会自动扩容为原容量的1.5倍，并复制原有数据到新数组

        // 源码机制：modCount 用于 fail-fast
        // 错误写法：遍历时直接修改结构会抛出 ConcurrentModificationException
        /*
        for (String s : list) {
            list.remove(s); // 错误写法，遍历时不能修改结构
        }
        */

        // 正确写法：用迭代器的 remove 方法
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            String s = it.next();
            if ("ArrayList".equals(s)) {
                it.remove(); // 迭代器的 remove 不会 fail-fast
            }
        }

        // 线程安全问题演示
        // 错误用法：多线程环境下直接操作 ArrayList 可能导致数据错乱
        /*
        List<String> unsafeList = new ArrayList<>();
        Runnable task = () -> {
            for (int i = 0; i < 1000; i++) {
                unsafeList.add(Thread.currentThread().getName() + "-" + i);
            }
        };
        new Thread(task).start();
        new Thread(task).start();
        // 可能出现数据丢失或异常，ArrayList 不是线程安全的
        */

        // 正确用法：多线程环境下使用 Collections.synchronizedList 或 CopyOnWriteArrayList
        List<String> syncList = Collections.synchronizedList(new ArrayList<>());
        List<String> cowList = new CopyOnWriteArrayList<>();

        // JDK9+ 不可变 List
        // List<String> immutableList = List.of("A", "B", "C"); // 不能 add/remove

        // 面试延伸：ArrayList 和 LinkedList 区别
        // 1. ArrayList 随机访问快，插入/删除慢；LinkedList 插入/删除快，随机访问慢
        // 2. ArrayList 底层是数组，LinkedList 是双向链表
        // 3. ArrayList 扩容时会整体复制，LinkedList 不需要扩容

        // 推荐记忆口诀：查快增慢用 ArrayList，增删多用 LinkedList，线程安全用 Vector 或并发包
    }
}

// 🌱 总结：ArrayList 是基于动态数组实现的，扩容机制和 fail-fast 是面试高频考点，注意线程安全和 remove 用法的区别。