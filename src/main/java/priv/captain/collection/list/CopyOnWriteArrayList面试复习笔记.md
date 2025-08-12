📘 Java 面试复习笔记：CopyOnWriteArrayList

> 适配读多写少的并发场景，快照迭代、写时复制；JDK 8 为主线讲解（JDK 11/17 行为一致）。

---

✅ 一、概念简介

- 定义：`CopyOnWriteArrayList` 是基于数组的线程安全 `List`，位于 `java.util.concurrent`，写操作采用“写时复制（Copy-On-Write）”策略。
- 适用场景：读多写少、元素规模中小型（如监听器列表、配置信息、白名单、菜单缓存）。
- 特性与优缺点：
  - ✅ 读无锁，读操作直接访问 `volatile` 数组快照，延迟低。
  - ✅ 迭代器弱一致（weakly consistent），遍历期间允许并发写，且不会 `ConcurrentModificationException`。
  - ❌ 写需要加锁并整体复制数组（O(n) + 产生临时数组），频繁写或大集合下内存与时间成本高。
  - ❌ 迭代器不可反映遍历期间的写入（快照语义），存在“读到旧数据”的一致性特征。
- 对比：
  - 与 `ArrayList`：后者非线程安全、读写都基于同一数组；前者线程安全，读快、写贵。
  - 与 `Vector` / `Collections.synchronizedList`：后两者采用整体锁，读也阻塞；`CopyOnWriteArrayList` 读无锁，读吞吐更高。
  - 与 `ConcurrentLinkedQueue`：后者链表无界队列，适合生产消费；`CopyOnWriteArrayList` 适合随机访问与快照遍历。

---

🔍 二、底层原理 + 源码分析（JDK 8）

- 核心数据结构与字段：

```java
// 位于 java.util.concurrent.CopyOnWriteArrayList
// 关键字段
final transient ReentrantLock lock = new ReentrantLock(); // 写操作的独占锁
private transient volatile Object[] array;                // 持有数据的快照数组，volatile 保证可见性

final Object[] getArray() {           // 读当前快照（读无锁）
    return array;
}

final void setArray(Object[] a) {     // 切换新快照（写时调用）
    array = a;
}
```

- 写时复制的 `add(E e)`：

```java
public boolean add(E e) {                 // 向列表尾部添加元素
    final ReentrantLock lock = this.lock; // 取得独占锁引用
    lock.lock();                          // 加锁：写操作互斥
    try {                                 // 进入临界区
        Object[] elements = getArray();   // 读取当前快照
        int len = elements.length;        // 记录当前长度
        Object[] newElements =            // 分配新数组，长度 +1
            Arrays.copyOf(elements, len + 1);
        newElements[len] = e;             // 将新元素追加到末尾
        setArray(newElements);            // 切换可见快照（volatile 写）
        return true;                      // 返回成功
    } finally {
        lock.unlock();                    // 释放锁，保证异常下也解锁
    }
}
```

- 定位访问的 `get(int index)`：

```java
@SuppressWarnings("unchecked")
public E get(int index) {                 // 基于数组的 O(1) 随机访问
    return (E) getArray()[index];         // 直接读取当前快照
}
```

- 替换元素的 `set(int index, E element)`：

```java
@SuppressWarnings("unchecked")
public E set(int index, E element) {         // 设置指定下标的新值
    final ReentrantLock lock = this.lock;    // 写操作必须加锁
    lock.lock();                             // 加锁
    try {
        Object[] elements = getArray();      // 当前快照
        E oldValue = (E) elements[index];    // 读取旧值
        if (oldValue != element) {           // 仅在值不同才复制
            Object[] newElements =           // 复制整个数组
                Arrays.copyOf(elements, elements.length);
            newElements[index] = element;    // 替换指定位置
            setArray(newElements);           // 切换新快照
        } else {                             // 相同值时
            setArray(elements);              // 仍进行 set（保持语义一致）
        }
        return oldValue;                     // 返回旧值
    } finally {
        lock.unlock();                       // 释放锁
    }
}
```

- 删除元素的 `remove(Object o)`（核心思路）：

