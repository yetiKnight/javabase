package priv.captain.locks;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class FirstLock implements Runnable{

	private final ReentrantLock lock = new ReentrantLock();
	
	private Condition condition1 = lock.newCondition(); 

	private  Map<String, Long> map = new HashMap<>();

	private static final long maxMs = 30 * 60 * 1000;
	
	private String k;
	
	public FirstLock(String k) {
		this.k = k;
	}

	public boolean canExecute(String k) {
		boolean cute = false;
		if (k == null) {
			System.out.println("key为null");
			return cute;
		}

		if (!map.containsKey(k)) {
			System.out.println("key不存在");
			map.put(k, System.currentTimeMillis());
			return true;
		}

		if (lock.tryLock()) {
			try {
				System.out.println(Thread.currentThread().getName()+"获得了锁,sleep1000ms");
				Thread.sleep(1000);
				long beforeTime = map.get(k);
				long nowTime = System.currentTimeMillis();
				if (nowTime - beforeTime > maxMs) {
					cute = true;
					map.put(k, nowTime);
				}
				System.out.println("执行完毕");
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				lock.unlock();
				System.out.println("释放了锁");
			}
		}
		return cute;
	}
	@Override
	public void run() {
		canExecute(k);
	}
	
	public static void main(String[] args) {
		FirstLock k1 = new FirstLock("k1");
		FirstLock k2 = new FirstLock("k1");
		ExecutorService pool = Executors.newFixedThreadPool(5);
		pool.execute(k1);
		pool.execute(k2);

		Future<String> kk = pool.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return "传统方式执行";
			}
		});
		
		Future<String> kkk = pool.submit(()->{return "lamda方式执行";});
		try {
			System.out.println(kk.get());
			System.out.println(kkk.get());
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		pool.shutdown();
	}
}
