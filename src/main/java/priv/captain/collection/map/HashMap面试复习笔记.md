📘 Java 面试复习笔记：HashMap（JDK 8）

✅ 一、概念简介

- **它是什么**：`HashMap` 是基于哈希表的键值对集合，允许 `null` 键和 `null` 值，非线程安全，迭代顺序不保证稳定。
- **典型场景**：
  - 高性能键值缓存、索引（如通过 ID → 对象）
  - 频繁查询、插入、删除，且对顺序无要求
  - 作为上层数据结构的基础（如 `LinkedHashMap`、`ConcurrentHashMap` 实现思路）
- **特性/优缺点**：
  - **优点**：平均 O(1) 的 `get/put/remove`；空间换时间；操作简单
  - **缺点**：迭代顺序不稳定；极端冲突退化前（JDK 8 之前）为 O(n)；非线程安全
- **对比**：
  - **HashMap vs Hashtable**：后者同步、低效、过时；不支持 `null` 键/值；不推荐
  - **HashMap vs ConcurrentHashMap**：后者线程安全，JDK 8 采用 CAS + 分段/桶级别锁与红黑树；API 更复杂
  - **HashMap vs TreeMap**：TreeMap 基于红黑树，按键有序，`O(log n)`；HashMap 无序但均摊更快

🔍 二、底层原理 + 源码分析（JDK 8）

- **数据结构**：数组 + 链表 + 红黑树
  - 主体为 `Node<K,V>[] table`
  - 同桶元素少时用单向链表；当链表长度 ≥ 8 且容量 ≥ 64 时转为红黑树（`TREEIFY_THRESHOLD=8`，`MIN_TREEIFY_CAPACITY=64`）
  - 当树中节点过少（≤ 6）时，退化回链表（`UNTREEIFY_THRESHOLD=6`）

- **关键字段**：
  - `transient Node<K,V>[] table`：桶数组，长度始终为 2 的幂
  - `int size`：键值对数量
  - `int modCount`：结构修改次数，fail-fast 迭代器使用
  - `int threshold`：扩容阈值（= 容量 × 负载因子）
  - `final float loadFactor`：负载因子，默认 0.75

- **Hash 计算与索引**：
  - `hash = (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16)`（高位扰动，减小碰撞）
  - `index = (n - 1) & hash`（n 为数组长度，2 的幂可用位与快速取模）
  - 设计意图：容量为 2 的幂，能让低位高位共同参与索引，均匀分布

- **put 流程（简化）**：
  1) 若 `table` 为空则 `resize` 初始化
  2) 定位桶 `i = (n - 1) & hash`
  3) 桶为空：新建节点插入
  4) 桶非空：
     - 若首节点 `hash`、`key` 都相等 → 覆盖
     - 若是树节点 → 按树规则插入
     - 否则在链表尾部插入（JDK 8 采用尾插，避免重排）；若长度达到阈值则树化
  5) 若 `size` 超过 `threshold` → `resize`

- **resize 扩容**：
  - 新容量为旧容量的 2 倍，重新分配数组
  - 迁移节点：链表场景可利用 `(oldIndex)` 或 `(oldIndex + oldCap)` 两条链分拆，O(n)
  - 设计意图：2 倍扩容使再散列代价低（只看多出的最高位）

- **关键源码（精简版，行级注释）**：

