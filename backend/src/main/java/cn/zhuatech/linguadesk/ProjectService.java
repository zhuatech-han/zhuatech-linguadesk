// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.linguadesk;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 项目、逐条文案、术语冻结、独立审校与不可变交付闭环。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Service
@Transactional
public class ProjectService {
  final Store db;
  final AccessService access;
  final AdminService admin;
  final Clock clock;

  /** 连接目录、权限、数据库和UTC时钟。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public ProjectService(Store db, AccessService access, AdminService admin, Clock clock) {
    this.db = db;
    this.access = access;
    this.admin = admin;
    this.clock = clock;
  }

  TranslationProject project(Long id, String code) {
    var p = db.get(TranslationProject.class, id);
    access.project(p, code);
    return p;
  }

  void version(TranslationProject p, Map<String, Object> b) {
    Rules.check(p.version == Rules.id(b.get("version")), "VERSION_CONFLICT");
  }

  void open(TranslationProject p) {
    Rules.check(Set.of("DRAFT", "ACTIVE", "PAUSED").contains(p.state), "PROJECT_LOCKED");
    Rules.check(db.get(Department.class, p.departmentId).enabled, "DEPARTMENT_DISABLED");
  }

  List<Segment> segments(Long id) {
    return db.query(
        Segment.class, "from Segment where projectId=?1 and active=true order by id", id);
  }

  List<GlossaryTerm> terms(Long id) {
    return db.query(GlossaryTerm.class, "from GlossaryTerm where projectId=?1 order by id", id);
  }

  /** 将状态动作与操作者写入业务历史和团队审计，不记凭证。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  void event(TranslationProject p, String action, String note) {
    var e = new ProjectEvent();
    e.projectId = p.id;
    e.action = action;
    e.actorId = access.current().id;
    e.note = note;
    e.createdAt = clock.instant();
    db.save(e);
    access.audit(action, p.id, p.departmentId);
  }

  /** 保存准确的文案版本、原译文和人工意见，不覆盖既有修订。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  void revision(Segment s, String action, String note) {
    var r = new SegmentRevision();
    r.projectId = s.projectId;
    r.segmentId = s.id;
    r.segmentVersion = s.version;
    r.source = s.source;
    r.target = s.target;
    r.status = s.status;
    r.note = note;
    r.action = action;
    r.actorId = access.current().id;
    r.createdAt = clock.instant();
    db.save(r);
  }

  /** 检查指派账号启用状态、当前权限和团队范围。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  Account assigned(Object value, Long dept, String perm) {
    var a = db.get(Account.class, Rules.id(value));
    var r = db.get(AccessRole.class, a.roleId);
    Rules.check(
        a.enabled
            && db.get(Department.class, a.departmentId).enabled
            && r.permissions.contains(perm)
            && (r.scope.equals("ALL") || Objects.equals(a.departmentId, dept)),
        "ASSIGNEE_INVALID");
    return a;
  }

  /** 按真实当前文案统计翻译、独立审批和规则异常。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  Map<String, Object> counts(TranslationProject p) {
    var rows = segments(p.id);
    int approved = 0, translated = 0, blocked = 0, warnings = 0;
    var terms = terms(p.id);
    for (var s : rows) {
      if (s.status.equals("APPROVED")) approved++;
      if (!s.target.isBlank()) translated++;
      var q = TextRules.qa(s, terms);
      if (!((List<?>) q.get("blockers")).isEmpty()) blocked++;
      if (!((List<?>) q.get("warnings")).isEmpty()) warnings++;
    }
    return Map.of(
        "total",
        rows.size(),
        "translated",
        translated,
        "approved",
        approved,
        "blocked",
        blocked,
        "warnings",
        warnings);
  }

  long dueDays(TranslationProject p) {
    return java.time.temporal.ChronoUnit.DAYS.between(
        LocalDate.now(clock.withZone(ZoneId.of(db.get(Department.class, p.departmentId).zone))),
        p.dueDate);
  }

  boolean dueSoon(TranslationProject p) {
    long days = dueDays(p);
    return !Set.of("CLOSED", "CANCELLED").contains(p.state)
        && days >= 0
        && days <= admin.setting("due_warning_days");
  }

  /** 项目筛选、分页和稳定排序仅处理本人可见范围。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Map<String, Object> list(
      String q, String state, int page, int size, String sort, String mine) {
    access.require("projects");
    Rules.check(
        page >= 1
            && size >= 1
            && size <= 100
            && Set.of("newest", "due").contains(sort)
            && Set.of("", "translate", "review").contains(mine),
        "INVALID_INPUT");
    String needle = Rules.text(q, 160, false).toLowerCase(Locale.ROOT);
    var rows =
        db.all(TranslationProject.class).stream()
            .filter(access::visible)
            .filter(
                p ->
                    mine.isEmpty()
                        || (mine.equals("translate")
                            ? p.translatorId.equals(access.current().id)
                            : mine.equals("review") && p.reviewerId.equals(access.current().id)))
            .filter(p -> needle.isEmpty() || p.name.toLowerCase(Locale.ROOT).contains(needle))
            .filter(p -> state.isEmpty() || p.state.equals(state))
            .sorted(
                sort.equals("due")
                    ? Comparator.comparing((TranslationProject p) -> p.dueDate)
                        .thenComparing(p -> p.id)
                    : Comparator.comparing((TranslationProject p) -> p.id).reversed())
            .toList();
    var aggregate = new HashMap<String, Integer>();
    for (String key : List.of("total", "translated", "approved", "blocked", "warnings"))
      aggregate.put(key, 0);
    for (var p : rows) {
      var c = counts(p);
      for (var key : aggregate.keySet())
        aggregate.put(key, aggregate.get(key) + (Integer) c.get(key));
    }
    return Map.of(
        "total",
        rows.size(),
        "page",
        page,
        "size",
        size,
        "summary",
        aggregate,
        "items",
        rows.stream()
            .skip((long) (page - 1) * size)
            .limit(size)
            .map(p -> Map.of("project", p, "counts", counts(p), "dueSoon", dueSoon(p)))
            .toList());
  }

  /** 项目详情的历史、术语和交付元数据不暴露跨范围记录。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Map<String, Object> detail(Long id) {
    var p = project(id, "projects");
    return Map.of(
        "project",
        p,
        "segments",
        segments(id),
        "terms",
        terms(id),
        "counts",
        counts(p),
        "events",
        db.query(ProjectEvent.class, "from ProjectEvent where projectId=?1 order by id desc", id),
        "deliveries",
        db
            .query(Delivery.class, "from Delivery where projectId=?1 order by number desc", id)
            .stream()
            .map(
                d ->
                    Map.of(
                        "id",
                        d.id,
                        "number",
                        d.number,
                        "name",
                        d.name,
                        "sha256",
                        d.sha256,
                        "projectVersion",
                        d.projectVersion,
                        "createdAt",
                        d.createdAt))
            .toList());
  }

  /** 业务选项只给可见团队、账号姓名和可指派权限，不泄露其他身份。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Map<String, Object> options() {
    access.require("projects");
    return Map.of(
        "departments",
        db.all(Department.class).stream().filter(d -> access.department(d.id)).toList(),
        "users",
        db.all(Account.class).stream()
            .filter(
                a ->
                    access.department(a.departmentId)
                        || db.get(AccessRole.class, a.roleId).scope.equals("ALL"))
            .map(
                a ->
                    Map.of(
                        "id",
                        a.id,
                        "displayName",
                        a.displayName,
                        "departmentId",
                        a.departmentId,
                        "enabled",
                        a.enabled,
                        "permissions",
                        db.get(AccessRole.class, a.roleId).permissions,
                        "scope",
                        db.get(AccessRole.class, a.roleId).scope))
            .toList(),
        "platforms",
        db.all(DictionaryEntry.class),
        "warningDays",
        admin.setting("due_warning_days"));
  }

  /** 新建完整语言对项目；译员和独立审校为不同有效账号。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Map<String, Object> create(Map<String, Object> b) {
    access.require("manage");
    admin.lock();
    var p = new TranslationProject();
    p.departmentId = Rules.id(b.get("departmentId"));
    Rules.check(
        access.department(p.departmentId) && db.get(Department.class, p.departmentId).enabled,
        "OUT_OF_SCOPE");
    p.name = Rules.text(b.get("name"), 160, true);
    p.sourceLocale = TextRules.locale(b.get("sourceLocale"));
    p.targetLocale = TextRules.locale(b.get("targetLocale"));
    Rules.check(!p.sourceLocale.equalsIgnoreCase(p.targetLocale), "SAME_LOCALE");
    p.platform = Rules.text(b.get("platform"), 60, true);
    Rules.check(
        db.query(
                    DictionaryEntry.class,
                    "from DictionaryEntry where type='PLATFORM' and code=?1 and enabled=true",
                    p.platform)
                .size()
            == 1,
        "PLATFORM_INVALID");
    p.ownerId = access.current().id;
    p.translatorId = assigned(b.get("translatorId"), p.departmentId, "translate").id;
    p.reviewerId = assigned(b.get("reviewerId"), p.departmentId, "review").id;
    Rules.check(!p.translatorId.equals(p.reviewerId), "INDEPENDENT_REVIEWER");
    p.dueDate = LocalDate.parse(Rules.text(b.get("dueDate"), 10, true));
    p.createdAt = clock.instant();
    db.save(p);
    event(p, "PROJECT_CREATED", "");
    return detail(p.id);
  }

  /** 修改期限或重新指派；已完结项目不可覆盖历史，指派必须仍有效。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Map<String, Object> update(Long id, Map<String, Object> b) {
    admin.lock();
    var p = project(id, "manage");
    open(p);
    version(p, b);
    p.name = Rules.text(b.get("name"), 160, true);
    p.dueDate = LocalDate.parse(Rules.text(b.get("dueDate"), 10, true));
    p.translatorId = assigned(b.get("translatorId"), p.departmentId, "translate").id;
    p.reviewerId = assigned(b.get("reviewerId"), p.departmentId, "review").id;
    Rules.check(!p.translatorId.equals(p.reviewerId), "INDEPENDENT_REVIEWER");
    p.version++;
    event(p, "PROJECT_UPDATED", "");
    return detail(id);
  }

  /** 导入预览不会写库；重复和超限整批拒绝。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Map<String, Object> preview(Long id, Map<String, Object> b) {
    var p = project(id, "manage");
    open(p);
    var rows = SourceCodec.parse(b, admin.setting("max_segments"));
    var existing = db.query(Segment.class, "from Segment where projectId=?1", id);
    Map<String, Segment> byKey = new HashMap<>();
    existing.forEach(s -> byKey.put(s.key.toLowerCase(Locale.ROOT), s));
    int added = 0, changed = 0;
    Set<String> incoming = new HashSet<>();
    for (var r : rows) {
      String key = r.key().toLowerCase(Locale.ROOT);
      incoming.add(key);
      var s = byKey.get(key);
      if (s == null || !s.active) added++;
      else if (!s.key.equals(r.key())
          || !s.source.equals(r.source())
          || !s.context.equals(r.context())
          || s.maxLength != r.maxLength()) changed++;
    }
    boolean replace = Rules.flag(b.get("replace"));
    long removed =
        replace
            ? existing.stream()
                .filter(s -> s.active && !incoming.contains(s.key.toLowerCase(Locale.ROOT)))
                .count()
            : 0;
    long total = replace ? rows.size() : existing.stream().filter(s -> s.active).count() + added;
    Rules.check(total <= admin.setting("max_segments"), "RESOURCE_LIMIT");
    return Map.of(
        "added", added, "changed", changed, "removed", removed, "total", total, "rows", rows);
  }

  /** 原文更新保留旧译文但使审校失效；已交付快照不变，移除项只归档。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Map<String, Object> importSource(Long id, Map<String, Object> b) {
    admin.lock();
    var p = project(id, "manage");
    open(p);
    version(p, b);
    preview(id, b);
    var rows = SourceCodec.parse(b, admin.setting("max_segments"));
    var existing = db.query(Segment.class, "from Segment where projectId=?1", id);
    Map<String, Segment> byKey = new HashMap<>();
    existing.forEach(s -> byKey.put(s.key.toLowerCase(Locale.ROOT), s));
    Set<String> incoming = new HashSet<>();
    for (var r : rows) {
      String k = r.key().toLowerCase(Locale.ROOT);
      incoming.add(k);
      var s = byKey.get(k);
      if (s == null) {
        s = new Segment();
        s.projectId = id;
        s.key = r.key();
        s.source = r.source();
        s.context = r.context();
        s.maxLength = r.maxLength();
        s.updatedAt = clock.instant();
        db.save(s);
        revision(s, "SOURCE_ADDED", "");
      } else if (!s.active
          || !s.key.equals(r.key())
          || !s.source.equals(r.source())
          || !s.context.equals(r.context())
          || s.maxLength != r.maxLength()) {
        s.key = r.key();
        s.source = r.source();
        s.context = r.context();
        s.maxLength = r.maxLength();
        s.active = true;
        s.reviewedBy = null;
        s.reviewNote = "";
        s.status = s.target.isBlank() ? "UNTRANSLATED" : "DRAFT";
        s.updatedAt = clock.instant();
        s.version++;
        revision(s, "SOURCE_UPDATED", "");
      }
    }
    if (Rules.flag(b.get("replace")))
      for (var s : existing)
        if (s.active && !incoming.contains(s.key.toLowerCase(Locale.ROOT))) {
          s.active = false;
          s.version++;
          s.updatedAt = clock.instant();
          revision(s, "SOURCE_REMOVED", "");
        }
    p.version++;
    event(p, "SOURCE_IMPORTED", "");
    return detail(id);
  }

  /** 归档单条源文；不删除修订或历史交付，准确核对项目与文案版本。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Map<String, Object> archive(Long id, Long segmentId, Map<String, Object> b) {
    admin.lock();
    var p = project(id, "manage");
    open(p);
    version(p, b);
    var row = db.get(Segment.class, segmentId);
    Rules.check(row.projectId.equals(id) && row.active, "NOT_FOUND");
    Rules.check(row.version == Rules.id(b.get("segmentVersion")), "VERSION_CONFLICT");
    row.active = false;
    row.version++;
    row.updatedAt = clock.instant();
    p.version++;
    revision(row, "SOURCE_REMOVED", "");
    event(p, "SOURCE_ARCHIVED", "");
    return detail(id);
  }

  /** 术语仅在草稿时管理，开始后冻结；删改需要准确版本。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Map<String, Object> term(Long id, Long termId, Map<String, Object> b, boolean delete) {
    admin.lock();
    var p = project(id, "manage");
    version(p, b);
    Rules.check(p.state.equals("DRAFT"), "GLOSSARY_FROZEN");
    var t = termId == null ? new GlossaryTerm() : db.get(GlossaryTerm.class, termId);
    if (termId != null)
      Rules.check(
          t.projectId.equals(id) && t.version == Rules.id(b.get("termVersion")),
          "VERSION_CONFLICT");
    if (delete) {
      db.delete(t);
    } else {
      String source = TextRules.text(b.get("source"), 200, true);
      Rules.check(
          terms(id).stream()
              .noneMatch(x -> !Objects.equals(x.id, termId) && x.source.equalsIgnoreCase(source)),
          "DUPLICATE_TERM");
      t.projectId = id;
      t.source = source;
      t.target = TextRules.text(b.get("target"), 200, true);
      t.note = Rules.text(b.get("note"), 500, false);
      if (termId == null) db.save(t);
      else t.version++;
    }
    p.version++;
    event(p, delete ? "TERM_REMOVED" : "TERM_SAVED", "");
    return detail(id);
  }

  /** 开始、暂停、恢复、取消或关单；发布后的编辑显式保留历史交付。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Map<String, Object> action(Long id, String action, Map<String, Object> b) {
    admin.lock();
    var p = project(id, "manage");
    version(p, b);
    String note = Rules.text(b.get("note"), 1000, false);
    switch (action) {
      case "start" -> {
        Rules.check(p.state.equals("DRAFT") && !segments(id).isEmpty(), "STATE_INVALID");
        assigned(p.translatorId, p.departmentId, "translate");
        assigned(p.reviewerId, p.departmentId, "review");
        p.state = "ACTIVE";
      }
      case "pause" -> {
        Rules.check(p.state.equals("ACTIVE"), "STATE_INVALID");
        p.state = "PAUSED";
      }
      case "resume" -> {
        Rules.check(p.state.equals("PAUSED"), "STATE_INVALID");
        p.state = "ACTIVE";
      }
      case "cancel" -> {
        open(p);
        Rules.check(!note.isBlank(), "NOTE_REQUIRED");
        p.state = "CANCELLED";
      }
      case "close" -> {
        Rules.check(p.state.equals("ACTIVE"), "STATE_INVALID");
        var releases =
            db.query(Delivery.class, "from Delivery where projectId=?1 order by number desc", id);
        Rules.check(
            !releases.isEmpty() && releases.getFirst().projectVersion == p.version,
            "UNRELEASED_CHANGES");
        p.state = "CLOSED";
      }
      default -> throw new Problem(404, "NOT_FOUND");
    }
    p.version++;
    event(p, "PROJECT_" + action.toUpperCase(Locale.ROOT), note);
    return detail(id);
  }

  /** 译员保存或送审；任何文字变更重置审校，空译文可保存但不能送审。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Map<String, Object> translate(Long id, Long segmentId, Map<String, Object> b) {
    admin.lock();
    var p = project(id, "translate");
    Rules.check(p.state.equals("ACTIVE"), "PROJECT_LOCKED");
    Rules.check(p.translatorId.equals(access.current().id), "TRANSLATOR_ONLY");
    var s = db.get(Segment.class, segmentId);
    Rules.check(s.projectId.equals(id) && s.active, "NOT_FOUND");
    Rules.check(s.version == Rules.id(b.get("version")), "VERSION_CONFLICT");
    s.target = TextRules.text(b.get("target"), 4000, false);
    boolean submit = Rules.flag(b.get("submit"));
    if (submit) Rules.check(!s.target.isBlank(), "EMPTY_TARGET");
    s.status = submit ? "REVIEW" : "DRAFT";
    s.editedBy = access.current().id;
    s.reviewedBy = null;
    s.reviewNote = "";
    s.version++;
    s.updatedAt = clock.instant();
    p.version++;
    revision(s, submit ? "SUBMITTED" : "TRANSLATED", "");
    access.audit(submit ? "SEGMENT_SUBMITTED" : "SEGMENT_SAVED", s.id, p.departmentId);
    return segmentView(s);
  }

  Map<String, Object> segmentView(Segment s) {
    return Map.of("segment", s, "qa", TextRules.qa(s, terms(s.projectId)));
  }

  /** 审校只能处理指定项目的送审译文，且不能批准自己编辑的版本；豁免警告必须留原因。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Map<String, Object> review(Long id, Long segmentId, Map<String, Object> b) {
    admin.lock();
    var p = project(id, "review");
    Rules.check(p.state.equals("ACTIVE"), "PROJECT_LOCKED");
    Rules.check(p.reviewerId.equals(access.current().id), "REVIEWER_ONLY");
    var s = db.get(Segment.class, segmentId);
    Rules.check(s.projectId.equals(id) && s.active, "NOT_FOUND");
    Rules.check(s.version == Rules.id(b.get("version")), "VERSION_CONFLICT");
    Rules.check(s.status.equals("REVIEW"), "STATE_INVALID");
    Rules.check(!Objects.equals(s.editedBy, access.current().id), "SELF_REVIEW");
    boolean approve = Rules.flag(b.get("approve"));
    String note = Rules.text(b.get("note"), 1000, false);
    var qa = TextRules.qa(s, terms(id));
    if (approve) {
      Rules.check(((List<?>) qa.get("blockers")).isEmpty(), "QA_BLOCKED");
      Rules.check(((List<?>) qa.get("warnings")).isEmpty() || !note.isBlank(), "WAIVER_REQUIRED");
    } else Rules.check(!note.isBlank(), "NOTE_REQUIRED");
    s.status = approve ? "APPROVED" : "CHANGES";
    s.reviewedBy = access.current().id;
    s.reviewNote = note;
    s.version++;
    s.updatedAt = clock.instant();
    p.version++;
    revision(s, approve ? "APPROVED" : "CHANGES_REQUESTED", note);
    access.audit(approve ? "SEGMENT_APPROVED" : "SEGMENT_RETURNED", s.id, p.departmentId);
    return segmentView(s);
  }

  /** 当前规则核查和修订记录仅返回可见项目。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Map<String, Object> segment(Long id, Long segmentId) {
    project(id, "projects");
    var s = db.get(Segment.class, segmentId);
    Rules.check(s.projectId.equals(id), "NOT_FOUND");
    return Map.of(
        "segment",
        s,
        "qa",
        TextRules.qa(s, terms(id)),
        "history",
        db.query(
            SegmentRevision.class,
            "from SegmentRevision where segmentId=?1 order by id desc",
            segmentId));
  }

  /** 经审校译文的精确匹配建议；同语言对和团队，可见范围内，应用仍要人工送审。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public List<Map<String, Object>> memory(Long id, Long segmentId) {
    var p = project(id, "projects");
    var s = db.get(Segment.class, segmentId);
    Rules.check(s.projectId.equals(id), "NOT_FOUND");
    var visible =
        db.all(TranslationProject.class).stream()
            .filter(
                x ->
                    access.visible(x)
                        && x.departmentId.equals(p.departmentId)
                        && x.sourceLocale.equals(p.sourceLocale)
                        && x.targetLocale.equals(p.targetLocale)
                        && !x.state.equals("CANCELLED"))
            .toList();
    var out = new ArrayList<Map<String, Object>>();
    for (var other : visible)
      for (var x :
          db.query(
              Segment.class,
              "from Segment where projectId=?1 and source=?2 and status='APPROVED' and active=true order by id",
              other.id,
              s.source))
        if (!x.id.equals(s.id) && x.source.equals(s.source))
          out.add(Map.of("project", other.name, "target", x.target, "segmentId", x.id));
    return out.stream().distinct().limit(10).toList();
  }

  /** 发布只能冻结全部通过的当前译文，任何未审、阻断或未解释警告都拒绝。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Map<String, Object> release(Long id, Map<String, Object> b) {
    admin.lock();
    var p = project(id, "publish");
    version(p, b);
    Rules.check(p.state.equals("ACTIVE"), "PROJECT_LOCKED");
    var rows = segments(id);
    Rules.check(!rows.isEmpty(), "EMPTY_PROJECT");
    var terms = terms(id);
    for (var s : rows) {
      var q = TextRules.qa(s, terms);
      Rules.check(
          s.status.equals("APPROVED")
              && s.reviewedBy != null
              && !Objects.equals(s.editedBy, s.reviewedBy),
          "REVIEW_REQUIRED");
      Rules.check(((List<?>) q.get("blockers")).isEmpty(), "QA_BLOCKED");
      Rules.check(
          ((List<?>) q.get("warnings")).isEmpty() || !s.reviewNote.isBlank(), "WAIVER_REQUIRED");
    }
    var existing =
        db.query(Delivery.class, "from Delivery where projectId=?1 order by number desc", id);
    Rules.check(
        existing.isEmpty() || existing.getFirst().projectVersion != p.version, "ALREADY_RELEASED");
    var d = new Delivery();
    d.projectId = id;
    d.number = existing.size() + 1;
    d.name = Rules.text(b.get("name"), 120, true);
    d.jsonContent = SourceCodec.exportJson(rows);
    d.csvContent = SourceCodec.exportCsv(rows);
    d.sha256 = sha(d.jsonContent);
    d.projectVersion = p.version;
    d.createdBy = access.current().id;
    d.createdAt = clock.instant();
    db.save(d);
    event(p, "DELIVERY_RELEASED", d.name);
    return detail(id);
  }

  /** 历史交付内容不可变，下载再检查实时身份和项目范围。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Delivery delivery(Long id, Long deliveryId) {
    project(id, "projects");
    var d = db.get(Delivery.class, deliveryId);
    Rules.check(d.projectId.equals(id), "NOT_FOUND");
    return d;
  }

  /** 按可见项目汇总当前进度，不把规则检查称为语义质量评分。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public List<Map<String, Object>> report() {
    access.require("reports");
    return db.all(TranslationProject.class).stream()
        .filter(access::visible)
        .map(
            p ->
                Map.of(
                    "project",
                    p,
                    "counts",
                    counts(p),
                    "overdue",
                    dueDays(p) < 0 && !Set.of("CLOSED", "CANCELLED").contains(p.state),
                    "dueSoon",
                    dueSoon(p)))
        .toList();
  }

  /** 完整审计只展示本人团队可见事件。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public List<AuditEvent> audit() {
    access.require("audit");
    return db.all(AuditEvent.class).stream()
        .filter(x -> access.department(x.departmentId))
        .sorted(Comparator.comparing((AuditEvent e) -> e.id).reversed())
        .toList();
  }

  /** 对UTF-8冻结内容计算可核对SHA-256。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public static String sha(String text) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
