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
//        实例锁	锁定实例对象 this	每个实例独立控制并发，实例之间不互相干扰
//        类对象锁	锁定类对象 Class	类级别的资源共享，多个实例间共享资源


        final ExecutorService executorService = Executors.newFixedThreadPool(2);

        SynchronizeExample example1 = new SynchronizeExample();
        SynchronizeExample example2 = new SynchronizeExample();

        executorService.execute(example1::func1);
        executorService.execute(example2::func1);

        executorService.shutdown();
    }
}
