package priv.captain.basictype;

import org.junit.Test;

/**
 * @description: 基本数据类型
 * @author: yetiKnight
 * @since: 2024-11-10
 **/
public class Demo {

    @Test
    public void test1() {
        //浮点数在计算机中是用二进制的近似值表示的
        double x = 3 * 0.1;
        System.out.println(x);
        System.out.println(0.3);
        System.out.println(x == 0.3);// 结果是false
    }

    @Test
    public void test2() {
        System.out.println(test3());
    }

    private String test3() {
        try {
            int i = 1;
            int j = 10;
            int k = i / j;
            System.out.println(k);
            return "111";
        } finally {
            //return "222";
            // 1、try里执行了return，finally里的代码块还是会执行
            // 2、finally里return的话，后面的代码都不能有，编译不会通过
            System.out.println("Finally 代码块执行");
        }
    }

    // 移位运算符
    @Test
    public void test() {
        /*
         * 1、左移 (<<)：乘以 2^n，即左移 n 位相当于乘以 2 的 n 次方。
         * 2、右移 (>>)：除以 2^n，对于负数，保留符号位（算术右移）。
         * 3、无符号右移 (>>>)：与算术右移不同，填充空位为 0，适用于无符号整数的位移
         * */
        int i = 10;
        int j = i >> 2;
        System.out.println("运算结果：" + j);
    }
}