```java
// 计算扰动后的 hash（JDK 8）
static final int hash(Object key) {
    int h;                                   // 临时变量存放原始 hashCode
    return (key == null) ? 0                 // null 键固定落在桶 0
        : (h = key.hashCode()) ^ (h >>> 16); // 高位右移 16 位异或，掺入低位，降低冲突
}

// put 的骨架（极简示意，省略并发/树化等细节）
public V put(K key, V value) {
    return putVal(hash(key), key, value, false, true); // 调用内部实现
}

final V putVal(int hash, K key, V value, boolean onlyIfAbsent, boolean evict) {
    Node<K,V>[] tab; Node<K,V> p; int n, i;
    if ((tab = table) == null || (n = tab.length) == 0)
        n = (tab = resize()).length;                   // 延迟初始化或扩容
    if ((p = tab[i = (n - 1) & hash]) == null)
        tab[i] = newNode(hash, key, value, null);      // 桶为空，直接放首节点
    else {
        Node<K,V> e; K k;
        if (p.hash == hash &&                           // 首节点 key 命中
            ((k = p.key) == key || (key != null && key.equals(k))))
            e = p;                                      // 记录命中节点用于后续覆盖
        else if (p instanceof TreeNode)
            e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value); // 树插
        else {
            for (int binCount = 0; ; ++binCount) {      // 遍历链表
                if ((e = p.next) == null) {             // 到尾部
                    p.next = newNode(hash, key, value, null); // 尾插新节点
                    if (binCount >= TREEIFY_THRESHOLD - 1)
                        treeifyBin(tab, i);             // 长度达阈值树化
                    break;
                }
                if (e.hash == hash &&                   // 链上命中则停止
                    ((k = e.key) == key || (key != null && key.equals(k))))
                    break;
                p = e;                                  // 继续向后
            }
        }
        if (e != null) {                                // 发生命中场景：覆盖旧值
            V oldValue = e.value;
            if (!onlyIfAbsent || oldValue == null)
                e.value = value;
            return oldValue;
        }
    }
    if (++size > threshold) resize();                   // 超阈值则扩容
    ++modCount;                                         // 结构修改计数（fail-fast）
    return null;                                        // 新增返回 null
}

// 扩容（要点：容量翻倍，节点按高位新旧两链分拆）
final Node<K,V>[] resize() {
    Node<K,V>[] oldTab = table;                          // 旧表
    int oldCap = (oldTab == null) ? 0 : oldTab.length;   // 旧容量
    int newCap = oldCap > 0 ? oldCap << 1 : 16;          // 首次 16，否则翻倍
    @SuppressWarnings("unchecked")
    Node<K,V>[] newTab = (Node<K,V>[]) new Node[newCap]; // 新表分配
    table = newTab;                                      // 赋值
    threshold = (int)(newCap * loadFactor);              // 新阈值
    if (oldTab != null) {
        for (Node<K,V> e : oldTab) {                     // 遍历旧桶
            while (e != null) {
                Node<K,V> next = e.next;                 // 保存后继
                int idx = (newCap - 1) & e.hash;         // 重新计算索引
                e.next = newTab[idx];                    // 头插到新桶
                newTab[idx] = e;                         // 迁移
                e = next;                                // 继续
            }
        }
    }
    return newTab;                                       // 返回新表
}
```

- **设计与优化点**：
  - 容量为 2 的幂 + 高位扰动 + 位与索引，简化并加速取模
  - 尾插避免并发下链表逆序问题（仍然不线程安全）
  - 树化在高冲突时将查找/插入复杂度从 O(n) 降为 O(log n)

 - 树插详解：TreeNode.putTreeVal（简化版 + 行级注释）

