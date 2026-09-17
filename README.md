# 新长入驻公司管理平台

## 项目概述

新长公司是创业园运营管理主体，向入驻企业提供经营办公场地租赁与证件代办配套服务。本系统为内部文员后台管理系统，用于房间资源、入驻企业、企业人员、证件代办资料导出的线上化管理，替代原有分散的 Excel 人工维护方式，实现业务全流程留痕、模板化文档导出，提升文员工作效率与数据准确性。

## 技术栈

| 层 | 技术 | 版本 |
|---|---|---|
| 后端语言 | Java | 17 |
| 后端框架 | Spring Boot | 3.3.5 |
| ORM | MyBatis-Plus | 3.5.7 |
| 权限框架 | sa-token | 1.39.0 |
| 数据库 | MySQL | 8.x |
| 前端框架 | Vue 3 + Vite + TypeScript | — |
| UI 组件库 | Element Plus | 2.8.x |
| 状态管理 | Pinia | 2.2.x |
| 文档处理 | Apache POI | 5.2.5 |

## 模块架构

```
xc-management/
├── backend/                         # Spring Boot 后端 (pom.xml)
│   └── src/main/java/com/xinchang/management/
│       ├── common/                  # 通用层（统一响应、全局异常、配置）
│       ├── config/                  # sa-token 拦截器配置
│       ├── auth/                    # 权限模块（登录、角色、用户管理）
│       ├── room/                    # 房间管理（楼栋→楼层→房间 CRUD）
│       ├── company/                 # 入驻企业（CRUD、房间分配、迁出/停办）
│       ├── employee/                # 企业人员（CRUD、附件管理、花名册导出）
│       ├── template/                # 代办模板（上传/删除、#{字段} 替换导出）
│       └── log/                     # 操作日志查询
├── frontend/                        # Vue 3 前端
│   └── src/
│       ├── api/                     # Axios 接口封装
│       ├── stores/                  # Pinia 状态管理（用户登录态）
│       ├── router/                  # 路由与守卫
│       ├── directives/              # v-permission 权限指令
│       ├── layout/                  # 主布局（侧边栏+顶栏+深色模式）
│       └── views/                   # 页面组件
│           ├── login/               # 登录页
│           ├── home/                # 首页
│           ├── room/                # 房间管理
│           ├── company/             # 入驻企业
│           ├── employee/            # 企业人员+附件
│           ├── template/            # 代办模板+导出
│           └── system/              # 用户管理+操作日志
└── uploads/                         # 文件上传存储目录
    ├── photos/                      # 人员照片
    ├── attachments/                 # 人员附件
    └── templates/                   # 上传的模板文件
```

## 模块功能一览

### 1. 房间管理（F1-01 ~ F1-05）
- 楼栋/楼层/房间的增删改查
- 同一楼栋房间号唯一性校验（应用层 + 数据库唯一索引双重保障）
- 删除保护：已分配房间不可删，含子数据的楼栋/楼层不可删
- 多条件筛选（楼栋/楼层/房间号/状态）+ 分页

### 2. 入驻企业管理（F2-01 ~ F2-06）
- 企业信息录入 + 多房间分配
- 迁出：释放全部房间 + 人员置离职 + 操作日志（含备注）
- 停办：人员同步置离职
- 批量导入：行号级错误提示、部分成功部分失败
- 批量导出：按筛选条件导出 Excel

### 3. 企业人员管理（F4-01 ~ F4-07）
- 人员信息 CRUD（姓名/性别/身份证号/毕业学校/专业/手机号/照片/岗位）
- 附件一对多：身份证正反面、毕业证照片、学历认证报告 PDF
- 在职↔离职状态变更日志（含旧/新状态）
- 花名册导出（Excel/Word，含嵌入式照片）

### 4. 代办模板与导出（F3-01 ~ F3-02）
- 模板上传/删除（仅 .docx）
- `#{字段名}` 变量替换引擎（正文 + 表格 + 页眉 + 页脚）
- 缺失字段留空 + 日志记录
- 批量导出全套代办资料 zip
- 任命书按人员逐个生成 zip
- 5 分钟超时保护

