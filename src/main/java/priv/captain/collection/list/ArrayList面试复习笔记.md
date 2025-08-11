📘 Java 面试复习笔记：ArrayList（JDK 8 为主）

✅ 一、概念简介

- **它是什么**：`ArrayList` 是基于动态数组的 `List` 实现，支持随机访问、允许 `null`、可重复元素，非线程安全。
- **使用场景**：
  - 读多写少、随机访问频繁（配置表、索引表、快照数据）
  - 需要按下标定位或批量顺序遍历
  - 作为底层载体支撑其他结构（如 `Collections` 工具方法、排序等）
- **特性/优缺点**：
  - **优点**：随机访问 O(1)、内存连续、CPU 缓存友好、API 丰富
  - **缺点**：中间插入/删除 O(n)、扩容需整体复制、非线程安全
- **对比**：
  - **ArrayList vs LinkedList**：前者查快、增删慢；后者增删快（已知节点）但查慢，局部性差
  - **ArrayList vs Vector**：`Vector` 同步、性能差、过时；`ArrayList` 非同步

🔍 二、底层原理 + 源码分析（JDK 8）

- **数据结构与关键字段**：
  - `transient Object[] elementData`：底层数组（`transient` 避免序列化多余空位）
  - `int size`：实际元素个数
  - `int modCount`：结构修改计数（fail-fast）
  - 常量：`DEFAULT_CAPACITY=10`、`DEFAULTCAPACITY_EMPTY_ELEMENTDATA`（延迟分配的空数组）

- **容量与扩容策略**：
  - 延迟分配：无参构造得到 0 长度的共享空数组；首个元素加入时分配容量 10
  - 扩容因子：1.5 倍（`newCap = oldCap + (oldCap>>1)`），不足则取 `minCapacity`
  - 复制：`Arrays.copyOf`（底层 `System.arraycopy`）整体搬迁，代价 O(n)

- **核心方法（精简源码 + 行级注释）**：

```java
// add(E e)：尾部追加
public boolean add(E e) {
    ensureCapacityInternal(size + 1);         // 确保容量：可能触发首次分配或扩容
    elementData[size++] = e;                  // 直接写入尾部，并递增 size
    return true;                              // 始终返回 true
}

// ensureCapacityInternal：计算最小所需容量
private void ensureCapacityInternal(int minCapacity) {
    if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
        minCapacity = Math.max(DEFAULT_CAPACITY, minCapacity); // 首次至少 10
    }
    ensureExplicitCapacity(minCapacity);        // 进入显式容量检查
}

// ensureExplicitCapacity：决定是否 grow，并维护 modCount
private void ensureExplicitCapacity(int minCapacity) {
    modCount++;                                  // 结构性修改计数（fail-fast）
    if (minCapacity - elementData.length > 0)    // 不够就扩容
        grow(minCapacity);
}

// grow：1.5 倍扩容，保底到 minCapacity
private void grow(int minCapacity) {
    int oldCapacity = elementData.length;        // 旧容量
    int newCapacity = oldCapacity + (oldCapacity >> 1); // 1.5 倍
    if (newCapacity - minCapacity < 0)
        newCapacity = minCapacity;               // 仍不足则取需求值
    elementData = Arrays.copyOf(elementData, newCapacity); // 复制到新数组
}

// add(int index, E element)：插入（可能移动尾段）
public void add(int index, E element) {
    rangeCheckForAdd(index);                     // 校验 0..size
    ensureCapacityInternal(size + 1);            // 确保容量
    System.arraycopy(elementData, index,         // 将 [index..size-1] 整体右移一位
                     elementData, index + 1,
                     size - index);
    elementData[index] = element;                // 写入新元素
    size++;                                      // 更新 size
}

// remove(int index)：删除（移动尾段左移）
public E remove(int index) {
    rangeCheck(index);                           // 校验 0..size-1
    modCount++;                                  // 结构修改计数
    E oldValue = elementData(index);             // 取旧值
    int numMoved = size - index - 1;             // 需要左移的元素个数
    if (numMoved > 0)
        System.arraycopy(elementData, index + 1, // 将右侧整体左移一位
                         elementData, index,
                         numMoved);
    elementData[--size] = null;                  // 置空尾部，助于 GC
    return oldValue;                             // 返回删除的元素
}

// Itr 迭代器的 fail-fast（片段）
public boolean hasNext() {
    return cursor != size;                       // 光标未到末尾
}
public E next() {
    checkForComodification();                    // 校验 modCount 是否变化
    int i = cursor;                              // 读取当前游标
    if (i >= size) throw new NoSuchElementException();
    cursor = i + 1;                              // 前进
    return (E) elementData[lastRet = i];         // 返回元素并记录 lastRet
}
public void remove() {
    if (lastRet < 0) throw new IllegalStateException();
    checkForComodification();                    // 再次校验
    ArrayList.this.remove(lastRet);              // 调用外部 remove，触发 modCount++
    cursor = lastRet;                            // 重置游标位置
    lastRet = -1;                                // 清空 lastRet
    expectedModCount = modCount;                 // 同步期望计数
}
```

