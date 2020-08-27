package priv.captain.designpattern.factory;

public class BigCar implements CarFactoryInterface {

	@Override
	public String go() {
		return "大车行驶";
	}

}
