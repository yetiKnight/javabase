好的，让我们开始复习 `CountDownLatch`。

📘 Java 面试复习笔记：CountDownLatch

✅ 一、概念简介

**是什么？**

`CountDownLatch`（倒计时门闩）是 `java.util.concurrent` 包下的一种同步工具。它允许一个或多个线程等待，直到一组在其他线程中执行的操作完成。

简单来说，你可以把它想象成一个**计数器**：

  * 当你创建一个 `CountDownLatch` 实例时，需要指定一个初始的计数值。
  * 任何线程调用 `await()` 方法时，都会进入等待状态。
  * 其他线程调用 `countDown()` 方法，会将计数器的值减一。
  * 当计数器减到 `0` 时，所有在 `await()` 上等待的线程都会被唤醒，然后继续执行。

**为什么用？**

🎯 **核心目的：** 实现线程间的同步协作，让一个或多个线程等待所有前置任务完成后再执行。

  * **主线程等待子线程：** 这是最典型的应用场景。主线程需要等待所有子线程执行完毕后，再进行汇总或下一步操作。例如，一个程序需要启动多个子任务（如加载配置、初始化模块、连接数据库等），主线程必须等待所有子任务完成后才能正式启动。
  * **分阶段任务：** 将一个大任务分解成若干个子任务，分发给不同的线程并发执行。主线程使用 `CountDownLatch` 等待所有子任务完成后，再继续执行。

-----

🔹 二、底层原理 + 源码分析

`CountDownLatch` 的底层实现同样基于 **AQS（AbstractQueuedSynchronizer）**。它利用了 AQS 的**共享模式**。

**核心字段**

  * `Sync`：一个内部类，继承自 AQS。`CountDownLatch` 的所有同步逻辑都委托给它。
  * `state`：一个 `int` 类型的 AQS 状态变量，用于表示当前的计数器值。

**源码分析 - `await()` 和 `countDown()`**

`CountDownLatch` 的核心逻辑都封装在 `Sync` 内部类中。

**1. `await()` 方法**

`await()` 方法调用的是 AQS 的 `acquireSharedInterruptibly()` 方法，这正是 AQS 共享模式的体现。

```java
// CountDownLatch.java (Sync 内部类)
public void await() throws InterruptedException {
    sync.acquireSharedInterruptibly(1);
}

// AbstractQueuedSynchronizer.java
public final void acquireSharedInterruptibly(int arg) throws InterruptedException {
    if (Thread.interrupted())
        throw new InterruptedException();
    // 尝试获取共享锁
    if (tryAcquireShared(arg) < 0)
        // 如果获取失败，进入阻塞队列等待
        doAcquireSharedInterruptibly(arg);
}
```

  * **`tryAcquireShared(int arg)`：** 这个方法在 `CountDownLatch.Sync` 中被重写。它的核心逻辑是：
    ```java
    protected int tryAcquireShared(int acquires) {
        // 如果 state 为 0，表示计数器已归零，获取成功，返回 1
        return (getState() == 0) ? 1 : -1;
    }
    ```
      * `await()` 方法尝试获取一个共享资源（但实际上并不存在），如果 `state` 为 `0`，则获取成功，线程可以继续执行。
      * 如果 `state` 不为 `0`，则获取失败，线程进入 AQS 的等待队列并阻塞。

**2. `countDown()` 方法**

`countDown()` 方法则调用了 AQS 的 `releaseShared()` 方法，这表示释放共享锁。

```java
// CountDownLatch.java
public void countDown() {
    sync.releaseShared(1);
}

// AbstractQueuedSynchronizer.java
public final boolean releaseShared(int arg) {
    // 尝试释放共享锁
    if (tryReleaseShared(arg)) {
        // 如果释放成功，并且 state 变为 0，则唤醒等待队列中的所有线程
        doReleaseShared();
        return true;
    }
    return false;
}
```

  * **`tryReleaseShared(int arg)`：** 这个方法在 `CountDownLatch.Sync` 中被重写。它的核心逻辑是：
    ```java
    protected boolean tryReleaseShared(int releases) {
        for (;;) {
            int c = getState();
            // 如果 state 已经为 0，不能再减了
            if (c == 0)
                return false;
            int nextc = c - 1;
            // 通过 CAS 减一
            if (compareAndSetState(c, nextc))
                // 如果减到 0，返回 true，触发 doReleaseShared()
                return nextc == 0;
        }
    }
    ```
      * `countDown()` 尝试通过 `CAS` 将 `state` 减一。
      * 当 `state` 被成功减到 `0` 时，`tryReleaseShared` 返回 `true`，AQS 就会调用 `doReleaseShared()`，**以广播的方式（`SIGNAL`）唤醒等待队列中的所有线程**，从而所有 `await()` 的线程都得以继续执行。

-----

✅ 三、常用方式 + 代码示例

`CountDownLatch` 的使用模式非常固定，通常是\*\*“一减，多等”**或**“多减，一等”\*\*。

```java
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CountDownLatchExample {
    
    // 初始化一个 CountDownLatch，计数器为 5
    private static final CountDownLatch latch = new CountDownLatch(5);

    public static void main(String[] args) throws InterruptedException {
        // 创建一个线程池，模拟并发执行任务
        ExecutorService executor = Executors.newFixedThreadPool(5);
        
        System.out.println("主线程：任务分发中...");
        
        // 提交 5 个子任务到线程池
        for (int i = 0; i < 5; i++) {
            final int taskId = i + 1;
            executor.execute(() -> {
                System.out.println("子线程-" + taskId + "：开始执行任务。");
                try {
                    Thread.sleep(1000); // 模拟任务耗时
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("子线程-" + taskId + "：任务执行完毕。");
                latch.countDown(); // 任务完成后，计数器减一
            });
        }
        
        // 主线程调用 await()，进入等待状态，直到计数器归零
        System.out.println("主线程：等待所有子任务完成...");
        latch.await();
        
        System.out.println("主线程：所有子任务已完成，继续执行后续逻辑。");
        executor.shutdown();
    }
}
```

