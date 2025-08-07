package priv.captain.basictype;

/**
 * @description: 测试continue、break、return
 * @author: yetiKnight
 * @since: 2025-05-15
 **/
public class ControlFlowDemo {
    public static void main(String[] args) {
        boolean finished = test();
        if (finished) {
            System.out.println("方法正常结束");
        } else {
            System.out.println("方法被提前return");
        }
        System.out.println("最后结束了");
    }

    /**
     * 测试continue、break、return的用法
     * @return true-正常执行到结尾，false-被return提前返回
     */
    private static boolean test() {
        for (int i = 1; i < 10; i++) {
            if (i == 5) {
                // continue 会跳过本次循环，继续下一次循环
                continue;
            }
            if (i == 7) {
                // return 会直接返回，结束方法
                return false;
            }
            System.out.println(i);
            if (i == 8) {
                // break 会直接结束当前循环
                break;
            }
        }
        System.out.println("end");
        return true;
    }
}
