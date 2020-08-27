package priv.captain.designpattern.factory;
/**
 * @ClassName:CarFactory   
 * @Description:汽车工厂，根据传入类型生成对应汽车   
 * @author:CNT-Captain 
 * @date:2020年8月27日 下午3:34:43    
 * @Copyright:2020 https://gitee.com/CNT-Captain Inc. All rights reserved.
 */
public class CarFactory {

  public CarFactoryInterface getInstance(String type) {
		CarFactoryInterface instance = null;
		switch (type) {
		case "smallCar":
			instance = new SmallCar();
			break;
		case "bigCar":
			instance = new BigCar();
			break;
		default:
			break;
		}
		return instance;
	}
}
