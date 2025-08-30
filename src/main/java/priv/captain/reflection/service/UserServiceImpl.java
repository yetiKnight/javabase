package priv.captain.reflection.service;

/**
 * UserService 的具体实现类
 * CGLIB 只能代理类，不能代理接口
 */
public class UserServiceImpl implements UserService {
    
    @Override
    public String getNameById(Long id) {
        System.out.println("实际执行业务逻辑，查询用户ID: " + id);
        return "用户" + id;
    }
}
