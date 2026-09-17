# OpenCode 开发指令集 —— 新长入驻公司管理平台

> 工作流：Plan-then-Build ｜ 依据文档：`产品需求文档(PRD).md`（V0.1）
> 技术栈基线：Java17 + SpringBoot3 + MySQL8 + MyBatis-Plus（优先不手写SQL）+ sa-token + Vue（前端另行指令集，本期聚焦后端）

---

## 执行总览

| 序号 | 指令 | 模式 | 用途 |
|---|---|---|---|
| S-0 | 全局上下文初始化 | 任意模式，执行 `/init` | 读取 PRD，生成 AGENTS.md |
| P-1 | 数据库与模型先行 | **Plan** | 数据模型、表结构、全局校验规则 |
| B-1 | 项目骨架与基础设施 | **Build** | 工程初始化、通用层、配置 |
| B-2 | 权限与登录模块 | **Build** | sa-token 鉴权、角色权限矩阵 |
| B-3 | 房间管理模块 | **Build** | 楼栋/楼层/房间 CRUD |
| B-4 | 入驻公司管理模块 | **Build** | 企业全生命周期、导入导出 |
| B-5 | 企业人员管理模块 | **Build** | 人员 CRUD、附件、花名册 |
| B-6 | 模板与资料导出模块 | **Build** | 模板管理、`#{变量}` 替换、压缩包 |
| F-1 | 前端骨架与登录鉴权 | **Build** | Vue3 工程、登录、路由守卫、布局、深色模式 |
| F-2 | 房间管理页面 | **Build** | 楼栋/楼层/房间维护、筛选分页 |
| F-3 | 入驻公司管理页面 | **Build** | 企业表单、房间分配、导入导出、迁出停办 |
| F-4 | 企业人员管理页面 | **Build** | 人员表单、附件上传预览、花名册导出 |
| F-5 | 模板与资料导出页面 | **Build** | 模板上传删除、批量导出 zip |
| F-6 | 系统用户与操作日志页面 | **Build** | 用户管理、日志查询 |
| T-1 | 单元测试生成 | **Build** | 核心规则测试 |
| R-1 | 代码审查 | **Review**（或 `/review`） | 安全、权限、规则合规审查（前后端） |

**【执行说明】**
- S-0：打开 OpenCode，执行 `/init`，粘贴下方 S-0 内容；完成 AGENTS.md 生成后进入下一步。
- P-1：按 `Tab` 切换到 **Plan 模式**，粘贴 P-1；AI 输出数据设计后**先人工确认**，再切换到 Build 模式。
- B-1～B-6：按 `Tab` 切换到 **Build 模式**，严格按序逐条粘贴；上一步验收通过后再贴下一步。
- F-1～F-6：仍在 **Build 模式**，待后端 B-1～B-6 验收通过后按序粘贴；F-1 需先确认后端 API 契约（统一响应、鉴权 403、字段命名），再逐页实现。
- T-1、R-1：T-1 在 Build 模式粘贴；R-1 切换到 **Review 模式**（或使用 `/review`）粘贴，审查范围覆盖前后端。

---

## S-0 全局上下文初始化（/init）

```text
请先执行 /init 初始化项目上下文，并完成以下任务：

1. 完整读取项目根目录下的「产品需求文档(PRD).md」，提炼业务背景：
   新长公司是创业园运营管理主体，向入驻企业提供场地租赁与证件代办服务；
   本系统为内部文员后台，用于房间资源、入驻企业、企业人员、代办资料导出的线上化管理，
   替代现有 Excel 人工维护方式。

2. 生成（或更新）项目根目录 AGENTS.md，必须覆盖以下章节：
   - 项目概述：一段话说明业务与系统定位；
   - 技术栈约束：Java17、SpringBoot3、MySQL8、MyBatis-Plus（优先不手写 SQL）、sa-token、Vue；
   - 模块地图：房间管理 / 入驻公司管理 / 证件代办模板与资料导出 / 企业人员管理 / 权限与日志；
   - 合规基线：
     * 管理员拥有全部功能；普通用户仅可查看、导出，接口层强制鉴权（不只前端隐藏按钮）；
     * 房间、企业、人员关键业务操作必须写操作日志（操作人、时间、内容），日志保留 6 个月；
     * 用户密码 BCrypt 加密存储；
     * 上传文件校验格式与大小，禁止可执行文件；
     * 业务占位符规范统一为 #{字段名}。

3. 将 AGENTS.md 声明为后续所有 Plan/Build 会话的默认上下文，不允许偏离上述技术栈与合规基线。
```