```java
// 同桶已是红黑树时，向该树插入新键值（若命中则返回命中节点）
final TreeNode<K,V> putTreeVal(HashMap<K,V> map, Node<K,V>[] tab,
                               int h, K k, V v) {
    TreeNode<K,V> root = root();                    // 找到该桶树的根节点（沿 parent 向上）
    TreeNode<K,V> p = root;                         // 从根开始查找插入位置
    int dir = 0;                                    // 记录向左(-1)/向右(+1)方向
    K key = k;                                      // 目标键的局部引用
    Comparator<? super K> kc = (map.comparator);    // 若提供比较器则用比较器（HashMap 默认为 null）
    while (p != null) {                             // 二叉查找树查找
        int ph = p.hash;                            // 当前节点 hash
        K pk = p.key;                               // 当前节点 key
        if (ph > h) dir = -1;                       // 目标 hash 更小 → 左
        else if (ph < h) dir = 1;                   // 目标 hash 更大 → 右
        else if (Objects.equals(pk, key))           // hash 相等且 equals 命中 → 返回命中节点
            return p;
        else if (kc != null)                        // 有比较器则用比较器比较 key
            dir = kc.compare(key, pk);
        else {                                      // 无比较器时，尝试用 Class 名称 + System.identityHashCode 打破平衡
            int tie = tieBreakOrder(key, pk);
            dir = tie <= 0 ? -1 : 1;                // 保持决策稳定，避免退化
        }
        TreeNode<K,V> xp = p;                       // 记录父
        p = (dir <= 0) ? p.left : p.right;          // 向左/右继续
        if (p == null) {                            // 找到空位，执行插入
            TreeNode<K,V> x = new TreeNode<>(h, k, v, null); // 新红节点（红黑树插入为红色）
            if (dir <= 0) xp.left = x; else xp.right = x;    // 挂到父的左/右
            x.parent = xp;                                  // 维护父引用
            moveRootToFront(tab, balanceInsertion(root, x)); // 红黑树平衡并把根移到桶首
            return null;                                    // 插入成功，返回 null 表示无命中
        }
    }
    return null;                                            // 不应到达（兜底）
}
```

- `treeifyBin` 概要（将链表桶转换为树或选择扩容）：

```java
final void treeifyBin(Node<K,V>[] tab, int index) {
    Node<K,V> first = tab[index];                       // 桶首节点（链表头）
    if (first == null) return;                          // 空桶直接返回
    if (tab.length < MIN_TREEIFY_CAPACITY) {            // 容量不足优先扩容
        resize();                                       // 扩容后可能自然分流，冲突下降
        return;
    }
    // 链表 → 树节点双向链，并最终构造红黑树
    TreeNode<K,V> hd = null, tl = null;                 // 头/尾指针（双向链）
    for (Node<K,V> e = first; e != null; e = e.next) {
        TreeNode<K,V> p = new TreeNode<>(e.hash, e.key, e.value, null);
        if ((p.prev = tl) == null) hd = p;              // 维护双向链表 prev/next
        else tl.next = p;
        tl = p;
    }
    tab[index] = hd;                                    // 桶首替换为树节点头
    // 构建红黑树（逐个插入 + 平衡）
    TreeNode<K,V> root = null;
    for (TreeNode<K,V> x = hd; x != null; x = x.next) {
        if (root == null) {                             // 第一个节点作为黑根
            x.red = false;                              // 根必为黑
            root = x;
        } else {
            // 以红黑树规则插入并平衡
            root = balanceInsertion(root, x);           // 旋转+染色维持性质
        }
    }
    moveRootToFront(tab, root);                         // 确保根在桶首，提高后续访问局部性
}
```

- 变量释义（以 `putVal` 为例）：
  - `tab`：当前使用的桶数组引用，来源于字段 `table`。`tab = table;` 若为空则 `resize()` 初始化。
  - `n`：`tab.length`，当前桶数组长度（始终为 2 的幂）。
  - `i`：当前键定位到的桶索引，`i = (n - 1) & hash`。
  - `p`：位于索引 `i` 的桶首节点（可能为 `null`/链表节点/树节点）。
  - `e`：查找过程中命中的节点引用，用于覆盖旧值或终止遍历。
  - `k`：临时保存被比较节点的 `key`，减少重复读取字段成本。
  - 其他：`binCount` 为计数当前桶链表长度，用于触发 `treeifyBin`；`onlyIfAbsent` 控制是否仅在不存在时写入。

✅ 三、常用方式 + 代码示例（含注释/易错点）

