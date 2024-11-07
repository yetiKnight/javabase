package priv.captain.list;


import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName:ListDemo   
 * @Description:对List的使用及原理分析
 * @author:CNT-Captain 
 * @date:2020年8月28日 下午4:59:53    
 * @Copyright:2020 https://gitee.com/CNT-Captain Inc. All rights reserved.
 */
public class ListDemo {

	@Test
	public void testList() {
		
		/*
		 * 1.底层基于数组的支持元素随机访问，其实现了标记接口RandomAccess，表示其支持随机访问。
		 * 2.LinkedList基于链表，其实现了Deque队列。
		 */
		/*
		 * 默认new空数组，，后第一放入时默认10续add都会检查是否超容量，若超容量执行grow(int minCapacity),
		 * 扩容规则：int newCapacity = oldCapacity + (oldCapacity >> 1);
		 */
		List<String> arrayList = new ArrayList<>();
		arrayList.add("666");
		arrayList.add(2,"5555");
		arrayList.forEach(System.out::println);

	}
}
