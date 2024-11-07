package priv.captain.queque;


import org.junit.Test;

import java.util.concurrent.*;

public class BlockQueque {

    @Test
    public void testQueque() throws InterruptedException {
            String exitFlag = "EXIT";
            BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);
            ExecutorService executorService = Executors.newFixedThreadPool(8);
            CountDownLatch latch = new CountDownLatch(3);

            for(int i =0;i<3;i++){
                executorService.execute(()->{
                    while (true){
                        try {
                            String message = queue.take();
                            if(message.equals(exitFlag)){
                                System.out.println("消费者退出"+Thread.currentThread().getName());
                                latch.countDown();
                                queue.put(exitFlag);
                                break;
                            }
                        } catch (Exception e) {
                            System.out.println("消费异常");
                        }
                    }
                });
            }

            for (int j =1;j<=10;j++){
                int finalJ = j;
                executorService.execute(()->{
                    try {
                        Thread.sleep(1000);
                        queue.put("数据"+ finalJ);
                        System.out.println("生产："+finalJ);
                        if(finalJ ==10){
                            System.out.println("放入毒药！");
                            queue.put(exitFlag);
                        }
                    } catch (InterruptedException e) {
                        System.out.println("生产异常");
                    }
                });
            }

            latch.await();
            System.out.println("任务完成！");
        }
}
