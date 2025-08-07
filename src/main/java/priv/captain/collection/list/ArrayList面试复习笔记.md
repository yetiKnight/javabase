# 📘 Java 面试复习笔记：ArrayList

## ✅ 一、概念简介
ArrayList 是 Java 集合框架中最常用的动态数组实现，属于 List 接口的可变长度实现，支持元素的有序存储、随机访问，允许存储重复元素。

- 主要作用：动态存储、快速随机访问、适合频繁查找/遍历场景。
- 线程不安全，适用于单线程或外部加锁的多线程场景。

## 🔹 二、底层原理 / 实现机制
- 底层基于 **动态数组**（Object[] elementData），初始容量默认 10（JDK8+）。
- 扩容机制：容量不足时，扩容为原容量的 1.5 倍（JDK8+）。
- 支持随机访问（get/set），时间复杂度 O(1)。
- 插入/删除元素时，涉及数组元素的移动，时间复杂度 O(n)。
- 允许 null 元素，元素可重复。
- modCount 变量用于快速失败（fail-fast）机制，检测并发修改。

## ✅ 三、常用方法 / 使用方式
- **add(E e)**：尾部添加元素
- **add(int index, E element)**：指定位置插入
- **get(int index)**：获取指定位置元素
- **set(int index, E element)**：替换指定位置元素
- **remove(int index/Object o)**：删除元素
- **size()**：获取元素个数
- **clear()**：清空所有元素
- **contains(Object o)**：判断是否包含
- **toArray()**：转为数组

```java
// 示例：常用方法演示
ArrayList<String> list = new ArrayList<>(); // 创建空列表
list.add("A"); // 添加元素A
list.add("B"); // 添加元素B
list.add(1, "C"); // 在索引1插入C，原B后移
String val = list.get(0); // 获取索引0的元素A
list.set(0, "D"); // 替换索引0的元素为D
list.remove("B"); // 删除元素B
boolean hasC = list.contains("C"); // 判断是否包含C
list.clear(); // 清空列表
```

**应用场景：**
- 频繁查找、遍历、随机访问
- 不适合频繁插入/删除（尤其是头部/中间）

**优缺点对比：**
- 优点：查询快、内存连续、支持随机访问
- 缺点：插入/删除慢、扩容有性能损耗、线程不安全

## 🔍 四、源码关键片段分析
- **add(E e) 方法核心片段（JDK8）：**
```java
public boolean add(E e) {
    ensureCapacityInternal(size + 1); // 检查扩容
    elementData[size++] = e; // 添加元素，size自增
    return true;
}
```
- **扩容机制：**
```java
private void grow(int minCapacity) {
    int oldCapacity = elementData.length;
    int newCapacity = oldCapacity + (oldCapacity >> 1); // 1.5倍扩容
    if (newCapacity < minCapacity)
        newCapacity = minCapacity;
    elementData = Arrays.copyOf(elementData, newCapacity);
}
```
- **modCount 变量：**
  - 每次结构性修改（如 add/remove）都会自增，用于 fail-fast 检测。

## 🎯 五、面试高频问题及解析
1. **ArrayList 和 LinkedList 区别？**
   - ArrayList 底层数组，查询快，插入/删除慢；LinkedList 底层双向链表，插入/删除快，查询慢。
2. **ArrayList 扩容机制？**
   - 默认容量 10，扩容为原来的 1.5 倍，数据迁移有性能损耗。
3. **线程安全问题？**
   - ArrayList 线程不安全，多线程需加锁或用 CopyOnWriteArrayList。
4. **fail-fast 机制？**
   - 迭代时结构被修改，抛 ConcurrentModificationException。
5. **允许 null 吗？如何判断元素存在？**
   - 允许 null，contains 通过 equals 判断。
6. **ArrayList 如何实现随机访问？**
   - 通过数组下标直接定位，O(1) 时间复杂度。

## 💡 六、口诀总结 / 图示助记
- 口诀：查快增慢，扩容一半，线程不安，fail-fast 报难。
- 图解：

| 操作       | 时间复杂度 | 备注         |
|------------|------------|--------------|
| get/set    | O(1)       | 随机访问快   |
| add/remove | O(n)       | 需移动元素   |
| contains   | O(n)       | 顺序查找     |

## 🎁 bonus：建议 vs 常见误区
- **建议：**
  - 预估容量，构造时指定初始容量，减少扩容次数。
  - 多线程场景用 Collections.synchronizedList 或 CopyOnWriteArrayList。
- **常见误区：**
  - 误以为线程安全，实际需手动加锁。
  - 误用 remove(int) 和 remove(Object) 导致删除错误。
  - 迭代时增删元素，易抛 ConcurrentModificationException。
