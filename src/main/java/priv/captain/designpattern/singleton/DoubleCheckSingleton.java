package priv.captain.designpattern.singleton;

/**
 * @ClassName:DoubleCheckSingleton   
 * @Description: 双重检查锁定
 * @author:CNT-Captain 
 * @date:2020年8月27日 上午11:44:40    
 * @Copyright:2020 https://gitee.com/CNT-Captain Inc. All rights reserved.
 */
public class DoubleCheckSingleton {

	/**
	 * 设置为volatile，主内存可见
	 */
	private volatile DoubleCheckSingleton doubleCheckSingleton;

	/**
	 * 私有构造方法，防止外部new实例
	 */
	private DoubleCheckSingleton() {
		
	}

	public DoubleCheckSingleton getInstance() {
		/**
		 * 1.双重检查保证多线程进入第一个if时，只有第一个进入synchronized的才会初始化，后续进入的判断实例是否为空，
		 * 这样保证了实例只被初始化一次，而后续不需要加锁获取。
		 * 2.可以延迟加载。
		 * 号外：由于Java 平台内存模型的无序写入，可能存在new DoubleCheckSingleton()之前，doubleCheckSingleton的引用已经是非空的了
		 * 在jvm里面的执行分为三步：
				  1.在堆内存开辟内存空间。
				  2.在堆内存中实例化SingleTon里面的各个参数。
				  3.把对象指向堆内存空间。
                    由于jvm存在乱序执行功能，所以可能在2还没执行时就先执行了3，
                     如果此时再被切换到线程B上，由于执行了3，INSTANCE 已经非空了，会被直接拿出来用，这样的话，就会出现异常。
		 */
		if (doubleCheckSingleton == null) {
			synchronized (DoubleCheckSingleton.class) {
				if(doubleCheckSingleton == null) {
					doubleCheckSingleton = new DoubleCheckSingleton();
				}
			}
		}
		return doubleCheckSingleton;
	}
}
