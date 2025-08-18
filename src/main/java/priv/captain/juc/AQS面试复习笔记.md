好的，让我们来系统地复习 AQS。

📘 Java 面试复习笔记：AQS (AbstractQueuedSynchronizer)

-----

### ✅ 一、概念简介

**是什么？**

AQS，全称 `AbstractQueuedSynchronizer`，抽象队列同步器。它是 Java 并发包 `java.util.concurrent.locks` 的核心，也是大多数同步器（如 `ReentrantLock`、`Semaphore`、`CountDownLatch`、`ReentrantReadWriteLock`）的**基石**。

AQS 提供了一个**骨架**，它定义了一套用于构建锁和同步器的机制，包括：

1.  **一个 `state` 状态变量：** 用于表示锁或同步器的状态。
2.  **一个 FIFO（先进先出）双向队列：** 用于管理等待资源的线程。
3.  **一套模板方法：** 子类只需重写这些方法，即可实现自己的同步逻辑。

可以把 AQS 想象成一个\*\*“锁工厂”\*\*。它为你提供了所有复杂的排队、阻塞、唤醒机制，你只需要告诉它如何“获取锁”和“释放锁”即可。

**为什么用？**

🎯 **核心目的：** 简化并发工具的开发，提供一个高性能、可扩展的、统一的同步框架。

  * **封装底层细节：** AQS 屏蔽了复杂的线程阻塞、唤醒、队列管理等细节，让开发者可以专注于上层同步逻辑的实现。
  * **高效且可靠：** AQS 基于 `volatile` 状态变量、`CAS`（Compare-And-Swap）原子操作和双向链表，实现了高性能的无锁并发和公平/非公平锁机制。
  * **统一范式：** 所有的同步器都遵循 AQS 的模式，这使得它们的实现更加规范，也便于理解和学习。

-----

### 🔹 二、底层原理 + 源码分析

AQS 的核心是 `state` 变量和等待队列。

**核心结构**

1.  **`volatile int state`：**

      * 这个状态变量是 AQS 的灵魂，它表示当前同步状态。
      * 对于 `ReentrantLock`，`state` 表示锁的重入次数。
      * 对于 `Semaphore`，`state` 表示当前可用的许可数。
      * 对于 `CountDownLatch`，`state` 表示需要完成的任务数。
      * `volatile` 保证了 `state` 在多线程间的**可见性**。

2.  **等待队列（`CLH` 队列的变种）：**

      * 一个双向链表，用于存放因未能获取资源而被阻塞的线程。
      * 每个节点（`Node`）封装了一个等待的线程，并存储了其前驱和后继节点。
      * `head` 和 `tail` 指针分别指向队列的头和尾。
      * 当线程获取资源失败时，会创建一个 `Node` 并通过 **`CAS` 尾插法**进入队列。

**两种同步模式**

AQS 支持两种模式，由子类重写对应方法来实现：

  * **独占模式（Exclusive）：** 资源只能被一个线程访问。如 `ReentrantLock`。
      * **获取资源：** `acquire(int arg)`
      * **释放资源：** `release(int arg)`
  * **共享模式（Shared）：** 资源可以被多个线程访问。如 `Semaphore`、`CountDownLatch`。
      * **获取资源：** `acquireShared(int arg)`
      * **释放资源：** `releaseShared(int arg)`

**源码解析 - `ReentrantLock` 的 `lock()`**

以 `ReentrantLock` 的独占模式为例，其 `lock()` 方法内部调用了 AQS 的 `acquire()`。