**代码注释：**

  * `new CountDownLatch(5)`：初始化计数器为 5，表示主线程需要等待 5 次 `countDown()` 调用。
  * `latch.countDown()`：每个子线程完成任务后调用此方法，计数器减一。
  * `latch.await()`：主线程调用此方法，如果计数器不为 0，则阻塞等待。
  * 只有当 5 个子线程都调用了 `countDown()`，计数器归零后，主线程才能从 `await()` 中返回。

-----

🎯 四、真实面试高频问题 + 深度解析

**1. `CountDownLatch` 和 `CyclicBarrier` 有什么区别？**

  * **标准答案：**
      * **`CountDownLatch`：** 是一次性的，计数器只能使用一次。它关注的是“一个或多个线程等待，直到所有任务完成”。
      * **`CyclicBarrier`：** 是可循环使用的，可以重置。它关注的是“一组线程互相等待，达到某个共同点（屏障）后，再一起继续执行”。
  * **详细解析：**
      * **使用场景：** `CountDownLatch` 适用于“**一等**（主线程）**多减**（子线程）”的场景；`CyclicBarrier` 适用于“**多等**（多个线程）**多减**（多个线程）”，且需要重复使用的场景，如游戏中的所有玩家都加载完毕后再一起开始。
      * **等待方式：** `CountDownLatch` 的 `await()` 是阻塞等待，`countDown()` 是非阻塞的。`CyclicBarrier` 的 `await()` 也是阻塞等待，但它可以用于一个线程等待多个线程，或多个线程互相等待。

**2. `CountDownLatch` 的底层原理是什么？**

  * **标准答案：** `CountDownLatch` 基于 AQS 实现，利用了 AQS 的共享模式。它的计数器就是 AQS 的 `state` 变量。
  * **详细解析：** `await()` 方法会检查 `state` 是否为 0，如果不是，则进入 AQS 的等待队列。`countDown()` 方法会通过 `CAS` 操作原子地将 `state` 减 1。当 `state` 减到 0 时，AQS 会以**共享模式**唤醒所有在等待队列中的线程。
  * **陷阱警告：** 面试官会追问如何通过 AQS 共享模式实现这个功能。你需要提到 `tryAcquireShared` 和 `tryReleaseShared` 方法，以及 `releaseShared` 成功后 AQS 会以广播方式唤醒所有线程。

**3. `CountDownLatch` 是可重用的吗？为什么？**

  * **标准答案：** 不可重用。
  * **详细解析：** `CountDownLatch` 的计数器一旦减到 0，就无法再增加。其内部的 `tryReleaseShared` 方法在 `state` 为 0 时直接返回 `false`，无法再进行 `countDown` 操作。因此，它的生命周期是一次性的。如果需要重用，应考虑使用 `CyclicBarrier`。

**4. `CountDownLatch` 和 `join()` 方法有什么区别？**

  * **标准答案：** `join()` 方法是**阻塞调用者线程**，直到目标线程执行完毕。而 `CountDownLatch` 提供了更灵活的同步机制，可以等待多个线程完成，或者只等待部分线程完成。
  * **详细解析：**
      * **`join()`：** 只能等待一个线程。如果需要等待多个，必须串行地多次调用 `join()`，这会增加代码复杂性，且不够灵活。
      * **`CountDownLatch`：** 可以同时等待多个线程，只需要在每个子线程中调用 `countDown()`，然后在主线程中调用一次 `await()` 即可。这使得代码更简洁，并且可以控制等待的粒度。

-----

💡 五、口诀 + 表格/图示辅助记忆

**`CountDownLatch` 口诀**

> **数到零，门打开。**
> **一锤子，买卖成。**
> **不可用，需重造。**

**`CountDownLatch` 工作流程图**

```mermaid
graph TD
    A[主线程] --> B{启动子线程};
    B --> C[子线程 1];
    B --> D[子线程 2];
    B --> E[子线程 N];
    F[主线程调用 latch.await()] --> G{计数器 > 0?};
    G -- 是 --> H[主线程阻塞];
    G -- 否 --> K[主线程继续执行];
    C --> I[任务完成];
    D --> I;
    E --> I;
    I --> J[调用 latch.countDown()];
    J --> L[计数器-1];
    L -- 减到0 --> F;
```

-----

🎁 六、建议 + 误区提醒

**误区提醒**

1.  **忘记 `countDown()`：** 如果某些任务中途失败或没有调用 `countDown()`，计数器将永远无法归零，导致 `await()` 的线程永久阻塞，造成死锁或程序无响应。
2.  **在 `countDown()` 中抛出异常：** 在 `countDown()` 的内部不要抛出异常，否则计数器可能无法正确减一，同样会导致 `await()` 线程阻塞。

**使用建议**

1.  **在 `finally` 块中调用 `countDown()`：** 这是一个健壮的编程习惯，确保即使任务执行过程中发生异常，计数器也能正确减一。
2.  **初始值设置：** `CountDownLatch` 的初始值应与需要同步的线程/任务数量保持一致。
3.  **超时机制：** `await()` 方法有带超时的重载版本 `await(long timeout, TimeUnit unit)`。在实际应用中，为了防止因意外情况导致的永久阻塞，应该优先使用带超时的方法。
4.  **一次性使用：** 记住 `CountDownLatch` 是一次性使用的，如果你的场景需要多次同步或重置，请考虑使用 `CyclicBarrier` 或其他同步工具。