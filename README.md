# LinguaDesk · 知华软件多语言翻译协作系统

[中文](README.md) | [English](README.en.md)

<img src="frontend/public/brand/logo.jpg" alt="知华科技" width="54" height="54">

**知华科技（上海如静知华信息科技有限公司）** · [官网](https://www.zhuatech.cn/)

LinguaDesk 为软件出海团队、独立应用开发者及软件本地化协作团队提供可自部署的文案工作区：导入界面 JSON，分配译员，逐条翻译与独立审校，发布不被后续修改覆盖的语言文件。后端 Java 21 / Spring Boot / MySQL，前端 Vue 3。源码公开、非商业使用；商用须书面授权。

它解决“表格与聊天文件中漏译、变量丢失、审校与交付版本不一致”的协作问题。每个项目对应一个目标语言，可管理多项目及不同语言对。规则检查辅助人工判断，不证明翻译语义正确。没有机器翻译，也不会向外部模型发送语料。

## 从原文到可交付文件

1. 管理员建立项目经理、译员和独立审校账号；项目经理建项目并导入扁平 JSON 原文。
2. 维护上下文、目标字符上限与术语。JSON 先验证预览，再原子导入；无效或重复键整批拒绝。
3. 指定译员在双语编辑台保存或送审，可人工使用同团队、同语言对、当前可见项目的已审批精确匹配建议。
4. 指定审校通过或退回具体版本。空译文、变量名称/次数差异、字符超限阻断审批；数字、术语和相同原译文提示可在记录具体原因后接受。
5. 全部当前文案通过独立审校才可冻结 JSON 与双语 CSV。原文或译文变更使相应审校失效，旧交付保持不变；重新审校和发布后可关单。

| 已实现模块 | 操作与边界 |
|---|---|
| 项目管理 | 新建、期限/指派修改、草稿/开始/暂停/恢复/关单/带原因取消；固定团队与语言对 |
| 文案管理 | 扁平 JSON 文件/粘贴导入、验证预览、合并或替换、逐条新增/修改上下文/字符上限、归档、搜索与状态筛选 |
| 翻译与审校 | 保存/送审/退回/通过、版本冲突、明确变量/长度阻断、数字/术语提示、独立身份、防自审 |
| 项目术语 | 草稿增改删，开始后冻结；区分大小写字面检查 |
| 修订与交付 | 当前文案完整修订、项目状态记录、冻结历史 JSON/CSV、SHA-256、实时权限下载 |
| 业务端 | 译员“我的译稿”、审校“审校队列”、手机布局、中文/英文界面；不自动代替人工翻译 |
| 管理端 | 账号、BCrypt12、密码重置/个人密码修改、角色、权限、团队、菜单、平台字典、参数、最后完整管理员保护 |
| 数据范围 | ALL/DEPARTMENT/ASSIGNED、实时身份检查、接口角色与项目范围；指定译员/审校限制同样适用管理员 |
| 汇总与运维 | 项目首页真实指标、分页/排序、进度报表与CSV、只读审计、健康、内置数据库一致性备份/隔离恢复 |

## 实际运行页面

截图来自隔离测试实例，业务名称与人员均标记 TEST，不含真实客户资料或登录密码。应用首次安装没有这些业务数据。

### 登录工作区

账号由实例管理员分配，不提供共享密码。

![登录工作区](docs/screenshots/01-login.jpg)

### 项目与进度首页

按真实项目、文案和审校结果汇总，支持筛选与分页。

![项目与进度首页](docs/screenshots/02-projects.jpg)

### 译员双语编辑台

原文、上下文、字符上限、送审与修订记录。

![译员双语编辑台](docs/screenshots/03-translator.jpg)

### 独立审校

指定审校处理准确的送审版本；变量丢失和超长不可豁免。

![独立审校](docs/screenshots/04-review.jpg)

### JSON 文件导入

从真实文件读取，先验证、预览新增/变更/归档，再确认导入。

![JSON 文件导入](docs/screenshots/05-import.jpg)

### 项目术语

维护明确源目标术语，开始后冻结。

![项目术语](docs/screenshots/06-glossary.jpg)

### 冻结版本交付

JSON 与双语 CSV 的历史版本，含可核对 SHA-256。

![冻结版本交付](docs/screenshots/07-deliveries.jpg)

### 账号管理

真实登录身份、团队、角色、启用状态和密码重置。

![账号管理](docs/screenshots/08-accounts.jpg)

### 角色与权限

接口权限、团队和指派范围由服务端校验。

![角色与权限](docs/screenshots/09-roles.jpg)

### 团队与系统参数

可管理 IANA 时区、平台、菜单及实例上限。

![团队与系统参数](docs/screenshots/10-settings.jpg)

### 进度报表

当前范围内翻译、审校、规则异常、期限与 CSV 导出。

![进度报表](docs/screenshots/11-reports.jpg)

### 操作审计

账号与业务操作真实持久化，不记录凭证。

![操作审计](docs/screenshots/12-audit.jpg)

### 手机界面

窄屏导航、文案列表和编辑面板。

![手机界面](docs/screenshots/13-mobile.jpg)

### 英文工作区

相同业务与权限，界面可切换英文。

![英文工作区](docs/screenshots/14-english.jpg)

## 架构与目录

浏览器 → Nginx 同源服务 → Spring Boot / Security / JPA → MySQL / Flyway。前后端分离，账号和业务数据来自数据库。关键依赖版本：Spring Boot 4.0.7、Vue 3.5.40、Vite 8.1.5、Node 24.19.0、MySQL 8.4、Maven 3.9 / Java 21。运行元数据和锁文件是准确依赖依据。

```text
backend/              Java 服务、认证、业务、目录与测试
  src/main/resources/db/migration/V1__lingua_schema.sql
frontend/             Vue 页面、英文/中文界面、Nginx及前端测试
docs/                 架构、接口、部署、安全、操作手册与真实截图
scripts/              私有环境初始化、HTTP验收、备份恢复、发布检查
compose.yaml          内置MySQL + 后端 + Nginx前端
.env.example          配置字段，不含真实凭证
LICENSE               自有非商业源码许可
```

15 张应用表及 Flyway 迁移历史：系统身份与目录、项目、文案、术语、不可覆盖修订、状态事件、交付文件。`V1__lingua_schema.sql` 建表/外键/索引；V2 迁移保持MySQL术语重音字符区分，不覆盖既有记录；Bootstrap 只初始化系统目录和私有管理员，没有虚构生产案例。Hibernate `ddl-auto=validate`，不会自动建表绕过迁移。

## 安装与首次使用

环境要求：Docker / Compose v2（支持 `--wait`）、可下载官方依赖的网络；初始化脚本 Python 3.11+。本机源码开发需要 Java 21 / Maven 3.9、Node 24.19.0 或兼容更高版本及 MySQL 8。

```bash
python3 scripts/init-env.py
docker compose -p linguadesk up -d --build --wait --wait-timeout 180
```

访问 [本机工作区](http://127.0.0.1:8126/)；[健康检查](http://127.0.0.1:8126/actuator/health)。首次账号默认 `admin`（由 ADMIN_USERNAME 配置），密码取本机私有 `.env` 的 ADMIN_PASSWORD。初始化脚本生成独立随机强密码并以0600保存，拒绝覆盖；**没有共享公开默认密码**。已有账号时重启不重置密码，修改 `.env` 不能替代账号重置。登录后先创建译员与审校，按 [操作手册](docs/operations.md) 完成实际业务。

数据库由 Compose 独立卷提供，无须复用电脑旧库；Flyway 自动初始化。升级前备份，后续改表使用新版本迁移，不修改已经执行的V1，也不使用 `down -v` 升级。当前验证全新安装、V1→V2术语排序修正与同版重启，未宣称跨历史版本迁移全覆盖。

本机开发：Spring Boot 不会自动读取 `.env`，需向当前进程导出 `.env.example` 对应私有环境变量；将 DATABASE_URL 指向自己的 MySQL、DATABASE_USER/DATABASE_PASSWORD 对应实例；`mvn -f backend/pom.xml spring-boot:run`，另在 frontend 执行 `npm ci && npm run dev`。Vite 默认端口5173，将同源 `/api` 代理到127.0.0.1:8080。不要将真实环境文件或凭证提交。

## 配置与部署

| 配置 | 说明 |
|---|---|
| MYSQL_ROOT_PASSWORD / DATABASE_PASSWORD | 内置数据库独立强密码 |
| ADMIN_USERNAME / ADMIN_PASSWORD | 仅首次Bootstrap管理员；既有密码通过账号管理修改 |
| WEB_PORT / BIND_ADDRESS | 默认8126 / 127.0.0.1；后台及MySQL不公开 |
| COOKIE_SECURE | 本机HTTP为false；可信HTTPS代理后设true |
| DATABASE_URL / DATABASE_USER | 可选外部MySQL；需可信TLS与VERIFY_IDENTITY |

系统参数：max_segments 默认2000（1–2000），due_warning_days 默认3（1–30）；临期/逾期按项目团队IANA时区判断。源/译文最多4000 UTF-16单元，字符上限按Unicode code point；导入JSON512000字节。稳定键仅ASCII字母/数字及 `_ . : / -`，不允许仅大小写不同的重复键。菜单、权限和参数代码固定，不支持任意新增路由或执行服务器脚本。

完整 [部署与恢复说明](docs/deployment.md)、[接口与参数](docs/api.md)、[架构](docs/architecture.md)。公网或真实商业业务部署需先取得授权，并自行完成TLS、备份、监控及安全评估；本仓库不宣称已经生产验收。

```bash
python3 scripts/backup.py --project linguadesk --output private-backups/linguadesk.zip
# 独立恢复env必须私有、不同端口；只向不存在容器/网络/卷的新项目恢复
python3 scripts/restore.py private-backups/linguadesk.zip --project linguadesk-recovery --env-file /absolute/private/recovery.env
```

备份含业务文案和密码散列，保持私有。脚本只支持内置MySQL，拒绝覆盖已有资源；只恢复可信自有备份，校验值不是签名。

## 测试与检查

```bash
mvn -B -f backend/pom.xml spotless:check test package
npm --prefix frontend ci --no-audit --no-fund
npm --prefix frontend run format:check
npm --prefix frontend run lint
npm --prefix frontend test
npm --prefix frontend run build
```

后端包含实际HTTP/JPA/Flyway集成与文本规则反例，前端包含CSRF、失败写入不重放、UTF-8、下载和筛选测试。Docker构建执行测试，不跳过。完整业务验收须使用**全新、可丢弃本机实例**，脚本拒绝非本机与非空项目数据库：

```bash
python3 -m venv .venv
.venv/bin/python -m pip install -r scripts/requirements-quality.txt
.venv/bin/python scripts/quality.py --base http://127.0.0.1:8126
.venv/bin/python scripts/verify-persistence.py --base http://127.0.0.1:8126 --state output/quality-state.json
python3 scripts/release-check.py
docker compose config --quiet
git diff --check
```

验收脚本创建明确TEST账号/业务，输出私有验收状态，绝不能向真实业务库运行。GUI完成额外操作后，重启/恢复比对前可用 `--refresh-snapshot` 更新私有快照。检查包括精确文件SHA和旧审计行，不把仅HTTP200当业务完成。建议同时手动走创建→导入→翻译→审校→发布→下载，并检查最窄需要支持的设备。

常见故障：首次构建下载失败应读日志检查网络；健康失败先看backend/MySQL日志，不删数据库卷。401重新登录；403核对角色、团队与指定人员；409核对状态/最新版本，先刷新再决定重做。空文件、重复键、嵌套JSON不导入；源文更新后审批失效是版本保护。网络断开写入结果未知，刷新查看记录而不要盲目重复提交。

## 已知限制、授权与反馈

这是小团队单实例源码系统，尚无外部用户或商业成交证明。没有机器翻译、SSO/MFA、邮箱找回、Git/CI仓库集成、通知、支付或云存储，无需此类第三方API密钥。无高并发/渗透测试、SaaS多租户隔离或生产适用保证；全局写锁、有界内存汇总与10000行安全上限限制规模。历史修订保留增长，须规划容量和访问管理。

仅支持扁平JSON与明确行输入；不支持嵌套JSON、数组、XLIFF、DOCX、PDF、字幕、完整ICU复数/性别语法或全部printf格式。术语采用字面匹配、数字地区格式可能误报；人工审校负责语义与格式。CSV防公式会改变公式前缀，精确程序交付使用JSON。未实现集成不能称为可配置的已完成能力。安全边界见 [安全说明](docs/security.md)。

自有代码按 [LICENSE](LICENSE) 仅限个人学习、技术研究与非商业交流，未经上海如静知华信息科技有限公司书面授权不得商用。企业内部真实使用、收费部署、SaaS、源码二次销售、商业项目交付和深度定制均须授权；这是“源码公开、非商业使用”，不是OSI标准开源许可。第三方依赖保留其原版权和许可。软件按现状提供，不作生产适用保证。

欢迎提交与真实功能相关的可复现问题和小范围改进，附版本、步骤、预期及脱敏证据；遵守既有许可证、测试和署名。不要上传密码、真实业务文案或数据库备份。安全漏洞请通过官网或商业咨询渠道私下反馈。

## 联系知华科技

商业授权或深度定制开发请联系知华科技。

- 公司：知华科技（上海如静知华信息科技有限公司）
- 官网：[https://www.zhuatech.cn/](https://www.zhuatech.cn/)
- 微信：`zhuatech`、`zhuatech2`
- 服务：软件定制、源码商业授权、私有化部署、二次开发及系统集成。

<table><tr><td align="center"><img src="docs/images/wechat-zhuatech.png" alt="微信 zhuatech" height="200"><br>zhuatech</td><td align="center"><img src="docs/images/wechat-zhuatech2.png" alt="微信 zhuatech2" height="200"><br>zhuatech2</td></tr></table>

本项目由知华科技（上海如静知华信息科技有限公司）提供公开源码学习版本，主要用于个人学习、技术研究与非商业交流。未经书面授权不得商用。企业信息化建设、中小企业数字化转型、中小企业 AI 转型、私有化部署、软件外包、软件项目外包、软件实施、FDE 外包、OPC 技术支持及深度定制开发，请访问知华科技官网 [https://www.zhuatech.cn/](https://www.zhuatech.cn/)，或添加微信 zhuatech、zhuatech2 咨询。
