### 📘 Java 面试复习笔记：LRU（Least Recently Used）

### ✅ 一、概念简介

**LRU 是什么？**

LRU，全称 **Least Recently Used**，即**最近最少使用**。它是一种**缓存淘汰策略**，其核心思想是：当缓存空间已满，需要淘汰一部分数据时，优先淘汰那些**最长时间没有被使用过**的数据。

可以把 LRU 想象成一个**书架**：

  - 书架的空间是有限的。
  - 你每拿出一本书（访问一个数据），就把它放到书架的最显眼处（最近使用）。
  - 当书架满了，你需要放一本新书时，就从书架最角落里那本积满灰尘的书（最久未使用）开始扔掉。

**为什么用？**

🎯 **核心目的：** 在有限的内存空间内，保留最可能被再次访问的热点数据，从而提高缓存命中率，减少对后端存储（如数据库、硬盘）的访问，提升系统性能。

  * **命中率高：** LRU 策略基于“局部性原理”（locality of reference），即最近被访问的数据在未来更有可能被访问。这使得 LRU 能够有效地保留热点数据。
  * **实现简单：** 相比其他复杂的淘汰算法（如 LFU），LRU 的概念和实现都相对简单。

-----

### 🔹 二、核心原理与数据结构

要实现一个 LRU 缓存，我们需要一个能够同时满足以下两个要求的数据结构：

1.  **快速查找：** 能够以 O(1) 的时间复杂度快速定位到某个数据。
2.  **快速移动/删除：** 能够在 O(1) 的时间复杂度将某个数据移动到链表头部或从链表尾部删除。

结合这两个需求，最经典、最常用的数据结构组合是：**哈希表（`HashMap`）+ 双向链表**。

  * **哈希表（`HashMap`）：**

      * 作用：提供 O(1) 时间复杂度的查找。
      * 存储内容：键 `Key` 映射到双向链表中的节点 `Node`。
      * `Map<Key, Node>`

  * **双向链表（`LinkedList`）：**

      * 作用：维持数据的访问顺序。链表头部是最近访问的数据，链表尾部是最久未访问的数据。
      * 存储内容：每个节点 `Node` 存放键值对 `(Key, Value)`。
      * **优点：** 可以在 O(1) 时间复杂度内进行节点的添加和删除，而无需遍历。

**LRU 缓存的操作流程**

1.  **访问数据（`get(key)`）：**

      * 在哈希表中查找 `key`。
      * 如果找到，将对应的节点从链表中**移除**，并将其**添加到链表头部**。然后返回节点的值。
      * 如果未找到，返回 `null`。

2.  **添加数据（`put(key, value)`）：**

      * 在哈希表中查找 `key`。
      * 如果键已存在：更新节点的值，并将节点移动到链表头部。
      * 如果键不存在：
          * 创建一个新的节点。
          * 检查缓存容量是否已满。
          * 如果已满，删除链表尾部的节点（最久未使用的），并在哈希表中移除对应的键。
          * 将新节点添加到链表头部，并在哈希表中添加该键值对。

-----

### ✅ 三、LRU 的实现方式

#### 1\. 基于 `LinkedHashMap` 实现（最简单）

这是最常见的实现方式，因为 `LinkedHashMap` 本身就实现了**哈希表 + 双向链表**的数据结构，并提供了**访问顺序**（`accessOrder=true`）模式以及一个可以重写的淘汰方法 `removeEldestEntry()`。

```java
import java.util.LinkedHashMap;
import java.util.Map;

public class LRUCacheWithLinkedHashMap<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;

    public LRUCacheWithLinkedHashMap(int capacity) {
        // initialCapacity, loadFactor, accessOrder
        super(capacity, 0.75f, true); 
        this.capacity = capacity;
    }

    /**
     * 重写此方法，当返回 true 时，LinkedHashMap 会移除最老的键值对。
     * 这个方法在 put 和 putAll 之后被调用。
     */
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }
}
```

#### 2\. 手动实现（面试常考）

这能更好地展示你对 LRU 底层原理的理解。

```java
import java.util.HashMap;
import java.util.Map;

// 定义双向链表节点
class Node<K, V> {
    K key;
    V value;
    Node<K, V> prev;
    Node<K, V> next;

    public Node() {}
    public Node(K key, V value) {
        this.key = key;
        this.value = value;
    }
}

public class LRUCache<K, V> {
    private final Map<K, Node<K, V>> cache;
    private final int capacity;
    private final Node<K, V> head; // 虚拟头节点
    private final Node<K, V> tail; // 虚拟尾节点

    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.cache = new HashMap<>();
        this.head = new Node<>();
        this.tail = new Node<>();
        head.next = tail;
        tail.prev = head;
    }

    // 移动节点到头部
    private void moveToHead(Node<K, V> node) {
        // 先移除当前节点
        node.prev.next = node.next;
        node.next.prev = node.prev;
        // 再将节点添加到头部
        addToHead(node);
    }

    // 添加节点到头部
    private void addToHead(Node<K, V> node) {
        node.prev = head;
        node.next = head.next;
        head.next.prev = node;
        head.next = node;
    }

    // 移除尾部节点
    private void removeTail() {
        Node<K, V> last = tail.prev;
        last.prev.next = tail;
        tail.prev = last.prev;
        // 从 Map 中移除
        cache.remove(last.key);
    }
    
    public V get(K key) {
        if (!cache.containsKey(key)) {
            return null;
        }
        Node<K, V> node = cache.get(key);
        moveToHead(node);
        return node.value;
    }

    public void put(K key, V value) {
        if (cache.containsKey(key)) {
            Node<K, V> node = cache.get(key);
            node.value = value;
            moveToHead(node);
        } else {
            Node<K, V> newNode = new Node<>(key, value);
            cache.put(key, newNode);
            addToHead(newNode);
            if (cache.size() > capacity) {
                removeTail();
            }
        }
    }
}
```

