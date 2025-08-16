📘 Java 面试复习笔记：ReentrantReadWriteLock

✅ 一、概念简介

**是什么？**

`ReentrantReadWriteLock` 是 Java 并发包 `java.util.concurrent.locks` 中的一个锁实现，它提供了比 `ReentrantLock` 更细粒度的锁控制。顾名思义，它由两把锁组成：一把**读锁**（`ReadLock`）和一把**写锁**（`WriteLock`）。

**为什么用？**

🎯 **核心目的：** 在读多写少的并发场景下，提高系统的吞吐量和性能。

  * **读读并发：** 允许多个线程同时持有读锁，进行并发读取。
  * **写写互斥：** 任何时候，只允许一个线程持有写锁，进行独占式写入。
  * **读写互斥：** 读锁和写锁是互斥的，当有线程持有读锁时，写锁无法获取；当有线程持有写锁时，读锁无法获取。

这种机制完美匹配了“读多写少”的场景。例如，一个配置项，大多数时候都是被读取，只有极少时候才需要更新。如果使用 `ReentrantLock`，即使是读取操作也需要排队，极大地降低了性能。而 `ReentrantReadWriteLock` 允许所有读线程并行执行，大大提高了并发效率。

-----

🔹 二、底层原理 + 源码分析

`ReentrantReadWriteLock` 的底层实现同样基于 **AQS（AbstractQueuedSynchronizer）**。它巧妙地将读锁和写锁的状态打包在一个 `int` 类型的 AQS 状态变量 `state` 中。

**核心字段**

  * `state`：一个 `int` 类型的 AQS 状态变量。`ReentrantReadWriteLock` 将其分为高 16 位和低 16 位。
      * **高 16 位：** 读锁的计数。表示当前有多少个线程持有读锁。
      * **低 16 位：** 写锁的计数。表示写锁被获取的次数。
  * **`ReadLock` 和 `WriteLock`：** 它们是 `ReentrantReadWriteLock` 的两个内部类，分别实现了 `Lock` 接口。它们共享同一个 `state` 变量。

**源码分析 - `acquire()` 方法**

`ReentrantReadWriteLock` 的核心逻辑都在其同步器 `Sync` 中。我们来分析读锁和写锁是如何操作 `state` 的。

**1. 写锁的获取（`WriteLock.lock()`）**

```java
// ReentrantReadWriteLock.java (Sync 内部类)
protected final boolean tryAcquire(int acquires) {
    // 尝试获取写锁，忽略可重入性、中断等
    Thread current = Thread.currentThread();
    int c = getState(); // 获取当前状态
    int w = exclusiveCount(c); // 低16位，写锁计数

    // 如果状态不为0，说明有线程持有锁
    if (c != 0) {
        // 如果不是写锁的重入，并且有读锁或写锁被其他线程持有，则获取失败
        if (w == 0 || current != getExclusiveOwnerThread())
            return false;
        // 如果是重入，且重入次数超限，抛出异常
        if (w + exclusiveCount(acquires) > MAX_COUNT)
            throw new Error("Maximum lock count exceeded");
        // 成功，更新 state
        setState(c + acquires);
        return true;
    }
    // 如果状态为0，且队列中没有等待的线程，或者 CAS 成功，则获取写锁
    if (writerShouldBlock() || !compareAndSetState(c, c + acquires))
        return false;
    // 成功，设置写锁持有者为当前线程
    setExclusiveOwnerThread(current);
    return true;
}
```

  * **写锁是独占的（Exclusive）**：它只关心低 16 位。`tryAcquire` 方法首先检查 `state` 是否为 0（即高 16 位和低 16 位都为 0），如果不是，说明有线程持有读锁或写锁，则获取失败。只有当 `state` 为 0 时，才尝试通过 `CAS` 独占地获取写锁。

**2. 读锁的获取（`ReadLock.lock()`）**

