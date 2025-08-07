package priv.captain.juc;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @description: Synchronize关键字使用示例
 * @author: yetiKnight
 * @since: 2024-11-22
 **/
public class SynchronizeExample {


    // 实例锁
    public void func1() {
        synchronized (this) {
            for (int i = 0; i < 10; i++) {
                System.out.print(i + " ");
            }
        }
    }

    // 实例锁
    public synchronized void func2() {
        for (int i = 0; i < 10; i++) {
            System.out.print(i + " ");
        }
    }

    // 类锁
    public void func3() {
        synchronized (SynchronizeExample.class) {
            for (int i = 0; i < 10; i++) {
                System.out.print(i + " ");
            }
        }
    }


public static void main(String[] args) {
    // 实例锁：锁定实例对象 this，每个实例独立控制并发，实例之间不互相干扰
    // 类对象锁：锁定类对象 Class，类级别的资源共享，多个实例间共享资源

    // 创建一个固定大小的线程池，大小为2
    final ExecutorService executorService = Executors.newFixedThreadPool(2);

    // 创建两个SynchronizeExample的实例
    SynchronizeExample example1 = new SynchronizeExample();
    SynchronizeExample example2 = new SynchronizeExample();

    // 使用线程池执行两个实例的func1方法
    executorService.execute(example1::func1);
    executorService.execute(example2::func1);

    // 关闭线程池，不再接受新的任务，等待所有已提交的任务完成
    executorService.shutdown();
}

}