### 5. 权限与日志（F5-01 ~ F5-04）
- 管理员：全部功能（增删改查导出）
- 普通用户：仅查看、导出（接口层 `@SaCheckRole` 强制鉴权）
- 关键业务操作全留痕，日志保留 6 个月

## 快速部署

### 环境要求

| 工具 | 版本要求 |
|---|---|
| JDK | 17+ |
| Maven | 3.8+ |
| MySQL | 8.0+ |
| Node.js | 18+ |
| npm | 9+ |

### 1. 数据库初始化

```sql
CREATE DATABASE IF NOT EXISTS xc_management DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

项目启动时会自动执行 `db/schema.sql` 创建全部表结构。

### 2. 后端启动

```bash
# 1. 确保 application.yml 中数据库连接信息正确
#    src/main/resources/application.yml
#    修改 spring.datasource.username 和 password

# 2. 编译运行
mvn spring-boot:run
```

启动后访问：`http://localhost:9998/api/health` 验证。

### 3. 前端启动

```bash
cd frontend

# 1. 安装依赖
npm install

# 2. 开发模式启动（默认代理到 localhost:9998）
npm run dev

# 3. 生产构建
npm run build
```

开发模式访问：`http://localhost:3000`

### 4. 默认管理员

首次启动自动创建管理员账号：

| 用户名 | 密码 | 角色 |
|---|---|---|
| `admin` | `admin123` | ADMIN（管理员） |

> 生产环境部署后请立即修改默认密码。

## API 概览

### 认证
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/auth/login` | 登录 |
| POST | `/api/auth/logout` | 登出 |
| GET | `/api/auth/me` | 当前用户信息 |
| PUT | `/api/auth/password` | 修改密码 |

### 房间管理
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/buildings` | 楼栋列表 |
| POST | `/api/buildings` | 新增楼栋 |
| PUT | `/api/buildings/{id}` | 修改楼栋 |
| DELETE | `/api/buildings/{id}` | 删除楼栋 |
| GET | `/api/floors` | 楼层列表 |
| POST | `/api/floors` | 新增楼层 |
| PUT | `/api/floors/{id}` | 修改楼层 |
| DELETE | `/api/floors/{id}` | 删除楼层 |
| GET | `/api/rooms` | 房间分页列表 |
| POST | `/api/rooms` | 新增房间 |
| PUT | `/api/rooms/{id}` | 修改房间 |
| DELETE | `/api/rooms/{id}` | 删除房间 |

### 企业管理
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/companies` | 企业分页列表 |
| GET | `/api/companies/{id}` | 企业详情 |
| POST | `/api/companies` | 新增企业 |
| PUT | `/api/companies/{id}` | 修改企业 |
| PUT | `/api/companies/{id}/status` | 状态变更 |
| GET | `/api/companies/export` | 导出 Excel |
| POST | `/api/companies/import` | 导入 Excel |
| GET | `/api/companies/template` | 下载导入模板 |

### 人员管理
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/employees` | 人员分页列表 |
| GET | `/api/employees/{id}` | 人员详情 |
| POST | `/api/employees` | 新增人员 |
| PUT | `/api/employees/{id}` | 修改人员 |
| PUT | `/api/employees/{id}/status` | 状态变更 |
| DELETE | `/api/employees/{id}` | 删除人员 |
| POST | `/api/employees/{id}/photo` | 上传照片 |
| DELETE | `/api/employees/{id}/photo` | 删除照片 |
| GET | `/api/employees/roster/excel` | 花名册 Excel |
| GET | `/api/employees/roster/word` | 花名册 Word |

### 附件管理
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/attachments` | 附件列表 |
| POST | `/api/attachments` | 上传附件 |
| DELETE | `/api/attachments/{id}` | 删除附件 |

### 模板与导出
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/templates` | 模板列表 |
| POST | `/api/templates` | 上传模板 |
| DELETE | `/api/templates/{id}` | 删除模板 |
| POST | `/api/export/company` | 导出企业全套代办资料 |
| POST | `/api/export/appointment` | 导出任命书 |

