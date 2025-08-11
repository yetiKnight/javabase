📘 Java 面试复习笔记：ConcurrentHashMap（JDK 8）

✅ 一、概念简介

- **它是什么**：`ConcurrentHashMap`（CHM）是线程安全的哈希映射容器，支持高并发读写，迭代为弱一致（weakly consistent），不抛 `ConcurrentModificationException`。
- **典型场景**：
  - 多线程共享配置/缓存/注册表（读多写多）
  - 统计累加、频繁 `computeIfAbsent` 的并发场景
  - 需要 Map 视图（`keySet`/`values`/`entrySet`）的并发遍历
- **特性/优缺点**：
  - **优点**：读操作无锁（volatile 读 + Unsafe 原子读）、写操作局部化锁粒度（桶级锁/树锁），支持并发扩容
  - **缺点**：不允许 `null` 键/值；部分复合操作仍可能需要外部控制；在极端冲突下仍有锁竞争
- **对比**：
  - **CHM vs HashMap**：前者线程安全、弱一致迭代；后者非线程安全
  - **CHM vs Hashtable**：后者粗粒度全表锁，吞吐差；CHM 细粒度 + 无锁读
  - **CHM vs Collections.synchronizedMap**：同步 Map 仍是粗锁，迭代需额外同步

🔍 二、底层原理 + 源码分析（JDK 8）

- **数据结构**：数组 + 链表 + 红黑树（与 HashMap 类似，但实现细节不同）
  - 主体：`Node<K,V>[] table`，元素为 `Node`、`TreeNode` 或迁移标记 `ForwardingNode`
  - 冲突处理：链表；链长≥8 且容量≥64 时树化（`TREEIFY_THRESHOLD=8`，`MIN_TREEIFY_CAPACITY=64`）

- **并发控制策略**：
  - 读：无锁；通过 `volatile` 与 `Unsafe` 的 `getObjectVolatile` 保证可见性
  - 写：
    - 桶为空：CAS 方式插入首节点
    - 桶非空：对桶首节点或 `TreeBin` 加 `synchronized` 锁，局部化写竞争
  - 扩容：多线程协作扩容（ForwardingNode + `helpTransfer`），避免单线程阻塞

- **关键字段**：
  - `transient volatile Node<K,V>[] table`：主数组，延迟初始化
  - `transient volatile Node<K,V>[] nextTable`：扩容时的新数组
  - `transient volatile int sizeCtl`：控制标志/阈值
    - `> 0`：下次扩容阈值（容量×负载因子）
    - `-1`：表示正在初始化
    - `< -1`：扩容进行中，低位存放参与迁移线程计数
  - `transient volatile int transferIndex`：扩容任务分配指针（从高到低切分区块）

- **哈希与索引**：
  - `h = spread(key.hashCode())`：与 HashMap 类似的高位扰动，减少冲突
  - `i = (n - 1) & h`：2 的幂取模

- **核心方法（精简示意 + 行级注释）**：

