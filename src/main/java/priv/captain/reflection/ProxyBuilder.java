package priv.captain.reflection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * 代理工具类
 */
public class ProxyBuilder<T> {

    private final T target;
    private final Class<T> interfaceType;

    private final List<MethodInterceptor> interceptors = new ArrayList<>();

    private ProxyBuilder(T target, Class<T> interfaceType) {
        this.target = target;
        this.interfaceType = interfaceType;
    }

    public static <T> ProxyBuilder<T> of(T target, Class<T> interfaceType) {
        return new ProxyBuilder<T>(target, interfaceType);
    }

    // 添加拦截器（链式调用）
    public ProxyBuilder<T> addInterceptor(MethodInterceptor interceptor) {
        interceptors.add(interceptor);
        return this;
    }

    
    @SuppressWarnings("unchecked")
    public T build() {
        return (T) Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                new Class[]{interfaceType},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        return proceedChain(0, proxy, method, args);
                    }

                    // 递归执行拦截器链
                    private Object proceedChain(int index, Object proxy, Method method, Object[] args) throws Throwable {
                        if (index == interceptors.size()) {
                            // 最后调用目标对象方法
                            return method.invoke(target, args);
                        }
                        MethodInterceptor interceptor = interceptors.get(index);
                        return interceptor.invoke(proxy, method, args, () -> proceedChain(index + 1, proxy, method, args));
                    }
                }
        );
    }
}
