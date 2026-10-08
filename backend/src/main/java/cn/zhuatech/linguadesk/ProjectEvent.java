// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2
package cn.zhuatech.linguadesk;

import jakarta.persistence.*;
import java.time.*;

/** 完整业务状态和冻结操作记录，不包含登录凭证。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "project_event")
public class ProjectEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "project_id", nullable = false)
  public Long projectId;

  @Column(name = "action", nullable = false, length = 50)
  public String action;

  @Column(name = "actor_id", nullable = false)
  public Long actorId;

  @Column(name = "note", nullable = false, length = 1000)
  public String note;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
