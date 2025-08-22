### 📘 Java 面试复习笔记：Set

### ✅ 一、概念简介

**Set 是什么？**

`Set` 是 `java.util` 包下的一个接口，它继承自 `Collection` 接口，用于表示一个**无序且不包含重复元素**的集合。`Set` 接口并没有定义任何新的方法，它完全依赖于 `Collection` 接口的方法，但其行为（特别是 `add()` 和 `equals()`）受到了特殊约束。

可以把 `Set` 想象成一个**数学中的集合**：

  - 集合中的每个元素都是唯一的。
  - 集合中的元素没有固定的顺序。

**为什么用？**

🎯 **核心目的：** 存储唯一元素，并高效地执行查找、添加和删除操作。

  * **去重：** 当你需要一个容器来存储一些元素，并且确保这些元素没有重复时，`Set` 是最佳选择。
  * **快速查找：** `Set` 的主要实现类（如 `HashSet`）底层通常使用哈希表，这使得元素的添加、删除和查找操作的平均时间复杂度为 O(1)。

-----

### 🔹 二、核心实现类

`Set` 接口有几个重要的实现类，每个类都有其独特的特性和适用场景。

#### 1\. `HashSet`

  * **特点：**
      * **无序：** 不保证元素的迭代顺序。
      * **唯一：** 不允许有重复元素。
      * **基于哈希表：** 底层由 `HashMap` 实现，所有元素都作为 `HashMap` 的键（`key`），而值（`value`）是一个固定的虚拟对象。
      * **线程不安全：** 在多线程环境下，需要通过 `Collections.synchronizedSet()` 或其他并发工具来保证线程安全。
  * **核心原理：**
      * `add()` 方法：当添加元素时，`HashSet` 会调用元素的 `hashCode()` 方法来确定其存储位置，并调用 `equals()` 方法来判断是否已存在相同的元素。
      * **重要提示：** 如果将自定义对象放入 `HashSet`，必须同时**重写 `hashCode()` 和 `equals()` 方法**，并且要遵循它们的约定，即如果两个对象相等（`equals()` 返回 `true`），它们的哈希码也必须相等。

#### 2\. `LinkedHashSet`

  * **特点：**
      * **有序：** 保持元素的**插入顺序**。
      * **唯一：** 不允许有重复元素。
      * **基于哈希表和链表：** 底层由 `LinkedHashMap` 实现，维护一个双向链表来记录元素的插入顺序。
  * **核心原理：**
      * 它在 `HashSet` 的哈希表基础上，增加了对元素插入顺序的维护。当你遍历 `LinkedHashSet` 时，元素的顺序就是你添加它们的顺序。

#### 3\. `TreeSet`

  * **特点：**
      * **有序：** 元素按照**自然排序**或**自定义排序**的顺序存储和迭代。
      * **唯一：** 不允许有重复元素。
      * **基于红黑树：** 底层由 `TreeMap` 实现，所有元素都作为 `TreeMap` 的键，因此元素会按照排序规则进行组织。
  * **核心原理：**
      * `add()` 方法：添加元素时，`TreeSet` 会使用元素的 `compareTo()` 方法（如果元素实现了 `Comparable` 接口）或自定义的 `Comparator` 来决定元素的位置。
      * **重要提示：** 如果将自定义对象放入 `TreeSet`，该对象必须实现 `Comparable` 接口，或者在创建 `TreeSet` 实例时提供一个 `Comparator`。

-----

### ✅ 三、常用方式 + 代码示例

```java
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

public class SetExample {
    public static void main(String[] args) {
        // --- 1. HashSet: 无序，去重 ---
        Set<String> hashSet = new HashSet<>();
        hashSet.add("Apple");
        hashSet.add("Orange");
        hashSet.add("Banana");
        hashSet.add("Apple"); // 重复元素，不会被添加
        System.out.println("HashSet (无序，不重复): " + hashSet); // 顺序不确定
        
        // --- 2. LinkedHashSet: 有序（插入顺序），去重 ---
        Set<String> linkedHashSet = new LinkedHashSet<>();
        linkedHashSet.add("Apple");
        linkedHashSet.add("Orange");
        linkedHashSet.add("Banana");
        linkedHashSet.add("Apple"); // 重复元素，不会被添加
        System.out.println("LinkedHashSet (按插入顺序): " + linkedHashSet); // 顺序为 Apple, Orange, Banana

        // --- 3. TreeSet: 有序（自然排序），去重 ---
        Set<String> treeSet = new TreeSet<>();
        treeSet.add("Apple");
        treeSet.add("Orange");
        treeSet.add("Banana");
        treeSet.add("Apple"); // 重复元素，不会被添加
        System.out.println("TreeSet (按自然排序): " + treeSet); // 顺序为 Apple, Banana, Orange

        // --- 4. 自定义对象去重 ---
        Set<User> userSet = new HashSet<>();
        userSet.add(new User(1, "Alice"));
        userSet.add(new User(2, "Bob"));
        userSet.add(new User(1, "Alice")); // 尽管内容相同，但如果没有重写 hashCode() 和 equals()，仍会被添加
        System.out.println("自定义对象去重（未重写）：" + userSet.size());
        
        // --- 重写 hashCode() 和 equals() 后 ---
        Set<UserWithOverride> userSet2 = new HashSet<>();
        userSet2.add(new UserWithOverride(1, "Alice"));
        userSet2.add(new UserWithOverride(2, "Bob"));
        userSet2.add(new UserWithOverride(1, "Alice")); // 现在只添加一个元素
        System.out.println("自定义对象去重（重写后）：" + userSet2.size());
    }
}

class User {
    int id;
    String name;
    public User(int id, String name) {
        this.id = id;
        this.name = name;
    }
}

class UserWithOverride {
    int id;
    String name;
    public UserWithOverride(int id, String name) {
        this.id = id;
        this.name = name;
    }
    
    @Override
    public int hashCode() {
        return id;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        UserWithOverride user = (UserWithOverride) obj;
        return id == user.id && name.equals(user.name);
    }
}
```

