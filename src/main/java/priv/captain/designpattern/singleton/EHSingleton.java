package priv.captain.designpattern.singleton;

/**
 * @ClassName:EHSingleton   
 * @Description:饿汉模式  
 * <p>单例模式应用场景：1.系统中的重量级对象，创建比较耗费资源。
 *  2.需要频繁创建、销毁的对象，如工具对象、数据源等。
 * </p>
 * @author:CNT-Captain 
 * @date:2020年8月27日 上午11:21:29    
 * @Copyright:2020 https://gitee.com/CNT-Captain Inc. All rights reserved.
 */
public class EHSingleton {
	
	/**
	 * 1.类加载的时候初始化实例，基于类加载器的线程安全。
	 * 2.不能延迟加载，不管用不用都会初始化。
	 */
	private EHSingleton ehSingleton = new EHSingleton();
	
	/**
	 * 私有化构造方法，防止外部new实例
	 */
	private EHSingleton() {
	}
	
	public EHSingleton getInstance() {
		return ehSingleton;
	}
	

}
