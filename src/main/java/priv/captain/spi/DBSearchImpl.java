package priv.captain.spi;

import java.util.List;

/**
 * @description: 普通数据库搜索实现
 * @author: yetiKnight
 * @since: 2024-11-12
 **/
public class DBSearchImpl implements Search{
    @Override
    public List<String> searchDocuments(String keyword) {
        System.out.println("执行普通搜索");
        return List.of("db");
    }
}