- **设计意图与优化**：
  - 延迟分配降低空列表开销；首个添加再分配 10 的默认容量
  - 1.5 倍扩容权衡“复制成本”与“扩容频率”；过小频繁扩容，过大浪费内存
  - `modCount` 提供 fail-fast，快速暴露并发错误用法
  - `elementData` 设为 `transient`，自定义序列化仅写 `size` 范围，减少体积

✅ 三、常用方式 + 代码示例（含注意点）

```java
// 1) 创建与容量管理
ArrayList<String> list = new ArrayList<>();           // 延迟分配，首次 add 才分配 10
ArrayList<Integer> capList = new ArrayList<>(1_000);  // 预估容量，减少扩容成本
capList.ensureCapacity(5_000);                        // 手动确保容量，批量插入前使用
capList.trimToSize();                                 // 回收多余空位（非必要勿频繁）

// 2) 基本操作
list.add("A");                                       // 尾部追加 O(1) 摊销
list.add(0, "B");                                    // 头部插入 O(n)（整体右移）
list.set(0, "C");                                    // 替换 O(1)
String first = list.get(0);                            // 随机访问 O(1)
list.remove(0);                                        // 删除 O(n)（整体左移）
list.remove("A");                                     // 删除首个匹配元素 O(n)

// 3) 遍历（避免 ConcurrentModificationException）
for (int i = 0; i < list.size(); i++) {                // 经典 for，可按需下标操作
    String x = list.get(i);
}
for (String x : list) {                                // 增强 for，易读
}
Iterator<String> it = list.iterator();                 // 迭代器
while (it.hasNext()) {
    String x = it.next();
    if (needRemove(x)) it.remove();                    // 正确的删除方式
}

// 4) 批量与转换
list.addAll(Arrays.asList("A", "B", "C"));           // 批量追加
String[] arr = list.toArray(new String[0]);            // 推荐：JDK 11+ 高效；JDK 8 亦可
// JDK 11+ 更推荐：list.toArray(String[]::new)

// 5) 排序/过滤/替换
list.sort(Comparator.naturalOrder());                  // 原地排序（稳定性取决于 TimSort）
list.removeIf(s -> s == null || s.isEmpty());          // 条件删除
list.replaceAll(String::trim);                         // 原地替换

// 6) 线程安全替代
List<String> syncList = Collections.synchronizedList(new ArrayList<>()); // 粗锁
CopyOnWriteArrayList<String> cow = new CopyOnWriteArrayList<>();         // 读多写少
```

- **线程安全/泛型/null/异常**：
  - 非线程安全；多线程写需使用外部同步或并发容器（如 `CopyOnWriteArrayList`）
  - 允许 `null`；`contains`、`remove` 基于 `equals`（注意自定义类型的实现）
  - 下标越界抛 `IndexOutOfBoundsException`；`subList` 与父列表结构共享，修改需谨慎

🎯 四、真实面试高频问题 + 深度解析

1) 题目：ArrayList 如何扩容？为什么是 1.5 倍？
- 标准答案：容量不足时按 1.5 倍扩容，使用 `Arrays.copyOf` 搬迁；在空间与扩容频率间折中。
- 详细解析：`grow(minCap)` 计算新容量；较小增幅会频繁复制，较大增幅浪费内存；1.5 倍为经验权衡。
- 陷阱警告：认为“每次增加固定常数”或“扩容为 2 倍”。

2) 题目：为什么无参构造初始长度不是 10？
- 标准答案：无参构造先给共享空数组，首次 `add` 才分配 10，降低空列表开销。
- 详细解析：`DEFAULTCAPACITY_EMPTY_ELEMENTDATA` 延迟分配；`ensureCapacityInternal` 决定首分配为 10。
- 陷阱警告：误以为创建对象立即占用 10 个槽位。

