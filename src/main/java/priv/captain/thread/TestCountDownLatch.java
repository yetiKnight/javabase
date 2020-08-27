package priv.captain.thread;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * CountDownLatch: 闭锁的使用，用于多线程协作。
 * CyclicBarrier:可重复使用的栅栏。
 * 目前发现：
 * CountDownLatch：主线程也会阻塞，直到CountDownLatch的线程执行完毕或超时。
 * CyclicBarrier：主线程不影响，会继续执行，只是CyclicBarrier的线程会相互等待。
 */
public class TestCountDownLatch {
	
	public static void main(String[] args) {
		countDownLatchMethod();
		//cyclicBarrierMethod();
	}
	
	public static void countDownLatchMethod() {
		// 定义线程任务个数
		CountDownLatch latch = new CountDownLatch(10);
		ExecutorService pool = Executors.newFixedThreadPool(10);
		for (int i = 0; i < 10; i++) {
			pool.execute(() -> {
				try {
					System.out.println(Thread.currentThread().getName() + "开始执行");
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					System.out.println(Thread.currentThread().getName() + "执行完毕");
					// 就算异常了计数一次
					latch.countDown();
				}
			});
		}
		try {
			// 有时间限制的等待
			latch.await(5, TimeUnit.SECONDS);
			if (latch.getCount() > 0) {
				System.out.println("已经超过了最大等待时间，主线程继续执行。");
			} else {
				System.out.println("所有线程执行完毕,主线程继续执行。");
			}
		} catch (InterruptedException e) {
			System.out.println("线程被打断");
		}
		pool.shutdown();
		System.out.println("执行完毕，主线程开始继续执行");
	}
	
	
	public static void cyclicBarrierMethod() {
		
		CyclicBarrier cyclicBarrier = new CyclicBarrier(5, () -> {
			System.out.println("所有线程任务都已经执行完毕。");
		});
		ExecutorService pool = Executors.newFixedThreadPool(10);
		for (int i = 0; i < 5; i++) {
			pool.execute(()->{
				System.out.println(Thread.currentThread().getName()+"开始执行");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out.println(Thread.currentThread().getName()+"已抵达栅栏");
				try {
					cyclicBarrier.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (BrokenBarrierException e) {
					e.printStackTrace();
				}
			});
		}
		pool.shutdown();
		System.out.println("主线程继续执行。。。。");
	}
}
