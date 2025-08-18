好的，让我们开始复习 `CyclicBarrier`。

📘 Java 面试复习笔记：CyclicBarrier

✅ 一、概念简介

**是什么？**

`CyclicBarrier`（循环屏障）是 `java.util.concurrent` 包下的一种同步工具。它允许一组线程在到达一个共同的屏障点（`barrier point`）时相互等待。一旦所有线程都到达屏障点，它们就会被同时释放，然后可以继续执行各自的任务。

简单来说，你可以把它想象成一个**多人赛跑的起跑线**：

  * 赛跑者（线程）到达起跑线（屏障点）后，必须停下来等待。
  * 只有当所有赛跑者都到达起跑线后，发令枪（屏障解除）才会响。
  * 所有赛跑者同时开始下一段赛程。
  * 更重要的是，这个起跑线是可以**循环使用的**，跑完一轮后，下一轮比赛还可以回到这个起跑线集合。

**为什么用？**

🎯 **核心目的：** 实现一组线程的同步协作，让它们在某个特定点上“步调一致”。

  * **分阶段任务：** 适用于将一个大任务分解为多个子任务，每个子任务都由一个线程来完成。所有线程必须完成第一阶段任务后，才能一起进入第二阶段。这种模式可以保证任务的并行性和阶段性。
  * **可重用性：** `CyclicBarrier` 的最大特点是可重用。当所有线程都通过屏障后，屏障会被自动重置，可以用于下一轮等待。这对于需要重复进行、分阶段执行的并行计算非常有用。

-----

🔹 二、底层原理 + 源码分析

`CyclicBarrier` 的底层实现与 `CountDownLatch` 有些不同，它主要依赖于 `ReentrantLock` 和 `Condition`，而不是 AQS 的 `state` 变量。

**核心字段**

  * **`parties`：** 屏障的总线程数。
  * **`count`：** 当前未到达屏障的线程数。初始值为 `parties`。
  * **`barrierCommand`：** 一个可选的 `Runnable` 任务。当屏障被打开时，由最后一个到达的线程执行。
  * **`lock`：** 一个 `ReentrantLock` 实例，用于保护 `count` 和其他状态的线程安全。
  * **`trip`：** 一个 `Condition` 实例，用于实现线程的等待和唤醒。

**源码分析 - `await()` 方法**

`CyclicBarrier` 的核心逻辑都在 `await()` 方法中。

```java
// CyclicBarrier.java
public int await() throws InterruptedException, BrokenBarrierException {
    return dowait(false, 0L);
}

private int dowait(boolean timed, long nanos) throws InterruptedException, BrokenBarrierException {
    final ReentrantLock lock = this.lock;
    lock.lock(); // 1. 获取独占锁
    try {
        final Generation g = generation;
        // 检查屏障是否已损坏
        if (g.broken) throw new BrokenBarrierException();

        int index = --count; // 2. 线程数减一，获取当前线程的索引
        // 3. 检查是否是最后一个到达的线程
        if (index == 0) { // 最后一个到达的线程
            boolean ranAction = false;
            try {
                final Runnable command = barrierCommand;
                if (command != null)
                    command.run(); // 4. 执行屏障任务
                ranAction = true;
                return 0; // 返回 0，表示当前线程是最后一个到达的
            } finally {
                // 5. 唤醒所有等待的线程，并重置屏障
                if (ranAction)
                    nextGeneration();
                else // 如果任务执行失败，则打破屏障
                    breakBarrier();
            }
        }

        for (;;) { // 6. 进入等待
            try {
                trip.await(); // 线程进入等待状态，释放锁
            } catch (InterruptedException ie) {
                // ... 中断处理逻辑
            }

            // 7. 当被唤醒时，检查屏障是否已更新
            if (g != generation) // 如果屏障已重置，说明可以继续执行了
                return index;
            if (g.broken) // 如果屏障被打破，抛出异常
                throw new BrokenBarrierException();
        }
    } finally {
        lock.unlock(); // 8. 释放锁
    }
}
```

