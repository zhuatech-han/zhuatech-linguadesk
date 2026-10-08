// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2
package cn.zhuatech.linguadesk;

import jakarta.persistence.*;
import java.time.*;

/** 发布时冻结的语言文件和双语CSV，之后编辑不会修改历史交付。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "delivery")
public class Delivery {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "project_id", nullable = false)
  public Long projectId;

  @Column(name = "number", nullable = false)
  public int number;

  @Column(name = "name", nullable = false, length = 120)
  public String name;

  @Column(name = "project_version", nullable = false)
  public long projectVersion;

  @Column(name = "json_content", nullable = false, columnDefinition = "longtext")
  public String jsonContent;

  @Column(name = "csv_content", nullable = false, columnDefinition = "longtext")
  public String csvContent;

  @Column(name = "sha256", nullable = false, length = 64)
  public String sha256;

  @Column(name = "created_by", nullable = false)
  public Long createdBy;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
