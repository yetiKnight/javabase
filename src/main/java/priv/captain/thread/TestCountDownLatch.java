package priv.captain.thread;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * CountDownLatch: 闭锁的使用，用于多线程协作。
 */
public class TestCountDownLatch {

	public static void main(String[] args) {
		// 定义线程任务个数
		CountDownLatch latch = new CountDownLatch(10);
		ExecutorService pool = Executors.newFixedThreadPool(10);
		for (int i = 0; i < 10; i++) {
			pool.execute(() -> {
				try {
					System.out.println("88888");
				} finally {
					//就算异常了计数一次
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
		System.out.println("执行完毕");
	}
}
