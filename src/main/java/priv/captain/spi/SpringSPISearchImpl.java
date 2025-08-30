package priv.captain.spi;

import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;


/**
 * @description: Spring SPI 实现类支持的写法示例
 * @author: yetiKnight
 * @since: 2025-08-30
 */
// 通过Order控制加载顺序，值越小，优先级越高
@Order(1)
// 加载条件：只有当 application.properties 里配置了 hello.custom.enabled=true 时，才会生效
@ConditionalOnProperty(name = "hello.custom.enabled", havingValue = "true", matchIfMissing = false)
public class SpringSPISearchImpl implements Search {
    @Override
    public List<String> searchDocuments(String keyword) {
        System.out.println("执行Spring SPI搜索");
        return List.of("spring Search");
    }
}