```java
// 1) 基本增删改查
Map<String, Integer> map = new HashMap<>();
map.put("A", 1);                           // 新增或覆盖（返回旧值或 null）
map.putIfAbsent("A", 2);                   // 仅在不存在时放入（线程不安全）
Integer v = map.get("A");                  // 读取
map.remove("A");                           // 按 key 移除
map.remove("B", 2);                        // key/value 同时匹配才移除

// 2) null 键/值
map.put(null, 0);                           // 允许 null 键
map.put("N", null);                         // 允许 null 值

// 3) 遍历（优先 entrySet，避免重复查找）
for (Map.Entry<String, Integer> e : map.entrySet()) {
    String k = e.getKey();                  // 取 key
    Integer val = e.getValue();             // 取 value
}

// 4) 计算型 API（JDK 8）
map.computeIfAbsent("key", k -> expensiveBuild(k)); // 不存在时才计算，函数可能较重
map.merge("k", 1, Integer::sum);                   // 若无则放1，有则累加

// 5) 容量估算（避免频繁扩容）
int expected = 10_000;
int initCap = (int) (expected / 0.75f) + 1; // 预估容量 = 预期大小/负载因子 + 1
Map<Long, Object> big = new HashMap<>(initCap);

// 6) 自定义 key：必须同时覆写 equals 与 hashCode，且二者一致
static final class UserKey {
    final String id;
    UserKey(String id) { this.id = id; }
    @Override public boolean equals(Object o) {
        if (this == o) return true;                 // 同一引用
        if (!(o instanceof UserKey)) return false;  // 类型检查
        return Objects.equals(id, ((UserKey)o).id); // 业务等价
    }
    @Override public int hashCode() {
        return Objects.hash(id);                    // 与 equals 一致
    }
}

// 7) 迭代期间修改：使用迭代器的 remove，避免 ConcurrentModificationException
Iterator<Map.Entry<String,Integer>> it = map.entrySet().iterator();
while (it.hasNext()) {
    if (shouldRemove(it.next())) {
        it.remove();                                // 正确做法
    }
}
// map.remove(key) 在外部直接调用会触发 fail-fast 抛 CME
```

- **线程安全提示**：
  - `HashMap` 非线程安全；多线程读写请使用 `ConcurrentHashMap` 或外部同步
  - 迭代器为 fail-fast：并发结构修改将抛 `ConcurrentModificationException`

🎯 四、真实面试高频问题 + 深度解析

1) 题目：为什么容量必须是 2 的幂？
- 标准答案：便于用位与 `(n-1)&hash` 取模，分布更均匀、计算更快。
- 详细解析：当 n 为 2 的幂时，低位掩码覆盖所有位；配合高位扰动使桶分布更均匀；非 2 的幂将导致偏斜。
- 陷阱警告：误答“随便取模就行”，忽略性能与分布性。

2) 题目：负载因子为什么默认 0.75？
- 标准答案：在时间与空间之间折中，经验值使冲突率较低且空间利用率较高。
- 详细解析：过小浪费空间、过大冲突上升；0.75 在多数场景下表现良好。
- 陷阱警告：盲目改为 1.0 或 0.5 而不做评估。

3) 题目：JDK 7 与 JDK 8 的差异？
- 标准答案：JDK 8 引入红黑树化，链表尾插；JDK 7 头插且无树化。
- 详细解析：JDK 7 在并发 rehash 可能形成循环链；JDK 8 尾插减少顺序反转风险，但仍非线程安全。
- 陷阱警告：声称“JDK 8 已线程安全”或“不会出现并发问题”。

4) 题目：Hash 冲突如何解决？
- 标准答案：先链表，达到阈值树化为红黑树，极端情况下保持 O(log n)。
- 详细解析：插入时若同桶冲突则挂链；`>=8` 且容量 `>=64` 树化；删除或缩小后可退化。
- 陷阱警告：忽视 `MIN_TREEIFY_CAPACITY` 导致误解“链表一到 8 就树化”。

5) 题目：`resize` 发生在什么时候？
- 标准答案：`size > threshold` 时触发；threshold = 容量 × 负载因子。
- 详细解析：扩容为 2 倍；迁移时根据新增最高位将链一分为二，代价 O(n)。
- 陷阱警告：误把 threshold 理解为“桶中链表长度阈值”。

