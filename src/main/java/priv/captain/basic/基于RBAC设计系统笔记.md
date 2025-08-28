---

## 1️⃣ 系统需求分析

### 1.1 核心功能

1. 用户管理

   * 用户基本信息（姓名、工号、部门等）
   * 用户所属角色、用户组
2. 角色管理

   * 支持**继承**（父角色 → 子角色）
   * 支持**互斥**（敏感角色不能同时分配）
3. 权限管理

   * 功能权限（菜单、接口）
   * 数据权限（部门、区域、业务对象）
   * 支持 **ABAC** 策略（基于属性的访问控制）
4. 用户组管理

   * 支持批量权限分配
   * 支持部门、项目、业务线等维度
5. 审计与日志

   * 用户操作日志
   * 权限变更日志

### 1.2 权限模型选择

* **RBAC3**（RBAC v3）:

  * 支持角色继承
  * 支持角色约束（互斥、层次化）
  * 支持用户组和角色分配
* **ABAC**:

  * 通过属性（用户属性、资源属性、环境属性）做动态授权
  * 与 RBAC 结合，提高灵活性
  * 适用于政务系统数据权限（部门、区域、业务对象等）

---

## 2️⃣ 数据模型分析

### 2.1 核心实体

1. **User 用户**

   * ID、姓名、账号、手机号、部门ID、状态
   * 属性：岗位、职级、区域等（用于 ABAC）
2. **Role 角色**

   * ID、角色名、描述
   * 属性：父角色ID（角色继承）、状态
3. **Permission 权限**

   * ID、权限类型（菜单/接口/数据）、资源标识、操作类型（CRUD）
   * 属性：资源属性（部门、业务对象等）
4. **UserGroup 用户组**

   * ID、组名、描述
5. **UserRole 用户-角色关联**

   * 用户ID、角色ID
6. **RolePermission 角色-权限关联**

   * 父角色ID、子角色ID
7. **RoleConstraint 角色约束**

   * 互斥角色ID列表
8. **ABAC策略表**

   * 策略ID、策略类型、条件表达式（JSON）

---

## 3️⃣ 数据库设计（示例 MySQL）

```sql
-- 用户表
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    full_name VARCHAR(50),
    dept_id BIGINT,
    position VARCHAR(50),
    level VARCHAR(20),
    status TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 角色表
CREATE TABLE role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_name VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    parent_id BIGINT DEFAULT NULL,
    status TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 用户-角色关联表
CREATE TABLE user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY(user_id, role_id),
    FOREIGN KEY(user_id) REFERENCES user(id),
    FOREIGN KEY(role_id) REFERENCES role(id)
);

-- 权限表
CREATE TABLE permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    permission_type ENUM('MENU','API','DATA') NOT NULL,
    resource VARCHAR(100) NOT NULL,
    action VARCHAR(50),
    resource_attributes JSON,
    description VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 角色-权限关联表
CREATE TABLE role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY(role_id, permission_id),
    FOREIGN KEY(role_id) REFERENCES role(id),
    FOREIGN KEY(permission_id) REFERENCES permission(id)
);

-- 角色互斥约束表
CREATE TABLE role_constraint (
    role_id BIGINT NOT NULL,
    mutex_role_id BIGINT NOT NULL,
    PRIMARY KEY(role_id, mutex_role_id),
    FOREIGN KEY(role_id) REFERENCES role(id),
    FOREIGN KEY(mutex_role_id) REFERENCES role(id)
);

-- 用户组表
CREATE TABLE user_group (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_name VARCHAR(50) NOT NULL,
    description VARCHAR(255)
);

-- 用户组-用户关联
CREATE TABLE user_group_member (
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY(group_id, user_id),
    FOREIGN KEY(group_id) REFERENCES user_group(id),
    FOREIGN KEY(user_id) REFERENCES user(id)
);

-- ABAC策略表
CREATE TABLE abac_policy (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100),
    policy_type ENUM('ALLOW','DENY'),
    target_type ENUM('USER','ROLE','GROUP','RESOURCE'),
    target_id BIGINT,
    condition JSON, -- JSON条件表达式
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

## 4️⃣ RBAC3设计

### 4.1 角色继承

* `RoleHierarchy` 或 `role.parent_id` 实现
* 继承逻辑：

  1. 子角色拥有父角色的所有权限
  2. 支持多层级继承

### 4.2 角色互斥

* `role_constraint` 表定义
* 系统在分配角色时检查：

  ```java
  if (user.hasRole(mutualRole)) {
      throw new Exception("角色互斥，无法分配");
  }
  ```

### 4.3 用户组

* 用户组绑定角色
* 用户继承用户组的角色权限
* ABAC 可用作细粒度控制

---

## 5️⃣ ABAC设计

### 5.1 属性维度

1. 用户属性：岗位、部门、职级、区域
2. 资源属性：组织机构、业务对象、区域、敏感等级
3. 环境属性：时间、访问终端、IP 等

### 5.2 策略表达式（JSON示例）

```json
{
  "user": {"dept_id": 101, "position": "科员"},
  "resource": {"dept_id": 101, "sensitivity": "普通"},
  "condition": "user.dept_id == resource.dept_id && resource.sensitivity != '敏感'"
}
```

### 5.3 权限决策流程

1. 用户发起请求
2. 检查 RBAC：

   * 是否拥有对应权限（角色/用户组）
   * 检查角色互斥和继承
3. 检查 ABAC：

   * 获取用户属性
   * 获取资源属性
   * 通过条件表达式决策是否允许
4. 返回允许或拒绝

---

## 6️⃣ 技术实现方案

### 6.1 技术栈

| 功能模块 | 技术选型                                                        |
| ---- | ----------------------------------------------------------- |
| 后端   | Spring Boot 3 + Spring Security + Spring Data JPA / MyBatis |
| 权限控制 | Spring Security + 自定义 RBAC + ABAC 策略引擎（SpEL 或 Drools）       |
| 数据库  | MySQL 8                                                     |
| 缓存   | Redis（存储用户角色权限，提高性能）                                        |
| 审计日志 | ELK / Logback + Kafka                                       |

### 6.2 关键实现点

#### 6.2.1 用户权限加载

```java
@Service
public class AuthService {
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired PermissionRepository permissionRepository;
    