### 🔍 四、真实面试高频问题 + 深度解析

**1. 什么是 LRU？请简述其原理和实现思路。**

  * **标准答案：** LRU 是一种缓存淘汰策略，优先淘汰最久未被使用的数据。实现思路是结合**哈希表和双向链表**。哈希表用于 O(1) 快速查找，双向链表用于维护访问顺序。访问或添加数据时，将节点移动到链表头部；当容量满时，淘汰链表尾部的节点。

**2. 为什么 `HashMap` 和 `LinkedList` 的组合能够实现 LRU？**

  * **标准答案：**
      * **`HashMap`：** 解决了快速查找的问题。通过键可以直接在哈希表中找到对应的链表节点，时间复杂度为 O(1)。
      * **`LinkedList`：** 解决了快速移动和删除的问题。双向链表使得我们可以在 O(1) 时间复杂度内将一个节点从链表中移除，并添加到链表的另一端。

**3. 如何用 `LinkedHashMap` 实现 LRU？请解释其原理。**

  * **标准答案：** `LinkedHashMap` 本身就内置了哈希表和双向链表。通过在其构造函数中传入 `accessOrder=true`，可以使其按照访问顺序排序。然后，重写 `removeEldestEntry()` 方法，在 Map 容量超过设定的值时返回 `true`，`LinkedHashMap` 就会自动移除链表头部（最老的）元素。

**4. 谈谈你对 LRU 算法的理解，以及它在实际项目中的应用。**

  * **标准答案：** LRU 算法基于局部性原理，是解决内存有限、数据访问不均的通用解决方案。
  * **实际应用：**
      * **Web 服务器缓存：** 缓存静态资源、热点 API 响应。
      * **数据库：** 数据库的缓存区（Buffer Pool）通常使用 LRU 或其变种来缓存热点数据页。
      * **CPU 缓存：** 计算机操作系统中的页面置换算法也常常使用 LRU。
      * **自定义缓存系统：** 在需要缓存一些经常访问的数据时，如用户信息、配置数据等。

**5. LRU 和 LFU（Least Frequently Used）有什么区别？**

  * **标准答案：**
      * **LRU：** 基于**访问时间**，优先淘汰最久未访问的数据。
      * **LFU：** 基于**访问频率**，优先淘汰访问次数最少的数据。
  * **优缺点：**
      * **LRU：** 实现简单，但无法很好地处理“突发性”访问。例如，一个数据在短时间内被高频访问，LRU 会将其保留，但随后该数据可能再也不会被访问。
      * **LFU：** 能够更好地应对“偶发性”访问，因为它关注的是长期访问频率。但实现更复杂，需要额外的数据结构（如堆）来维护访问频率，且访问频率的计算本身就有开销。

### 💡 五、口诀 + 表格/图示辅助记忆

**LRU 实现口诀**

> **哈希表，找得快。**
> **双向链，序排列。**
> **新来旧走，头尾换。**
> **存满溢出，尾部删。**

**LRU vs LFU 对比表**

| 特性 | **LRU（最近最少使用）** | **LFU（最不常用）** |
| :--- | :--- | :--- |
| **淘汰依据** | 最后一次访问时间 | 访问频率（次数） |
| **实现难度** | 简单（哈希表 + 双向链表） | 复杂（通常需结合哈希表和堆） |
| **适用场景** | 大多数热点数据场景 | 访问频率相对稳定的场景 |
| **优点** | 实现简单，性能好 | 更好地保留高频数据 |
| **缺点** | 无法处理突发性访问 | 实现复杂，开销大 |

-----

### 🎁 六、建议 + 误区提醒

**误区提醒**

1.  **认为 LRU 只是一种算法，没有实际应用：** LRU 是一个非常重要的缓存策略，在工程实践中被广泛使用。
2.  **混淆 `get` 和 `put` 的操作：** 记住，`get` 和 `put` 操作都会导致数据被移动到链表头部。
3.  **认为 LRU 总是最佳选择：** LRU 并不总是最好的，对于某些特定的访问模式，其他算法（如 LFU 或 ARC）可能表现更好。

**使用建议**

1.  **优先使用 `LinkedHashMap`：** 在大多数情况下，直接使用 `LinkedHashMap` 来实现 LRU 是最简单、最可靠的方式。
2.  **手写实现：** 在面试中，手写 LRU 实现可以更好地展示你的数据结构和算法功底。
3.  **考虑并发：** 这里的实现都是单线程的。如果需要在多线程环境中使用，需要进行外部同步（例如，使用 `synchronized`）或选择并发安全的替代方案。
4.  **理解 LRU 的局限性：** 任何缓存算法都有其局限性，理解 LRU 的优缺点能让你在设计系统时做出更明智的选择。