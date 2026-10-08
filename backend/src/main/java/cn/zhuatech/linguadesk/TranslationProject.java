// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2
package cn.zhuatech.linguadesk;

import jakarta.persistence.*;
import java.time.*;

/** 翻译项目、语言对与指定译员和审校；版本更新使旧请求失效。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "translation_project")
public class TranslationProject {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "department_id", nullable = false)
  public Long departmentId;

  @Column(name = "owner_id", nullable = false)
  public Long ownerId;

  @Column(name = "translator_id", nullable = false)
  public Long translatorId;

  @Column(name = "reviewer_id", nullable = false)
  public Long reviewerId;

  @Column(name = "name", nullable = false, length = 160)
  public String name;

  @Column(name = "source_locale", nullable = false, length = 40)
  public String sourceLocale;

  @Column(name = "target_locale", nullable = false, length = 40)
  public String targetLocale;

  @Column(name = "platform", nullable = false, length = 60)
  public String platform;

  @Column(name = "state", nullable = false, length = 20)
  public String state = "DRAFT";

  @Column(name = "due_date", nullable = false)
  public LocalDate dueDate;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  @Column(name = "version", nullable = false)
  public long version = 1;
}
