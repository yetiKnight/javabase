package priv.captain.reflection;

import java.lang.reflect.Method;

// 🔹 日志拦截器
public class LogInterceptor implements MethodInterceptor {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args, MethodInvocation original) throws Throwable {
        System.out.println("[LOG] Before " + method.getName());
        Object result = original.proceed();
        System.out.println("[LOG] After " + method.getName());
        return result;
    }
}