```java
// AbstractQueuedSynchronizer.java (部分伪代码)
public final void acquire(int arg) {
    // 1. 尝试获取锁，如果成功则直接返回
    if (!tryAcquire(arg) &&
        // 2. 如果失败，将当前线程加入等待队列
        //    acquireQueued 会阻塞线程，直到获取锁成功
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        selfInterrupt(); // 如果被中断过，则重新设置中断标志位
}
```

  * **`tryAcquire(int arg)`：**

      * 这是一个**模板方法**，需要由子类（如 `ReentrantLock` 的 `Sync`）重写。
      * 它的核心是**尝试用 CAS 修改 `state` 变量**。
      * **如果成功：** 返回 `true`，获取锁成功。
      * **如果失败：** 返回 `false`，进入下一步。

  * **`addWaiter(Node.EXCLUSIVE)`：**

      * 如果 `tryAcquire` 失败，AQS 会将当前线程封装成一个**独占模式**的 `Node`。
      * 然后通过**自旋和 `CAS`** 将该 `Node` 添加到等待队列的尾部。

  * **`acquireQueued()`：**

      * 这是一个**自旋**方法。
      * 它会不断地检查当前线程的 `Node` 是否是队列的**第二个节点**（即 `head` 的后继节点）。
      * 如果是，再次尝试 `tryAcquire`。如果成功，则成为新的 `head`，并**唤醒后继节点**。
      * 如果失败，或者不是第二个节点，则调用 `LockSupport.park(this)` **阻塞**当前线程。

-----

### ✅ 三、常用方式 + 代码示例

AQS 是一个抽象类，我们不能直接使用它。我们通常通过使用基于 AQS 实现的同步工具类来间接利用其能力。

**1. `ReentrantLock` (独占锁)**

```java
import java.util.concurrent.locks.ReentrantLock;

public class AqsExample_ReentrantLock {
    // AQS 的 state 变量被 ReentrantLock 封装
    private static final ReentrantLock lock = new ReentrantLock(); 

    public static void main(String[] args) {
        lock.lock(); // 调用 AQS 的 acquire() 方法
        try {
            System.out.println("线程 " + Thread.currentThread().getName() + " 获取到锁。");
            // 业务逻辑...
        } finally {
            lock.unlock(); // 调用 AQS 的 release() 方法
            System.out.println("线程 " + Thread.currentThread().getName() + " 释放了锁。");
        }
    }
}
```

**2. `CountDownLatch` (共享锁)**

```java
import java.util.concurrent.CountDownLatch;

public class AqsExample_CountDownLatch {
    // AQS 的 state 变量被 CountDownLatch 封装
    private static final CountDownLatch latch = new CountDownLatch(3);

    public static void main(String[] args) throws InterruptedException {
        new Thread(() -> {
            try {
                System.out.println("子线程 " + Thread.currentThread().getName() + " 等待中...");
                // 调用 AQS 的 acquireShared() 方法
                latch.await(); 
                System.out.println("子线程 " + Thread.currentThread().getName() + " 被唤醒，继续执行。");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        System.out.println("主线程在执行任务...");
        Thread.sleep(1000); // 模拟耗时

        for (int i = 0; i < 3; i++) {
            System.out.println("主线程完成第 " + (i + 1) + " 个任务。");
            // 调用 AQS 的 releaseShared() 方法
            latch.countDown();
            Thread.sleep(500);
        }
    }
}
```

-----

### 🔍 四、真实面试高频问题 + 深度解析

**1. AQS 是什么？它解决了什么问题？**

  * **标准答案：** AQS 是一个用于构建锁和同步器的抽象框架。它通过一个 `int` 状态变量和 FIFO 队列，解决了线程排队、阻塞、唤醒等复杂的同步问题，为开发者提供了统一的工具。
  * **详细解析：** 在 AQS 出现之前，开发者需要自己处理线程状态、竞争条件、锁的实现细节，代码复杂且容易出错。AQS 的出现将这些通用、繁琐的逻辑封装起来，让开发者可以专注于核心的“获取”和“释放”逻辑。这是 Java 并发包的精髓所在。

**2. AQS 有哪些核心组件？**

  * **标准答案：** `state` 状态变量、FIFO 双向等待队列（`head` 和 `tail` 指针）、以及队列中的 `Node` 节点。
  * **详细解析：**
      * `state`：是所有同步器的核心状态，通过 `volatile` 和 `CAS` 保证线程安全。
      * `Node`：每个节点封装了线程本身、等待模式（独占/共享）、以及前驱和后继指针，构成了队列。
      * 等待队列：是实现线程排队、阻塞、唤醒机制的关键。它解决了多线程竞争时的排队问题。

