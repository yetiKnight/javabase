package priv.captain.aop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验注解
 */
@Target(ElementType.METHOD) // 表明注解可以作用于方法
@Retention(RetentionPolicy.RUNTIME) // 表示运行时生效
@Documented
public @interface PermissionCheck {

    String value() default "";// 权限标识字符串，如home:select

}
