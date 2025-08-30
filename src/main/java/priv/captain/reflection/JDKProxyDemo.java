package priv.captain.reflection;

import java.lang.reflect.Proxy;

import priv.captain.reflection.service.UserService;
import priv.captain.reflection.service.UserServiceImpl;

/**
 * JDK动态代理demo
 */
public class JDKProxyDemo {

    public static void main(String[] args) {
        // 1️⃣ 创建真实对象
        UserService userService = new UserServiceImpl();

        // 2️⃣ 创建 InvocationHandler
        MyInvocationHandler handler = new MyInvocationHandler(userService);

        // 3️⃣ 创建代理对象
        UserService proxy = (UserService) Proxy.newProxyInstance(
                userService.getClass().getClassLoader(), // 类加载器
                new Class[] { UserService.class }, // 代理的接口
                handler // 处理器
        );
        proxy.getNameById(1l);

        // 🔹 生成代理对象，支持多拦截器，高级封装写法
        UserService advanceProxy = ProxyBuilder.of(userService, UserService.class)
                .addInterceptor(new LogInterceptor())
                .build();

        advanceProxy.getNameById(1l);
    }

}
