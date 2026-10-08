// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.linguadesk;

import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/** 项目及后台接口；所有业务操作在服务端检查角色、数据范围、状态和版本。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@RestController
@RequestMapping("/api")
public class ApiController {
  final ProjectService projects;
  final AdminService admin;

  /** 接入翻译流程与系统管理。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public ApiController(ProjectService projects, AdminService admin) {
    this.projects = projects;
    this.admin = admin;
  }

  /** 可见项目分页检索。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/projects")
  public Object list(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "") String state,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "newest") String sort,
      @RequestParam(defaultValue = "") String mine) {
    return projects.list(q, state, page, size, sort, mine);
  }

  /** 当前用户可用的团队、指派人员与平台字典。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/options")
  public Object options() {
    return projects.options();
  }

  /** 建立单目标语言项目。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/projects")
  public Object create(@RequestBody Map<String, Object> b) {
    return projects.create(b);
  }

  /** 读取项目和当前文案、术语与历史交付。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/projects/{id}")
  public Object detail(@PathVariable Long id) {
    return projects.detail(id);
  }

  /** 修改期限和有效译员审校指派。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PutMapping("/projects/{id}")
  public Object update(@PathVariable Long id, @RequestBody Map<String, Object> b) {
    return projects.update(id, b);
  }

  /** 校验原文导入而不改变数据库。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/projects/{id}/import/preview")
  public Object preview(@PathVariable Long id, @RequestBody Map<String, Object> b) {
    return projects.preview(id, b);
  }

  /** 原子导入、变更和归档源文。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/projects/{id}/import")
  public Object source(@PathVariable Long id, @RequestBody Map<String, Object> b) {
    return projects.importSource(id, b);
  }

  /** 草稿项目添加术语。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/projects/{id}/terms")
  public Object term(@PathVariable Long id, @RequestBody Map<String, Object> b) {
    return projects.term(id, null, b, false);
  }

  /** 草稿项目修订术语。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PutMapping("/projects/{id}/terms/{termId}")
  public Object termUpdate(
      @PathVariable Long id, @PathVariable Long termId, @RequestBody Map<String, Object> b) {
    return projects.term(id, termId, b, false);
  }

  /** 草稿项目移除术语，版本冲突不覆盖。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @DeleteMapping("/projects/{id}/terms/{termId}")
  public Object termDelete(
      @PathVariable Long id, @PathVariable Long termId, @RequestBody Map<String, Object> b) {
    return projects.term(id, termId, b, true);
  }

  /** 开始、暂停、恢复、取消和关单的明确状态动作。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/projects/{id}/actions/{action}")
  public Object action(
      @PathVariable Long id, @PathVariable String action, @RequestBody Map<String, Object> b) {
    return projects.action(id, action, b);
  }

  /** 指定译员保存或送审当前译文。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PutMapping("/projects/{id}/segments/{segmentId}")
  public Object translate(
      @PathVariable Long id, @PathVariable Long segmentId, @RequestBody Map<String, Object> b) {
    return projects.translate(id, segmentId, b);
  }

  /** 归档当前源文键，所有历史仍保留。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @DeleteMapping("/projects/{id}/segments/{segmentId}")
  public Object archive(
      @PathVariable Long id, @PathVariable Long segmentId, @RequestBody Map<String, Object> b) {
    return projects.archive(id, segmentId, b);
  }

  /** 指定审校通过或退回译文，规则阻断不可豁免。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/projects/{id}/segments/{segmentId}/review")
  public Object review(
      @PathVariable Long id, @PathVariable Long segmentId, @RequestBody Map<String, Object> b) {
    return projects.review(id, segmentId, b);
  }

  /** 查看真实修订历史和质量规则结果。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/projects/{id}/segments/{segmentId}")
  public Object segment(@PathVariable Long id, @PathVariable Long segmentId) {
    return projects.segment(id, segmentId);
  }

  /** 精确源文匹配的经审校历史译文，建议应用必须重新送审。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/projects/{id}/segments/{segmentId}/memory")
  public Object memory(@PathVariable Long id, @PathVariable Long segmentId) {
    return projects.memory(id, segmentId);
  }

  /** 冻结全量已通过的当前交付版本。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/projects/{id}/deliveries")
  public Object release(@PathVariable Long id, @RequestBody Map<String, Object> b) {
    return projects.release(id, b);
  }

  /** 历史交付下载，包括HEAD预检；文件名不用客户输入。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @RequestMapping(
      value = "/projects/{id}/deliveries/{deliveryId}/{format}",
      method = {RequestMethod.GET, RequestMethod.HEAD})
  public ResponseEntity<byte[]> download(
      @PathVariable Long id,
      @PathVariable Long deliveryId,
      @PathVariable String format,
      org.springframework.web.context.request.WebRequest request) {
    var d = projects.delivery(id, deliveryId);
    if (!Set.of("json", "csv").contains(format)) throw new Problem(404, "NOT_FOUND");
    String data = format.equals("json") ? d.jsonContent : d.csvContent;
    var bytes = data.getBytes(StandardCharsets.UTF_8);
    return ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=linguadesk-" + id + "-v" + d.number + "." + format)
        .contentType(
            MediaType.parseMediaType(
                format.equals("json")
                    ? "application/json;charset=UTF-8"
                    : "text/csv;charset=UTF-8"))
        .body(bytes);
  }

  /** 范围内当前业务统计。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/reports")
  public Object reports() {
    return projects.report();
  }

  /** 报表导出只包含业务信息，并防止CSV公式执行。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @RequestMapping(
      value = "/reports.csv",
      method = {RequestMethod.GET, RequestMethod.HEAD})
  public ResponseEntity<byte[]> reportCsv() {
    var data =
        new StringBuilder(
            "project,source_locale,target_locale,state,due_date,total,translated,approved,blocked\r\n");
    for (var r : projects.report()) {
      var p = (TranslationProject) r.get("project");
      var c = (Map<?, ?>) r.get("counts");
      data.append(Rules.csv(p.name))
          .append(',')
          .append(Rules.csv(p.sourceLocale))
          .append(',')
          .append(Rules.csv(p.targetLocale))
          .append(',')
          .append(Rules.csv(p.state))
          .append(',')
          .append(p.dueDate)
          .append(',')
          .append(c.get("total"))
          .append(',')
          .append(c.get("translated"))
          .append(',')
          .append(c.get("approved"))
          .append(',')
          .append(c.get("blocked"))
          .append("\r\n");
    }
    return ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=linguadesk-progress.csv")
        .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
        .body(data.toString().getBytes(StandardCharsets.UTF_8));
  }

  /** 实时团队范围内的只读审计。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/audit")
  public Object audit() {
    return projects.audit();
  }

  /** 后台账号配置最小选项。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/admin/options")
  public Object adminOptions() {
    return admin.options();
  }

  /** 按独立权限读取后台目录。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/admin/{kind}")
  public Object adminRead(@PathVariable String kind) {
    return admin.read(kind);
  }

  /** 后台新增目录记录。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/admin/{kind}")
  public Object adminCreate(@PathVariable String kind, @RequestBody Map<String, Object> b) {
    return admin.save(kind, null, b);
  }

  /** 后台更新目录并记录真实审计。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PutMapping("/admin/{kind}/{id}")
  public Object adminUpdate(
      @PathVariable String kind, @PathVariable Long id, @RequestBody Map<String, Object> b) {
    return admin.save(kind, id, b);
  }
}
