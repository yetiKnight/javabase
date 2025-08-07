package priv.captain.collection.list;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ListStudyDemo
 * 系统性展示List常用知识点和用法，便于理解和运用。
 */
public class ListStudyDemo {
    public static void main(String[] args) {
        // 1. List的常见实现类
        List<String> arrayList = new ArrayList<>(); // 线程不安全，查询快，增删慢
        List<String> linkedList = new LinkedList<>(); // 适合频繁增删
        List<String> vector = new Vector<>(); // 线程安全，效率低
        List<String> cowList = new CopyOnWriteArrayList<>(); // 适合读多写少的并发场景

        // 2. 添加元素
        arrayList.add("A");
        arrayList.add("B");
        arrayList.add("C");
        System.out.println("ArrayList添加元素后：" + arrayList);

        // 3. 指定位置插入元素
        arrayList.add(1, "X");
        System.out.println("指定位置插入元素：" + arrayList);

        // 4. 获取元素
        String value = arrayList.get(2);
        System.out.println("获取下标2的元素：" + value);

        // 5. 修改元素
        arrayList.set(2, "Y");
        System.out.println("修改下标2的元素：" + arrayList);

        // 6. 删除元素
        arrayList.remove("X"); // 按值删除
        arrayList.remove(1);    // 按下标删除
        System.out.println("删除元素后：" + arrayList);

        // 7. 遍历List
        System.out.println("for-each遍历：");
        for (String s : arrayList) {
            System.out.println(s);
        }
        System.out.println("for循环遍历：");
        for (int i = 0; i < arrayList.size(); i++) {
            System.out.println(arrayList.get(i));
        }
        System.out.println("迭代器遍历：");
        Iterator<String> it = arrayList.iterator();
        while (it.hasNext()) {
            System.out.println(it.next());
        }

        // 8. 判断是否包含元素
        System.out.println("是否包含A：" + arrayList.contains("A"));

        // 9. List转数组
        String[] arr = arrayList.toArray(new String[0]);
        System.out.println("List转数组：" + Arrays.toString(arr));

        // 10. 清空List
        arrayList.clear();
        System.out.println("清空后：" + arrayList);

        // 11. 不可变List（JDK9+）
        List<String> immutableList = List.of("a", "b", "c");
        System.out.println("不可变List：" + immutableList);
        // immutableList.add("d"); // 会抛出UnsupportedOperationException

        // 12. List排序
        List<Integer> numList = Arrays.asList(3, 1, 2);
        numList.sort(Integer::compareTo);
        System.out.println("排序后的List：" + numList);

        // 13. List去重
        List<String> dupList = Arrays.asList("a", "b", "a", "c");
        List<String> distinctList = new ArrayList<>(new LinkedHashSet<>(dupList));
        System.out.println("去重后的List：" + distinctList);

        // 14. List的线程安全包装
        List<String> syncList = Collections.synchronizedList(new ArrayList<>());
        syncList.add("safe");
        System.out.println("线程安全List：" + syncList);

        // 15. CopyOnWriteArrayList线程安全用法
        cowList.add("read");
        cowList.add("write");
        System.out.println("CopyOnWriteArrayList内容：" + cowList);
    }
}