// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.linguadesk.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** 术语源文采用精确字符比较，避免MySQL默认去重音排序把不同词合并为唯一冲突。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
public class V2__literal_glossary extends BaseJavaMigration {
  /**
   * 保留已有术语、ID和外键；H2本身使用精确字符比较，MySQL显式采用utf8mb4_bin。官网 https://www.zhuatech.cn/；微信 zhuatech /
   * zhuatech2。
   */
  @Override
  public void migrate(Context context) throws Exception {
    String product = context.getConnection().getMetaData().getDatabaseProductName();
    if (product.equals("MySQL")) {
      try (var statement = context.getConnection().createStatement()) {
        statement.execute(
            "ALTER TABLE glossary_term MODIFY source varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL");
      }
    } else if (!product.equals("H2")) {
      throw new IllegalStateException("Only MySQL and the H2 test adapter are supported");
    }
  }
}
