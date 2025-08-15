package priv.captain.thread;

import org.junit.Test;

import java.util.concurrent.*;

/**
 * CountDownLatch: 闭锁的使用，用于多线程协作
 * CyclicBarrier:可重复使用的栅栏
 * 目前发现：
 * CountDownLatch：主线程也会阻塞，直到CountDownLatch的线程执行完毕或超时。
 * CyclicBarrier：主线程不影响，会继续执行，只是CyclicBarrier的线程会相互等待。
 */
public class TestCountDownLatch {
	// 用于控制访问资源，还可以用于限流
   private final Semaphore semaphore = new Semaphore(5);


   @Test
   public void testSemaphore() throws InterruptedException {
	   try{
		   if (semaphore.tryAcquire(5,TimeUnit.SECONDS)){
			   System.out.println("获得访问资源许可！");
		   }
	   }finally {
		   semaphore.release();
	   }
   }


	   @Test
	   public void testCountDownLatch() {
		   // 定义线程任务个数
		   CountDownLatch latch = new CountDownLatch(3);
		   ExecutorService pool = Executors.newFixedThreadPool(3);
		   PriorityBlockingQueue que = new PriorityBlockingQueue();
		   for (int i = 0; i < 3; i++) {
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


	   @Test
	   public void testCyclicBarrier() {
		   // 创建一个CyclicBarrier，设置屏障点为3，表示需要3个线程到达屏障点才能继续
		   CyclicBarrier cyclicBarrier = new CyclicBarrier(3, () -> {
			   System.out.println("所有线程任务都已经执行完毕。");
		   });
		   ExecutorService pool = Executors.newFixedThreadPool(3);
		   for (int i = 0; i < 3; i++) {
			   pool.execute(()->{
				   System.out.println(Thread.currentThread().getId()+"开始执行");
				   try {
					   Thread.sleep(100);
				   } catch (InterruptedException e) {
					   e.printStackTrace();
				   }
				   System.out.println(Thread.currentThread().getId()+"已抵达栅栏");
				   try {
					   cyclicBarrier.await();// 等待其他线程到达屏障点
					   System.out.println(Thread.currentThread().getId()+"线程继续执行");
				   } catch (InterruptedException | BrokenBarrierException e) {
					   e.printStackTrace();
				   }
			   });
		   }
		   // 停止接收新任务
		   pool.shutdown();
		   // 确保已经运行的任务可以运行完毕
		   try {
			   if(!pool.awaitTermination(60, TimeUnit.SECONDS)){
				   pool.shutdownNow();
			   }
		   } catch (InterruptedException e) {
			   pool.shutdownNow();// 被异常中断了，则直接关闭
		   }
	   }
}