---

## P-1 数据库与模型先行（Plan 模式）

```text
【模式：Plan】仅基于 PRD 完成数据建模设计，严禁创建或修改任何代码、配置文件。

1. 梳理全部核心实体并输出字段定义（字段名、类型、长度、必填、默认值、说明）：
   - 楼栋 Building、楼层 Floor、房间 Room（状态：空闲/已分配/禁用）；
   - 入驻企业 Company（含业务状态，覆盖正常运营/迁出/停办，按 PRD 推导并说明取值）；
   - 企业-房间分配关系 CompanyRoom（一个企业可分配多个房间，多对多）；
   - 企业人员 Employee（姓名、性别、身份证号、毕业学校、专业、手机号、照片、岗位；
     状态：在职/离职）；
   - 人员附件 Attachment（类型：身份证正反面、毕业证照片、学历认证报告 PDF、人员照片；
     一对多，支持覆盖更新）；
   - 代办模板 DocTemplate（名称、存储路径、类型、上传时间等元数据）；
   - 操作日志 OperationLog（操作人、时间、模块、动作、操作内容）；
   - 系统用户 SysUser（账号、密码密文、角色：管理员/普通用户）。

2. 输出 MySQL8 建表 SQL 与索引设计，必须落实：
   - 楼栋-楼层-房间三层层级外键关联；
   - 同一楼栋内房间号唯一（唯一索引）；
   - 企业-房间分配关系唯一约束（同企业同房间不重复）；
   - 人员归属企业、附件归属人员的外键与级联策略（附件磁盘文件不随记录删除）。

3. 输出全局校验规则清单：
   - 唯一性：同楼栋房间号唯一；
   - 删除保护：已分配房间不可删；含楼层/房间子数据的楼栋/楼层不可删；
   - 状态机：房间 空闲→已分配→空闲（迁出释放）；人员 在职↔离职；
   - 导入校验：行号级错误提示、部分成功部分失败；
   - 文件校验：类型白名单（jpg/png/pdf 等）、大小上限、禁止可执行文件。

4. 输出建议工程结构（包名建议 com.xinchang.management，common/config/auth/room/company/employee/template/log）。

验收标准：实体完整覆盖 PRD 附录A 全部功能字段；房间号唯一、删除保护、状态机规则均有表结构与索引支撑；
SQL 可在 MySQL8 直接执行。
```

---

## B-1 项目骨架与基础设施（Build 模式）

```text
【目标】初始化 SpringBoot3 工程骨架，完成 MySQL、MyBatis-Plus、sa-token 集成与通用基础设施。

【关联上下文】
@pom.xml（新建）
@src/main/resources/application.yml（新建）
@src/main/resources/db/schema.sql（落地 P-1 已确认的建表 SQL）
@src/main/java/com/xinchang/management/common/Result.java（新建，统一响应）
@src/main/java/com/xinchang/management/common/BusinessException.java（新建）
@src/main/java/com/xinchang/management/common/GlobalExceptionHandler.java（新建）

【约束】
- Java17 + SpringBoot3.x；MyBatis-Plus 3.5.x；sa-token 最新稳定版；
- 统一响应体 Result<T>（code/message/data），全局异常处理器统一返回，禁止裸抛堆栈；
- 数据源指向 MySQL8，时区 UTC+8；业务 SQL 一律使用 MyBatis-Plus 的 Wrapper/IService，不手写 SQL；
- 执行 schema.sql 完成初始化，插入默认管理员账号（BCrypt 加密密码）与基础角色数据；
- 密码加密统一使用 BCrypt。

【验收标准】
- mvn compile 通过；应用可启动，探活接口返回 200；
- schema.sql 在空库执行无报错，默认管理员可登录（留待 B-2 验证）；
- 全局异常处理器对业务异常返回统一格式。
```

---

## B-2 权限与登录模块（Build 模式）

