📘 Java 面试复习笔记：wait/notify 与 Condition 线程通信机制

✅ 一、概念简介

- **是什么**：
  - **wait/notify**：基于 `Object` 监视器（Monitor）的原生线程通信机制，必须与 `synchronized` 同用。
  - **Condition**：基于 `Lock`（通常是 `ReentrantLock`）的条件变量，属于 JUC（AQS）框架，支持多条件队列。
- **典型场景**：生产者-消费者、多阶段有序执行（A→B→C）、资源等待/通知、背压。
- **优缺点**：
  - **wait/notify**：简单轻量；只有一个等待队列、无法精确唤醒；容易误用（未加锁、用 if 代替 while）。
  - **Condition**：多条件队列、精确唤醒、更强的 API（支持可中断/超时等待、可公平锁）；API 更复杂，需要显式加锁/解锁。
- **对比**：

| 维度 | wait/notify | Condition |
|---|---|---|
| 归属 | `Object` | `java.util.concurrent.locks` |
| 依赖 | `synchronized` | `Lock`/`ReentrantLock` |
| 等待队列 | 1 个（对象 Wait Set） | 多个（每个 Condition 1 个） |
| 唤醒 | `notify`/`notifyAll` | `signal`/`signalAll`（可精确到条件） |
| 虚假唤醒 | 可能发生，需 `while` 重检 | 同样可能发生，仍需 `while` 重检 |
| 中断与超时 | `wait(long)`/`wait(long,int)` | `await`/`awaitNanos`/`awaitUntil`/`awaitUninterruptibly` |
| 公平性 | 不提供 | 取决于锁（如 `ReentrantLock(true)`） |


🔹 二、底层原理 + 源码分析

- **Monitor 与 Wait Set（wait/notify）**：
  - 每个对象有一个隐式监视器和一个 Wait Set。
  - 线程进入 `synchronized` 获得对象锁（进入临界区）；`wait()`：释放锁并入 Wait Set；`notify`：从 Wait Set 唤醒一个进入 Entry Set 重新竞争锁；`notifyAll`：唤醒全部。
  - 唤醒后不是立刻运行，仍需重新竞争锁。

- **AQS 条件队列（Condition）**：
  - 每个 `Condition` 维护一条“条件等待队列”（单向链表：`firstWaiter/lastWaiter`）。
  - `await`：将当前线程包装为 `Node` 加入条件队列，释放锁；被 `signal` 转移到 AQS 同步队列后，再次竞争锁；成功获取后继续执行。

- **关键字段/设计意图**：
  - `ReentrantLock` 用 `state` 记录重入次数；`ConditionObject` 用 `Node.nextWaiter` 串起等待者。
  - 采用“条件队列 → 同步队列”的两段式转移，避免直接与持锁线程竞争导致的时序混乱。

- **JDK 8 关键逻辑（节选，加注释）**：

```java
// AQS 的 ConditionObject#await（JDK8 关键路径，省略无关细节）
public final void await() throws InterruptedException {          // 可中断等待
    if (Thread.interrupted())                                    // 若已中断，立即响应
        throw new InterruptedException();
    Node node = addConditionWaiter();                            // 1) 当前线程入“条件队列”
    int savedState = fullyRelease(node);                         // 2) 释放当前锁（含重入层数）
    int interruptMode = 0;                                       // 3) 中断模式标记
    while (!isOnSyncQueue(node)) {                               // 4) 不在“同步队列”就阻塞
        LockSupport.park(this);                                  //    阻塞等待 signal/中断/超时
        if ((interruptMode = checkInterruptWhileWaiting(node))   // 5) 被唤醒后，检查是否中断
                != 0)
            break;                                               //    处理中断退出等待循环
    }
    if (acquireQueued(node, savedState)                          // 6) 转入同步队列后，按 AQS 规则
            && interruptMode != THROW_IE)                        //    抢回原先的锁状态
        interruptMode = REINTERRUPT;                             //    补处理中断语义
    if (node.nextWaiter != null)                                 // 7) 清理条件队列中已取消节点
        unlinkCancelledWaiters();
    if (interruptMode != 0)                                      // 8) 统一报告/恢复中断语义
        reportInterruptAfterWait(interruptMode);
}
```