**执行流程：**

1.  **获取锁**：每个线程调用 `await()` 时，都会先获取 `ReentrantLock`，以保证 `count` 变量操作的原子性。
2.  **计数器减一**：当前线程到达屏障，`count` 减 1。
3.  **判断是否是最后一个**：
      * **如果 `count` 减到 `0`**：说明所有线程都已到达。当前线程（即最后一个到达的线程）将执行屏障任务（如果存在），然后调用 `nextGeneration()` 方法。`nextGeneration()` 会唤醒 `Condition` 上所有等待的线程，并将 `count` 重置回 `parties`，实现屏障的循环利用。
      * **如果 `count` 未到 `0`**：说明还有其他线程未到达。当前线程会调用 `trip.await()` 进入等待状态，并释放锁。
4.  **唤醒**：当最后一个线程到达并调用 `nextGeneration()` 后，所有在 `trip.await()` 上等待的线程都会被唤醒，并从 `await()` 方法返回。

-----

✅ 三、常用方式 + 代码示例

`CyclicBarrier` 的典型应用是分阶段任务。

```java
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CyclicBarrierExample {
    
    // 创建一个 CyclicBarrier，等待 3 个线程
    // 当所有线程都到达屏障后，执行一个屏障任务
    private static final CyclicBarrier barrier = new CyclicBarrier(3, () -> {
        System.out.println("屏障动作：所有线程第一阶段任务完成，屏障已解除，准备进入第二阶段...");
    });

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        
        for (int i = 0; i < 3; i++) {
            final int taskId = i + 1;
            executor.execute(() -> {
                try {
                    System.out.println("线程-" + taskId + "：开始执行第一阶段任务。");
                    Thread.sleep(1000 * taskId); // 模拟耗时，以体现等待效果
                    System.out.println("线程-" + taskId + "：第一阶段任务完成，到达屏障点。");
                    
                    barrier.await(); // 线程等待，直到所有线程都到达
                    
                    System.out.println("线程-" + taskId + "：屏障已解除，开始执行第二阶段任务。");
                } catch (InterruptedException | BrokenBarrierException e) {
                    System.err.println("线程-" + taskId + " 屏障等待被中断或屏障已损坏。");
                }
            });
        }
        
        executor.shutdown();
    }
}
```

**代码注释：**

  * `new CyclicBarrier(3, ...)`：创建屏障，`parties` 为 3，表示需要 3 个线程都到达后才解除屏障。第二个参数是屏障解除后执行的 `Runnable` 任务，这里会打印一条提示信息。
  * `barrier.await()`：每个线程执行完第一阶段任务后，调用此方法等待。第一个和第二个线程会进入阻塞状态，直到第三个线程也调用 `await()`。
  * 当第三个线程调用 `await()` 时，屏障被打开，所有线程被同时释放，继续执行第二阶段的任务。

-----

🎯 四、真实面试高频问题 + 深度解析

**1. `CyclicBarrier` 和 `CountDownLatch` 有什么区别？**

  * **标准答案：**
      * **`CountDownLatch`：** “减法”同步，一次性使用，关注“一个或多个线程等待所有任务完成”。
      * **`CyclicBarrier`：** “加法”同步，可重用，关注“一组线程互相等待，达到某个共同点后，再一起执行”。
  * **详细解析：**
      * **计数方式：** `CountDownLatch` 计数器递减（`countDown`），`CyclicBarrier` 计数器递增（`await` 中 `count` 递减，但每次使用后都重置）。
      * **任务执行顺序：** `CountDownLatch` 中，调用 `await()` 的线程会阻塞，而调用 `countDown()` 的线程不阻塞。`CyclicBarrier` 中，所有线程都必须阻塞等待。
      * **核心思想：** `CountDownLatch` 是让**主线程等待子线程**，`CyclicBarrier` 是让**子线程互相等待**。