### 🔍 四、真实面试高频问题 + 深度解析

**1. `Set` 有什么特点？常用的实现类有哪些？**

  * **标准答案：** `Set` 的特点是**无序且不包含重复元素**。常用的实现类有 `HashSet`、`LinkedHashSet` 和 `TreeSet`。

**2. `HashSet` 如何保证元素的唯一性？**

  * **标准答案：** `HashSet` 底层依赖于 `HashMap`。它通过元素的 `hashCode()` 方法来确定存储位置，然后通过 `equals()` 方法来比较是否已存在相同的元素。只有当两个对象的 `hashCode()` 和 `equals()` 都相同时，`HashSet` 才认为它们是重复的。
  * **深入：** 如果自定义对象没有重写 `hashCode()` 和 `equals()`，那么它会使用 `Object` 类的默认实现，该实现通常是基于对象的内存地址。这会导致即使两个对象的内容完全一样，它们也被认为是不同的，从而无法去重。

**3. `HashSet`、`LinkedHashSet` 和 `TreeSet` 有什么区别？**

  * **标准答案：**
      * **`HashSet`：** **无序**，性能最好。
      * **`LinkedHashSet`：** **按插入顺序有序**，性能略低于 `HashSet`。
      * **`TreeSet`：** **按元素自然排序或自定义排序有序**，性能最差（O(log n)），因为它需要维护红黑树结构。
  * **场景选择：**
      * 如果你只需要去重，对顺序没有要求，用 `HashSet`。
      * 如果你既要去重，又要保持元素的插入顺序，用 `LinkedHashSet`。
      * 如果你既要去重，又要对元素进行排序，用 `TreeSet`。

**4. 为什么重写 `equals()` 方法时，通常也需要重写 `hashCode()` 方法？**

  * **标准答案：** 这是为了保证 `Set` 和 `Map` 等哈希表容器的正确行为。
  * **深入：** `Set` 的 `add()` 方法会先调用 `hashCode()` 找到对应的桶，然后遍历该桶中的元素，通过 `equals()` 进行比较。如果只重写了 `equals()`，没有重写 `hashCode()`，那么两个逻辑上相等的对象可能会有不同的哈希码，被存放在不同的桶中，导致 `Set` 无法正确判断它们是重复的，从而违反了 `Set` 的唯一性约束。

### 💡 五、口诀 + 表格/图示辅助记忆

**Set 三兄弟口诀**

> **HashSet，快如风，无序去重靠哈希。**
> **LinkedHashSet，链表串，插入有序不乱跑。**
> **TreeSet，红黑树，自然排序有规律。**

**`Set` 实现类核心特性对比**

| 特性 | **`HashSet`** | **`LinkedHashSet`** | **`TreeSet`** |
| :--- | :--- | :--- | :--- |
| **底层数据结构**| `HashMap` | `LinkedHashMap` | `TreeMap` (红黑树) |
| **元素顺序** | 无序 | 按插入顺序 | 自然排序或自定义排序 |
| **性能** | 最佳 (O(1)) | 良好 (O(1)) | 较差 (O(log n)) |
| **是否线程安全**| 否 | 否 | 否 |
| **主要应用** | 快速去重 | 保持去重且有序 | 排序去重 |

-----

### 🎁 六、建议 + 误区提醒

**误区提醒**

1.  **忘记重写 `hashCode()`：** 这是最常见的错误，会导致 `Set` 无法正确去重自定义对象。
2.  **混淆 `LinkedHashSet` 和 `TreeSet` 的顺序：** `LinkedHashSet` 是按**插入顺序**，`TreeSet` 是按**排序顺序**。

**使用建议**

1.  **根据需求选择：** 优先考虑最简单的 `HashSet`，除非你对顺序有特殊要求。
2.  **重写方法：** 只要你将自定义对象放入哈希表相关的集合（如 `HashSet`、`HashMap`），就必须同时重写 `equals()` 和 `hashCode()`。
3.  **线程安全：** 如果在多线程环境中使用 `Set`，请使用 `Collections.synchronizedSet(new HashSet<>())` 或 `ConcurrentHashMap.newKeySet()` 来创建线程安全的 `Set`。