package priv.captain.reflection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/*
 * 动态代理处理器
 */
public class MyInvocationHandler implements InvocationHandler {

    // 目标对象，真正要操作的对象
    private final Object tarObject;

    public MyInvocationHandler(Object tarObject) {
        this.tarObject = tarObject;
    }

    /**
     * 🔹 每次代理对象调用方法时，都会进入 invoke 方法
     * 
     * @param proxy  代理对象
     * @param method 被调用的方法对象
     * @param args   方法参数
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        System.out.println("前置增强");

        Object result = method.invoke(tarObject, args);

        System.out.println("后置增强");
        return result;
    }

}