**2. `CyclicBarrier` 的底层实现原理是什么？**

  * **标准答案：** `CyclicBarrier` 的底层实现基于 `ReentrantLock` 和 `Condition`。
  * **详细解析：**
      * `lock` 和 `count` 保证了对线程数的安全操作。
      * `Condition` 的 `await()` 和 `signalAll()` 方法实现了线程的等待和唤醒。
      * 当线程调用 `await()` 时，如果不是最后一个，则在 `Condition` 上等待；如果是最后一个，则执行屏障任务，然后调用 `signalAll()` 唤醒所有等待的线程，并重置 `count`。
  * **陷阱警告：** 面试官会考察你对 `ReentrantLock` 和 `Condition` 组合使用的理解，以及如何通过这种组合来实现线程的同步等待和唤醒。

**3. 为什么 `CyclicBarrier` 会抛出 `BrokenBarrierException`？**

  * **标准答案：** 当屏障被“打破”时，会抛出 `BrokenBarrierException`。屏障被打破通常由以下几种情况导致：
    1.  一个线程在等待时被**中断**（`InterruptedException`）。
    2.  一个线程在屏障动作执行时**抛出异常**。
    3.  一个线程在等待时**超时**。
  * **详细解析：** 当一个线程因为上述原因之一离开屏障，它会“打破”屏障，并通知其他正在等待的线程。这样做的目的是防止部分线程继续等待一个永远无法满足的条件，从而避免死锁。所有被唤醒的线程都会抛出 `BrokenBarrierException`，表明屏障已经失效。

**4. `CyclicBarrier` 的屏障任务是由哪个线程执行的？**

  * **标准答案：** 由最后一个到达屏障点的线程执行。
  * **详细解析：** 在 `dowait()` 方法的源码中，当 `index` 变为 0 时（即最后一个线程到达），它会检查 `barrierCommand` 是否存在，如果存在则由当前线程（即最后一个线程）来执行。

-----

💡 五、口诀 + 表格/图示辅助记忆

**`CyclicBarrier` 口诀**

> **同起跑，共进退，屏障可重用。**
> **最后一个，开闸门，继续下一程。**

**`CyclicBarrier` 和 `CountDownLatch` 对比表**

| 特性 | CyclicBarrier | CountDownLatch |
| :--- | :--- | :--- |
| **功能** | 线程相互等待，达到同步点后继续 | 一个或多个线程等待所有前置任务完成 |
| **计数器** | 每次使用后重置 | 一次性，用完即销毁 |
| **适用场景** | 多线程分阶段并行计算 | 启动主任务前，等待所有子任务完成 |
| **核心实现** | `ReentrantLock` + `Condition` | AQS 共享模式 |
| **线程阻塞** | 所有线程在屏障点阻塞 | 只有 `await` 线程阻塞 |

-----

🎁 六、建议 + 误区提醒

**误区提醒**

1.  **忘记屏障会损坏：** 很多开发者不知道 `CyclicBarrier` 会因为中断或超时而失效。在实际应用中，必须正确处理 `BrokenBarrierException`，否则可能会导致程序逻辑错误。
2.  **在屏障任务中执行耗时操作：** 屏障任务由最后一个到达的线程执行，这个任务会阻塞所有等待的线程。如果在屏障任务中执行耗时操作，将大大降低系统的吞吐量，失去使用 `CyclicBarrier` 的意义。

**使用建议**

1.  **使用 `try-catch` 捕获异常：** 在调用 `await()` 时，务必捕获 `InterruptedException` 和 `BrokenBarrierException`，并妥善处理，如中断当前线程或进行日志记录。
2.  **合理设置屏障任务：** 屏障任务应该是轻量级的，例如打印日志、发送通知等，不应包含耗时的 I/O 或复杂的计算。
3.  **超时机制：** 像 `CountDownLatch` 一样，`CyclicBarrier` 的 `await()` 方法也提供了带超时的版本 `await(long timeout, TimeUnit unit)`。在实际应用中，这可以有效防止因意外情况导致的永久阻塞。