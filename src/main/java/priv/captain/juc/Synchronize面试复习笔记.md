好的，让我们来系统地复习 `synchronized`。

-----

### 📘 Java 面试复习笔记：synchronized

### ✅ 一、概念简介

**是什么？**

`synchronized` 是 Java 中的一个关键字，它提供了一种内置的同步机制，用于实现对共享资源的**互斥访问**。它是一种**悲观锁**，其核心思想是：在访问共享资源前，先获取锁，确保同一时刻只有一个线程可以进入被 `synchronized` 修饰的代码块或方法，从而保证线程安全。

**为什么用？**

🎯 **核心目的：** 保证原子性、可见性和有序性，解决多线程环境下共享数据的访问冲突问题。

  * **原子性：** 确保被 `synchronized` 包裹的代码块是一个不可分割的整体，要么全部执行，要么都不执行。
  * **可见性：** 当一个线程释放 `synchronized` 锁时，它对共享变量的修改会立即刷新到主内存中。当另一个线程获取同一个锁时，它会读取主内存中的最新值。
  * **有序性：** 保证了被 `synchronized` 修饰的代码块在同一个线程内是顺序执行的。

### 🔹 二、底层原理 + 源码分析

`synchronized` 的底层实现与 JDK 版本（1.6 之后）和锁的状态（无锁、偏向锁、轻量级锁、重量级锁）密切相关。

**JVM 实现原理**

1.  **对于同步代码块：**

      * `synchronized(object)` 语法糖的背后是 `monitorenter` 和 `monitorexit` 两个字节码指令。
      * `monitorenter` 指令在进入同步代码块前执行，用于获取对象的监视器（`Monitor`）。
      * `monitorexit` 指令在退出同步代码块时执行，用于释放监视器。
      * **一个 `monitorenter` 对应两个 `monitorexit`**，一个用于正常退出，另一个用于处理异常退出，确保锁能被正确释放。

2.  **对于同步方法：**

      * `synchronized` 方法的实现更加简洁，它没有 `monitorenter` 和 `monitorexit` 指令。
      * JVM 会在方法的 `access flags` 中设置 `ACC_SYNCHRONIZED` 标志位。
      * 当线程调用该方法时，JVM 会自动检查这个标志位，如果存在，就会先获取对象的监视器，方法执行完毕后释放监视器。

**Java 对象头与监视器**

  * 每个 Java 对象在内存中都有一个对象头（`Object Header`）。
  * 对象头中的一部分称为 **Mark Word**，用于存储对象的哈希码、GC 年龄、锁状态等信息。
  * `synchronized` 锁的实现，就是通过修改 `Mark Word` 中的锁标志位，并与\*\*监视器（Monitor）\*\*关联起来。
  * 监视器是一个C++实现的，与操作系统线程相关联，负责线程的阻塞和唤醒。

**锁升级过程（JDK 1.6+）**

为了优化性能，`synchronized` 引入了锁升级机制，从轻量到重量，按需切换。

1.  **无锁状态：** 对象头中无锁标志。
2.  **偏向锁：** 只有一个线程访问时，它会偏向该线程。`Mark Word` 会记录这个线程的 ID，下次该线程进入时无需加锁，只需比对线程 ID 即可，开销极小。
3.  **轻量级锁：** 当第二个线程尝试获取锁时，偏向锁升级为轻量级锁。
      * 第二个线程会通过**自旋**（`spin`）来尝试获取锁，而不是立即阻塞。
      * 它会在自己的栈帧中创建一个锁记录（`Lock Record`），将对象头中的 `Mark Word` 复制到这里，然后通过 **CAS** 尝试将 `Mark Word` 的指针指向自己的锁记录。
      * 如果 CAS 成功，表示获取锁成功。如果失败，表示有竞争，自旋重试。
4.  **重量级锁：** 当自旋达到一定次数或竞争过于激烈时，轻量级锁升级为重量级锁。
      * 线程会进入阻塞状态，JVM 将其放入**监视器（`Monitor`）的等待队列，并挂起**。
      * 这涉及到用户态到内核态的切换，开销较大，但避免了无谓的 CPU 资源消耗。

### ✅ 三、常用方式 + 代码示例

`synchronized` 的使用方式分为两种：同步代码块和同步方法。

```java
public class SynchronizedExample {
    // 共享资源
    private int count = 0;

    // 1. 同步方法：锁对象是当前实例（this）
    public synchronized void incrementMethod() {
        count++;
        System.out.println("方法同步：" + Thread.currentThread().getName() + " -> " + count);
    }

    // 2. 同步代码块：锁对象可以是任何对象
    public void incrementBlock() {
        // 使用一个独立的锁对象，可以实现更细粒度的控制
        // 避免锁住整个方法，提高并发性
        synchronized (this) { // 锁对象是当前实例
            count++;
            System.out.println("代码块同步：" + Thread.currentThread().getName() + " -> " + count);
        }
    }
    
    // 3. 同步静态方法：锁对象是 Class 对象
    public synchronized static void incrementStatic() {
        // ... 静态共享资源的修改
    }

    public static void main(String[] args) throws InterruptedException {
        SynchronizedExample example = new SynchronizedExample();
        
        Runnable task1 = () -> {
            for (int i = 0; i < 1000; i++) {
                example.incrementMethod();
            }
        };

        Runnable task2 = () -> {
            for (int i = 0; i < 1000; i++) {
                example.incrementBlock();
            }
        };

        Thread t1 = new Thread(task1, "Task-Method");
        Thread t2 = new Thread(task2, "Task-Block");

        t1.start();
        t2.start();

        t1.join(); // 等待线程执行完毕
        t2.join();

        System.out.println("最终结果：" + example.count);
        // 最终结果必然是 2000，这是线程安全的
    }
}
```