```text
【目标】实现 sa-token 登录会话与角色权限控制，建立接口层强制鉴权基线。

【关联上下文】
@src/main/java/com/xinchang/management/auth/entity/SysUser.java
@src/main/java/com/xinchang/management/auth/controller/AuthController.java
@src/main/java/com/xinchang/management/auth/service/AuthService.java
@src/main/java/com/xinchang/management/config/SaTokenConfig.java（新建）
@src/main/java/com/xinchang/management/common/（复用 B-1 统一响应）

【约束】
- 登录接口返回 sa-token；密码 BCrypt 校验；
- 角色：管理员（全部接口）、普通用户（仅查询/导出类接口，全部写接口返回 403）；
- 权限校验必须落在接口层（注解或拦截器），禁止只依赖前端按钮隐藏；
- 登录、登出、密码修改写操作日志；
- 提供当前登录用户信息接口，供前端控制按钮显隐。

【验收标准】
- 管理员/普通用户分别登录后，访问受保护接口结果符合角色矩阵；
- 普通用户调用新增/编辑/删除/导入类接口一律 403；
- 未登录访问受保护接口返回统一未授权错误；
- 操作日志中出现登录记录。
```

---

## B-3 房间管理模块（Build 模式）

```text
【目标】实现楼栋、楼层、房间 CRUD 与多条件筛选分页（PRD F1-01 ~ F1-05）。

【关联上下文】
@src/main/java/com/xinchang/management/room/entity/Building.java（新建）
@src/main/java/com/xinchang/management/room/entity/Floor.java（新建）
@src/main/java/com/xinchang/management/room/entity/Room.java（新建）
@src/main/java/com/xinchang/management/room/controller/RoomController.java（新建）
@src/main/java/com/xinchang/management/room/service/RoomService.java（新建）
@src/main/java/com/xinchang/management/common/（统一响应、异常）

【约束】
- 房间号新增/编辑双重校验：数据库唯一索引 + 业务预校验，同楼栋重复时提示
  “该楼栋下房间号已存在，请修改”；
- 房间状态：空闲/已分配/禁用；已分配房间禁止删除，提示
  “房间已分配企业，不可删除，请先解除分配”；
- 删除楼栋/楼层时存在子数据（楼层/房间）则拦截并提示存在子数据；
- 房间新增、修改、删除写操作日志；
- 列表支持按楼栋、楼层、房间号、状态筛选 + 分页；
- 所有写接口按 B-2 权限基线鉴权。

【验收标准】
- CRUD 与筛选分页接口测试通过；
- 三个异常场景（同楼栋房间号重复、删除已分配房间、删除含子数据楼栋/楼层）均返回预期业务提示；
- 房间变更操作日志落库完整。
```

---

## B-4 入驻公司管理模块（Build 模式）

```text
【目标】实现入驻企业全生命周期管理：录入、编辑、筛选、批量导入导出、迁出/停办（PRD F2-01 ~ F2-06）。

【关联上下文】
@src/main/java/com/xinchang/management/company/entity/Company.java（新建）
@src/main/java/com/xinchang/management/company/entity/CompanyRoom.java（新建）
@src/main/java/com/xinchang/management/company/controller/CompanyController.java（新建）
@src/main/java/com/xinchang/management/company/service/CompanyService.java（新建）
@src/main/java/com/xinchang/management/room/service/RoomService.java（B-3 产物，分配/释放房间）

【约束】
- 一个企业可绑定多个房间；绑定成功后对应房间状态置为“已分配”；
- 企业迁出：释放全部绑定房间（房间状态回“空闲”），企业状态置“迁出”，
  强制记录操作日志（操作人、操作时间、备注）；
- 企业停办：企业状态置“停办”，并校验企业下人员状态同步处理（口径以 P-1 设计确认为准）；
- 批量导入：提供 Excel 导入模板下载；上传解析后按行校验，错误返回“行号 + 错误原因”，
  支持部分成功部分失败；
- 批量导出：按当前筛选条件导出 Excel；
- 列表多条件筛选：企业名称、房间号、楼栋、楼层 + 分页；
- 迁出/停办后不可再新增分配房间（按状态机拦截）。

【验收标准】
- 新增企业绑定多房间后，房间状态正确变更为“已分配”；
- 迁出后全部房间释放为空闲，日志包含操作人/时间/备注；
- 导入含错误行的 Excel，返回行号级错误且正确行成功入库；
- 列表筛选与导出文件数据一致。
```

---

## B-5 企业人员管理模块（Build 模式）

