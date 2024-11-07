package priv.captain.designpattern.factory;

import org.mapstruct.Mapper;

@Mapper
public class BigCar implements CarFactoryInterface {

	@Override
	public String go() {
		return "大车行驶";
	}

}
