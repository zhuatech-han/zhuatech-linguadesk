知华科技（上海如静知华信息科技有限公司） · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2

# 接口与配置

认证：`GET /api/auth/csrf` 返回 header/token；POST login 使用真实账号密码和相应 CSRF 头，会话为 HttpOnly Cookie。GET me 返回最新菜单与权限；POST logout 使当前会话失效；POST password 检查旧密码并失效旧会话。没有浏览器 LocalStorage Token。原始密码不返回、不记录，数据库仅 BCrypt 12。

| 接口 | 能力与权限 |
|---|---|
| GET/POST /api/projects | 分页读取（projects）/创建（manage） |
| GET/PUT /api/projects/{id} | 详情/配置与指派 |
| GET /api/options | 范围内团队、可指派账号和平台 |
| POST /api/projects/{id}/import/preview | 验证导入，不写库 |
| POST /api/projects/{id}/import | 原子导入，更新原文，归档缺失键 |
| POST/PUT/DELETE /api/projects/{id}/terms[/{termId}] | 草稿术语管理 |
| POST /api/projects/{id}/actions/{action} | start/pause/resume/cancel/close |
| GET/PUT/DELETE /api/projects/{id}/segments/{sid} | 修订详情/指定译员编辑/经理归档 |
| GET /api/projects/{id}/segments/{sid}/memory | 可见已审批精确匹配建议 |
| POST /api/projects/{id}/segments/{sid}/review | 指定独立审校审批/退回 |
| POST /api/projects/{id}/deliveries | 冻结当前完整审批内容 |
| GET/HEAD /api/projects/{id}/deliveries/{did}/{json/csv} | 实时鉴权下载固定历史内容 |
| GET /api/reports、GET/HEAD /api/reports.csv | 范围内汇总和CSV |
| GET /api/audit | 团队范围只读操作审计 |
| GET/POST/PUT /api/admin/{kind}[/{id}] | users/roles/departments/dictionaries/menus/permissions/settings；独立目录权限 + ALL |
| GET /actuator/health | 健康，不泄露配置 |

项目读取支持 q、state、page（1 起）、size（1–100）、sort=newest/due、mine=translate/review。界面默认页大小 20；原文导入为 `{version,replace,content}`（原始扁平JSON）或 `{version,replace,rows:[{key,source,context,maxLength}]}`。预览没有 version，可随时再次验证，确认仍需最新项目版本。文案写入 `{version,target,submit}`，审校 `{version,approve,note}`，归档 `{version:项目版本,segmentVersion:文案版本}`。账号/角色/团队变更同样检查版本。

状态错误为409，权限不足403，失效会话401，输入400，限量413，登录频繁429；响应仅 code（无SQL、堆栈、凭证）。事务失败整批回滚。网络断开时写入结果未知，前端不自动重试；先读详情再判断结果。

| 环境变量 | 说明 |
|---|---|
| MYSQL_ROOT_PASSWORD | 内置数据库独立强密码，不公开 |
| DATABASE_PASSWORD | 应用数据库密码，与Compose一致 |
| ADMIN_USERNAME | 首次管理员，默认 admin；稳定小写用户名 |
| ADMIN_PASSWORD | 首次私有强密码，Bootstrap不覆盖已有账号 |
| WEB_PORT / BIND_ADDRESS | 默认8126 / 127.0.0.1 |
| COOKIE_SECURE | 默认false用于本机HTTP；HTTPS部署设true |
| DATABASE_URL / DATABASE_USER | 可选外部MySQL连接；需可信TLS/身份验证 |

系统参数 max_segments=2000（1–2000），due_warning_days=3（1–30）。源文/目标文案长度最大4000 UTF-16单元，上下文1000，规则上限按Unicode code point计算。不接收文件路径，不支持任意服务器文件读取。交付文件名由项目ID、版本和固定格式生成。
