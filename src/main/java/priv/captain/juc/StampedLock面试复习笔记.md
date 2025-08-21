好的，让我们来系统地复习 `StampedLock`。

-----

### 📘 Java 面试复习笔记：StampedLock

### ✅ 一、概念简介

**是什么？**

`StampedLock` 是 Java 8 引入的一个新的锁机制，它提供了比 `ReentrantReadWriteLock` **更细粒度**的控制。它是一种**乐观读锁**，其核心思想是：在读取数据时，不加锁，而是通过一个\*\*时间戳（`stamp`）\*\*来验证数据是否在读取期间被其他线程修改过。

可以把它想象成一个**图书馆管理员**：

  - 当读者（线程）进入图书馆（共享资源）时，管理员不给钥匙（不加锁），而是给一个**入馆卡（`stamp`）**。
  - 读者可以自由阅览。如果想借书（写操作），需要交还入馆卡，并获取借书卡（写锁）。
  - 如果另一个读者在借书，第一个读者就会发现他的入馆卡已过期，需要重新办理，确保他看到的是最新版的数据。

**为什么用？**

🎯 **核心目的：** 在读多写少的并发场景下，进一步**提高吞吐量和性能**，解决 `ReentrantReadWriteLock` 的**写饥饿**问题。

  * **乐观读：** `StampedLock` 引入乐观读（`optimistic read`），允许在没有锁的情况下进行读操作。这极大地减少了读锁的开销，在读操作远超写操作的场景下，性能优势明显。
  * **解决写饥饿：** `ReentrantReadWriteLock` 在读线程非常多时，写锁可能长时间无法获取，导致写线程饥饿。`StampedLock` 的乐观读机制避免了读线程长时间持有锁，从而给了写线程更多的机会。

### 🔹 二、底层原理 + 源码分析

`StampedLock` 的底层实现非常复杂，它不直接基于 `AQS`，而是依赖于\*\*原子操作（CAS）**和`long`类型的**戳（`stamp`）\*\*来管理锁的状态。

**核心字段**

  * **`state`：** 一个 `volatile long` 类型的变量，用来存储锁的状态和戳。
      * **戳（`stamp`）：** 这是一个不断增长的数字，代表锁的当前版本。每次写操作都会原子性地修改这个戳，从而使所有旧的读戳失效。
  * **`Node`：** 内部的等待队列节点。

**三种模式**

`StampedLock` 提供了三种模式的锁：

1.  **写锁（`writeLock()`）：** 独占锁，与 `ReentrantLock` 类似。一旦获取，其他线程（读或写）都必须阻塞。

      * 成功获取会返回一个非零的 `stamp`。
      * **底层：** 通过 `CAS` 竞争。如果获取失败，线程会进入一个基于 `spin-wait` 和 `park-unpark` 机制的等待队列。

2.  **悲观读锁（`readLock()`）：** 共享锁，与 `ReentrantReadWriteLock` 的读锁类似。允许多个线程同时获取。

      * 成功获取会返回一个非零的 `stamp`。
      * **底层：** 通过 `CAS` 竞争。如果获取失败，线程会进入等待队列。

3.  **乐观读（`tryOptimisticRead()`）：**

      * **不加锁：** 它返回当前锁的 `stamp` 值，**不进行任何加锁操作**。
      * **无阻塞：** 这是一种非阻塞的读模式。
      * **验证：** 需要在使用后通过 `validate(stamp)` 方法来验证 `stamp` 是否有效。如果有效，说明读取期间没有写操作发生；如果失效，则需要升级为悲观读锁或重试。

**核心流程：乐观读**

1.  **获取戳：** `long stamp = lock.tryOptimisticRead();`
2.  **读取数据：** 在无锁状态下，读取共享变量的副本。
3.  **验证戳：** `if (lock.validate(stamp)) { ... }`
4.  **如果有效：** 说明读取期间没有写操作，可以安全地使用数据。
5.  **如果失效：** 说明数据已被修改，需要**重新获取悲观读锁**或**重新尝试乐观读**。

### ✅ 三、常用方式 + 代码示例

`StampedLock` 的使用比 `ReentrantReadWriteLock` 复杂，因为它需要手动管理 `stamp` 的验证和锁的升级。

```java
import java.util.concurrent.locks.StampedLock;

public class StampedLockExample {
    private double x, y;
    // 创建一个 StampedLock 实例
    private final StampedLock lock = new StampedLock();

    /**
     * 写操作：独占锁
     */
    public void move(double deltaX, double deltaY) {
        long stamp = lock.writeLock(); // 获取写锁，返回一个 stamp
        try {
            System.out.println("线程 " + Thread.currentThread().getName() + " 获取写锁...");
            // 模拟写操作耗时
            Thread.sleep(100); 
            x += deltaX;
            y += deltaY;
            System.out.println("线程 " + Thread.currentThread().getName() + " 释放写锁...");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock(stamp); // 释放写锁
        }
    }

    /**
     * 读操作：乐观读
     */
    public double distanceFromOrigin() {
        // 1. 尝试乐观读，获取 stamp
        long stamp = lock.tryOptimisticRead();
        double currentX = x;
        double currentY = y;
        
        // 2. 验证 stamp 是否有效
        if (!lock.validate(stamp)) {
            // stamp 无效，说明在读取期间有写操作发生
            System.out.println("乐观读失败，升级为悲观读锁...");
            // 3. 升级为悲观读锁
            stamp = lock.readLock();
            try {
                currentX = x;
                currentY = y;
            } finally {
                // 4. 释放悲观读锁
                lock.unlockRead(stamp);
            }
        }
        
        System.out.println("线程 " + Thread.currentThread().getName() + " 读取数据...");
        // 5. 使用安全的数据
        return Math.sqrt(currentX * currentX + currentY * currentY);
    }
}
```

