// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.linguadesk;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 全新数据库仅初始化系统目录与私有管理员，不制造业务案例。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Component
public class Bootstrap implements ApplicationRunner {
  static final List<String> CODES =
      List.of(
          "projects",
          "manage",
          "translate",
          "review",
          "publish",
          "reports",
          "users",
          "roles",
          "settings",
          "audit");
  final Store db;
  final BCryptPasswordEncoder encoder;
  final String username, password;

  /** 首次启动配置；凭证不输出日志。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Bootstrap(
      Store db,
      BCryptPasswordEncoder encoder,
      @Value("${linguadesk.admin-username}") String username,
      @Value("${linguadesk.admin-password}") String password) {
    this.db = db;
    this.encoder = encoder;
    this.username = username;
    this.password = password;
  }

  /** 重启不改写现有账号和权限。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!db.all(Account.class).isEmpty()) return;
    AdminService.validatePassword(password);
    if (!username.matches("[a-zA-Z0-9_.-]{3,60}"))
      throw new IllegalStateException("Invalid initial administrator name");
    for (var c : CODES) {
      var p = new Permission();
      p.code = c;
      p.name = c;
      db.save(p);
    }
    var d = new Department();
    d.name = "主团队 / Main team";
    d.zone = "Asia/Shanghai";
    db.save(d);
    var admin = role("管理员 / Administrator", "ALL", CODES);
    role(
        "项目经理 / Project manager",
        "DEPARTMENT",
        List.of("projects", "manage", "publish", "reports", "audit"));
    role("译员 / Translator", "ASSIGNED", List.of("projects", "translate"));
    role("审校 / Reviewer", "ASSIGNED", List.of("projects", "review"));
    role("只读 / Reader", "DEPARTMENT", List.of("projects", "reports"));
    var a = new Account();
    a.username = username.toLowerCase(Locale.ROOT);
    a.displayName = "管理员 / Administrator";
    a.passwordHash = encoder.encode(password);
    a.roleId = admin.id;
    a.departmentId = d.id;
    db.save(a);
    String[][] menus = {
      {"projects", "翻译项目", "Projects", "projects"},
      {"workbench", "我的译稿", "My translations", "translate"},
      {"review", "审校队列", "Review queue", "review"},
      {"reports", "进度报表", "Reports", "reports"},
      {"users", "登录账号", "Accounts", "users"},
      {"roles", "角色与权限", "Roles & permissions", "roles"},
      {"settings", "团队与设置", "Teams & settings", "settings"},
      {"audit", "操作记录", "Audit trail", "audit"}
    };
    for (int i = 0; i < menus.length; i++) {
      var m = new NavMenu();
      m.code = menus[i][0];
      m.name = menus[i][1];
      m.nameEn = menus[i][2];
      m.permissionCode = menus[i][3];
      m.position = i;
      m.enabled = true;
      db.save(m);
    }
    String[][] types = {{"WEB", "网站", "Website"}, {"APP", "应用", "App"}, {"GAME", "游戏", "Game"}};
    for (var t : types) {
      var x = new DictionaryEntry();
      x.type = "PLATFORM";
      x.code = t[0];
      x.name = t[1];
      x.nameEn = t[2];
      db.save(x);
    }
    setting("max_segments", "2000");
    setting("due_warning_days", "3");
  }

  private AccessRole role(String name, String scope, List<String> codes) {
    var r = new AccessRole();
    r.name = name;
    r.scope = scope;
    r.permissions.addAll(codes);
    return db.save(r);
  }

  private void setting(String code, String value) {
    var x = new SystemSetting();
    x.code = code;
    x.value = value;
    db.save(x);
  }
}
