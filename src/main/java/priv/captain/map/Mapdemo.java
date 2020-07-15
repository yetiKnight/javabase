package priv.captain.map;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class Mapdemo {

	private static final ReentrantLock lock = new ReentrantLock();
	
	private static Map<String, Object> map = new ConcurrentHashMap<>();
	
	public static void main(String[] args) throws InterruptedException {
		map.put("001", "第一次插入的值");
		System.out.println("get值5555="+map.get("001"));
	   ExecutorService sv = Executors.newCachedThreadPool();
	   for(int i =0;i<10;i++) {
		   sv.execute(()->{
			   kk();
		   });
	   }
	   sv.shutdown();
	   sv.awaitTermination(1, TimeUnit.SECONDS);
	    System.out.println("最终值="+map.get("001"));
	}
	
	private static void kk() {
		String k1 = "001";
		try {
			if(lock.tryLock(100,TimeUnit.SECONDS)) {
				System.out.println(Thread.currentThread().getName()+"拿到锁="+System.nanoTime());
				try {
					map.compute(k1, (k, v) -> {
						String op = "1111";
						if (v.equals("第一次插入的值")) {
							op = "88888";
						}
						return op;
					});
					System.out.println(System.nanoTime()+"get值="+map.get(k1));
				} finally {
					lock.unlock();
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	/*
	 * Object v1= map.computeIfAbsent(k1, K->{ String kk ="计算哈哈哈"; return kk; });
	 */
}
