-----

### 📘 Java 面试复习笔记：LFU（Least Frequently Used）

### ✅ 一、概念简介

**LFU 是什么？**

LFU，全称 **Least Frequently Used**，即**最不常用**。它是一种缓存淘汰策略，其核心思想是：当缓存空间已满，需要淘汰一部分数据时，优先淘汰那些**访问频率最低**的数据。

可以把 LFU 想象成一个**热门歌曲排行榜**：

  - 播放器（缓存）的空间有限。
  - 每首歌被播放一次，它的播放次数（访问频率）就增加一次。
  - 当需要添加一首新歌时，播放器会找到播放次数最少的那首歌，并将其从排行榜上移除。

**为什么用？**

🎯 **核心目的：** 在有限的内存空间内，保留那些长期以来被证明是热点的数据，从而提高缓存命中率，减少对后端存储的访问。

  * **长期有效：** LFU 策略能够更好地应对**突发性访问**。一个数据即使在短时间内被高频访问，但其长期访问频率不高，LFU 也会将其淘汰。这比 LRU（只看最近一次访问时间）更有优势。
  * **保留核心数据：** LFU 算法倾向于保留那些持续被高频访问的核心数据，这对于某些数据访问模式（如日志分析、热门商品推荐）非常有效。

-----

### 🔹 二、核心原理与数据结构

要实现一个 LFU 缓存，我们需要一个能够同时满足以下两个要求的数据结构：

1.  **快速查找：** 能够以 O(1) 的时间复杂度快速定位到某个数据及其访问频率。
2.  **快速淘汰：** 能够以 O(log n) 或更好的时间复杂度找到并删除访问频率最低的数据。

为了满足这些要求，LFU 的实现通常比 LRU 复杂得多。一种经典、高效的实现方式是**哈希表 + 频率列表**。

  * **哈希表（`HashMap`）：**

      * 作用：提供 O(1) 的快速查找。
      * 存储内容：键 `Key` 映射到双向链表中的节点 `Node`。
      * `Map<Key, Node>`

  * **频率列表（`LinkedHashSet` / `LinkedList` 数组）：**

      * 作用：将具有相同访问频率的节点组织在一起。
      * 存储内容：
          * 一个 `Map<Integer, LinkedList<Node>>`，键是访问频率，值是双向链表，链表中存放着所有具有该频率的节点。
          * 另有一个变量 `minFrequency`，用于记录当前缓存中最小的访问频率。

**LFU 缓存的操作流程**

1.  **访问数据（`get(key)`）：**

      * 在哈希表中查找 `key`。
      * 如果找到，将对应的节点**访问频率加 1**。
      * 将该节点从原频率的链表中**移除**，并将其**添加到新频率的链表中**。
      * 如果原频率的链表为空，并且该频率等于 `minFrequency`，则 `minFrequency` 加 1。
      * 如果未找到，返回 `null`。

2.  **添加数据（`put(key, value)`）：**

      * 在哈希表中查找 `key`。
      * 如果键已存在：更新节点的值，并执行与 `get` 相同的操作（频率加 1，移动到新链表）。
      * 如果键不存在：
          * 检查缓存容量是否已满。
          * 如果已满，找到 `minFrequency` 对应的链表，**删除该链表的尾部节点**，并在哈希表中移除对应的键。
          * 创建一个新的节点，频率为 1。
          * 将 `minFrequency` 重置为 1。
          * 将新节点添加到频率为 1 的链表头部，并在哈希表中添加该键值对。

-----

### ✅ 三、LFU 的实现方式

由于 LFU 的底层数据结构复杂，手动实现通常是面试的重点。

```java
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

// LFU 缓存的简单实现
public class LFUCache {
    private final int capacity;
    // 存储键值对，键是 key，值是 Node 节点
    private final Map<Integer, Node> cache;
    // 存储频率到双向链表的映射
    private final Map<Integer, LinkedHashSet<Node>> freqMap;
    // 记录最小频率
    private int minFreq;

    public LFUCache(int capacity) {
        this.capacity = capacity;
        this.cache = new HashMap<>();
        this.freqMap = new HashMap<>();
        this.minFreq = 0;
    }

    public int get(int key) {
        if (!cache.containsKey(key)) {
            return -1;
        }
        Node node = cache.get(key);
        // 访问频率加1，并移动到新频率的链表
        increaseFreq(node);
        return node.value;
    }

    public void put(int key, int value) {
        if (capacity == 0) return;
        
        if (cache.containsKey(key)) {
            Node node = cache.get(key);
            node.value = value;
            increaseFreq(node);
            return;
        }

        // 容量已满，进行淘汰
        if (cache.size() >= capacity) {
            LinkedHashSet<Node> minFreqNodes = freqMap.get(minFreq);
            Node nodeToRemove = minFreqNodes.iterator().next();
            minFreqNodes.remove(nodeToRemove);
            cache.remove(nodeToRemove.key);
        }
        
        // 添加新节点
        Node newNode = new Node(key, value, 1);
        cache.put(key, newNode);
        LinkedHashSet<Node> newFreqNodes = freqMap.getOrDefault(1, new LinkedHashSet<>());
        newFreqNodes.add(newNode);
        freqMap.put(1, newFreqNodes);
        minFreq = 1;
    }

    // 访问频率加1
    private void increaseFreq(Node node) {
        int oldFreq = node.freq;
        LinkedHashSet<Node> oldFreqNodes = freqMap.get(oldFreq);
        oldFreqNodes.remove(node);
        
        if (oldFreqNodes.isEmpty()) {
            freqMap.remove(oldFreq);
            if (oldFreq == minFreq) {
                minFreq++;
            }
        }
        
        node.freq++;
        LinkedHashSet<Node> newFreqNodes = freqMap.getOrDefault(node.freq, new LinkedHashSet<>());
        newFreqNodes.add(node);
        freqMap.put(node.freq, newFreqNodes);
    }
    
    // 节点类
    private static class Node {
        int key;
        int value;
        int freq;

        public Node(int key, int value, int freq) {
            this.key = key;
            this.value = value;
            this.freq = freq;
        }

        // 重写 hashCode 和 equals
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return key == node.key;
        }

        @Override
        public int hashCode() {
            return key;
        }
    }
}
```

