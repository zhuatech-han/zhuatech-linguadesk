// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2
package cn.zhuatech.linguadesk;

import jakarta.persistence.*;
import java.time.*;

/** 按项目冻结的明确术语对；匹配采用区分大小写的字面包含。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "glossary_term")
public class GlossaryTerm {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "project_id", nullable = false)
  public Long projectId;

  @Column(name = "source", nullable = false, length = 200)
  public String source;

  @Column(name = "target", nullable = false, length = 200)
  public String target;

  @Column(name = "note", nullable = false, length = 500)
  public String note = "";

  @Column(name = "version", nullable = false)
  public long version = 1;
}
