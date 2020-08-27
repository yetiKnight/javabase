package priv.captain.designpattern.factory;

public class SmallCar implements CarFactoryInterface {

	@Override
	public String go() {
		return "小车行驶";
	}

}