### 🔍 四、真实面试高频问题 + 深度解析

**1. 什么是 LFU？它和 LRU 有什么区别？**

  * **标准答案：** LFU 是一种缓存淘汰策略，基于访问频率。LRU 基于最近访问时间。
  * **核心区别：**
      * **淘汰依据：** LFU 关注的是**历史总访问次数**，LRU 关注的是**最后一次访问时间**。
      * **适用场景：** LFU 更适合那些有稳定访问频率的场景，而 LRU 更适合访问模式呈局部性原理的场景。
      * **实现复杂度：** LFU 通常比 LRU 复杂得多。

**2. 为什么 LFU 算法比 LRU 更能应对突发性访问？**

  * **标准答案：** LRU 算法的缺点在于，一个很久没被访问过的数据，如果突然被访问了一次，就会被提到缓存的“最前面”，从而在短时间内获得“豁免权”，但它可能在之后再也不会被访问。
  * **深入：** LFU 算法则不同，它关注的是**长期访问频率**。一个数据的历史总访问次数不会因为一次突发访问而发生巨大变化。因此，LFU 能够更稳定地保留那些真正热门的核心数据。

**3. 如何高效地实现 LFU 算法？**

  * **标准答案：** 高效的 LFU 实现通常结合了**哈希表**和**频率列表**。
      * **哈希表**：用于 O(1) 查找节点。
      * **频率列表**：用 `Map<Integer, LinkedList<Node>>` 结构，将相同频率的节点放在一个双向链表中。这样在 `get` 时，只需 O(1) 就能找到旧链表，O(1) 移除节点，然后 O(1) 插入新链表。淘汰时，只需 O(1) 找到最小频率的链表，然后 O(1) 移除链表尾部节点。

**4. 为什么 LFU 算法的实现比 LRU 复杂？**

  * **标准答案：** 因为 LFU 的状态管理更复杂。LFU 不仅需要记录每个节点的访问次数，还需要一种高效的方式来组织这些节点，以便能快速找到并移除访问次数最少的节点。而 LRU 只需要一个双向链表就可以。

### 💡 五、口诀 + 表格/图示辅助记忆

**LFU 核心口诀**

> **哈希表，找节点。**
> **频率图，链表串。**
> **访问加一，挪个链。**
> **容量满，最小频，链尾除，新头添。**

**LRU vs LFU 对比表**

| 特性 | **LRU（最近最少使用）** | **LFU（最不常用）** |
| :--- | :--- | :--- |
| **淘汰依据** | 最后一次访问时间 | 访问频率（次数） |
| **实现难度** | 简单 | 复杂 |
| **经典实现** | `LinkedHashMap` 或 哈希表 + 双向链表 | 哈希表 + 频率列表 |
| **优点** | 实现简单，通用性强 | 应对突发访问，长期命中率高 |
| **缺点** | 无法处理突发访问 | 实现复杂，开销大 |

-----

### 🎁 六、建议 + 误区提醒

**误区提醒**

1.  **认为 LFU 性能一定比 LRU 差：** LFU 的 `get` 和 `put` 操作通常是 O(1)（或近似 O(1)），但其内部维护的复杂数据结构会带来额外的开销。在某些场景下，LFU 的高命中率可以弥补其实现开销。
2.  **混淆 LRU 和 LFU 的适用场景：** 如果你的数据访问模式是短期热点（比如短视频的热度榜），LRU 可能会更合适。如果你的数据访问模式是长期热点（比如热门新闻或商品），LFU 可能会更有优势。

**使用建议**

1.  **面试时，先从 LRU 讲起：** LRU 的实现相对简单，可以作为你理解缓存淘汰策略的起点。
2.  **手写 LFU 实现：** 能够手写 LFU 算法的面试者，通常能获得更高的评价。这证明你对数据结构和算法有深刻的理解。
3.  **了解变种：** 很多实际应用中的缓存算法都是 LRU 或 LFU 的变种，例如 `LRU-K`、`ARC` 等。了解这些变种能帮助你更好地理解缓存设计。