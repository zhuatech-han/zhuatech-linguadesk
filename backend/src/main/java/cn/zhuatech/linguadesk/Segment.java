// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2
package cn.zhuatech.linguadesk;

import jakarta.persistence.*;
import java.time.*;

/** 逐条源文、译文、上下文、字符上限与版本绑定的审校结果。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "segment")
public class Segment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "project_id", nullable = false)
  public Long projectId;

  @Column(name = "string_key", nullable = false, length = 160)
  public String key;

  @Column(name = "source", nullable = false, length = 4000)
  public String source;

  @Column(name = "target", nullable = false, length = 4000)
  public String target = "";

  @Column(name = "context", nullable = false, length = 1000)
  public String context = "";

  @Column(name = "max_length", nullable = false)
  public int maxLength = 0;

  @Column(name = "active", nullable = false)
  public boolean active = true;

  @Column(name = "status", nullable = false, length = 20)
  public String status = "UNTRANSLATED";

  @Column(name = "edited_by", nullable = true)
  public Long editedBy;

  @Column(name = "reviewed_by", nullable = true)
  public Long reviewedBy;

  @Column(name = "review_note", nullable = false, length = 1000)
  public String reviewNote = "";

  @Column(name = "updated_at", nullable = false)
  public Instant updatedAt;

  @Column(name = "version", nullable = false)
  public long version = 1;
}
