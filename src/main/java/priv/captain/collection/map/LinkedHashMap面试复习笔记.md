### 📘 Java 面试复习笔记：LinkedHashMap

### ✅ 一、概念简介

**LinkedHashMap 是什么？**

`LinkedHashMap` 是 `java.util` 包下的一个 Map 接口实现，它继承自 `HashMap`。它结合了哈希表和双向链表的优点，既能像 `HashMap` 一样提供快速的键值查找（O(1)），又能像 `LinkedList` 一样保持元素的**插入顺序**或**访问顺序**。

可以把 `LinkedHashMap` 想象成一个**有序的 HashMap**：

  - 它在 `HashMap` 的基础上，为每个键值对（`Entry`）增加了一对前后指针。
  - 这些指针将所有键值对连接成了一个双向链表，从而维持了特定的顺序。

**为什么用？**

🎯 **核心目的：** 在保证高效查找的同时，维持元素的插入或访问顺序。

  * **保持顺序：** 在需要遍历 Map 且希望顺序可控的场景下，`LinkedHashMap` 是比 `HashMap` 更好的选择。
  * **实现 LRU 缓存：** `LinkedHashMap` 提供了一个特殊的构造函数，可以使其按照元素的访问顺序进行排序。通过重写 `removeEldestEntry()` 方法，它可以非常方便地实现一个简单的 **LRU（最近最少使用）缓存**淘汰策略。

-----

### 🔹 二、底层原理与数据结构

`LinkedHashMap` 的底层数据结构是**哈希表 + 双向链表**。

#### 1\. 继承自 `HashMap`

  - `LinkedHashMap` 继承了 `HashMap` 的所有特性，包括数组、链表、红黑树等。
  - 它保留了 `HashMap` 的核心查找机制，即通过哈希值快速定位到数组索引。

#### 2\. 增强的双向链表

  - `LinkedHashMap` 在 `HashMap` 的 `Node` 节点基础上，增加了一个 `before` 和一个 `after` 指针。
  - 这些指针将所有 `Node` 节点（即键值对）按顺序串联起来，形成一个双向链表。
  - 链表的头（`head`）指向最早插入或最近最少访问的节点，尾（`tail`）指向最新插入或最近访问的节点。

#### 3\. 两种排序模式

`LinkedHashMap` 有一个布尔类型的成员变量 `accessOrder`，用于控制其排序模式。

  * **插入顺序（`accessOrder = false`，默认）：**
      - 当一个新元素被插入时，它会被添加到链表的尾部。
      - 元素的顺序与它们被放入 Map 的顺序一致。
  * **访问顺序（`accessOrder = true`）：**
      - 当你使用 `get()` 或 `put()` 方法访问一个键值对时，这个键值对会被移动到链表的尾部。
      - 这样，链表的头部总是最近最少访问的元素，非常适合实现 LRU 缓存。

-----

### ✅ 三、常用方式 + 代码示例

`LinkedHashMap` 的用法与 `HashMap` 类似，但其排序特性是关键。

#### 1\. 默认的插入顺序模式

```java
import java.util.LinkedHashMap;
import java.util.Map;

public class LinkedHashMapExample {
    public static void main(String[] args) {
        // 创建一个 LinkedHashMap，使用默认的插入顺序模式
        Map<String, String> map = new LinkedHashMap<>();
        
        map.put("key1", "value1");
        map.put("key2", "value2");
        map.put("key3", "value3");
        
        System.out.println("按插入顺序遍历 LinkedHashMap:");
        for (Map.Entry<String, String> entry : map.entrySet()) {
            System.out.println(entry.getKey() + " = " + entry.getValue());
        }
        
        // 即使访问元素，顺序也不会改变
        map.get("key2");
        System.out.println("\n访问 key2 后，再次遍历：");
        for (Map.Entry<String, String> entry : map.entrySet()) {
            System.out.println(entry.getKey() + " = " + entry.getValue());
        }
    }
}
```

#### 2\. 访问顺序模式与 LRU 缓存实现

```java
import java.util.LinkedHashMap;
import java.util.Map;

public class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;

    // 构造函数，设置容量和访问顺序
    public LRUCache(int capacity) {
        // initialCapacity, loadFactor, accessOrder
        super(capacity, 0.75f, true);
        this.capacity = capacity;
    }

    // 重写此方法，当返回 true 时，会删除最老的键值对
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        // 当 Map 的大小超过容量时，返回 true，触发删除最老的元素
        return size() > capacity;
    }

    public static void main(String[] args) {
        LRUCache<Integer, String> cache = new LRUCache<>(3);
        
        cache.put(1, "value1");
        cache.put(2, "value2");
        cache.put(3, "value3");
        System.out.println("初始缓存: " + cache); // {1=value1, 2=value2, 3=value3}
        
        // 访问 key2，它会移动到链表尾部
        cache.get(2); 
        System.out.println("访问 key2 后: " + cache); // {1=value1, 3=value3, 2=value2}
        
        // 插入 key4，超过容量，最老的元素（key1）被移除
        cache.put(4, "value4");
        System.out.println("插入 key4 后: " + cache); // {3=value3, 2=value2, 4=value4}
    }
}
```

