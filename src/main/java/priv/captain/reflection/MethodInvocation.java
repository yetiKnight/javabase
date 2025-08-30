package priv.captain.reflection;

/**
 * 🔹 封装目标方法调用
 */
@FunctionalInterface
public interface MethodInvocation {
    Object proceed() throws Throwable;
}