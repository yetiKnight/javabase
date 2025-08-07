package priv.captain.basictype;

import org.junit.Test;

/**
 * @description: Java基础数据类型和常见知识点示例
 * @author: yetiKnight
 * @since: 2024-11-10
 **/
public class Demo {

    /**
     * 演示基本数据类型的取值范围和默认值
     */
    @Test
    public void testDataTypeRange() {
        // 整数类型
        byte b = 127;                 // -128 ~ 127 (1字节)
        short s = 32767;             // -32768 ~ 32767 (2字节)
        int i = 2147483647;          // -2^31 ~ 2^31-1 (4字节)
        long l = 9223372036854775807L; // -2^63 ~ 2^63-1 (8字节)

        // 浮点类型
        float f = 3.14159f;          // 精确到7位小数 (4字节)
        double d = 3.141592653589793;// 精确到15位小数 (8字节)

        // 字符类型
        char c = 'A';                // 0 ~ 65535 (2字节)

        // 布尔类型
        boolean bool = true;          // true/false (1位)

        System.out.println("数据类型的最大值：");
        System.out.println("byte: " + Byte.MAX_VALUE);
        System.out.println("short: " + Short.MAX_VALUE);
        System.out.println("int: " + Integer.MAX_VALUE);
        System.out.println("long: " + Long.MAX_VALUE);
    }

    /**
     * 演示类型转换
     */
    @Test
    public void testTypeConversion() {
        // 自动类型转换（隐式）
        byte b = 100;
        int i = b;    // byte -> int
        long l = i;   // int -> long
        float f = l;  // long -> float
        double d = f; // float -> double

        // 强制类型转换（显式）
        int x = (int) 3.14159;    // double -> int，小数部分被截断
        System.out.println("3.14159 强制转换为int: " + x);

        // 注意精度损失
        int large = 1234567890;
        float f1 = large;
        System.out.println("int -> float -> int 精度损失: " + (int)f1);
    }

    /**
     * 演示字符串相关操作
     */
    @Test
    public void testString() {
        // 字符串创建
        String s1 = "hello";              // 字面量，存储在字符串常量池
        String s2 = new String("hello");  // 新对象，存储在堆内存

        // 字符串比较
        System.out.println("s1 == s2: " + (s1 == s2));           // false
        System.out.println("s1.equals(s2): " + s1.equals(s2));   // true

        // 字符串不可变性
        String str = "hello";
        str += " world";  // 实际上创建了新的字符串对象

        // StringBuilder（线程不安全，性能好）
        StringBuilder sb = new StringBuilder();
        sb.append("Hello").append(" ").append("World");
        System.out.println(sb.toString());
    }

    /**
     * 演示包装类型
     */
    @Test
    public void testWrapper() {
        // 自动装箱
        Integer i = 100;    // Integer.valueOf(100)

        // 自动拆箱
        int j = i;         // i.intValue()

        // 缓存池（-128 到 127）
        Integer a = 127;
        Integer b = 127;
        System.out.println("127: a == b: " + (a == b));    // true

        Integer c = 128;
        Integer d = 128;
        System.out.println("128: c == d: " + (c == d));    // false
    }

    /**
     * 演示位运算
     */
    @Test
    public void testBitOperation() {
        int a = 60;  // 60 = 0011 1100
        int b = 13;  // 13 = 0000 1101

        System.out.println("a & b = " + (a & b));   // 与
        System.out.println("a | b = " + (a | b));   // 或
        System.out.println("a ^ b = " + (a ^ b));   // 异或
        System.out.println("~a = " + (~a));         // 取反
        System.out.println("a << 2 = " + (a << 2)); // 左移
        System.out.println("a >> 2 = " + (a >> 2)); // 右移
        System.out.println("a >>> 2 = " + (a >>> 2)); // 无符号右移
    }

    /**
     * 演示浮点数精度问题
     */
    @Test
    public void testFloatPrecision() {
        // 浮点数精度问题
        double x = 3 * 0.1;
        System.out.println(x);                  // 0.30000000000000004
        System.out.println(0.3);                // 0.3
        System.out.println(x == 0.3);           // false

        // 解决方案：使用BigDecimal
        java.math.BigDecimal bd1 = new java.math.BigDecimal("0.1");
        java.math.BigDecimal bd2 = bd1.multiply(new java.math.BigDecimal("3"));
        System.out.println("使用BigDecimal: " + bd2);  // 0.3
    }
}