**代码注释：**

  * `incrementMethod()` 锁的是 `this` 对象，其锁范围是整个方法体。
  * `incrementBlock()` 锁的也是 `this` 对象，但锁范围可以自定义，通常用于锁定更小的代码片段，以减少锁的持有时间。
  * `incrementStatic()` 锁的是 `SynchronizedExample.class` 对象，与实例无关。

### 🔍 四、真实面试高频问题 + 深度解析

**1. `synchronized` 和 `ReentrantLock` 有什么区别？**

  * **标准答案：** `synchronized` 是 Java 的内置关键字，由 JVM 实现，无需手动释放锁，属于非公平锁；`ReentrantLock` 是一个类，需要手动加锁和释放锁，提供了更灵活的功能，如可中断锁、超时等待、公平锁。
  * **详细解析：**
      * **实现方式：** `synchronized` 是 JVM 层面，`ReentrantLock` 是 `java.util.concurrent.locks` 包提供的 API。
      * **锁的释放：** `synchronized` 自动释放，`ReentrantLock` 需要在 `finally` 块中手动 `unlock()`。
      * **功能扩展：** `ReentrantLock` 功能更丰富，如 `tryLock()`（尝试获取锁）、`lockInterruptibly()`（可中断）、`Condition`（条件变量）。
      * **公平性：** `synchronized` 是非公平锁。`ReentrantLock` 默认非公平，但可以设置为公平锁。
      * **性能：** 在 JDK 1.6 之后，`synchronized` 经过锁升级优化，性能已与 `ReentrantLock` 相当，甚至在某些场景下更优。

**2. 什么是 `synchronized` 的锁升级？**

  * **标准答案：** JVM 为了优化 `synchronized` 的性能，引入了锁升级机制，将锁分为无锁、偏向锁、轻量级锁和重量级锁，以适应不同的并发竞争情况，减少锁的开销。
  * **详细解析：**
      * **偏向锁：** 解决无竞争时的性能问题。
      * **轻量级锁：** 解决少量竞争时的性能问题，通过自旋而非阻塞来获取锁。
      * **重量级锁：** 解决激烈竞争时的性能问题，将线程挂起以避免空转。

**3. `synchronized` 是可重入的吗？**

  * **标准答案：** 是。当一个线程持有某个对象的锁时，它可以再次获取同一个对象的锁，而不会被阻塞。
  * **详细解析：** 可重入性体现在 `ReentrantLock` 的名字中，但 `synchronized` 也是可重入的。底层原理是，`Monitor` 会记录持有锁的线程以及重入次数。当线程再次获取锁时，重入次数加 1；当释放锁时，重入次数减 1。只有当重入次数归零时，锁才真正被释放。

**4. `synchronized` 和 `volatile` 的区别？**

  * **标准答案：** `synchronized` 保证了**原子性、可见性和有序性**；`volatile` 只能保证**可见性和有序性**，不能保证原子性。
  * **详细解析：** `volatile` 保证了共享变量的读写操作直接在主内存进行，确保可见性，但它不能保证复合操作（如 `i++`）的原子性。`synchronized` 锁住了整个代码块，确保代码块中的所有操作都是原子性的。因此，`volatile` 适用于只对单个变量进行读写的简单场景，而 `synchronized` 适用于需要对多个操作进行同步的复杂场景。

### 💡 五、口诀 + 表格/图示辅助记忆

**锁升级口诀**

> **无锁偏向它，独占心不慌。**
> **来了第二人，轻量自旋忙。**
> **人多挤不进，重量进队列，排队等锁放。**

**`synchronized` 核心特性**

| 特性 | 描述 |
| :--- | :--- |
| **可重入** | 同一线程可以多次获取同一个锁。 |
| **不可中断** | 线程无法在等待锁时被中断。 |
| **非公平** | 无法保证等待队列中的线程按顺序获取锁。 |
| **内置** | 语言层面的关键字，由 JVM 实现。 |
| **锁对象** | 实例方法锁 `this`，静态方法锁 `Class`，代码块锁指定对象。 |

### 🎁 六、建议 + 误区提醒

**误区提醒**

1.  **认为 `synchronized` 性能差：** 这是过时的观念。在 JDK 1.6 之后，`synchronized` 的性能得到了极大的提升，在无竞争或低竞争的场景下，其性能甚至优于 `ReentrantLock`。
2.  **锁定范围过大：** 锁定范围越大，并发性越低。只对真正需要同步的代码加锁，并且尽量缩短锁的持有时间。
3.  **使用非 `final` 对象作为锁：** 如果使用非 `final` 字段作为锁对象，该字段的值可能在运行时被改变，导致不同的线程在锁定不同的对象，从而失去同步作用。

**使用建议**

1.  **优先使用 `synchronized`：** 在大多数简单的同步场景下，`synchronized` 更加简单、安全，且无需手动释放锁，避免了因忘记 `unlock()` 导致的死锁。
2.  **选择 `ReentrantLock` 的场景：** 当你需要**可中断**、**超时等待**、**公平锁**，或者需要结合 `Condition` 实现更复杂的同步机制时，应选择 `ReentrantLock`。
3.  **细粒度锁定：** 尽量使用同步代码块，而不是同步方法，以缩小锁的范围，提高并发效率。