```java
public boolean remove(Object o) {               // 按值删除首个匹配元素
    final ReentrantLock lock = this.lock;       // 写操作加锁
    lock.lock();                                // 加锁
    try {
        Object[] elements = getArray();         // 获取快照
        int len = elements.length;              // 当前长度
        // 线性查找匹配位置（O(n)）
        int idx = indexOf(o, elements, 0, len); // 找到则返回下标，否则 -1
        if (idx < 0)                            // 未找到直接返回
            return false;
        Object[] newElements = new Object[len - 1]; // 创建新数组，长度 -1
        System.arraycopy(elements, 0, newElements, 0, idx);          // 拷贝左半段
        System.arraycopy(elements, idx + 1, newElements, idx, len - idx - 1); // 拷贝右半段
        setArray(newElements);                  // 切换新快照
        return true;                            // 删除成功
    } finally {
        lock.unlock();                          // 释放锁
    }
}
```

- 迭代器的快照与弱一致性：

```java
// 迭代器持有创建时的数组快照，遍历不受后续写入影响
static final class COWIterator<E> implements ListIterator<E> {
    private final Object[] snapshot;     // 捕获的快照数组
    private int cursor;                  // 遍历游标

    COWIterator(Object[] elements, int initialCursor) {
        this.snapshot = elements;        // 固定快照
        this.cursor = initialCursor;     // 起始位置
    }

    public boolean hasNext() {           // 是否有下一个
        return cursor < snapshot.length; // 基于快照长度判断
    }

    @SuppressWarnings("unchecked")
    public E next() {                    // 返回下一个元素
        if (!hasNext()) throw new NoSuchElementException();
        return (E) snapshot[cursor++];   // 读取快照，不受并发写影响
    }

    public void remove() {               // 不支持原地修改
        throw new UnsupportedOperationException();
    }
}
```

- 设计意图与优化点：
  - 通过 `volatile Object[] array` 实现读的可见性；读路径零加锁，极致优化读吞吐。
  - 所有写操作在 `ReentrantLock` 保护下进行，生成新数组再一次性“切换指针”，避免读写竞争。
  - 迭代器弱一致：遍历期间的写对当前迭代不可见，避免 `fail-fast` 与结构修改异常。

---

✅ 三、常用方式 + 代码示例（含注释）

```java
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class COWALDemo {
    public static void main(String[] args) {
        // 1) 创建：允许 null，维持插入顺序
        CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
        list.add("A");                 // 写：复制+追加
        list.add("B");                 // 写：复制+追加
        list.addIfAbsent("B");         // 仅在不存在时添加（线性检查+复制）

        // 2) 读：无锁、快照语义
        String first = list.get(0);     // O(1) 读取

        // 3) 遍历：弱一致，不会抛 ConcurrentModificationException
        Iterator<String> it = list.iterator(); // 捕获当前快照
        list.add("C");                 // 并发写：复制+切换快照（迭代器不可见）
        while (it.hasNext()) {
            System.out.println(it.next()); // 仅输出 A, B（看不到 C）
        }

        // 4) 原子批量：addAllAbsent(集合) —— 仅添加不存在的元素
        int added = list.addAllAbsent(List.of("B", "C", "D")); // 只会插入 D
        System.out.println("addAllAbsent added = " + added);

        // 5) 替换 / 删除
        list.set(1, "B2");             // 写：复制+替换
        list.remove("A");               // 写：复制+删除

        // 6) 并发读写示意（读快、写慢、写会复制）
        // 适合监听器/配置快照：读路径稳定、写偶发
    }
}
```

- 实战建议：
  - 数据量较大（> 10^5）或写频繁（> 10%）不推荐，内存和复制成本高。
  - 监听器、黑白名单、灰度规则等“读多写少且可读旧数据”非常合适。
  - 需要实时可见最新写入的数据迭代，请改用 `ConcurrentHashMap` + 快照视图，或基于读写锁的结构。

---

🎯 四、真实面试高频问题 + 深度解析

1) 题目：为什么 `CopyOnWriteArrayList` 的读不需要加锁？
- 标准答案：读直接访问 `volatile` 数组快照，具有可见性且与写分离。
- 详细解析：`array` 为 `volatile`，读到的是某一时刻的“不可变快照”；写在锁内复制数组，最后用 `volatile` 写切换指针，建立先行发生（happens-before）。
- 陷阱警告：读到的是旧数据是预期行为，不是“可见性问题”。

2) 题目：为何迭代器不会抛 `ConcurrentModificationException`？
- 标准答案：迭代器基于创建瞬间的快照，不感知后续结构修改。
- 详细解析：内部 `COWIterator` 保存 `snapshot` 数组引用，不读取最新 `array`；因此是弱一致而非 fail-fast。
- 陷阱警告：迭代器的 `remove()` 不支持，调用会抛 `UnsupportedOperationException`。

