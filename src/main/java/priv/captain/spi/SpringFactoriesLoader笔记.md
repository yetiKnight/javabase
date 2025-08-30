### 📘 Spring 复习笔记：SpringFactoriesLoader

### ✅ 一、概念简介

**`SpringFactoriesLoader` 是什么？**

`SpringFactoriesLoader` 是 Spring 框架内部提供的一种核心机制，它用于从 classpath 下的 `META-INF/spring.factories` 文件中加载配置类、工厂接口等。它在 Spring Boot 的**自动化配置**和 \*\*SPI（服务提供者接口）\*\*实现中扮演着至关重要的角色。

可以把 `SpringFactoriesLoader` 想象成一个\*\*“寻宝图”\*\*：

  - **`META-INF/spring.factories` 文件**：就是这张寻宝图，它告诉 Spring 在哪里可以找到各种“宝藏”（即配置类、事件监听器、自动化配置类等）。
  - **`SpringFactoriesLoader` 类**：就是根据这张图去寻找宝藏的“探险家”。

**为什么用？**

🎯 **核心目的：** 实现 Spring Boot 的**自动配置**和**扩展点**，降低模块间的耦合。

  * **解耦**：允许第三方库在不修改 Spring 框架核心代码的情况下，提供自己的配置和实现。
  * **自动化**：Spring Boot 能够根据 classpath 下的 `spring.factories` 文件，自动加载并应用所需的配置，无需开发者手动配置。
  * **可扩展**：任何模块都可以通过创建 `spring.factories` 文件来向 Spring Boot 提供自己的扩展能力。

-----

### 🔹 二、核心原理

`SpringFactoriesLoader` 的核心原理可以概括为：**`ClassLoader` + `META-INF/spring.factories` + 反射**。

#### 1\. 工作流程

1.  **查找文件**：`SpringFactoriesLoader` 通过 `ClassLoader` 的 `getResources("META-INF/spring.factories")` 方法，在当前 classpath 下的所有 jar 包中查找名为 `spring.factories` 的文件。
2.  **解析内容**：读取并解析每个 `spring.factories` 文件。这个文件是一个简单的 **`key-value` 格式**，其中 `key` 是接口或抽象类的全限定名，`value` 是一个逗号分隔的实现类的全限定名。
3.  **合并和去重**：将所有文件中找到的同名 `key` 对应的 `value` 进行合并和去重。
4.  **反射实例化**：使用反射机制，实例化所有找到的实现类，并将它们返回给调用方。

#### 2\. 文件格式示例

```properties
# 自动化配置
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.example.MyAutoConfiguration

# 应用启动监听器
org.springframework.context.ApplicationListener=\
com.example.MyApplicationListener1,\
com.example.MyApplicationListener2

# 引导程序监听器
org.springframework.boot.SpringApplicationRunListener=\
com.example.MySpringApplicationRunListener
```

在这个例子中：

  * `EnableAutoConfiguration` 是 `key`，它告诉 Spring Boot 哪些类是自动化配置类。
  * `ApplicationListener` 是 `key`，它告诉 Spring 哪些类是应用事件监听器。

-----

### 🔹 三、典型应用场景

`SpringFactoriesLoader` 在 Spring Boot 中有非常广泛的应用。

#### 1\. 自动化配置（Auto-Configuration）

这是 `SpringFactoriesLoader` 最重要的应用。每个 Spring Boot Starter 模块（如 `spring-boot-starter-web`）都在其 jar 包中包含一个 `spring.factories` 文件，文件内容指定了该模块的自动化配置类。

```properties
# spring-boot-autoconfigure/META-INF/spring.factories
# ...
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration,\
org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration,\
# ... and many more
```

当应用启动时，`SpringFactoriesLoader` 会加载这些配置类，并根据条件（如 `@ConditionalOnClass`）来决定是否将其应用到 Spring 容器中。

#### 2\. 事件监听器（Event Listeners）

Spring Boot 通过 `SpringFactoriesLoader` 查找和注册**应用启动监听器** (`SpringApplicationRunListener`) 和**普通应用监听器** (`ApplicationListener`)，从而在应用启动的不同阶段执行自定义逻辑。

#### 3\. 外部化配置

`SpringFactoriesLoader` 也可以用来加载外部化配置源 (`PropertySourceLoader`)，让你可以自定义配置文件的加载方式，例如从数据库或远程配置中心加载配置。

-----

### 🔍 四、面试高频问题 + 深度解析

**1. `SpringFactoriesLoader` 和 SPI 有什么区别和联系？**

  * **标准答案：**
      * **联系**：`SpringFactoriesLoader` 是 Spring 框架对 Java SPI 机制的**增强和扩展**。它们的核心思想都是**解耦和可插拔**。
      * **区别**：
        1.  **配置文件位置和格式**：Java SPI 的配置文件在 `META-INF/services/` 目录下，文件名是接口全限定名。`SpringFactoriesLoader` 的配置文件固定在 `META-INF/spring.factories`，格式是 `key=value`。
        2.  **加载方式**：Java SPI 依赖于 `ServiceLoader`，`SpringFactoriesLoader` 依赖于自身的实现，可以加载多个同名的 `key`，并进行合并。
        3.  **核心用途**：Java SPI 主要用于**服务发现**，而 `SpringFactoriesLoader` 更专注于**Spring 框架内部的扩展点加载**，尤其是自动化配置。

**2. 为什么说 `SpringFactoriesLoader` 是 Spring Boot 自动配置的基石？**

  * **标准答案：** 因为 `SpringFactoriesLoader` 解决了自动配置的\*\*“发现”\*\*问题。它使得 Spring Boot 无需硬编码去查找和加载各个 starter 模块中的配置类。
  * **深入解析**：当一个模块（如 `spring-boot-starter-data-redis`）被引入时，`SpringFactoriesLoader` 会在编译好的 `redis-starter.jar` 包中找到 `spring.factories` 文件，并根据文件中的配置，加载 `RedisAutoConfiguration` 等自动化配置类。这些配置类再根据条件注解 (`@ConditionalOnClass`) 决定是否创建 `RedisTemplate` 等 Bean，最终实现零配置使用 Redis。

**3. `SpringFactoriesLoader` 在处理多个 `spring.factories` 文件时，如何处理？**

  * **标准答案：** `SpringFactoriesLoader` 会找到所有 jar 包中的 `spring.factories` 文件，然后将它们的内容**合并**。对于相同的 `key`，它会将所有 `value` 合并成一个列表，并进行**去重**。这确保了即使多个库都提供了对同一个接口的扩展，它们也能被正确地加载。

-----

### 🎁 总结与建议

  * `SpringFactoriesLoader` 是 Spring Boot 能够实现“零配置”和“开箱即用”魔力的核心所在。
  * 理解它，就是理解了 Spring Boot 启动时的**自动化配置原理**。
  * 在面试中，能把它与 Java SPI 进行对比，并用自动化配置的例子来解释它的工作原理，将是非常好的加分项。
  * 记住它的核心：**一个类，一个文件，实现了 Spring 的自动化和可扩展性。**