6) 题目：为什么迭代时结构修改会抛 `ConcurrentModificationException`？
- 标准答案：fail-fast 机制通过 `modCount` 监测结构变化，发现不一致立即抛异常。
- 详细解析：保证迭代器弱一致性与简单实现；非并发设计，不保证迭代期可见性正确。
- 陷阱警告：以为是“线程安全保护”。

7) 题目：`equals` 与 `hashCode` 的约定？
- 标准答案：若 `equals` 相等则 `hashCode` 必须相等；反之不要求。
- 详细解析：违背约定会导致查找失败、重复键、或散列退化。
- 陷阱警告：只重写了一个方法；或 key 可变、修改后作为键导致不可见。

8) 题目：`computeIfAbsent` 有哪些注意点？
- 标准答案：当 key 不存在或映射为 null 才执行函数；函数可能被调用一次，且若函数返回 null 将不放入条目。
- 详细解析：函数可能代价较高且可能抛异常，应保证幂等与无副作用；在并发 Map 中还有可见性与锁粒度考量（此处 HashMap 非线程安全）。
- 陷阱警告：在高频路径中放入重函数；误以为“存在也会覆盖”。

9) 题目：为何要有 `TREEIFY_THRESHOLD=8` 与 `UNTREEIFY_THRESHOLD=6`？
- 标准答案：设定滞回阈值避免频繁树化/退化抖动。
- 详细解析：8/6 的经验阈值兼顾空间与时间；小 Map 直接扩容更划算。
- 陷阱警告：把阈值当常量规律背诵而不理解滞回设计。

10) 题目：如何选择初始容量？
- 标准答案：按 `expectedSize / loadFactor` 上取整，避免多次扩容。
- 详细解析：如期望 10w 条，0.75 负载因子 → 约 133,334 初始容量。
- 陷阱警告：新建默认容量在高写入场景反复 resize。

💡 五、口诀 + 对比表

- **口诀**：
  - ✅ 二次幂取模快，扰动均匀撒；链表八成树，阈值六回拉。
  - ✅ 读写均摊快，线程别乱来；键等哈相配，修改要警戒。

- **对比表**：

| 集合 | 底层结构 | 顺序性 | 线程安全 | 允许 null | 典型复杂度 | 适用场景 |
| --- | --- | --- | --- | --- | --- | --- |
| HashMap | 数组+链表+红黑树 | 无序 | 否 | 是 | 均摊 O(1) | 通用 KV 存储 |
| LinkedHashMap | HashMap + 双向链表 | 插入序/访问序 | 否 | 是 | 均摊 O(1) | LRU/LFU（配合 accessOrder） |
| ConcurrentHashMap | 分桶+CAS+红黑树 | 无序 | 是 | 否(null 不允许) | 均摊 O(1) | 并发场景 |
| Hashtable | 同步 Hash 表 | 近似无序 | 是 | 否 | 慢 | 兼容历史，不推荐 |
| TreeMap | 红黑树 | 键有序 | 否 | 否 | O(log n) | 需要有序视图 |

🎁 六、建议 + 误区提醒（实战）

- 选择初始容量与负载因子：写多场景预估容量，减少扩容；读多写少可适当提高负载因子
- 键对象不可变：作为键放入后不要修改其参与 `equals/hashCode` 的字段
- 迭代修改使用 `Iterator.remove`：避免 `ConcurrentModificationException`
- 高冲突风险降噪：优质 `hashCode`、避免热点键；必要时使用 `LinkedHashMap` 有序特性或 `TreeMap`
- 并发下改用 `ConcurrentHashMap`：不要用 `Collections.synchronizedMap` 粗锁在高并发下性能差
- 大 Map 清理：及时 `clear()` 或置换新实例帮助 GC；热点数据可考虑 `LinkedHashMap` + 访问顺序做 LRU
- 版本差异关注：JDK 8 树化/尾插；JDK 7 头插且可能出现并发 rehash 环形链问题（非线程安全）

—— 完 ——