**代码注释：**

  * **`tryOptimisticRead()`：** 这是乐观读的核心，它几乎没有开销，但返回的数据可能是不一致的。
  * **`validate(stamp)`：** 这是乐观读的灵魂，用于验证数据是否在读取期间被修改。
  * **锁升级：** 如果乐观读失败，代码会**降级**（不是升级）为悲观读，以确保获取到一致的数据。
  * **`unlock(stamp)` 和 `unlockRead(stamp)`：** 释放锁时必须传入 `stamp`，这是为了确保释放的是正确的锁。

### 🔍 四、真实面试高频问题 + 深度解析

**1. `StampedLock` 和 `ReentrantReadWriteLock` 的区别？**

  * **标准答案：** `StampedLock` 提供了三种模式（写、悲观读、乐观读），而 `ReentrantReadWriteLock` 只有读和写两种。`StampedLock` 的乐观读模式在读多写少的场景下，性能远优于 `ReentrantReadWriteLock`，因为它**没有加锁**。
  * **深入：**
      * **性能：** `StampedLock` 的乐观读几乎没有开销，因为它不涉及线程阻塞、唤醒，甚至不修改对象头。`ReentrantReadWriteLock` 的读锁是共享锁，尽管允许多个线程访问，但仍然需要修改状态，存在一定的开销。
      * **锁饥饿：** `ReentrantReadWriteLock` 在大量读线程存在时，可能导致写线程饥饿。`StampedLock` 的乐观读不阻塞写操作，从而解决了这个问题。
      * **可重入：** `ReentrantReadWriteLock` 是可重入的，`StampedLock` **不可重入**。
      * **中断支持：** `ReentrantReadWriteLock` 支持读写锁的中断，而 `StampedLock` 不支持中断。

**2. 为什么 `StampedLock` 不支持可重入？**

  * **标准答案：** 主要是为了保证其性能和复杂性平衡。如果支持重入，需要维护一个重入计数器，这会增加锁的开销，尤其是在乐观读模式下，将变得更加复杂。
  * **深入：** `StampedLock` 的设计哲学是极致的性能。为了避免维护重入计数器带来的额外开销和复杂性，它牺牲了可重入性。因此，在使用 `StampedLock` 时，需要特别注意不要在已持有锁的情况下再次调用加锁方法，否则会引发死锁。

**3. `StampedLock` 的乐观读为什么不能保证线程安全？如何解决？**

  * **标准答案：** 乐观读不加锁，它只返回一个 `stamp` 值，数据在读取期间可能被其他线程修改。
  * **解决：** 需要在读取数据后，通过 `validate(stamp)` 方法验证 `stamp` 是否仍然有效。如果无效，则说明数据已被修改，需要**重新获取悲观读锁**，然后重新读取数据。这也被称为**锁的降级**。

**4. `StampedLock` 为什么不支持中断？**

  * **标准答案：** `StampedLock` 的阻塞方法（如 `writeLock()`）不支持中断，这是为了防止在 `Lock` 内部的 `park` 方法被中断后，导致锁的状态不一致。
  * **深入：** `StampedLock` 提供了 `tryWriteLock(long timeout, TimeUnit unit)` 和 `tryReadLock(long timeout, TimeUnit unit)` 等带超时的方法，可以在超时后放弃等待，这可以在一定程度上弥补不支持中断的不足。

### 💡 五、口诀 + 表格/图示辅助记忆

**`StampedLock` 口诀**

> **读不锁，拿个戳，乐观读，快如梭。**
> **读完戳，需验证，戳无效，锁降格。**
> **写锁重，读写互，不重入，要记着。**
> **高性能，有代价，复杂性，需掌握。**

**`StampedLock` vs `ReentrantReadWriteLock` 对比表**

| 特性 | StampedLock | ReentrantReadWriteLock |
| :--- | :--- | :--- |
| **读锁模式** | 乐观读、悲观读 | 悲观读 |
| **可重入** | ❌ | ✅ |
| **中断支持** | ❌ | ✅ |
| **锁类型** | `long` 戳，原子操作 | `AQS` 状态 |
| **性能** | 乐观读性能极高 | 读写并发性能较好 |
| **写饥饿** | 解决了 | 可能存在 |
| **使用复杂性**| 高，需手动验证和降级 | 低，使用简单 |

### 🎁 六、建议 + 误区提醒

**误区提醒**

1.  **认为 `StampedLock` 总是比 `ReentrantReadWriteLock` 好：** 这是一个常见误区。`StampedLock` 更复杂，且不支持重入和中断，在大多数情况下，如果性能瓶颈不明显，使用 `ReentrantReadWriteLock` 或 `synchronized` 会更安全、更简单。
2.  **忘记验证 `stamp`：** 如果使用乐观读模式，但忘记了 `validate(stamp)`，可能导致读取到脏数据，造成难以察觉的 bug。
3.  **在持有 `StampedLock` 期间，再次调用加锁方法：** `StampedLock` 不可重入，这将导致死锁。

**使用建议**

1.  **谨慎使用 `StampedLock`：** 只有在读操作远超写操作，且对性能有极致要求，并且对并发编程有深入理解的情况下，才考虑使用 `StampedLock`。
2.  **`StampedLock` 的经典用法：** 乐观读 -\> 失败后升级为悲观读。这是其最核心、最能体现性能优势的用法。
3.  **在 `finally` 块中释放锁：** 无论是写锁还是悲观读锁，都必须在 `finally` 块中调用 `unlock` 或 `unlockRead` 来释放锁，以避免死锁。
4.  **避免在 `StampedLock` 内部执行耗时操作：** 这会阻塞其他线程，特别是写操作，从而降低性能。