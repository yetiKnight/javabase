package priv.captain.basictype;

import java.util.Optional;

import org.junit.Assert;

public class NpeHandle {
    public static void main(String[] args) {
        // 示例1：直接对null对象调用方法会抛出NullPointerException
        String str = null;
        try {
            // 这里会抛出NPE
            int len = str.length();
        } catch (NullPointerException e) {
            System.out.println("示例1：捕获到NPE，str为null，不能调用length方法");
        }

        // 示例2：使用Objects.requireNonNull进行参数校验，提前抛出异常
        try {
            printLength(null);
        } catch (NullPointerException e) {
            System.out.println("示例2：参数为null时，提前抛出NPE，避免后续业务异常");
        }

        // 示例3：使用Optional避免NPE
        String value = null;
        // Optional.ofNullable允许传入null，避免NPE
        int length = Optional.ofNullable(value).map(String::length).orElse(0);
        System.out.println("示例3：使用Optional安全获取字符串长度：" + length);

        // 示例4：三元运算符进行null判断
        String s = null;
        int l = (s != null) ? s.length() : 0;
        System.out.println("示例4：三元运算符避免NPE，长度为：" + l);

        // 示例5：常见NPE场景——自动拆箱
        Integer num = null;
        try {
            // 这里会抛出NPE，因为num为null，自动拆箱为int时出错
            int n = num;
        } catch (NullPointerException e) {
            System.out.println("示例5：自动拆箱null为int时抛出NPE");
        }

        // 示例6：使用断言(assert)防止NPE（需开启断言 -ea）
        String assertStr = null;
        try {
            // 断言在assertStr为null时会抛出AssertionError
            assert assertStr != null : "assertStr不能为null";
            System.out.println("示例6：断言通过，字符串长度：" + assertStr.length());
        } catch (AssertionError e) {
            System.out.println("示例6：断言失败，捕获到AssertionError，信息：" + e.getMessage());
        }

        // 示例7：使用Spring的Assert工具类防止NPE
        String springStr = null;
        try {
            // org.springframework.util.Assert.notNull在springStr为null时会抛出IllegalArgumentException
           Assert.assertNotNull(springStr, "springStr不能为null");
            System.out.println("示例7：Spring Assert断言通过，字符串长度：" + springStr.length());
        } catch (IllegalArgumentException e) {
            System.out.println("示例7：Spring Assert断言失败，捕获到IllegalArgumentException，信息：" + e.getMessage());
        }
    }

    /**
     * 打印字符串长度，参数不能为空
     * @param s 字符串
     */
    public static void printLength(String s) {
        // Objects.requireNonNull会在s为null时抛出NPE
        java.util.Objects.requireNonNull(s, "参数s不能为null");
        System.out.println("字符串长度：" + s.length());
    }
}