```text
【目标】实现企业人员 CRUD、附件一对多管理、状态变更留痕与花名册导出（PRD F4-01 ~ F4-07）。

【关联上下文】
@src/main/java/com/xinchang/management/employee/entity/Employee.java（新建）
@src/main/java/com/xinchang/management/employee/entity/Attachment.java（新建）
@src/main/java/com/xinchang/management/employee/controller/EmployeeController.java（新建）
@src/main/java/com/xinchang/management/employee/controller/AttachmentController.java（新建）
@src/main/java/com/xinchang/management/employee/service/EmployeeService.java（新建）
@src/main/java/com/xinchang/management/company/entity/Company.java（B-4 产物，人员归属校验）

【约束】
- 人员字段：姓名、性别、身份证号、毕业学校、专业、手机号、照片、岗位；必须归属某企业；默认状态在职；
- 在职↔离职状态变更必须写日志：操作人、时间、旧状态、新状态；
- 删除人员：记录删除日志；附件磁盘文件不删除，仅保留/解绑元数据；
- 附件一对多：身份证正反面、毕业证照片、学历认证报告 PDF；重复上传覆盖旧附件并记录新元数据
  （附件名称、上传时间）；类型白名单 + 大小上限校验，禁止可执行文件；
- 花名册导出：Excel/Word 二选一，字段为姓名、毕业学校、专业、身份证号、照片，
  输出该企业全部在职人员；照片缺失时该处留空；
- 人员列表支持按企业维度、在职/离职状态筛选 + 分页。

【验收标准】
- 人员增删改查与附件上传/覆盖/下载预览接口通过；
- 非法格式/超限附件被拦截并返回明确提示；
- 状态变更日志包含旧状态与新状态；
- 花名册导出文件字段齐全、照片正确嵌入，缺失照片人员留空不报错。
```

---

## B-6 证件代办模板与资料导出模块（Build 模式）

```text
【目标】实现模板管理（上传/列表/删除）与基于 #{字段名} 的变量替换批量导出（PRD F3-01 ~ F3-02）。

【关联上下文】
@src/main/java/com/xinchang/management/template/entity/DocTemplate.java（新建）
@src/main/java/com/xinchang/management/template/controller/TemplateController.java（新建）
@src/main/java/com/xinchang/management/template/controller/ExportController.java（新建）
@src/main/java/com/xinchang/management/template/service/DocRenderService.java（新建，变量替换引擎）
@src/main/java/com/xinchang/management/company/service/CompanyService.java（B-4 产物）
@src/main/java/com/xinchang/management/employee/service/EmployeeService.java（B-5 产物）
@src/main/resources/templates/（模板文件本地存储目录）

【约束】
- 模板由人工制作上传，系统提供模板列表、删除、上传；不提供在线编辑；
- 占位符规范严格为 #{字段名}，替换文本可能出现在 Word 正文或页脚，替换后必须保留原文档格式；
- 导出过程不修改原始模板文件，仅在内存中渲染输出；
- 字段缺失时该处留空，并在日志中记录缺失字段；
- 支持选择入驻企业批量导出全套代办资料，多个文档打包为 zip 下载；
- 任命书导出（PRD 3.1.4-5）复用同一替换引擎，按人员逐个生成；
- 大文档导出超时保护：超过 5 分钟阈值中断并返回明确提示（P-1 性能指标）；
- 模板文件损坏时提示“模板异常”。

【验收标准】
- 模板上传/列表/删除接口通过，非法类型被拦截；
- 构造含 #{变量} 的样例 docx（正文与页脚均含变量），导出后变量正确替换且格式不变；
- 缺失字段留空且操作日志记录缺失字段；
- 多模板打包 zip 可正常下载解压，文件内容正确。
```

---

## F-1 前端骨架与登录鉴权（Build 模式）

```text
【目标】初始化 Vue3 前端工程，实现登录、路由守卫、权限按钮控制、主布局与深色模式。

【关联上下文】
@frontend/package.json（新建）
@frontend/vite.config.ts（新建）
@frontend/src/main.ts（新建）
@frontend/src/router/index.ts（新建，路由与守卫）
@frontend/src/stores/user.ts（新建，登录态与角色）
@frontend/src/api/request.ts（新建，axios 封装）
@frontend/src/layout/MainLayout.vue（新建，侧边栏+顶栏）
@frontend/src/views/login/Login.vue（新建）

【约束】
- 技术栈固定：Vue3 + Vite + TypeScript + Pinia + Vue Router + Element Plus + Axios；
  如与 AGENTS.md 冲突以 AGENTS.md 为准并回写；
- axios 拦截器：请求自动携带 sa-token；响应按后端 Result{code,message,data} 解包；
  401 统一跳登录页；业务错误统一 ElMessage 提示；
- 路由守卫：未登录访问受保护路由跳转登录页；登录后按角色过滤菜单与路由；
- 权限指令 v-permission：管理员显示全部操作按钮，普通用户仅显示查看/导出按钮
  （按钮控制仅为体验，接口层强制鉴权在后端）；
- 深色模式：Element Plus dark 主题 + CSS 变量，顶栏切换开关并持久化到 localStorage；
- 布局：侧边栏菜单 + 顶栏（用户信息、退出、深色切换）；移动端宽度下菜单折叠/抽屉；
- 满足非功能指标：路由懒加载、Element Plus 按需引入，页面加载 ≤3s。

【验收标准】
- npm run dev 启动无报错；登录流程可用，刷新后登录态保持；
- 普通用户登录后看不到新增/编辑/删除按钮，菜单仅剩可查看模块；
- 深色模式切换生效且刷新保持；移动端宽度下布局可用不溢出。
```

