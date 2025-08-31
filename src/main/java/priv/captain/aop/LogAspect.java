package priv.captain.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 日志记录切面
 * 说明：
 * 1. @Aspect 表示这是一个切面类
 * 2. @Component 将切面加入 Spring 容器
 */
@Aspect
@Component
public class LogAspect {

    /**
     * 环绕通知示例
     * 说明：
     * 1、"execution(* priv.captain.service..*(..))"
     * 切入点表达式，表示匹配对应包路径下的所有方法。在Spring中默认通知可以直接内联切入表达式
     * 2、ProceedingJoinPoint 连接点，手动控制目标方法执行。
     * 1）获取目标方法信息（方法名、参数、目标对象等）。
     * 2）决定是否执行目标方法，并可在执行前后增加自定义逻辑。
     * 3）可以修改方法参数或返回值。
     * 
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around("execution(* priv.captain.service..*(..))")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long t1 = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long t2 = System.currentTimeMillis();
        String methodSign = joinPoint.getSignature().toLongString();
        System.out.println("执行方法：" + methodSign + "，耗时：" + (t2 - t1) + "ms");
        return result;
    }

}