3) 题目：fail-fast 如何实现？
- 标准答案：`modCount` 记录结构修改次数，迭代器持有 `expectedModCount`，不等即抛 `CME`。
- 详细解析：`add/remove/clear` 等结构性修改都会 `modCount++`；迭代器的 `remove` 会同步期望值。
- 陷阱警告：把 fail-fast 当作线程安全保障。

4) 题目：`remove(int)` 与 `remove(Object)` 的差异？
- 标准答案：前者按下标删除，后者按 equals 删除首个匹配元素。
- 详细解析：`list.remove(1)` 与 `list.remove(Integer.valueOf(1))` 在 `List<Integer>` 中语义不同。
- 陷阱警告：装箱导致误删；遍历删除时用迭代器的 `remove`。

5) 题目：`subList` 有哪些坑？
- 标准答案：`subList` 与父列表共享同一 `elementData` 视图，结构修改会相互影响，迭代改动易抛 `CME`。
- 详细解析：`subList` 维护 `parent`, `offset`, `size`；跨列表结构性修改需谨慎，必要时 `new ArrayList<>(subList)`。
- 陷阱警告：当作独立列表使用；父或子结构改变后继续互操作。

6) 题目：`toArray()`、`toArray(new T[0])` 与 `toArray(new T[size])` 哪个更好？
- 标准答案：推荐 `toArray(new T[0])` 或 JDK 11+ 的 `toArray(T[]::new)`。
- 详细解析：`new T[0]` 由 JDK 优化可分配精确大小数组；`new T[size]` 在某些版本可减少一次分配，但差异很小。
- 陷阱警告：忽略泛型擦除导致 `ClassCastException`。

7) 题目：时间复杂度与性能特征？
- 标准答案：`get/set` O(1)，中间插入/删除 O(n)，尾部追加均摊 O(1)。
- 详细解析：数组搬迁用 `System.arraycopy`；连续内存带来更好缓存命中。
- 陷阱警告：认为“插入删除都是 O(1)”。

8) 题目：与 `CopyOnWriteArrayList` 的取舍？
- 标准答案：读多写少时 `COW` 更合适，写时复制，读无锁且弱一致。
- 详细解析：写放大与内存开销较大；频繁写不合适。
- 陷阱警告：在高写入场景使用 `COW`。

9) 题目：如何降低扩容成本？
- 标准答案：创建时估算初始容量或批量前 `ensureCapacity`。
- 详细解析：减少 `Arrays.copyOf` 的次数；大批量导入时收益明显。
- 陷阱警告：每次小量追加导致多次扩容抖动。

10) 题目：为什么 `elementData` 标注 `transient`？
- 标准答案：避免序列化未使用的空槽，自定义 `writeObject` 只序列化有效元素。
- 详细解析：缩小序列化体积与时间。
- 陷阱警告：以为会导致“反序列化丢数据”。

💡 五、口诀 + 表格/图示辅助记忆

- **口诀**：
  - ✅ 查快增删慢，数组实现靠扩容；十起一五涨，搬迁成本高。
  - ✅ 迭代看计数，子视图要小心；装箱分方法，线程别乱来。

- **对比表**：

| 维度 | ArrayList | LinkedList | Vector | CopyOnWriteArrayList |
| --- | --- | --- | --- | --- |
| 底层 | 动态数组 | 双向链表 | 动态数组（同步） | 动态数组（写时复制） |
| 访问 | O(1) | O(n) | O(1) | O(1) |
| 插入/删除 | 中间 O(n) | O(1)（已知节点） | 中间 O(n) | 写放大（复制全表） |
| 线程安全 | 否 | 否 | 是（粗锁） | 读无锁，写复制 |
| null | 允许 | 允许 | 允许 | 允许 |
| 典型场景 | 读多写少 | 频繁插入/删除 | 兼容老代码 | 读多极少写 |

🎁 六、建议 + 误区提醒（实战）

- 预估容量或批量前 `ensureCapacity`，减少扩容次数
- 遍历删除用 `Iterator.remove`，避免 `ConcurrentModificationException`
- 对 `List<Integer>` 使用 `remove` 时区分 `remove(int)` 与 `remove(Object)`
- `subList` 尽量转新列表再操作：`new ArrayList<>(subList)`
- 排序/替换等“结构性写”集中处理，减少中间移动
- 多线程写入用 `Collections.synchronizedList` 或 `CopyOnWriteArrayList`
- 大对象列表删除后注意及时清空引用（如 `clear()`）以助 GC

—— 完 ——