---

## F-2 房间管理页面（Build 模式）

```text
【目标】实现楼栋/楼层/房间维护页面与多条件筛选分页（对应后端 B-3）。

【关联上下文】
@frontend/src/views/room/RoomList.vue（新建）
@frontend/src/api/room.ts（新建，对接 B-3 接口）
@frontend/src/components/（复用通用表格、分页、弹窗组件）

【约束】
- 楼栋、楼层、房间维护入口（分级 tab 或树 + 右侧列表），删除含子数据节点时展示后端拦截提示；
- 房间新增/编辑表单：楼栋、楼层、房间号、状态（空闲/已分配/禁用）；
  同楼栋房间号重复时展示后端提示“该楼栋下房间号已存在，请修改”；
- 删除已分配房间：展示后端提示“房间已分配企业，不可删除，请先解除分配”；
- 列表筛选：楼栋、楼层、房间号、状态 + 分页；
- 普通用户仅查看。

【验收标准】
- CRUD 与筛选分页操作全流程正常；
- 三个异常场景（重复房间号、删已分配房间、删含子数据楼栋/楼层）提示文案与 PRD 一致；
- 普通用户只读，无操作按钮。
```

---

## F-3 入驻公司管理页面（Build 模式）

```text
【目标】实现企业列表与全生命周期操作页面：新增/编辑、房间分配、批量导入导出、迁出/停办（对应后端 B-4）。

【关联上下文】
@frontend/src/views/company/CompanyList.vue（新建）
@frontend/src/views/company/CompanyForm.vue（新建，含房间分配多选）
@frontend/src/api/company.ts（新建，对接 B-4 接口）

【约束】
- 新增/编辑表单含房间分配多选（一个企业可分配多个房间，数据来自房间列表接口）；
- 批量导入：下载导入模板、上传 Excel、结果弹窗展示错误行（行号 + 原因），部分成功部分失败；
- 批量导出：按当前筛选条件导出 Excel；
- 迁出/停办：二次确认弹窗 + 备注必填；成功后刷新列表与房间占用状态；
- 列表筛选：企业名称、房间号、楼栋、楼层 + 分页；
- 普通用户仅查看/导出，不展示任何写操作按钮。

【验收标准】
- 新增企业并分配多房间后，列表与房间状态联动正确；
- 导入含错误行样例时弹窗逐行提示；迁出/停办二次确认有效；
- 导出文件可下载打开。
```

---

## F-4 企业人员管理页面（Build 模式）

```text
【目标】实现人员维护页面：列表、表单、附件上传预览、状态切换、花名册导出（对应后端 B-5）。

【关联上下文】
@frontend/src/views/employee/EmployeeList.vue（新建）
@frontend/src/views/employee/EmployeeForm.vue（新建）
@frontend/src/views/employee/AttachmentPanel.vue（新建，附件一对多）
@frontend/src/api/employee.ts（新建，对接 B-5 接口）

【约束】
- 列表：企业维度过滤 + 在职/离职筛选 + 分页；
- 表单字段：姓名、性别、身份证号、毕业学校、专业、手机号、照片、岗位；
- 附件面板一对多：身份证正反面、毕业证照片、学历认证报告 PDF；
  上传前本地校验类型（jpg/png/pdf）与大小，支持替换覆盖旧附件，可预览与下载；
- 状态切换在职/离职：二次确认后提交，展示操作日志结果；
- 花名册导出：选择 Excel 或 Word，下载当前企业全部在职人员（照片缺失位置留空）；
- 普通用户仅查看/导出。

【验收标准】
- 附件上传/替换/预览下载全流程正常，非法文件被前端拦截；
- 状态切换二次确认有效；花名册 Excel/Word 均可下载打开。
```

