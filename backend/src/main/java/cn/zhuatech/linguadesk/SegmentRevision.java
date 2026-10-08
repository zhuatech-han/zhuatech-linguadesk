// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2
package cn.zhuatech.linguadesk;

import jakarta.persistence.*;
import java.time.*;

/** 不可编辑的逐条修订证据，包括源文更新、翻译和审校。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "segment_revision")
public class SegmentRevision {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "segment_id", nullable = false)
  public Long segmentId;

  @Column(name = "project_id", nullable = false)
  public Long projectId;

  @Column(name = "segment_version", nullable = false)
  public long segmentVersion;

  @Column(name = "source", nullable = false, length = 4000)
  public String source;

  @Column(name = "target", nullable = false, length = 4000)
  public String target;

  @Column(name = "status", nullable = false, length = 20)
  public String status;

  @Column(name = "note", nullable = false, length = 1000)
  public String note;

  @Column(name = "action", nullable = false, length = 30)
  public String action;

  @Column(name = "actor_id", nullable = false)
  public Long actorId;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
