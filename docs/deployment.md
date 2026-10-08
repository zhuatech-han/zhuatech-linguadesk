知华科技（上海如静知华信息科技有限公司） · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2

# 部署、升级与备份

环境：Docker Engine / Desktop 与 Compose v2（支持 `--wait`）；完整构建需要联网下载官方依赖。独立开发需要 Java 21、Maven 3.9、Node.js 24.19.0 或更高兼容版本、Python 3.11+（验收脚本）。默认端口 8126，仅绑定 127.0.0.1，MySQL 和后端不对宿主公开。

```bash
python3 scripts/init-env.py
docker compose -p linguadesk up -d --build --wait --wait-timeout 180
```

初始化脚本只创建一次 `.env`（0600），拒绝覆盖；没有固定演示密码。首次登录为 `ADMIN_USERNAME`（默认 admin）及本机 `.env` 的 `ADMIN_PASSWORD`。数据库已有账号时重启不改密码，编辑 `.env` 不会重置既有账号。通过账号管理安全重置；保留第二位完整管理员有助于正常管理，但不能使用共享公开默认密码。

访问 http://127.0.0.1:8126/；健康检查 http://127.0.0.1:8126/actuator/health。查看状态与日志：`docker compose -p linguadesk ps`、`docker compose -p linguadesk logs --tail=100 backend`。不要公开包含凭证或客户文案的日志。

公网部署需先取得商业授权，再自行配置域名、可信 TLS、备份、监控及必要边界防护。HTTPS 反向代理后设置 COOKIE_SECURE=true；TLS 终止代理须向内部服务传递正确 X-Forwarded-Proto，应用仅信任部署边界的代理。非浏览器登录调试勿设置错误的 HTTPS Cookie 条件。默认网关拒绝跨站脚本/嵌入、关闭访问日志；登录失败限速基于实例内存，后端重启清空，不能替代公网入口限速。

外部数据库可设置 DATABASE_URL/DATABASE_USER；使用受信任 CA 和 `sslMode=VERIFY_IDENTITY`。默认 sslMode=REQUIRED 仅适用于隔离 Compose 内部服务，不能称为外部主机身份验证。数据库与账号的生产配置需独立维护。本项目备份脚本只支持随 Compose 的内置数据库。

升级先备份、读取发布兼容说明，再 `docker compose -p linguadesk up -d --build --wait`；Flyway 自动验证并执行新迁移。不得修改已运行的 V1，后续改表用 V2 等增量迁移；Hibernate ddl-auto=validate 不自动改表。当前 V1 建表，V2 保持术语重音字符区分；全新安装、V1→V2及同版重启已验证，未宣称跨历史版本升级覆盖。不要用 `down -v` 升级或解决迁移错误。

```bash
python3 scripts/backup.py --project linguadesk --output private-backups/linguadesk.zip
# 手工建立单独恢复环境 .env（独立强密码、不同 WEB_PORT、127.0.0.1），不能覆盖原环境。
python3 scripts/restore.py private-backups/linguadesk.zip --project linguadesk-recovery --env-file /absolute/private/recovery.env
```

备份会短暂停止该实例后端，完成内置数据库一致性导出后恢复服务。ZIP 包含 SQL、时间、大小和校验值，权限 0600；SQL 含账号密码散列和所有业务文案，必须私有保存并做访问限制/加密，不得上传源码平台。恢复校验成员、产品、大小和 SHA 后，仅向没有容器、网络和数据卷的新项目恢复；最大 SQL 512 MiB。校验值不是签名，只能恢复自己生成的可信备份，不能执行别人发来的任意 SQL 包。恢复后先检查健康、登录、角色范围、项目记录及交付文件 SHA，再决定使用。只清理自己的测试资源，不动既有业务数据库。

源码没有自动邮件、SSO、Git/CI、支付、云存储或机器翻译集成；无须提供此类密钥。