```java
// 扰动函数：高位参与，屏蔽符号位（HASH_BITS = 0x7fffffff）
static final int spread(int h) {
    return (h ^ (h >>> 16)) & 0x7fffffff;           // 让高位参与低位索引计算
}

// 读取：无锁，弱一致
public V get(Object key) {
    Node<K,V>[] tab = table;                         // 读 volatile table 引用
    int n, h = spread(key.hashCode());               // 计算扰动哈希
    Node<K,V> e = (tab == null || (n = tab.length) == 0) ?
                  null : tabAt(tab, (n - 1) & h);   // 原子读取桶首节点
    if (e == null) return null;                      // 桶为空
    if (e.hash == h) {                               // 桶首命中
        K k = e.key;                                 // 读取 key
        if (k == key || (k != null && k.equals(key)))
            return e.val;                            // 命中返回值
    }
    if (e.hash < 0)                                  // 负值：可能是 ForwardingNode 或 TreeBin
        return findInTreeOrForward(e, h, key);       // 在树或新表继续查找
    for (Node<K,V> p = e.next; p != null; p = p.next) { // 链表遍历
        if (p.hash == h) {
            K k = p.key;                             // 对比 key
            if (k == key || (k != null && k.equals(key)))
                return p.val;                        // 命中返回
        }
    }
    return null;                                     // 未找到
}

// 写入：CAS 尝试占位，冲突时锁桶
final V putVal(K key, V value, boolean onlyIfAbsent) {
    int h = spread(key.hashCode());                  // 计算哈希
    for (;;) {                                       // 外层自旋，处理并发迁移等
        Node<K,V>[] tab = table;                     // 读 table
        int n;
        if (tab == null || (n = tab.length) == 0)   // 延迟初始化
            tab = initTable();                       // 可能与他人竞争，由 sizeCtl 控制
        int i = (n - 1) & h;                         // 定位桶
        Node<K,V> f = tabAt(tab, i);                 // 原子读桶首
        if (f == null) {                             // 桶空：CAS 放首节点
            if (casTabAt(tab, i, null, new Node<K,V>(h, key, value)))
                break;                               // 成功，占位完成
            else continue;                           // 失败：重试
        }
        if (f.hash == MOVED) {                       // 正在扩容：协助迁移
            helpTransfer(tab, f);
            continue;                                // 迁移后重试
        }
        V oldVal = null;                             // 记录旧值
        synchronized (f) {                            // 锁住桶首（链/树头）
            if (tabAt(tab, i) == f) {                // 再校验：仍是当前头
                if (f.hash >= 0) {                    // 链表插入/覆盖
                    int binCount = 1;                // 桶内计数
                    for (Node<K,V> e = f;; ++binCount) {
                        if (e.hash == h &&            // 命中同 key
                            (e.key == key || (key != null && key.equals(e.key)))) {
                            oldVal = e.val;          // 记录旧值
                            if (!onlyIfAbsent) e.val = value; // 覆盖（非仅在不存在时）
                            break;
                        }
                        Node<K,V> next = e.next;     // 继续向后
                        if (next == null) {          // 到尾部
                            e.next = new Node<K,V>(h, key, value); // 尾插
                            if (binCount >= TREEIFY_THRESHOLD)
                                treeifyBin(tab, i);  // 树化
                            break;
                        }
                        e = next;                    // 前进
                    }
                } else if (f instanceof TreeBin) {   // 树插入/覆盖
                    Node<K,V> p = ((TreeBin<K,V>)f).putTreeVal(h, key, value);
                    if (p != null) {                 // 命中旧节点
                        oldVal = ((TreeNode<K,V>)p).val;
                        if (!onlyIfAbsent)
                            ((TreeNode<K,V>)p).val = value; // 覆盖
                    }
                }
            }
        }
        if (oldVal != null) return oldVal;           // 覆盖返回旧值
        break;                                       // 新增成功
    }
    addCount(1L, true);                               // 维护计数并检查扩容
    return null;                                      // 新增返回 null
}

// 初始化 table：基于 sizeCtl 的原子状态机
private final Node<K,V>[] initTable() {
    for (;;) {
        Node<K,V>[] tab = table;                      // 读 table
        if (tab != null && tab.length > 0)            // 已初始化直接返回
            return tab;
        int sc = sizeCtl;                             // 读 sizeCtl
        if (sc < 0) Thread.yield();                   // 其他线程正在 init/transfer，让步
        else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) { // CAS 占位
            try {
                if (table == null || table.length == 0) {
                    int n = (sc > 0) ? sc : 16;       // 初始容量，默认 16
                    @SuppressWarnings("unchecked")
                    Node<K,V>[] nt = (Node<K,V>[]) new Node[n];
                    table = nt;                       // 赋值
                    sc = n - (n >>> 2);               // 阈值 = 0.75n
                }
            } finally {
                sizeCtl = sc;                         // 释放占位
            }
            return table;                             // 返回
        }
    }
}

// 计数与扩容触发：多线程协作迁移
private final void addCount(long x, boolean check) {
    long s = sumCount() + x;                          // LongAdder 风格累加（省略细节）
    if (check && s >= sizeCtl)                        // 达阈值触发扩容
        tryPresize((int)s);                           // 尝试扩容（含并发迁移协调）
}

// 迁移：ForwardingNode 标记 + 多线程分片搬运
private final void transfer(Node<K,V>[] tab, Node<K,V>[] nextTab) {
    int n = tab.length;                               // 旧容量
    int stride = Math.max(1, (n >>> 3) / NCPU);       // 分片步长，按核数切片
    for (int i = n - 1; i >= 0; i -= stride) {        // 从高位向低位抢占区间
        Node<K,V> f = tabAt(tab, i);                  // 读桶
        if (f == null) {                              // 空桶
            if (casTabAt(tab, i, null, new ForwardingNode<>(nextTab)))
                continue;                             // 放置前导标记
        } else if (f.hash == MOVED) {
            // 已迁移，跳过
        } else {
            synchronized (f) {                         // 锁桶，安全搬运
                if (tabAt(tab, i) != f) continue;      // 再校验
                // 按 (idx) 与 (idx + oldCap) 划分低/高两链
                Node<K,V> lo = null, hi = null;         // 低位链/高位链
                for (Node<K,V> e = f; e != null; e = e.next) {
                    if ((e.hash & n) == 0) {           // 最高位为 0 → 低位链
                        e.next = lo; lo = e;
                    } else {                            // 最高位为 1 → 高位链
                        e.next = hi; hi = e;
                    }
                }
                setTabAt(nextTab, i, lo);               // 放入新表低位桶
                setTabAt(nextTab, i + n, hi);           // 放入新表高位桶
                setTabAt(tab, i, new ForwardingNode<>(nextTab)); // 标记已迁移
            }
        }
    }
}
```

