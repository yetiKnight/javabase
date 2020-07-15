package priv.captain.time;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import org.junit.jupiter.api.Test;

public class TimeDemo {

	@Test
	public void kk() {
		DateTimeFormatter df = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG);
		 LocalDate d= LocalDate.now();
		 System.out.println(d.format(df));
	}
}
