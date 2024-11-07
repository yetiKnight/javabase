package priv.captain.time;

import org.junit.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class TimeDemo {

	@Test
	public void kk() {
		DateTimeFormatter df = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG);
		 LocalDate d= LocalDate.now();
		 System.out.println(d.format(df));
	}

	@Test
	public void streamList(){
		List<String> stringList = new ArrayList<>();
		stringList.add("1");
		stringList.add("2");
		stringList.add("3");

		System.out.println(stringList.stream().map(Integer::parseInt).collect(Collectors.toList()));
	}
}
