好的，让我们来系统地复习 `CompletableFuture`。

📘 Java 面试复习笔记：CompletableFuture

-----

### ✅ 一、概念简介

**是什么？**

`CompletableFuture` 是 Java 8 引入的一个强大的异步编程工具，它实现了 `Future` 和 `CompletionStage` 接口。它代表一个**可编程的异步计算结果**，你可以手动完成这个结果，或者指定当结果完成后如何处理。

可以把它想象成一个**可编程的“占位符”**：

  - 你把一个耗时任务（比如网络请求、数据库查询）交给后台线程去执行。
  - 你立刻拿到一个 `CompletableFuture` 对象，这个对象就像一个承诺书，它承诺将来会给你一个结果。
  - 最重要的是，你可以在这个承诺书上写下**回调**：“当结果出来后，帮我用它做这件事”或“如果出错了，帮我用这个备用值”。

**为什么用？**

🎯 **核心目的：** 解决传统 `Future` 的痛点，实现非阻塞的、可编排的、反应式编程。

  * **非阻塞：** 告别 `Future.get()` 带来的线程阻塞。你可以通过回调函数指定任务完成后的操作，主线程无需原地等待，可以继续执行其他任务，从而提高系统的吞吐量。
  * **链式编程：** 它可以轻松地将多个异步任务串联或组合起来，形成一个流畅的任务流。一个任务的输出可以作为下一个任务的输入，解决了传统 `Future` 难以编排的困境。
  * **异常处理：** 提供了更加优雅的异常处理机制，可以像 `try-catch` 一样在异步任务链中捕获和处理异常，而不是等到 `get()` 时才抛出。
  * **异步化：** 帮助你更好地利用多核 CPU，将 I/O 密集型任务（如远程调用）从主线程中剥离，交给后台线程池处理。

-----

### 🔹 二、底层原理 + 源码分析

`CompletableFuture` 的实现不依赖 `AQS`，而是基于**回调**和\*\*任务窃取（Work-Stealing）\*\*线程池。

**核心思想**

`CompletableFuture` 的设计核心是：**将任务的依赖关系封装成一个链式结构**。

  - **任务节点：** 每个 `thenApply`, `thenAccept` 等方法都会创建一个新的 `CompletableFuture` 实例，它依赖于前一个实例的结果。
  - **回调链：** 这种依赖关系形成一个链表。当一个任务完成时，它会检查是否有依赖它的下游任务，如果有，就将其提交到线程池执行。
  - **线程池：** `CompletableFuture` 的异步方法（如 `supplyAsync`）如果没有指定线程池，默认会使用 **`ForkJoinPool.commonPool()`**。这是一个高效的、支持任务窃取的线程池，非常适合处理计算密集型任务。

**源码解析**

`CompletableFuture` 的核心逻辑隐藏在其内部类 `UniCompletion` 和 `BiCompletion` 中。

当你调用 `future.thenApply(fn)` 时，底层会执行以下操作：

1.  创建一个新的 `CompletableFuture` 实例。
2.  将 `fn` 包装成一个 `UniApply` 任务，并注册到 `future` 的一个内部链表（`stack`）中。
3.  `thenApply` 方法会立即返回新的 `CompletableFuture` 实例，不会阻塞。

当 `future` 完成时（例如，调用 `complete()`），它会：

1.  遍历其内部的 `stack`，找到所有依赖它的任务。
2.  将这些任务提交给线程池执行。
3.  例如，`UniApply` 任务被执行时，它会从 `future` 中获取结果，应用 `fn` 函数，然后将新结果设置到新的 `CompletableFuture` 中，从而触发新实例的下游任务。

-----

### ✅ 三、常用方式 + 代码示例

`CompletableFuture` 提供了丰富的 API，可以轻松实现各种异步任务编排。

**1. 基本用法**

