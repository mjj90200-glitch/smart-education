# SmartEdu-Platform 项目配置记录

## 项目启动步骤

### 1. 数据库初始化
```bash
mysql -u root -p < database/schema.sql
```

### 2. 更新测试账号密码
```bash
mysql -u root -p < database/update_password.sql
```

### 3. 启动后端
```bash
cd smartedu-backend
mvn spring-boot:run
```
后端地址：http://localhost:8080/api/swagger-ui.html

### 4. 启动前端
```bash
cd smartedu-ui
npm install
npm run dev
```
前端地址：http://localhost:3000

## 测试账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| student001 | 123456 | 学生 |
| teacher001 | 123456 | 教师 |
| admin001 | 123456 | 管理员 |

## 已修复的问题

1. **LoginResponse.java** - 添加缺失的 `import com.smartedu.vo.UserInfoVO;`
2. **AuthController.java** - 修复 `Result.success()` 泛型推断错误
3. **SecurityConfig.java** - 添加 `PasswordEncoder` Bean
4. **UserService.java** - 新建用户服务类，实现登录/注册逻辑
5. **variables.scss** - 创建缺失的 SCSS 变量文件
6. **logo.svg** - 创建缺失的 Logo 文件
7. **数据库密码** - 更新测试账号为正确的 BCrypt 哈希

## 项目结构

```
SmartEdu-Platform/
├── database/                 # 数据库脚本
├── smartedu-backend/        # Spring Boot 后端
└── smartedu-ui/             # Vue 3 前端
```

## 技术栈

### 后端
- Spring Boot 3.2
- Spring Security
- MyBatis-Plus
- MySQL 8.0
- JWT

### 前端
- Vue 3
- Vite
- TypeScript
- Element Plus
- Pinia
- Vue Router