- **设计与优化点**：
  - 读路径无锁：降低竞争，迭代弱一致
  - 写路径局部化锁：仅锁定桶首（或 `TreeBin`），缩小锁粒度
  - ForwardingNode + 多线程迁移：扩容期间读操作可沿标记跳转到 `nextTable`
  - `sizeCtl` 状态机：统一控制初始化、阈值与扩容协作
  - 树化/退化阈值与 HashMap 一致，兼顾性能与空间

✅ 三、常用方式 + 代码示例（含注释/易错点）

```java
// 1) 基本用法
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
map.put("A", 1);                          // 线程安全的写
map.putIfAbsent("A", 2);                  // 原子：不存在时才放入
map.replace("A", 1, 3);                   // 原子：匹配旧值才替换
Integer v = map.get("A");                 // 无锁读

// 2) 计算型 API（原子语义按 key 粒度）
map.computeIfAbsent("key", k -> createExpensive(k)); // 同一 key 上串行执行
map.compute("cnt", (k, oldVal) -> oldVal == null ? 1 : oldVal + 1); // 原子更新
map.merge("k", 1, Integer::sum);          // 无需先 get 再 put 的竞态窗口

// 3) 并发遍历（弱一致，不抛 CME）
for (Map.Entry<String,Integer> e : map.entrySet()) {
    // 遍历过程中，其他线程的插入/删除可能可见，也可能不可见
}

// 4) 初始化容量（建议预估，减少扩容）
int expected = 50_000;                     // 预估条目数
int initCap = (int)(expected / 0.75f) + 1; // 近似 HashMap 经验
ConcurrentHashMap<Long, Object> big = new ConcurrentHashMap<>(initCap);

// 5) 计数热点：LongAdder 更适合高并发累加
LongAdder adder = new LongAdder();
map.compute("hits", (k, v0) -> { adder.increment(); return v0; });
// 或使用内置 Map 结构存储 LongAdder，减少竞争
```

- **线程安全与异常**：
  - 不允许 `null` 键/值（避免歧义：`get` 返回 `null` 表示“无映射”还是“映射为 null”）
  - 迭代弱一致，不抛 `ConcurrentModificationException`
  - 计算型函数应避免抛异常与副作用，确保幂等

🎯 四、真实面试高频问题 + 深度解析

1) 题目：JDK 7 与 JDK 8 的结构差异？
- 标准答案：JDK 7 采用 `Segment` 分段锁；JDK 8 移除段，采用桶级锁 + CAS + 红黑树 + 并发扩容。
- 详细解析：段锁降低竞争但增加层级与内存；JDK 8 通过细粒度锁和原子操作提升吞吐与空间效率。
- 陷阱警告：误答“JDK 8 仍然是 Segment”。

2) 题目：为什么不允许 `null` 键/值？
- 标准答案：避免并发下 `get` 返回 `null` 的语义歧义，简化 API 与实现。
- 详细解析：`HashMap` 允许 null，但 CHM 在并发读写与计算型 API 下需区分“无映射”与“值为 null”。
- 陷阱警告：以为是“技术限制”，忽略语义选择。

3) 题目：`get` 为什么可以无锁？
- 标准答案：读取依赖 `volatile` 可见性与 `Unsafe` 的原子读；写操作保证 `happens-before`。
- 详细解析：节点指针、值字段为 `volatile`；插入/覆盖在发布后对并发读可见；扩容有 ForwardingNode 跳转。
- 陷阱警告：以为所有操作都“无锁”。