    public Set<Permission> getUserPermissions(Long userId) {
        Set<Role> roles = getUserRoles(userId);
        Set<Permission> perms = new HashSet<>();
        for (Role role : roles) {
            perms.addAll(permissionRepository.findByRoleId(role.getId()));
        }
        return perms;
    }

    private Set<Role> getUserRoles(Long userId) {
        // 获取直接角色 + 用户组角色 + 父角色继承
        Set<Role> roles = new HashSet<>(roleRepository.findByUserId(userId));
        Set<Role> parentRoles = new HashSet<>();
        for (Role role : roles) {
            parentRoles.addAll(getParentRoles(role));
        }
        roles.addAll(parentRoles);
        return roles;
    }
    
    private Set<Role> getParentRoles(Role role) {
        Set<Role> parents = new HashSet<>();
        Role parent = role.getParent();
        while (parent != null) {
            parents.add(parent);
            parent = parent.getParent();
        }
        return parents;
    }
}
```

#### 6.2.2 ABAC策略检查

```java
public boolean checkABAC(User user, Resource resource, ABACPolicy policy) {
    // 可以用SpEL解析JSON条件
    ExpressionParser parser = new SpelExpressionParser();
    StandardEvaluationContext context = new StandardEvaluationContext();
    context.setVariable("user", user);
    context.setVariable("resource", resource);
    Boolean result = parser.parseExpression(policy.getCondition()).getValue(context, Boolean.class);
    return Boolean.TRUE.equals(result);
}
```

#### 6.2.3 权限决策

1. 先 RBAC 决策
2. 再 ABAC 决策
3. RBAC Deny → 拒绝
   RBAC Allow → ABAC Allow → 允许 / ABAC Deny → 拒绝

---

## 7️⃣ 安全与审计

1. **审计**

   * 操作日志表：记录用户ID、操作类型、目标对象、时间
   * 权限变更日志表：记录谁修改了角色/权限
2. **安全措施**

   * 权限缓存加 Redis，提高性能
   * 接口防重复提交 / 令牌机制
   * 数据权限动态过滤（MyBatis + SQLInterceptor 或 Spring Data Specification）

---

## ✅ 总结

这个方案实现了政务组织机构权限管理的全链路：

* **RBAC3**：角色继承、互斥、用户组
* **ABAC**：动态数据权限控制
* **数据模型**：用户、角色、权限、用户组、ABAC策略
* **数据库设计**：完整 MySQL 表结构
* **技术实现**：Spring Boot + Spring Security + SpEL/Drools
* **审计与安全**：操作日志、权限缓存、数据过滤

可以满足政务部门复杂组织机构、层级权限和数据敏感控制的需求。

---