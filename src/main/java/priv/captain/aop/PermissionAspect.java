package priv.captain.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import priv.captain.common.exception.PermissionDeniedException;

/**
 * 权限注解切面
 */
@Aspect
@Component
public class PermissionAspect {

    /**
     * 注解环绕通知
     * 注意：只有Around能控制方法的执行，其他的不能干扰方法的执行。
     * 
     * @param joinPoint       连接点
     * @param permissionCheck 注解
     * @return
     * @throws Throwable
     */
    @Around("@annotation(permissionCheck)") // 这表达式中permissionCheck 是方法参数名，不是注解名
    public Object permissionAround(ProceedingJoinPoint joinPoint, PermissionCheck permissionCheck) throws Throwable {
        String permissionValue = permissionCheck.value();

        // 这里使用RuntimeException，否则可能事务不回滚。
        if (!hashPermission(permissionValue)) {
            throw new PermissionDeniedException("权限不足");
        }

        Object result = joinPoint.proceed();
        return result;
    }

    /*
     * 模拟权限判断，实际业务场景会调用对应的权限判断service
     */
    private Boolean hashPermission(String permissionValue) {
        return "pass".equals(permissionValue);
    }
}