4) 题目：扩容如何并发进行？
- 标准答案：使用 `ForwardingNode` 标记已迁移桶，多线程基于 `transferIndex` 分片搬运，读操作可沿标记访问新表。
- 详细解析：`sizeCtl` 控制状态；参与迁移线程数编码在负值中；搬运按最高位拆分低/高链，O(n)。
- 陷阱警告：以为扩容期间读写会阻塞整个表。

5) 题目：链表什么时候树化？
- 标准答案：链长≥8 且表容量≥64 时树化；节点减少到 ≤6 退化为链表。
- 详细解析：小容量优先扩容而非树化，避免树结构开销；与 HashMap 阈值一致。
- 陷阱警告：忽略 `MIN_TREEIFY_CAPACITY`，误认为“到 8 就树化”。

6) 题目：`computeIfAbsent` 是否会重复计算？
- 标准答案：对同一 key 原子串行，函数最多被调用一次；高并发下其他线程会等待该桶锁。
- 详细解析：在桶级锁内执行映射函数；若函数抛异常，键映射不会被建立。
- 陷阱警告：在函数中执行长阻塞/IO，导致同键竞争严重。

7) 题目：为什么迭代是弱一致？
- 标准答案：遍历时允许看到结构的某些更新，避免全表锁或快照开销。
- 详细解析：基于 `volatile` 可见性，遍历期间的插入/删除可能反映在结果中，但不保证完整视图。
- 陷阱警告：要求“强一致”而忽略并发代价。

8) 题目：如何合理设置初始容量？
- 标准答案：按 `expectedSize / 负载因子` 估算；高写入场景预分配以降低扩容开销。
- 详细解析：默认负载因子约 0.75；并发扩容虽协作但仍昂贵。
- 陷阱警告：无脑默认容量导致热启动期抖动。

9) 题目：与 `Collections.synchronizedMap` 相比优势在哪？
- 标准答案：细粒度锁 + 无锁读，吞吐与可伸缩性显著更好；迭代弱一致且无需手工外部同步。
- 详细解析：同步 Map 需要在迭代期手动 `synchronized(map)`，否则不安全。
- 陷阱警告：认为“加个同步包装等价”。

10) 题目：树化后写入是否仍可能退化？
- 标准答案：删除导致节点数减少到阈值以下可退化回链表；写入仍在桶级锁下进行。
- 详细解析：采用 `TreeBin` 管理红黑树，并在冲突下降后释放树结构开销。
- 陷阱警告：认为“一旦树化就永久保持”。

💡 五、口诀 + 对比表/图示

- **口诀**：
  - ✅ 读路不加锁，写锁扣桶头；标记引路迁，协作把表扩。
  - ✅ 八树六回链，阈值看容量；空桶用 CAS，弱保可见性。

- **对比表**：

| 集合 | 底层结构 | 并发策略 | 允许 null | 迭代一致性 | 典型复杂度 | 适用场景 |
| --- | --- | --- | --- | --- | --- | --- |
| HashMap | 数组+链表+树 | 无并发 | 是 | fail-fast | 均摊 O(1) | 单线程/外部同步 |
| ConcurrentHashMap | 数组+链表+树 | 桶级锁 + CAS + 并发扩容 | 否 | 弱一致 | 均摊 O(1) | 高并发通用 |
| Hashtable | 数组+链表 | 全表锁 | 否 | 强一致（但吞吐低） | O(1) | 兼容历史，不推荐 |
| SynchronizedMap | 委托 HashMap | 外部单锁 | 是 | 需手动同步 | O(1) | 低并发场景 |

🎁 六、建议 + 误区提醒（实战）

- 避免长时间在计算型函数内阻塞（如 IO/RPC），会阻塞同 key 的并发
- 高并发计数使用 `LongAdder` 或 `ConcurrentHashMap<String, LongAdder>` 模式
- 不要依赖迭代获得强一致视图；需要快照考虑 `copyOnWrite` 或批量复制
- 预估初始容量，减少扩容；热点 key 尽量均衡哈希，避免树化与锁竞争
- 严禁存入 `null`；`get` 返回 `null` 一律表示“未映射”
- 读多写少场景，避免不必要的结构性写操作（如频繁 `compute`）

—— 完 ——


