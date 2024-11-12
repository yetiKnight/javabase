package priv.captain.collection.list;


import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @ClassName:ListDemo
 * @Description:对List的使用及原理分析
 * @author:CNT-Captain
 * @date:2020年8月28日 下午4:59:53
 * @Copyright:2020 https://gitee.com/CNT-Captain Inc. All rights reserved.
 */
public class ListDemo {

    @Test
    public void arrayList() {

        /*
         * 底层基于动态数组的支持元素随机访问，其实现了标记接口RandomAccess，表示其支持随机访问。
         *
         * 默认new空数组，，后第一放入时默认10续add都会检查是否超容量，若超容量执行grow(int minCapacity),
         * 扩容规则：int newCapacity = oldCapacity + (oldCapacity >> 1); 大约1.5
         */
        List<String> arrayList = new ArrayList<>();
        arrayList.add("666");
        arrayList.add(2, "5555");
        arrayList.forEach(System.out::println);
    }

    @Test
    public void linkedList() {
        /*
         * 底层是一个双向链表
         * 使用场景：
         *   适用于需要频繁插入和删除元素，特别是当这些操作发生在集合的头部或尾部时。
         *   LinkedList 适用于需要按照位置访问元素（按索引）且不需要元素唯一性的场景，如实现队列、栈等数据结构。
         */
        List<Integer> list = new LinkedList<>();
        list.add(1);
        list.add(2);
        list.add(3);
    }

    /*
      List的线程安全办法
     */
    @Test
    public void sync() {
        List<Integer> list = new ArrayList<>();
        // 方法1：使用Collections.synchronizedList,对所有操作（如添加、删除、查询）都加synchronized
        List<Integer> synchronizedList = Collections.synchronizedList(list);
        synchronizedList.add(1);
        synchronizedList.add(2);

        /*
         * 方法2：使用CopyOnWriteArrayList
         *  它通过复制整个数组来保证并发写操作的安全性。每次修改（例如添加、删除、设置元素）都会创建一个新的底层数组，
         *  因此在读取操作时没有任何锁竞争，保证了高效的读取性能。写操作相对较慢，因为每次修改都会涉及到数组的复制。
         *     synchronized (lock) {
         *        XXXX
         *     }
         *   特点：
         *   高效的读操作：读操作不需要加锁，适合读多写少的场景。
         *   较慢的写操作：写操作需要复制整个数组，因此在高频繁写操作的场景下性能较差。
         */
        List<Integer> clist = new CopyOnWriteArrayList<>();
        clist.add(1);
        clist.add(2);
    }
}
