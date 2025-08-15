package priv.captain.juc;

import java.util.concurrent.atomic.AtomicStampedReference;


/** 
 * CAS ABA问题解决
*/
public class AtomicStampedReferenceDemo {
    

    private static AtomicStampedReference<Integer> asRef = new AtomicStampedReference<>(100, 1);


    public static void main(String[] args) throws InterruptedException {
        
        // 模拟ABA
        Runnable task1 = ()->{
            int stamp = asRef.getStamp();
            System.out.println("线程1初始版本："+ stamp);

            // 复现ABA问题
            asRef.compareAndSet(100,101, stamp, stamp+1);
            asRef.compareAndSet(101, 100, stamp+1, stamp+2);
        };

        // 基于预期值修改
        Runnable task2 = ()->{
            int stamp = asRef.getStamp();
            int value = asRef.getReference();
            System.out.println("线程2 初始值="+value+",初始版本="+stamp);

            // 确保完成ABA
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            boolean success = asRef.compareAndSet(100, 101, stamp, stamp+1);
            System.out.println("CAS结果："+success);
        };

        Thread t1 = new Thread(task1);
        Thread t2 = new Thread(task2);
        t1.start();
        t2.start();

        t1.join();
        t2.join();
        System.out.println("asRef 最新值："+asRef.getReference());
    }


}