### 🔍 四、真实面试高频问题 + 深度解析

**1. `LinkedHashMap` 和 `HashMap` 有什么区别？**

  * **标准答案：** `LinkedHashMap` 在 `HashMap` 的基础上，通过一个双向链表额外维护了元素的**插入顺序**（或访问顺序），而 `HashMap` 是无序的。
  * **性能：** `LinkedHashMap` 的 `put()`、`get()`、`remove()` 等操作的平均时间复杂度依然是 O(1)，但由于需要维护链表，其性能会略低于 `HashMap`。

**2. `LinkedHashMap` 和 `TreeMap` 有什么区别？**

  * **标准答案：**
      * **底层实现：** `LinkedHashMap` 基于**哈希表 + 双向链表**，`TreeMap` 基于**红黑树**。
      * **顺序：** `LinkedHashMap` 保持**插入顺序**或**访问顺序**，`TreeMap` 保持**自然排序**或**自定义排序**。
      * **性能：** `LinkedHashMap` 的基本操作平均时间复杂度为 O(1)，`TreeMap` 为 O(log n)。
  * **选择：** 如果你需要保持插入顺序，用 `LinkedHashMap`。如果你需要按键进行排序，用 `TreeMap`。

**3. 如何用 `LinkedHashMap` 实现 LRU 缓存？**

  * **标准答案：**
    1.  创建一个 `LinkedHashMap` 实例，并使用其带 `accessOrder=true` 的构造函数，使其按照访问顺序排序。
    2.  重写 `LinkedHashMap` 的 `protected boolean removeEldestEntry(Map.Entry<K, V> eldest)` 方法。
    3.  在这个方法中，判断当前 Map 的大小是否超过了预设的容量，如果超过则返回 `true`，`LinkedHashMap` 会自动移除链表头部最老的那个元素。

**4. `LinkedHashMap` 是线程安全的吗？**

  * **标准答案：** `LinkedHashMap` 和 `HashMap` 一样，都是**线程不安全**的。
  * **解决方案：** 在多线程环境下，你可以使用 `Collections.synchronizedMap()` 方法来获取一个线程安全的 `LinkedHashMap` 视图，或者使用 `ConcurrentHashMap`，但后者不保留顺序。

### 💡 五、口诀 + 表格/图示辅助记忆

**`LinkedHashMap` 口诀**

> **哈希表，双向链。**
> **查找快，顺序在。**
> **LRU，靠它建。**
> **线程不安全，别乱用。**

**Map 三兄弟核心特性对比**

| 特性 | **`HashMap`** | **`LinkedHashMap`** | **`TreeMap`** |
| :--- | :--- | :--- | :--- |
| **底层数据结构**| 哈希表 | 哈希表 + 双向链表 | 红黑树 |
| **元素顺序** | 无序 | 插入顺序或访问顺序 | 自然排序或自定义排序 |
| **性能** | 最佳 (O(1)) | 良好 (O(1)) | 较差 (O(log n)) |
| **是否线程安全**| 否 | 否 | 否 |
| **主要应用** | 快速查找 | 顺序遍历或 LRU 缓存 | 按键排序 |

-----

### 🎁 六、建议 + 误区提醒

**误区提醒**

1.  **忘记手动设置 `accessOrder=true`：** 默认情况下，`LinkedHashMap` 是按插入顺序排列的。如果你想实现 LRU 缓存，必须在构造函数中明确指定 `true`。
2.  **在多线程环境中使用：** 这是一个常见错误，`LinkedHashMap` 本身不提供同步保证，如果多线程访问，可能会导致数据不一致。

**使用建议**

1.  **根据需求选择：** 当你既需要 `Map` 的功能，又需要保持元素的插入顺序时，`LinkedHashMap` 是最合适的选择。
2.  **LRU 缓存：** `LinkedHashMap` 是实现简单 LRU 缓存的绝佳工具。但如果需要更高级的并发功能，可能需要考虑其他专业的缓存框架。
3.  **线程安全：** 如果在并发场景下使用 `LinkedHashMap`，请务必进行外部同步，或者考虑使用支持并发的替代方案。