package priv.captain.reflection;

import java.lang.reflect.Method;

public interface MethodInterceptor {
    Object invoke(Object proxy, Method method, Object[] args, MethodInvocation original) throws Throwable;
}