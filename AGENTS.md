# Java微服务架构开发指南

## 项目概述
这是一个基于Spring Boot的微服务架构项目，采用DDD（领域驱动设计）和CQRS模式。

## 技术栈
- **框架**: Spring Boot 3.x, Spring Cloud
- **数据库**: MySQL 8.0, Redis
- **消息队列**: RabbitMQ/Kafka
- **服务发现**: Nacos/Eureka
- **配置中心**: Nacos/Apollo
- **网关**: Spring Cloud Gateway
- **监控**: Prometheus + Grafana
- **链路追踪**: Zipkin/SkyWalking
- **容器化**: Docker + Kubernetes

## 项目结构规范

### 标准微服务模块结构
```
service-name/
├── src/main/java/priv/captain/
│   ├── controller/          # REST API控制器
│   ├── service/             # 业务逻辑层
│   │   ├── impl/           # 服务实现
│   │   └── dto/            # 数据传输对象
│   ├── repository/         # 数据访问层
│   ├── entity/             # 实体类
│   ├── config/             # 配置类
│   ├── exception/          # 异常处理
│   ├── util/               # 工具类
│   └── constant/           # 常量定义
├── src/main/resources/
│   ├── application.yml      # 应用配置
│   ├── application-dev.yml  # 开发环境配置
│   └── application-prod.yml # 生产环境配置
└── src/test/java/           # 测试代码
```

## 编码规范

### 命名规范
- **类名**: PascalCase (如: UserService, OrderController)
- **方法名**: camelCase (如: getUserById, createOrder)
- **变量名**: camelCase (如: userId, orderList)
- **常量**: UPPER_SNAKE_CASE (如: MAX_RETRY_COUNT)
- **包名**: 小写字母，点分隔 (如: priv.captain.service)

### 代码风格
- 使用4个空格缩进，不使用Tab
- 每行最大长度120个字符
- 类必须有JavaDoc注释
- 公共方法必须有JavaDoc注释
- 使用中文注释，便于团队理解

### 异常处理
- 自定义异常继承RuntimeException
- 异常类名以"Exception"结尾
- Service层抛出业务异常
- Controller层统一处理异常
- 使用有意义的异常信息

## 微服务特定规范

### Controller层规范
```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    
    private final UserService userService;
    
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        log.info("获取用户信息: {}", id);
        UserDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }
}
```

### Service层规范
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    
    public UserDTO getUserById(Long id) {
        log.debug("根据ID获取用户: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("用户不存在: " + id));
        return UserDTO.fromEntity(user);
    }
}
```

### Repository层规范
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE u.status = :status")
    List<User> findByStatus(@Param("status") UserStatus status);
}
```

## 数据库设计规范

### 表命名
- 使用下划线命名法 (如: user_info, order_detail)
- 主键统一使用id
- 创建时间字段: create_time
- 更新时间字段: update_time
- 软删除字段: deleted (0-未删除, 1-已删除)

### 字段命名
- 使用下划线命名法
- 避免使用数据库关键字
- 字段名要有意义，避免缩写

## API设计规范

### RESTful API设计
- 使用HTTP动词: GET, POST, PUT, DELETE
- URL使用名词，不使用动词
- 版本控制: /api/v1/
- 统一响应格式

### 统一响应格式
```java
@Data
@Builder
public class ApiResponse<T> {
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;
}
```

### 分页查询规范
```java
@Data
public class PageRequest {
    private Integer page = 1;
    private Integer size = 10;
    private String sortBy;
    private String sortOrder = "ASC";
}
```

## 配置管理

### 配置文件结构
```yaml
spring:
  application:
    name: user-service
  profiles:
    active: dev
  datasource:
    url: jdbc:mysql://localhost:3306/user_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  redis:
    host: ${REDIS_HOST}
    port: ${REDIS_PORT}
```

### 环境变量
- 敏感信息使用环境变量
- 不同环境使用不同的配置文件
- 使用Spring Cloud Config进行配置管理

## 日志规范

### 日志级别使用
- ERROR: 系统错误，需要立即处理
- WARN: 警告信息，可能存在问题
- INFO: 重要业务操作
- DEBUG: 调试信息，开发环境使用

### 日志格式
```java
log.info("用户登录成功: username={}, ip={}", username, ip);
log.error("用户登录失败: username={}, reason={}", username, reason, exception);
```

## 测试规范

### 单元测试
- 测试类名以"Test"结尾
- 测试方法名描述测试场景
- 使用@SpringBootTest进行集成测试
- 使用@MockBean模拟外部依赖

### 测试数据准备
```java
@BeforeEach
void setUp() {
    // 准备测试数据
}

@Test
void shouldCreateUserSuccessfully() {
    // 测试用户创建成功
}
```

## 安全规范

### 输入验证
- 所有输入参数必须进行校验
- 使用@Valid注解进行参数校验
- 敏感信息不暴露在API响应中

### 权限控制
- 使用Spring Security进行认证授权
- 实现适当的权限控制
- 使用JWT进行无状态认证

## 性能优化

### 数据库优化
- 使用连接池管理数据库连接
- 避免N+1查询问题
- 使用分页查询处理大数据量
- 合理使用索引

### 缓存策略
- 使用Redis进行缓存
- 合理设置缓存过期时间
- 实现缓存更新策略

### 异步处理
- 使用@Async进行异步处理
- 使用消息队列处理耗时操作
- 实现异步任务监控

## 监控和运维

### 健康检查
```java
@Component
public class CustomHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        // 实现健康检查逻辑
        return Health.up().build();
    }
}
```

### 指标监控
- 使用Micrometer收集指标
- 集成Prometheus进行监控
- 使用Grafana进行可视化

### 链路追踪
- 集成Zipkin或SkyWalking
- 记录关键业务操作的链路
- 实现分布式追踪

## 部署规范

### Docker化
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

### Kubernetes部署
- 使用ConfigMap管理配置
- 使用Secret管理敏感信息
- 实现健康检查和自动扩缩容

## 开发流程

### 代码提交规范
- 提交信息使用中文描述
- 使用feat、fix、docs、style、refactor、test、chore等前缀
- 每次提交只包含一个功能或修复

### 代码审查
- 提交前进行代码审查
- 检查代码质量和安全性
- 确保符合项目规范

## 常见问题解决

### 服务间通信
- 使用Feign进行服务间调用
- 实现熔断和降级机制
- 使用重试机制处理临时故障

### 数据一致性
- 使用分布式事务或最终一致性
- 实现补偿机制
- 使用事件驱动架构

### 性能调优
- 使用JVM参数调优
- 实现数据库连接池优化
- 使用缓存减少数据库压力

## 最佳实践

1. **单一职责原则**: 每个服务只负责一个业务领域
2. **服务自治**: 服务之间松耦合，独立部署
3. **数据隔离**: 每个服务拥有自己的数据库
4. **API版本管理**: 实现向后兼容的API版本控制
5. **监控告警**: 建立完善的监控和告警机制
6. **文档维护**: 及时更新API文档和架构文档
7. **安全第一**: 始终考虑安全性，实现多层防护
8. **性能优化**: 持续监控和优化系统性能