```java
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class BasicCompletableFutureExample {

    public static void main(String[] args) throws Exception {
        // 使用自定义线程池，避免使用默认的 ForkJoinPool
        ExecutorService executor = Executors.newFixedThreadPool(5);
        
        // 1. supplyAsync: 异步执行一个有返回值的任务
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("任务在线程 " + Thread.currentThread().getName() + " 中执行");
            try {
                TimeUnit.SECONDS.sleep(2); // 模拟耗时操作
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Hello World";
        }, executor);

        // 2. thenApply: 链式调用，对上一个任务的结果进行转换
        // thenApply() 方法默认在同一个线程中执行
        CompletableFuture<Integer> resultFuture = future.thenApply(s -> {
            System.out.println("转换任务在线程 " + Thread.currentThread().getName() + " 中执行");
            return s.length();
        });

        // 3. thenAccept: 消费上一个任务的结果，无返回值
        // thenAccept() 也默认在同一个线程中执行
        resultFuture.thenAccept(length -> {
            System.out.println("结果消费任务在线程 " + Thread.currentThread().getName() + " 中执行");
            System.out.println("字符串长度是: " + length);
        });
        
        System.out.println("主线程继续执行，不会阻塞...");

        // 等待所有任务完成，以便主线程不退出
        TimeUnit.SECONDS.sleep(3);
        executor.shutdown();
    }
}
```

**2. 组合和异常处理**

```java
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class AdvancedCompletableFutureExample {
    public static void main(String[] args) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(5);

        // 任务1：模拟一个网络请求，返回用户ID
        CompletableFuture<String> userFuture = CompletableFuture.supplyAsync(() -> {
            System.out.println("任务1: 获取用户ID...");
            return "user-123";
        }, executor);

        // 任务2：模拟另一个网络请求，根据用户ID获取订单列表
        // thenCompose: 扁平化，将一个 CompletableFuture 链扁平化
        CompletableFuture<String> orderFuture = userFuture.thenCompose(userId -> {
            System.out.println("任务2: 根据用户ID [" + userId + "] 获取订单...");
            return CompletableFuture.supplyAsync(() -> "Order-A, Order-B", executor);
        });

        // 任务3：allOf：组合多个任务，等待所有任务都完成
        CompletableFuture<Void> allOfFuture = CompletableFuture.allOf(userFuture, orderFuture);

        // thenRun：当所有任务完成后，执行一个不依赖结果的任务
        allOfFuture.thenRun(() -> {
            System.out.println("所有任务都已完成!");
        });

        // 异常处理：在任务链中处理异常
        CompletableFuture<String> errorFuture = CompletableFuture.supplyAsync(() -> {
            System.out.println("异常任务开始...");
            throw new RuntimeException("Something went wrong!");
        }).exceptionally(ex -> { // 捕获异常并返回一个备用值
            System.err.println("捕获到异常: " + ex.getMessage());
            return "Default Value";
        });
        
        System.out.println("最终结果: " + errorFuture.join()); // join() 阻塞等待，但不会抛出 CompletionException
        executor.shutdown();
    }
}
```

-----

### 🔍 四、真实面试高频问题 + 深度解析

**1. `Future` 和 `CompletableFuture` 有什么区别？**

  * **标准答案：** `Future` 只能被动等待任务完成，`get()` 方法会阻塞线程，缺乏任务编排和异常处理能力。`CompletableFuture` 是对 `Future` 的增强，支持非阻塞式回调、链式编程、多任务组合和完善的异常处理。
  * **详细解析：** 这是最基础也是最重要的一个问题。你需要从**阻塞性**（`get` vs 回调）、**可组合性**（是否支持 `thenCombine`、`allOf` 等）、**异常处理**（`get` 抛出 `ExecutionException` vs `exceptionally`、`handle`）等多个维度进行对比。核心在于说明 `CompletableFuture` 解决了 `Future` 在高并发、异步场景下的主要痛点。

**2. `thenApply` 和 `thenCompose` 的区别？**

  * **标准答案：** `thenApply` 用于**同步转换**，其回调函数返回一个非 `CompletableFuture` 的值，并自动封装到新的 `CompletableFuture` 中。`thenCompose` 用于**异步转换**，其回调函数返回一个 `CompletableFuture`，并将其结果直接作为下一个任务的输入，避免了 `CompletableFuture` 的嵌套。
  * **详细解析：** 举例说明最清晰。`thenApply` 适用于简单的值转换，如 `string -> int`；`thenCompose` 适用于扁平化异步链，如 `获取用户ID -> 根据ID异步查询订单`。这个区别体现了 `CompletableFuture` 在处理复杂任务流时的强大能力。

