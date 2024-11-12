package priv.captain.spi;

import java.util.List;

/**
 * @description: es搜索实现
 * @author: yetiKnight
 * @since: 2024-11-12
 **/
public class ESSearchImpl implements Search{
    @Override
    public List<String> searchDocuments(String keyword) {
        System.out.println("执行es搜索");
        return List.of("es");
    }
}
