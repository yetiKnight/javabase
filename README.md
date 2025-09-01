# JavaBase Learning 📚

> 一个全面的Java基础知识学习和面试复习项目，涵盖Java核心概念、源码分析和实战案例

[![Java Version](https://img.shields.io/badge/Java-11-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.2.7-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

## 📖 项目简介

JavaBase Learning 是一个系统性的Java基础知识学习项目，专为Java开发者和面试准备者设计。项目包含了丰富的理论笔记、源码分析和实践代码，帮助开发者深入理解Java核心技术。

### 🎯 项目特色

- 📝 **系统化笔记**：涵盖Java基础到高级特性的详细笔记
- 🔍 **源码分析**：深入分析Java核心类库的实现原理
- 💻 **实战代码**：提供可运行的示例代码和Demo
- 📋 **面试导向**：针对Java面试高频问题的深度解析
- 🏗️ **设计模式**：经典设计模式的Java实现

## 🏗️ 项目结构

```
javabase-learning/
├── src/main/java/priv/captain/
│   ├── aop/                    # AOP切面编程
│   │   ├── LogAspect.java      # 日志切面
│   │   ├── PermissionAspect.java # 权限切面
│   │   └── AOP笔记.md
│   ├── basic/                  # Java基础知识
│   │   ├── JVM面试复习笔记.md
│   │   ├── 泛型面试复习笔记.md
│   │   ├── 注解笔记.md
│   │   └── ...
│   ├── basictype/              # 基本类型
│   │   ├── Demo.java
│   │   ├── NpeHandle.java      # 空指针处理
│   │   └── 数组面试复习笔记.md
│   ├── collection/             # 集合框架
│   │   ├── list/              # List相关
│   │   │   ├── ArrayList面试复习笔记.md
│   │   │   ├── LinkedList面试复习笔记.md
│   │   │   └── ListDemo.java
│   │   ├── map/               # Map相关
│   │   │   ├── LRUCache.java  # LRU缓存实现
│   │   │   └── MapExample.java
│   │   └── set/               # Set相关
│   ├── designpattern/          # 设计模式
│   │   ├── factory/           # 工厂模式
│   │   └── singleton/         # 单例模式
│   │       └── DoubleCheckSingleton.java
│   ├── io/                     # IO操作
│   │   ├── BIO-NIO-AIO复习笔记.md
│   │   ├── IOCompareDemo.java
│   │   └── 分片上传功能实现笔记.md
│   ├── juc/                    # 并发编程
│   │   ├── Java内存模型面试复习笔记.md
│   │   ├── AQS面试复习笔记.md
│   │   ├── ReentrantLockDemo.java
│   │   └── ...
│   ├── reflection/             # 反射与代理
│   │   ├── JDKProxyDemo.java   # JDK动态代理
│   │   ├── CglibDemo.java      # CGLIB代理
│   │   └── 动态代理笔记.md
│   ├── spi/                    # SPI机制
│   │   ├── SPI机制笔记.md
│   │   ├── SPITest.java
│   │   └── 三者对比笔记.md
│   └── thread/                 # 线程相关
│       ├── ThreadLocal面试复习笔记.md
│       └── 线程池面试复习笔记.md
└── pom.xml
```

## 📚 学习内容

### 🔥 核心模块

#### 1. Java基础 (basic/)
- **JVM虚拟机**：内存区域、类加载、垃圾回收
- **泛型机制**：类型擦除、通配符、边界
- **注解系统**：元注解、自定义注解、反射处理
- **RBAC权限模型**：权限系统设计

#### 2. 集合框架 (collection/)
- **ArrayList**：动态数组、扩容机制、源码分析
- **LinkedList**：双向链表、插入删除性能
- **HashMap**：哈希表、红黑树、并发问题
- **CopyOnWriteArrayList**：写时复制、并发安全

#### 3. 并发编程 (juc/)
- **Java内存模型**：happens-before、volatile、synchronized
- **AQS框架**：同步器、锁机制、条件队列
- **ReentrantLock**：可重入锁、公平/非公平锁
- **ThreadLocal**：线程本地存储、内存泄漏

#### 4. IO操作 (io/)
- **BIO/NIO/AIO**：同步异步、阻塞非阻塞
- **零拷贝技术**：Files.copy原理分析
- **分片上传**：大文件上传解决方案

#### 5. 设计模式 (designpattern/)
- **单例模式**：双重检查锁定、枚举实现
- **工厂模式**：简单工厂、抽象工厂

#### 6. 高级特性
- **SPI机制**：服务提供者接口、插件化架构
- **反射与代理**：JDK代理、CGLIB代理
- **AOP编程**：切面、通知、权限控制

## 🚀 快速开始

### 环境要求

- Java 11+
- Maven 3.6+
- IDE (推荐 IntelliJ IDEA)

### 运行项目

1. **克隆项目**
```bash
git clone <your-repo-url>
cd javabase-learning
```

2. **编译项目**
```bash
mvn clean compile
```

3. **运行测试**
```bash
mvn test
```

4. **启动应用**
```bash
mvn spring-boot:run
```

### 学习建议

1. **按模块学习**：建议按照基础 → 集合 → 并发 → IO → 设计模式的顺序学习
2. **理论结合实践**：每个模块都包含理论笔记和实践代码
3. **源码分析**：重点关注核心类的源码实现
4. **面试准备**：笔记中包含大量面试高频问题

## 📋 重点知识点

### 🎯 面试高频

#### JVM相关
- 内存区域划分（堆、栈、方法区）
- 垃圾回收算法（标记清除、复制、标记整理）
- 类加载机制（双亲委派模型）
- JVM调优参数

#### 集合框架
- ArrayList vs LinkedList 性能对比
- HashMap 底层实现（数组+链表+红黑树）
- ConcurrentHashMap 并发安全机制
- fail-fast 机制原理

#### 并发编程
- synchronized 锁升级过程
- volatile 可见性和有序性
- AQS 同步器框架
- 线程池核心参数

#### IO模型
- BIO/NIO/AIO 区别和适用场景
- select/poll/epoll 多路复用
- 零拷贝技术原理

## 🛠️ 技术栈

- **核心框架**：Spring Boot 2.2.7
- **构建工具**：Maven
- **日志框架**：Spring Boot默认日志
- **测试框架**：JUnit
- **AOP支持**：Spring AOP
- **代码映射**：MapStruct

## 📖 学习资源

### 推荐阅读顺序

1. **Java基础**
   - 先阅读 `basic/` 目录下的笔记
   - 重点理解JVM内存模型和垃圾回收

2. **集合框架**
   - 学习 `collection/` 下的源码分析
   - 运行相关的Demo代码

3. **并发编程**
   - 深入学习 `juc/` 模块
   - 理解Java内存模型和同步机制

4. **高级特性**
   - 学习反射、代理、SPI等机制
   - 了解设计模式的实际应用

### 实践建议

- 每个知识点都有对应的测试代码，建议动手运行
- 修改参数观察不同的运行结果
- 尝试写出自己的实现版本

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 👥 作者

**CNT-Captain** - [Gitee](https://gitee.com/CNT-Captain)

## 🙏 致谢

感谢所有为Java技术发展做出贡献的开发者们！

---

⭐ 如果这个项目对你有帮助，请给它一个星标！

💬 有问题欢迎提 Issue 讨论！
