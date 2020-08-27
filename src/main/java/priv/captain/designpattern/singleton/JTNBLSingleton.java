package priv.captain.designpattern.singleton;
/**
 * @ClassName:JTNBLSingleton   
 * @Description:静态内部类方式（推荐）   
 * @author:CNT-Captain 
 * @date:2020年8月27日 下午2:51:48    
 * @Copyright:2020 https://gitee.com/CNT-Captain Inc. All rights reserved.
 */
public class JTNBLSingleton {

	/**
	 * 私有构造方法，防止外部new实例
	 */
    private JTNBLSingleton() {
    	
    }
    
    public JTNBLSingleton getInstance() {
    	return SingletonInstance.INSTANCE;
    }
    
    public static class SingletonInstance{
    	public static JTNBLSingleton INSTANCE = new JTNBLSingleton();
    }
}
