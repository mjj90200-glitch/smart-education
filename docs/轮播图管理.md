# 轮播图管理功能说明

## 功能概述

轮播图管理功能允许管理员自定义首页轮播图展示的新闻内容及其顺序。

## 主要功能

### 1. 轮播图管理入口

- 访问新闻管理页面：http://localhost:5173/admin/news
- 点击页面右上角的"轮播图管理"按钮

### 2. 功能特点

#### 拖拽排序
- 拖动左侧轮播图列表中的拖拽手柄（≡ 图标）调整顺序
- 也可以使用上移/下移按钮微调顺序

#### 添加新闻
- 从右侧候选列表中选择新闻
- 点击"添加"按钮将新闻加入轮播图
- 最多支持 6 张轮播图

#### 移除新闻
- 点击轮播图列表中的"移除"按钮（垃圾桶图标）
- 仅从轮播图移除，不删除新闻本身

#### 批量操作
- 在新闻列表中勾选多条新闻
- 使用"批量操作"下拉菜单：
  - 设为轮播图：将选中新闻设为轮播图类型
  - 设为列表新闻：将选中新闻设为列表类型
  - 批量删除：删除选中的新闻

### 3. 技术实现

#### 前端
- 组件：`vue-draggable-plus`
- UI 框架：Element Plus
- 框架：Vue 3 + TypeScript

#### 后端
- 框架：Spring Boot 3.2
- ORM：MyBatis-Plus
- 数据库：MySQL 8.0

## API 接口

### 保存轮播图排序
```
POST /news/admin/carousel/order
Content-Type: application/json
Authorization: Bearer <token>

{
  "newsIds": [1, 2, 3, 4, 5, 6]
}
```

### 批量更新新闻类型
```
PUT /news/admin/batch-update-type
Content-Type: application/json
Authorization: Bearer <token>

{
  "ids": [1, 2, 3],
  "newsType": 1  // 1=轮播图，2=列表
}
```

## 数据库变更

### 新增字段
新闻表 (`news`) 新增字段：
- `order_index` INT：排序序号（轮播图用）

### 迁移脚本
执行以下脚本更新数据库：
```bash
cd database
run_migration.bat  # Windows
```

## 使用说明

1. **启动后端应用**
   - 确保数据库已执行迁移脚本
   - 启动 Spring Boot 应用

2. **启动前端应用**
   ```bash
   cd smartedu-ui
   npm install  # 首次运行
   npm run dev
   ```

3. **登录管理员账号**
   - 账号：`admin001`
   - 密码：`123456`

4. **访问轮播图管理**
   - 导航至：http://localhost:5173/admin/news
   - 点击"轮播图管理"按钮

## 注意事项

1. 轮播图最多显示 6 张
2. 添加到轮播图的新闻会自动设置为轮播图类型（newsType=1）
3. 从轮播图移除的新闻不会删除，只是类型变为列表新闻
4. 保存后会立即更新首页轮播图展示

## 数据结构

### News 实体
```java
public class News {
    private Long id;
    private String title;
    private String summary;
    private String content;
    private String imageUrl;
    private String sourceUrl;
    private String sourceName;
    private Integer newsType;      // 1=轮播图，2=列表
    private Integer isTop;         // 0=否，1=是
    private Integer isManual;      // 0=自动，1=手动
    private Integer orderIndex;    // 排序序号
    private LocalDateTime publishTime;
    // ...
}
```

## 常见问题

### Q: 拖拽不生效？
A: 确保使用的是 `vue-draggable-plus` 组件，并且已正确安装依赖。

### Q: 保存后轮播图顺序未更新？
A: 检查后端接口是否正确接收 `newsIds` 参数，并更新 `order_index` 字段。

### Q: 首页轮播图未更新？
A: 清除浏览器缓存或刷新页面，确保前端重新请求轮播图数据。

---

**更新日期：** 2026-03-23
**版本：** v1.0
