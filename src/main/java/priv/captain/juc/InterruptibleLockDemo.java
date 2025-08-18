package priv.captain.juc;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 死锁模拟，灵活使用可中断锁：lock.lockInterruptibly();
 */
public class InterruptibleLockDemo {
    // 两把锁
    private static final ReentrantLock lock1 = new ReentrantLock();
    private static final ReentrantLock lock2 = new ReentrantLock();

    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> task(lock1, lock2), "线程A");
        Thread t2 = new Thread(() -> task(lock2, lock1), "线程B");

        t1.start();
        t2.start();

        // 给他们1秒钟时间制造死锁
        Thread.sleep(1000);

        // 发现死锁，打断一个线程（线程B）
        System.out.println("⚠️ 检测到死锁，准备打断线程B...");
        t2.interrupt();
    }

    public static void task(ReentrantLock first, ReentrantLock second) {
        try {
            // 第一次锁，可中断
            first.lockInterruptibly();
            System.out.println(Thread.currentThread().getName() + " 获得 " + first);

            Thread.sleep(500); // 模拟业务逻辑

            // 第二次锁，可中断
            second.lockInterruptibly();
            System.out.println(Thread.currentThread().getName() + " 获得 " + second);

        } catch (InterruptedException e) {
            System.out.println(Thread.currentThread().getName() + " 被中断，退出执行。");
        } finally {
            // 释放已获得的锁
            if (first.isHeldByCurrentThread()) {
                first.unlock();
            }
            if (second.isHeldByCurrentThread()) {
                second.unlock();
            }
        }
    }
}