```java
// AQS 的 ConditionObject#signal（JDK8 关键路径，省略无关细节）
public final void signal() {
    if (!isHeldByCurrentThread())                                 // 1) 必须由持锁线程调用
        throw new IllegalMonitorStateException();
    Node first = firstWaiter;                                     // 2) 取条件队列头
    if (first != null)
        doSignal(first);                                          // 3) 转移到“同步队列”等待抢锁
}
```

👉 结论：`await/signal` 与 `wait/notify` 的本质差异在于“等待队列的归属与转移机制”以及“API 维度的可控性”，两者都可能出现“虚假唤醒”，务必使用 `while` 重检条件。


✅ 三、常用方式 + 代码示例

1) wait/notify 版生产者-消费者（单条件）

```java
public class WaitNotifyPC {
    private final Object lock = new Object();                 // 作为监视器的锁对象
    private boolean hasData = false;                          // 共享状态：是否有数据
    private String data;                                      // 共享数据

    // 生产者
    public void produce(String newData) throws InterruptedException {
        synchronized (lock) {                                 // 必须持有锁
            while (hasData) {                                 // while 防虚假唤醒
                lock.wait();                                  // 释放锁，进入 Wait Set
            }
            data = newData;                                   // 写入数据
            hasData = true;                                   // 修改状态
            lock.notify();                                    // 唤醒一个等待者（可能是消费者）
        }                                                     // 退出同步块，自动释放锁
    }

    // 消费者
    public String consume() throws InterruptedException {
        synchronized (lock) {                                 // 必须持有锁
            while (!hasData) {                                // while 防虚假唤醒
                lock.wait();                                  // 释放锁，进入 Wait Set
            }
            String result = data;                             // 读取数据
            hasData = false;                                  // 修改状态
            data = null;                                      // 清理引用（帮助 GC）
            lock.notify();                                    // 唤醒可能等待的生产者
            return result;                                    // 返回结果
        }
    }
}
```

要点：
- 必须在 `synchronized` 内调用 `wait/notify`，否则抛 `IllegalMonitorStateException`。
- 使用 `while` 重检条件，避免虚假唤醒与竞态条件。
- `notify` 可能唤醒错误角色，多角色时常用 `notifyAll` 配合条件判断。

2) Condition 版有界缓冲区（双条件：非空/非满）

```java
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConditionBoundedBuffer<E> {
    private final Lock lock = new ReentrantLock();            // 显式锁，便于精细控制
    private final Condition notEmpty = lock.newCondition();   // 条件：队列非空
    private final Condition notFull  = lock.newCondition();   // 条件：队列未满

    private final Object[] items;                             // 环形数组作为缓冲
    private int putIdx = 0, takeIdx = 0, count = 0;           // 写索引、读索引、元素数

    public ConditionBoundedBuffer(int capacity) {             // 构造时指定容量
        if (capacity <= 0) throw new IllegalArgumentException("capacity");
        this.items = new Object[capacity];
    }

    public void put(E e) throws InterruptedException {        // 生产
        lock.lock();                                          // 加锁（可响应中断）
        try {
            while (count == items.length) {                   // 满则等待
                notFull.await();                              // await 会释放锁并阻塞
            }
            items[putIdx] = e;                                // 写入元素
            putIdx = (putIdx + 1) % items.length;             // 环形前进
            count++;                                          // 元素数+1
            notEmpty.signal();                                // 精确唤醒一个消费者
        } finally {
            lock.unlock();                                    // 确保释放锁
        }
    }

    @SuppressWarnings("unchecked")
    public E take() throws InterruptedException {              // 消费
        lock.lock();                                          // 加锁
        try {
            while (count == 0) {                              // 空则等待
                notEmpty.await();                             // await 会释放锁并阻塞
            }
            E e = (E) items[takeIdx];                         // 读取元素
            items[takeIdx] = null;                            // 助 GC
            takeIdx = (takeIdx + 1) % items.length;           // 环形前进
            count--;                                          // 元素数-1
            notFull.signal();                                 // 精确唤醒一个生产者
            return e;                                         // 返回结果
        } finally {
            lock.unlock();                                    // 确保释放锁
        }
    }
}
```

要点：
- 可将不同角色（生产者/消费者）放到不同的 `Condition`，减少无效唤醒。
- `signal` 精确唤醒匹配条件的等待线程；多角色/复杂条件时优势明显。
- `ReentrantLock(true)` 可选公平策略，减小饥饿但吞吐略降。