```java
// ReentrantReadWriteLock.java (Sync 内部类)
protected final int tryAcquireShared(int unused) {
    // 尝试获取读锁，忽略重入等
    for (;;) {
        int c = getState();
        // 如果写锁被其他线程持有，读锁获取失败
        if (exclusiveCount(c) != 0 &&
            getExclusiveOwnerThread() != Thread.currentThread())
            return -1; // 失败
        
        int r = sharedCount(c); // 高16位，读锁计数
        // 如果读锁计数达到最大值，抛出异常
        if (r == MAX_COUNT)
            throw new Error("Maximum lock count exceeded");

        // 尝试 CAS 增加读锁计数，如果成功则获取成功
        if (compareAndSetState(c, c + SHARED_UNIT)) {
            // ... 后续逻辑，包括重入和设置第一个读锁持有者
            return 1; // 成功
        }
    }
}
```

  * **读锁是共享的（Shared）**：它只关心高 16 位。`tryAcquireShared` 循环尝试获取读锁。它会检查写锁计数是否为 0。只要写锁没有被其他线程持有（即使有读锁），它就可以通过 `CAS` 增加高 16 位的读锁计数。这允许多个线程同时获取读锁，实现了读读并发。

**关键设计：** 将一个 `int` 状态变量拆分成高低 16 位，巧妙地用一位原子变量实现了两种锁的状态管理，这是 `ReentrantReadWriteLock` 的精髓所在。

-----

✅ 三、常用方式 + 代码示例

`ReentrantReadWriteLock` 的使用非常简单，通常会定义一个 `final` 实例，然后通过它的 `readLock()` 和 `writeLock()` 方法分别获取读写锁。

```java
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CacheData {
    private Object data;
    // 创建一个读写锁实例
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    // 读锁
    private final ReentrantReadWriteLock.ReadLock readLock = rwl.readLock();
    // 写锁
    private final ReentrantReadWriteLock.WriteLock writeLock = rwl.writeLock();

    /**
     * 模拟读操作，允许多线程并发读取
     */
    public Object read() {
        readLock.lock(); // 获取读锁
        try {
            System.out.println(Thread.currentThread().getName() + " 开始读取数据...");
            // 模拟读取耗时
            Thread.sleep(100); 
            System.out.println(Thread.currentThread().getName() + " 读取数据完成。");
            return data;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            readLock.unlock(); // 释放读锁
        }
    }

    /**
     * 模拟写操作，独占锁，一次只能一个线程写入
     */
    public void write(Object newData) {
        writeLock.lock(); // 获取写锁
        try {
            System.out.println(Thread.currentThread().getName() + " 正在写入数据...");
            // 模拟写入耗时
            Thread.sleep(1000); 
            this.data = newData;
            System.out.println(Thread.currentThread().getName() + " 写入数据完成。");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            writeLock.unlock(); // 释放写锁
        }
    }

    public static void main(String[] args) {
        CacheData cache = new CacheData();

        // 模拟多个读线程
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                cache.read();
            }, "Reader-" + i).start();
        }

        // 模拟一个写线程
        new Thread(() -> {
            cache.write("new data");
        }, "Writer-0").start();
    }
}
```

**代码注释：**

  * **`readLock.lock()` 和 `readLock.unlock()`：** 读操作，允许并发，所以多个 `Reader` 线程会几乎同时开始和结束。
  * **`writeLock.lock()` 和 `writeLock.unlock()`：** 写操作，独占锁，`Writer-0` 线程会独占资源，在其执行期间，所有读线程都会被阻塞，直到写锁释放。
  * **`try-finally` 块：** 这是一个最佳实践，确保无论代码是否抛出异常，锁都能被正确释放。

-----

🎯 四、真实面试高频问题 + 深度解析

**1. 为什么要用 `ReentrantReadWriteLock`？它和 `ReentrantLock` 有什么区别？**

  * **标准答案：** `ReentrantReadWriteLock` 适用于读多写少的场景，它允许多个读线程并发访问，而 `ReentrantLock` 是一种独占锁，任何时候只允许一个线程访问，即使是读操作也需要排队。
  * **详细解析：** `ReentrantLock` 是一种悲观锁，只要有线程访问共享资源，就认为会发生冲突，所以都加锁。而 `ReentrantReadWriteLock` 是一种更智能的锁，它根据操作类型（读或写）来决定是否需要独占，这大大提升了读多写少场景的性能。
  * **陷阱警告：** 仅仅说一个支持读写并发，另一个不支持是远远不够的。你需要说明其背后的**思想差异（乐观 vs 悲观）**，以及由此带来的**性能和适用场景的区别**。

