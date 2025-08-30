package priv.captain.spi;

import org.junit.Test;

import java.util.ServiceLoader;

/**
 * @description: spi测试
 * @author: yetiKnight
 * @since: 2024-11-12
 **/
public class SPITest {

    @Test
    public void testSearch(){
        // 惰性加载，只有遍历的时候才去实例化
        final ServiceLoader<Search> load = ServiceLoader.load(Search.class);
        for (Search search : load){
            search.searchDocuments("关键字").forEach(System.out::println);
        }
    }
}
