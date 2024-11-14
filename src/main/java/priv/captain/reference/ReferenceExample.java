package priv.captain.reference;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * @description: 弱引用示例
 * @author: yetiKnight
 * @since: 2024-11-14
 **/
public class ReferenceExample {

    public static void main(String[] args) throws InterruptedException {
        /*
         * 应用场景：
         * 缓存管理：当缓存对象被回收时，可以通过 ReferenceQueue 来清除与这些对象相关的资源（如数据库连接、文件句柄等）。
         * 对象生命周期管理：某些情况下，你需要监控对象何时被回收，ReferenceQueue 允许你在对象被垃圾回收后执行一些清理任务。
         * 内存敏感的应用：当系统内存不足时，可能需要触发某些清理操作，ReferenceQueue 提供了一个可以处理回收对象的机制。
         */

        // WeakHashMap 也常用于缓存

        // 引用队列，用于存储被垃圾回收器回收的对象的引用，当某个通过弱引用、软引用或虚引用引用的对象被回收时，这些对象的引用会被放入与之关联的 ReferenceQueue 中。
        ReferenceQueue<MyObject> queue = new ReferenceQueue<>();

        MyObject myObject = new MyObject();
        myObject.setName("小新");

        // 将对象和弱引用队列关联
        WeakReference<MyObject> weakRef = new WeakReference<>(myObject,queue);

        myObject = null;

        System.gc();

        Thread.sleep(1000);

        if(weakRef.isEnqueued()){
            System.out.println("弱引用对象已经被回收");
        }

        Reference<? extends MyObject> refFromQueue = queue.poll();
        if (refFromQueue != null) {
            System.out.println("引用已经被添加至队列中: " + refFromQueue);
            doSomeThing();
        }
    }

    public static void doSomeThing(){
        System.out.println("执行清理等其他操作");
    }
}

