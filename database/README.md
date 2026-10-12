# database/ 数据库脚本目录

## 目录结构

```
database/
├── init/
│   └── schema_full.sql        # 权威全量初始化脚本（建库 + 25 张表 + 种子数据）
└── test/
    └── homework_test_data.sql # 作业模块演示/测试数据
```

## 使用说明

### 手动初始化
```bash
mysql -u root -p < database/init/schema_full.sql
```
脚本自动创建 `smartedu_platform` 库（utf8mb4），并插入种子数据：
测试账号（student001 / teacher001 / admin001，密码均为 BCrypt 哈希）、课程、新闻、视频与问答演示数据。

### Docker 部署（自动初始化）
`docker-compose.yml` 已将 `database/init/schema_full.sql` 挂载到 MySQL 容器的
`/docker-entrypoint-initdb.d/`，首次 `docker-compose up -d` 时自动执行，无需手动导入。

### 导入测试数据（可选）
```bash
mysql -u root -p smartedu_platform < database/test/homework_test_data.sql
```

## 维护约定

1. **表结构变更必须同步修改 `init/schema_full.sql`**，保证新环境一次初始化即可得到完整最新结构。
2. 线上增量变更请在此目录新增 `migrations/` 子目录按序号存放（如 `001_xxx.sql`），并同步回全量脚本。
3. 一次性修复脚本不入库：修复完成后其结果已体现在全量脚本中。