3) 多阶段顺序执行（A→B→C 循环）

```java
import java.util.concurrent.locks.*;

public class SequenceABC {
    private final Lock lock = new ReentrantLock();            // 重入锁
    private final Condition ca = lock.newCondition();         // A 的条件
    private final Condition cb = lock.newCondition();         // B 的条件
    private final Condition cc = lock.newCondition();         // C 的条件
    private int state = 1;                                    // 1 表示该 A，2 表示该 B，3 表示该 C

    public void printA() throws InterruptedException {
        lock.lock();
        try {
            while (state != 1) ca.await();                    // 不是我就等
            System.out.println("A");                         // 打印 A
            state = 2;                                        // 轮到 B
            cb.signal();                                      // 精确唤醒 B
        } finally { lock.unlock(); }
    }

    public void printB() throws InterruptedException {
        lock.lock();
        try {
            while (state != 2) cb.await();                    // 不是我就等
            System.out.println("B");                         // 打印 B
            state = 3;                                        // 轮到 C
            cc.signal();                                      // 精确唤醒 C
        } finally { lock.unlock(); }
    }

    public void printC() throws InterruptedException {
        lock.lock();
        try {
            while (state != 3) cc.await();                    // 不是我就等
            System.out.println("C");                         // 打印 C
            state = 1;                                        // 回到 A
            ca.signal();                                      // 精确唤醒 A
        } finally { lock.unlock(); }
    }
}
```


🔍 四、真实面试高频问题 + 深度解析

1) 为什么 `wait/notify` 必须在 `synchronized` 内使用？
- **标准答案**：因为它们需要操作对象监视器，只有持有监视器的线程才能进入/退出 Wait Set。
- **解析**：`wait` 需要“释放锁”，`notify` 需要“从 Wait Set 唤醒并转入竞争队列”，两者都要求当前线程为监视器拥有者，否则抛 `IllegalMonitorStateException`。
- **陷阱**：误以为 `wait`/`notify` 是“全局”的，忽略“对象级别监视器”。

2) `wait()` 与 `sleep()` 区别？
- **标准答案**：`wait` 释放锁需被唤醒；`sleep` 不释放锁、到时自动苏醒。
- **解析**：用途不同（协作 vs 定时），状态不同（WAITING vs TIMED_WAITING），异常不同（`wait` 抛中断异常、`sleep` 也抛但不改变锁）。
- **陷阱**：`sleep` 不应用于线程间协作，不要企图用 `sleep` 代替 `wait`。

3) 什么是“虚假唤醒”？如何应对？
- **标准答案**：线程在未满足条件且未被明确唤醒时被唤醒。统一以 `while` 循环重检条件。
- **解析**：JVM/OS 层面允许 spurious wakeup；`wait` 与 `Condition.await` 都可能出现，必须 `while` 重检，不能用 `if`。
- **陷阱**：回答“只有 wait 会出现，Condition 不会”是错误的。

4) 何时用 `notify`，何时用 `notifyAll`？
- **标准答案**：单角色/单条件队列可用 `notify`；多角色/复杂条件用 `notifyAll` 配合条件判断。
- **解析**：`notify` 可能唤醒“错误角色”，导致再次睡眠；`notifyAll` 放大竞争但能保证“正确线程”得到机会。
- **陷阱**：不要一味推崇 `notifyAll`，要结合条件判断与性能考量。

5) `Condition` 相比 `wait/notify` 的核心优势？
- **标准答案**：多条件队列 + 精确唤醒 + 更丰富的等待语义（可中断/超时）+ 可选择公平锁。
- **解析**：将不同角色拆分到不同 `Condition`，显著减少“惊群效应”；支持 `awaitNanos/awaitUntil` 更易实现超时退避策略。
- **陷阱**：认为 `Condition` 天生“无虚假唤醒”，结论是错的。

6) 如何自己实现一个阻塞队列？
- **标准答案**：`ReentrantLock` + 两个 `Condition`（notEmpty/notFull）+ 环形数组。
- **解析**：满则 `await(notFull)`，空则 `await(notEmpty)`；put/take 成功后分别 `signal` 对侧；注意中断处理与 `finally` 解锁。
- **陷阱**：用 `if` 替代 `while`、遗漏 `finally` 解锁、在已取消节点上继续等待。