**2. `ReentrantReadWriteLock` 的读写锁是如何实现的？**

  * **标准答案：** 读写锁是基于 AQS 的 `state` 状态变量实现的。`state` 的高 16 位用于表示读锁的计数，低 16 位用于表示写锁的计数。
  * **详细解析：**
      * **写锁获取：** 写锁是独占的，它通过 `tryAcquire()` 方法尝试将 `state` 从 0 通过 `CAS` 变为 1。如果 `state` 不为 0，说明有线程持有读锁或写锁，获取失败。
      * **读锁获取：** 读锁是共享的，它通过 `tryAcquireShared()` 方法尝试增加 `state` 的高 16 位。只要写锁计数为 0，读锁就可以通过 `CAS` 成功获取。
  * **陷阱警告：** 面试官会追问 `state` 的高低位是如何划分的，以及如何通过位运算进行操作，这需要你对 AQS 的源码有一定了解。

**3. `ReentrantReadWriteLock` 的写锁是可重入的吗？读锁呢？**

  * **标准答案：** 都是可重入的。
  * **详细解析：**
      * **写锁重入：** 如果一个线程已经持有了写锁，那么它可以再次获取写锁。源码中 `getExclusiveOwnerThread() == current` 就是判断是否是同一个线程在重入。
      * **读锁重入：** 如果一个线程已经持有了读锁，它可以再次获取读锁。读锁的重入计数器在 `ThreadLocal` 中维护，而不是 AQS 的 `state` 中。这是为了避免并发读锁计数器冲突。
  * **陷阱警告：** 容易忽略读锁的重入性，或者不知道读锁的重入计数是单独维护的。

**4. 为什么读锁不能升级为写锁？**

  * **标准答案：** 为了防止死锁。
  * **详细解析：**
      * 假设线程 A 持有读锁，想升级为写锁。
      * 线程 B 也持有读锁，也想升级为写锁。
      * 线程 A 需要等待所有读锁释放（包括线程 B 的读锁），才能获取写锁。
      * 线程 B 需要等待所有读锁释放（包括线程 A 的读锁），才能获取写锁。
      * 两个线程都在等待对方释放读锁，形成循环依赖，导致死锁。
  * **陷阱警告：** 这道题考察你对锁升级和死锁的理解，是面试中的高频题。

-----

💡 五、口诀 + 表格/图示辅助记忆

**锁的特性口诀**

> **读读可以，写写互斥。**
> **读写互斥，读写不升级。**

**`ReentrantReadWriteLock` 工作流**

**锁的对比表**

| 特性 | ReentrantLock | ReentrantReadWriteLock |
| :--- | :--- | :--- |
| **锁类型** | 独占锁 | 读写锁 |
| **并发度** | 低，任意操作都需排队 | 高，允许多个读线程并行 |
| **适用场景**| 读写均衡或写多读少 | 读多写少 |
| **实现** | 基于 AQS 独占模式 | 基于 AQS 共享和独占模式 |
| **重入性** | 可重入 | 读写锁都可重入 |

-----

🎁 六、建议 + 误区提醒

**误区提醒**

1.  **读锁不等于线程安全：** 读锁只保证并发读的线程安全，并不能保证写操作的线程安全。如果你在读锁保护的范围外对共享变量进行修改，仍然可能出现问题。
2.  **忘记释放锁：** 这是最常见的错误，如果没有在 `finally` 块中调用 `unlock()`，一旦发生异常，锁将永远不会被释放，导致其他线程永久阻塞。

**使用建议**

1.  **明确使用场景：** 只有在你的程序中读操作远远多于写操作时，才考虑使用 `ReentrantReadWriteLock`，否则 `ReentrantLock` 或 `synchronized` 简单锁就足够了。
2.  **避免在锁内执行耗时操作：** 不管是读锁还是写锁，都不要在 `lock()` 和 `unlock()` 之间执行耗时过长的操作（如网络请求、数据库查询），这会大大降低锁的性能。
3.  **注意锁降级：** `ReentrantReadWriteLock` 支持锁降级。即：先获取写锁 -\> 执行写操作 -\> 获取读锁 -\> 释放写锁 -\> 执行读操作。这样做可以保证写锁释放后，仍然持有读锁，防止其他线程在此期间修改数据。
      * **注意：** 锁升级（读锁 -\> 写锁）是不被允许的。