package priv.captain.juc;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 公平锁和非公平锁测试代码示例
 */
public class ReentrantLock1 {

    // 公平锁 严格按照等待时间的先后顺序取锁
    private static ReentrantLock gpLock = new ReentrantLock(true);

    // 非公平锁 新来的线程可以插队，吞吐量高
    private static ReentrantLock fgpLock = new ReentrantLock();

    public static void main(String[] args) {
        System.out.println("公平锁测试");
        testLock(gpLock);

        // System.out.println("非公平锁测试");
        // testLock(fgpLock);
    }

    public static void testLock(ReentrantLock lock) {
        Runnable task = () -> {
            for (int i = 0; i < 3; i++) {
                // 使用 lock() 方法，会阻塞直到获取锁
                lock.lock();
                try {
                    System.out.println(Thread.currentThread().getName() + "获取了锁");
                    // 模拟一些工作
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    lock.unlock();
                }
            }
        };

        for (int i = 0; i < 3; i++) {
            new Thread(task).start();
        }
    }

}