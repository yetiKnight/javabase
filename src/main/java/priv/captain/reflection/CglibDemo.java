package priv.captain.reflection;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import priv.captain.reflection.service.UserServiceImpl;

/**
 * CGLIB 动态代理示例
 * 注意：CGLIB 只能代理类，不能代理接口
 */
public class CglibDemo {
    public static void main(String[] args) {
        // 使用 Spring 的 MethodInterceptor 接口
        MethodInterceptor interceptor = new MethodInterceptor() {
            @Override
            public Object intercept(Object obj, java.lang.reflect.Method method, Object[] args, MethodProxy proxy) throws Throwable {
                System.out.println("=== CGLIB 代理开始 ===");
                System.out.println("Before method: " + method.getName());
                
                // 调用父类原方法
                Object result = proxy.invokeSuper(obj, args);
                
                System.out.println("After method: " + method.getName());
                System.out.println("=== CGLIB 代理结束 ===");
                return result;
            }
        };

        // 使用 Enhancer 创建代理对象
        Enhancer enhancer = new Enhancer();
        // 设置父类（必须是具体类，不能是接口）
        enhancer.setSuperclass(UserServiceImpl.class);
        // 拦截器实现回调
        enhancer.setCallback(interceptor);

        // 创建代理对象
        UserServiceImpl proxy = (UserServiceImpl) enhancer.create();
        
        // 调用代理方法
        String result = proxy.getNameById(1L);
        System.out.println("代理方法返回结果: " + result);
    }
}