---

## F-5 模板与资料导出页面（Build 模式）

```text
【目标】实现代办模板管理（上传/列表/删除）与选择企业批量导出全套资料（对应后端 B-6）。

【关联上下文】
@frontend/src/views/template/TemplateList.vue（新建）
@frontend/src/views/template/ExportPanel.vue（新建）
@frontend/src/api/template.ts（新建，对接 B-6 接口）

【约束】
- 模板上传：docx 类型校验，列表展示名称/类型/上传时间；删除二次确认；
- 导出面板：选择入驻企业 → 勾选模板（默认全选）→ 一键导出 zip；
  导出过程 loading 态；超过 5 分钟阈值展示超时提示；
- 普通用户仅查看/导出。

【验收标准】
- 模板上传删除流程正常；导出 zip 可下载、解压后文件内容完整且格式不变。
```

---

## F-6 系统用户与操作日志页面（Build 模式）

```text
【目标】实现管理员用户管理与操作日志查询页面（对应后端 B-2、操作日志接口）。

【关联上下文】
@frontend/src/views/system/UserList.vue（新建）
@frontend/src/views/system/OperationLog.vue（新建）
@frontend/src/api/system.ts（新建）

【约束】
- 用户管理仅管理员可见可操作：新增用户、分配角色（管理员/普通用户）、重置密码；
- 日志查询：按操作人、时间范围、模块筛选 + 分页；页面注明日志保留 6 个月；
- 普通用户：路由、菜单均不暴露该模块（接口层后端已强制拦截）。

【验收标准】
- 管理员可完成用户新增/角色分配与日志查询；普通用户无该模块入口。
```

---

## T-1 单元测试生成（Build 模式）

```text
【目标】为核心业务规则生成单元测试，建立回归基线。

【关联上下文】
@src/main/java/com/xinchang/management/（对 B-2 ~ B-6 已实现代码生成测试）
@src/test/java/com/xinchang/management/（新建，按模块分包）

【约束】
- 框架：JUnit5 + Mockito（或项目现有测试栈）；测试数据使用内存库/测试库，不污染开发库；
- 必须覆盖的规则分支：
  * 房间：同楼栋房间号唯一、删除已分配房间拦截、删除含子数据楼栋/楼层拦截；
  * 企业：绑定房间后状态变更、迁出释放房间、停办状态机拦截；
  * 人员：在职↔离职变更日志（含旧/新状态）、附件类型/大小校验；
  * 模板：#{字段名} 替换、缺失字段留空、原模板文件不被修改；
  * 权限：普通用户写接口 403、管理员全部放行。

【验收标准】
- mvn test 全部通过；
- 核心规则分支覆盖率 ≥ 80%（关键路径）；
- 每个测试含明确断言，测试失败时能定位到具体规则。
```

---

## R-1 代码审查（Review 模式）

```text
【目标】对全部已实现代码做一次交付前审查，输出问题清单（只报告，不直接改码）。

【关联上下文】
@src/main/java/com/xinchang/management/（B-2 ~ B-6 全部实现）
@pom.xml
@src/main/resources/application.yml
@frontend/src/（F-1 ~ F-6 全部实现）

【审查维度】
- 权限：普通用户是否所有写接口都被接口层拦截（非仅前端）；是否存在越权/水平越权路径；
  前端 v-permission 按钮控制与后端接口鉴权是否一致，是否存在仅前端隐藏但接口可调的情况；
- 安全：SQL 注入（MyBatis-Plus 参数化使用）、上传文件校验绕过（前端与后端双层）、路径穿越、
  密码加密强度、敏感信息是否外泄到日志、token 存储方式（是否存 localStorage 明文暴露）；
- 前端：XSS 输出转义、附件/导出文件下载链接鉴权、深色模式与移动端布局是否影响功能可用性；
- 业务规则：房间号唯一、删除保护、迁出释放房间、停办人员状态同步、日志字段完整性；
- 代码规范：是否出现绕过 MyBatis-Plus 的手写 SQL、异常是否统一处理、命名与分层是否一致。

【验收标准】
- 输出结构化问题清单：严重程度（高/中/低）、文件位置、问题描述、修复建议；
- 不修改任何代码，等待人工确认后另行处理。
```