### 系统管理
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/users` | 用户列表 |
| POST | `/api/users` | 新增用户 |
| PUT | `/api/users/{id}/role` | 分配角色 |
| PUT | `/api/users/{id}/password` | 重置密码 |
| GET | `/api/logs` | 操作日志查询 |

> 全部写接口（POST/PUT/DELETE）均需 `ADMIN` 角色。
> 普通用户仅可访问 GET 查询接口。

## 占位符规范

Word 模板中使用 `#{字段名}` 标记变量，导出时自动替换。

### 可用字段

| 字段 | 来源 | 示例 |
|---|---|---|
| `#{companyName}` | 企业 | 新长科技有限公司 |
| `#{unifiedCode}` | 企业 | 91440101MA5... |
| `#{legalPerson}` | 企业 | 张三 |
| `#{contactPerson}` | 企业 | 李四 |
| `#{contactPhone}` | 企业 | 13800138000 |
| `#{address}` | 企业 | 广州市天河区... |
| `#{name}` | 人员 | 王五 |
| `#{gender}` | 人员 | 男 |
| `#{idCard}` | 人员 | 440106... |
| `#{school}` | 人员 | 华南理工大学 |
| `#{major}` | 人员 | 计算机科学与技术 |
| `#{phone}` | 人员 | 13900139000 |
| `#{position}` | 人员 | 工程师 |

> 替换位置支持：正文段落、表格单元格、页眉、页脚。
> 缺失字段导出处留空，系统日志记录缺失字段。

## 开发说明

### 权限模型

两种角色，接口层强制鉴权：

| 角色 | 权限 |
|---|---|
| `ADMIN`（管理员） | 全部功能（增删改查 + 导入导出） |
| `USER`（普通用户） | 仅查看、导出，无写操作权限 |

前端使用 `v-permission="'ADMIN'"` 指令控制按钮显隐，后端使用 `@SaCheckRole("ADMIN")` 强制鉴权。

### 校验规则

- **文件上传**：仅允许 `jpg/jpeg/png/pdf/docx`，禁止可执行文件（`.exe/.bat/.sh/.dll` 等）
- **照片大小**：≤ 10MB
- **附件大小**：≤ 50MB
- **模板格式**：仅 `.docx`

## 已知问题与改进建议

以下为交付前审查发现的问题清单，待人工确认后处理：

### 高危

| # | 问题 | 文件 |
|---|---|---|
| 1 | Blob 下载被响应拦截器拦截导致导出功能不可用 | `frontend/src/api/request.ts:19-26` |
| 2 | 移动端导航抽屉缺少触发按钮 | `frontend/src/layout/MainLayout.vue:63-79` |
| 3 | 操作日志页面首次加载不请求数据 | `frontend/src/views/system/OperationLog.vue:71` |

### 中危

| # | 问题 | 文件 |
|---|---|---|
| 4 | 3 处写操作按钮缺少 `v-permission` 守卫 | `CompanyList.vue:62`, `EmployeeList.vue:52,53-55` |
| 5 | `writeLog` 方法在 7 个类中重复，未抽取公共组件 | 多个 service 文件 |
| 6 | `company_room.released_time` 未在房间释放时写入 | `CompanyService.java:147-152` |
| 7 | `ExportController` 混入业务逻辑 | `ExportController.java` |
| 8 | 附件磁盘文件删除策略不一致 | `EmployeeService.java:199-204` |
| 9 | 401 响应未重置 Pinia store 状态 | `frontend/src/api/request.ts:29-33` |
| 10 | 前端文件校验仅检查扩展名，未禁止可执行文件 | 多个上传组件 |

### 低危

| # | 问题 | 文件 |
|---|---|---|
| 11 | 实体类未使用 Lombok 注解 | 全部实体类 |
| 12 | 8 处文件/导出操作缺少操作日志 | 多个 service 文件 |
| 13 | `RoomController` 使用构造器注入，其余使用字段注入 | `RoomController.java` |
| 14 | 控制器使用 `Map<String, Object>` 而非 DTO | 全部控制器 |
| 15 | 组织选择弹窗显示楼栋 ID 而非名称 | `CompanyForm.vue:90` |

## License

内部项目 — 新长公司