3) 题目：`CopyOnWriteArrayList` 写入的时间复杂度与影响？
- 标准答案：写为 O(n) 且需要整数组复制，写放大明显。
- 详细解析：`add/remove/set` 都会锁定并复制数组；写多场景会导致 CPU/GC 压力升高，吞吐下降。
- 陷阱警告：避免在高 QPS 写场景、或大数组场景使用。

4) 题目：与 `Collections.synchronizedList(new ArrayList<>())` 的区别？
- 标准答案：后者读写都需要锁，前者读无锁、写复制。
- 详细解析：同步列表在读多场景会被锁竞争拖慢；COW 列表可显著提升读吞吐，但以写放大为代价。
- 陷阱警告：不要误以为 COW 列表在任何并发场景都更快。

5) 题目：`addIfAbsent` 与 `add` 的差异？
- 标准答案：`addIfAbsent` 仅当元素不存在时才添加，避免重复。
- 详细解析：实现为加锁后在线性检查快照中查找，不存在则复制数组并追加；常用于监听器去重。
- 陷阱警告：该操作仍是 O(n) 检查 + 复制，不适合大数据集高频调用。

6) 题目：为什么它适合“监听器列表”这类场景？
- 标准答案：读占绝对多数、可容忍读取到旧快照、写不频繁。
- 详细解析：事件分发时遍历监听器需要稳定视图（遍历过程不受注册/注销影响），COW 天然提供快照一致性。
- 陷阱警告：如果监听器数量很大且频繁注册/注销，请改用其他数据结构。

7) 题目：是否支持 null 元素？
- 标准答案：支持。
- 详细解析：其等值比较对 null 做了兼容处理（如 `o == null ? e == null : o.equals(e)`），语义与一般 `List` 一致。
- 陷阱警告：业务主键或语义敏感集合中不推荐使用 null。

8) 题目：内存语义如何保证写对读可见？
- 标准答案：`ReentrantLock` 的释放与 `volatile` 写 `setArray` 共同建立 `happens-before`。
- 详细解析：写线程在锁内构建新数组后通过 `setArray`（volatile 写）发布；读线程读取 `array`（volatile 读）获取可见快照。
- 陷阱警告：不要在未发布前泄露正在构建的新数组引用。

---

💡 五、口诀 + 表格/图示辅助记忆

- 记忆口诀：
  - “读走旧照不加锁，写时复制要开销；迭代不炸弱一致，读多写少它称好。”

- 对比表：

| 维度/结构                       | ArrayList             | SynchronizedList       | CopyOnWriteArrayList        | ConcurrentLinkedQueue |
|--------------------------------|-----------------------|------------------------|------------------------------|----------------------|
| 线程安全                       | 否                    | 是（整体锁）           | 是（读无锁，写加锁）         | 是                   |
| 读性能                         | 高（单线程）          | 低-中（受锁影响）      | 高（快照，几乎不受写影响）   | 中                   |
| 写性能                         | 中（扩容时退化）      | 低（锁+原地修改）      | 低（锁+整复制）              | 高                   |
| 迭代一致性                     | fail-fast（单线程）    | fail-fast              | 弱一致（快照）               | 弱一致               |
| 适用场景                       | 单线程随机访问        | 少量并发，读写均衡      | 读多写少、需要快照遍历        | 高并发队列/生产消费 |

- 执行流程图（写时复制）：

```mermaid
graph TD
  W[线程请求写入] --> L{获取ReentrantLock}
  L -- 成功 --> R[读取旧数组快照]
  R --> C[复制新数组+应用修改]
  C --> S[setArray(volatile 写)切换快照]
  S --> U[释放锁]
  L -- 失败(等待) --> L
```

---

🎁 六、建议 + 误区提醒

- 建议：
  - 读多写少的配置、监听器、白名单等优先考虑；写多或大集合优先用 `ConcurrentHashMap`、分段结构或读写锁策略。
  - 合理控制元素规模，避免在热路径频繁 `add/remove`；可以批量构建后一次性替换引用。
  - 对一致性要求高（必须读到最新写入）的遍历，不要用它。
- 误区：
  - 误以为“线程安全 = 任意场景更快”：在写多场景反而更慢、且更耗内存。
  - 误以为“遍历能看见最新写入”：迭代是快照，天然看不到后续变化。
  - 忽视 `addIfAbsent/addAllAbsent` 的 O(n) 存在性检查与复制成本。

---

如需我把本笔记补充对比 `CopyOnWriteArraySet`、或加上 JMH 压测模板，告诉我即可。