7) `await` 的中断与超时语义？
- **标准答案**：`await` 可中断；`awaitUninterruptibly` 不可中断；`awaitNanos/awaitUntil` 支持超时。
- **解析**：可结合业务超时、重试、补偿；中断策略要么“吃掉中断后重置标志”，要么“抛出并上抛处理”。
- **陷阱**：吞掉中断而不恢复标志，破坏上层的中断约定。

8) 公平锁对唤醒/调度的影响？
- **标准答案**：公平锁按队列顺序获取锁，降低饥饿，吞吐下降；非公平锁吞吐高但可能短期不公平。
- **解析**：`ReentrantLock(true)` 构造公平锁；与条件唤醒结合时，可提升整体可预测性。
- **陷阱**：误以为公平锁一定“更快”。

9) 为什么推荐 `signal` 精确唤醒替代 `notifyAll`？
- **标准答案**：减少无谓竞争与上下文切换，提升吞吐。
- **解析**：前提是将角色分离到不同 `Condition`；否则 `signal` 也可能唤错对象。
- **陷阱**：未区分条件就滥用 `signal`，出现“唤错了还得再睡”的抖动。

10) `wait(timeout)` 与 `awaitNanos` 的取舍？
- **标准答案**：`awaitNanos` 精度更高并返回剩余时间，便于循环剩余预算；`wait(timeout)` 粒度较粗。
- **解析**：在高性能场景，推荐纳秒级 API 实现“剩余超时预算”的循环等待。
- **陷阱**：忽视时钟回拨、平台定时精度，未做保护。


🎯 五、口诀 + 表格/图示辅助记忆

- **口诀（wait/notify）**：
  - “锁中等待，锁中唤醒，条件重检防假醒”。
- **口诀（Condition）**：
  - “Lock 来控权，Condition 分角色，signal 精唤醒”。
- **线程通信心法**：
  - “两队两转移：条件队列入、同步队列出；唤醒不等于运行，拿到锁才继续”。

对比速览：

| 维度 | wait/notify | Condition |
|---|---|---|
| 是否必须持有锁 | 是（对象监视器） | 是（Lock 持有者） |
| 等待释放 | 释放对象锁 | 释放 ReentrantLock |
| 被唤醒后 | 回到 EntrySet 抢锁 | 转入同步队列抢锁 |
| 多条件支持 | 否 | 是（多 Condition） |
| API 丰富度 | 基础 | 丰富（中断/超时/不可中断） |

简图（文本版）：

```
wait/notify：
线程A(持锁) --wait--> 进入WaitSet(释放锁) --notify--> 进入EntrySet(阻塞) --获取锁--> 继续

Condition：
线程A(持锁) --await--> 条件队列(释放锁) --signal--> 同步队列(阻塞) --获取锁--> 继续
```


💡 六、Bonus：建议 + 误区提醒

- **最佳实践**：
  - 始终用 `while` 而非 `if` 检查等待条件（两边都适用）。
  - 解锁放在 `finally`；等待前后正确维护共享状态。
  - 多角色/多条件用多个 `Condition`，配合精确 `signal` 减少“惊群”。
  - 需要可预期调度时考虑公平锁；吞吐优先时用非公平锁。
  - 复杂协作优先使用 JUC 现成组件：`BlockingQueue`、`Semaphore`、`CountDownLatch`、`CyclicBarrier`、`Phaser`。

- **易错点**：
  - 未持锁调用 `wait/notify` 或 `signal`（抛异常或逻辑错误）。
  - 用 `if` 防护条件（虚假唤醒或被唤醒后条件已变）。
  - `notify` 唤醒错误角色导致抖动；不区分条件就滥用 `signal`。
  - 吞掉中断不恢复标志或不向上抛，破坏线程中断协议。
  - 认为 `Condition` 不会虚假唤醒（错误）。

- **替代方案建议**：
  - 生产者-消费者优先 `BlockingQueue`（如 `ArrayBlockingQueue`）。
  - 链式异步协作可用 `CompletableFuture`（更少显式同步）。
  - 等待多个并行任务完成用 `CountDownLatch`；阶段性屏障用 `CyclicBarrier/Phaser`。

—— 完 ——


