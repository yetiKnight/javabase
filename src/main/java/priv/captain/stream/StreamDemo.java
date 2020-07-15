package priv.captain.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class StreamDemo {

	public static void main(String[] args) {
		
		List<String> list = new ArrayList<String>();
		list.add("11");	
		list.add("12");	
		list.add("13");	
		list.add("14");
		/***
		 * 注意，流只能作一次有运算
		 * 筛选  filter等
		 * 映射 map(Function c)
		 * 排序sorted(Comparator comp)
		 * 查找/匹配 anyMatch
		 * stream.collect(Collectors.toList());
		 * 收集》》》Collectors类提供静态方法将流转换成list Set等
		 */
		//获取一个并行流list.parallelStream(),顺序流list.stream()
		Stream<String> stream = list.parallelStream();
		
		//filter 过滤数据,limit 限制数量,forEach 遍历数据 
		stream.filter((p) ->p.equals("11")).limit(2).forEach(System.out::println);
	}
}