**3. `ReentrantLock` 和 `synchronized` 有什么区别？AQS 在其中扮演了什么角色？**

  * **标准答案：** `synchronized` 是 JVM 层面的悲观锁，由编译器和 JVM 实现，是不可中断的。`ReentrantLock` 是 `java.util.concurrent` 包下的乐观锁，由 AQS 实现，是可重入的，支持公平/非公平锁，并且可以中断、超时等待。
  * **详细解析：** AQS 是 `ReentrantLock` 的幕后英雄。`ReentrantLock` 的 `lock()` 方法就是调用 AQS 的 `acquire()`，而 `unlock()` 调用 `release()`。AQS 通过其内部的 `state` 变量和等待队列，实现了 `ReentrantLock` 的可重入性、公平/非公平策略、以及线程阻塞和唤醒功能。

**4. AQS 队列中的线程是如何被唤醒的？**

  * **标准答案：** 当一个线程释放锁时，AQS 会唤醒等待队列中 `head` 节点的后继节点。
  * **详细解析：** AQS 使用 `LockSupport.unpark(thread)` 方法来唤醒线程。在独占模式下，`release` 成功后会唤醒队列头部的后继节点。被唤醒的线程会再次尝试获取锁（通常是再次 `tryAcquire`）。在共享模式下，`releaseShared` 成功后会**以广播的形式**唤醒所有等待的节点，这也被称为“唤醒传播”。

-----

### 💡 五、口诀 + 表格/图示辅助记忆

**AQS 核心口诀**

> **一状态，一队列。**
> **独占共享两模式，**
> **获取失败入队里。**
> **自旋等待不放弃，**
> **前驱放行才唤起。**

**AQS 状态流转图**

```mermaid
graph TD
    A[线程获取资源] --> B{tryAcquire()};
    B -- 成功 --> C[获取成功];
    B -- 失败 --> D[入队，并park()阻塞];
    D --> E[等待被unpark()];
    E --> F[被unpark()唤醒];
    F --> G[再次尝试获取资源];
    G -- 成功 --> C;
    G -- 失败 --> D;
```

-----

### 🎁 六、建议 + 误区提醒

**误区提醒**

1.  **AQS 是一个锁：** AQS **不是**锁，它是一个用于构建锁和同步器的基础框架。我们通常不会直接使用 AQS，而是使用基于它实现的 `ReentrantLock`、`Semaphore` 等。
2.  **`volatile` 解决了所有问题：** AQS 中的 `state` 变量虽然是 `volatile` 的，但它只能保证可见性，不能保证原子性。AQS 的原子操作是通过底层的 `CAS` 指令来实现的。
3.  **误解 `park` 和 `unpark`：** `LockSupport` 的 `park` 和 `unpark` 机制是 AQS 阻塞和唤醒线程的关键。它不受 `synchronized` 锁的限制，可以精确地唤醒指定线程，这比 `Object.wait()`/`notify()` 更加灵活。

**使用建议**

1.  **理解 AQS 源码：** 如果想深入理解 Java 并发，学习 AQS 源码是必经之路。理解 `tryAcquire`、`addWaiter`、`acquireQueued` 等核心方法，能让你对所有 `juc` 工具的底层机制豁然开朗。
2.  **避免自己造轮子：** 在大多数情况下，Java 并发包已经提供了功能强大且经过优化的同步工具，如 `ReentrantLock`、`Semaphore`、`CountDownLatch`、`CyclicBarrier` 等。在没有特殊需求的情况下，直接使用这些工具即可。
3.  **掌握 AQS 独占和共享模式的差异：** 独占模式（如 `ReentrantLock`）的线程是一个一个获取资源的，而共享模式（如 `CountDownLatch`）的线程可以批量获取或被唤醒。理解这两种模式能帮助你更好地选择和使用同步工具。