**3. 如何优雅地处理 `CompletableFuture` 的异常？**

  * **标准答案：** 主要有三种方式：
    1.  **`exceptionally(fn)`：** 类似于 `try-catch`，当任务抛出异常时执行，并可以提供一个备用值作为结果。
    2.  **`handle(fn)`：** 类似于 `finally`，无论任务成功还是失败都会执行，它接收两个参数（结果和异常），可以根据异常来决定返回什么。
    3.  **`whenComplete(fn)`：** 类似于 `finally`，无论成功或失败都执行，但它无法修改结果，主要用于日志记录等副作用操作。
  * **陷阱警告：** 很多人只知道 `get()` 或 `join()` 会抛异常，但不知道可以在任务链中间提前处理。如果你不处理，异常会一直传播，直到 `join()` 时才抛出 `CompletionException`，这会让调试变得困难。

**4. `CompletableFuture` 使用的线程池是什么？有哪些注意事项？**

  * **标准答案：** 默认使用 `ForkJoinPool.commonPool()`，这是一个全局的、共享的线程池。
  * **详细解析：**
      * **优点：** 方便，无需手动创建线程池。
      * **缺点：** 1. 如果任务是阻塞的（如 I/O 密集型），可能会耗尽公共线程池的线程，影响其他不相关的任务。2. 线程数是固定的，无法根据任务类型调整。
      * **注意事项：** 对于生产环境，强烈建议使用带有 `Async` 后缀的方法（如 `supplyAsync(supplier, executor)`），并传入自定义的线程池。根据任务类型（I/O 或 CPU 密集型）来调整线程池参数。

-----

### 💡 五、口诀 + 表格/图示辅助记忆

**`CompletableFuture` 核心方法口诀**

> **一步到位 `Supply`，无返任务 `Run` 走。**
> **数据转换 `Apply`，异步扁平 `Compose`。**
> **任务消费 `Accept`，所有完成 `AllOf`。**
> **两任务合 `Combine`，最快一个 `AnyOf`。**
> **异常处理 `Except` `Handle`，最终善后 `Complete` 兜。**

**`CompletableFuture` vs `Future` 核心差异表**

| 特性 | `Future` | `CompletableFuture` |
| :--- | :--- | :--- |
| **异步模型** | 阻塞式 | 非阻塞式，基于回调 |
| **任务编排** | 不支持 | 支持链式、组合、并行编排 |
| **异常处理** | 只能在 `get()` 时被动捕获 | 支持链式异常处理，如 `exceptionally` |
| **结果完成** | 由线程池完成 | 可手动完成 (`complete`) |
| **适用场景** | 简单异步任务 | 复杂任务流，微服务编排 |

-----

### 🎁 六、建议 + 误区提醒

**误区提醒**

1.  **滥用 `join()` 和 `get()`：** `CompletableFuture` 的精髓在于非阻塞。如果你的代码里到处都是 `join()` 或 `get()`，说明你可能没有真正理解和利用它的优势。
2.  **不处理异常：** 异步任务中的异常很容易被“吞掉”。如果你在链式调用中没有使用 `exceptionally` 或 `handle`，异常会默默传播，直到你调用 `join()` 或 `get()` 时才以 `CompletionException` 形式抛出。
3.  **默认线程池风险：** 默认的 `ForkJoinPool.commonPool()` 并非万能。对于 I/O 密集型任务，它可能因线程阻塞而导致性能下降，甚至影响整个 JVM 的其他任务。

**使用建议**

1.  **优先使用 `CompletableFuture`：** 在任何需要异步编程的场景中，都应优先使用 `CompletableFuture` 而不是传统的 `FutureTask`。
2.  **自定义线程池：** 在生产环境中，为 `supplyAsync` 和 `runAsync` 等方法传入一个专门的线程池，以隔离任务、防止互相影响。
3.  **使用 `try-catch` 处理 `join()`：** `join()` 方法和 `get()` 类似，都是阻塞的。不同的是，`join()` 不会抛出受检查异常。在使用时，仍然需要用 `try-catch` 块来捕获 `CompletionException`。
4.  **清晰的命名和日志：** 异步任务流的调试相对复杂，在回调函数中添加清晰的日志，可以帮助你追踪任务的执行过程。