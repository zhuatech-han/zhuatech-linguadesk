// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.linguadesk;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** 真实HTTP/JPA/迁移业务验证，无业务服务模拟；数据明确TEST。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@SpringBootTest
@AutoConfigureMockMvc(print = org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint.NONE)
class LinguaIntegrationTest {
  static final String PASSWORD = "Aa9" + UUID.randomUUID();

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add(
        "spring.datasource.url",
        () -> "jdbc:h2:mem:lingua;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
    r.add("spring.datasource.username", () -> "sa");
    r.add("spring.datasource.password", () -> "");
    r.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");
    r.add("linguadesk.admin-password", () -> PASSWORD);
  }

  @Autowired MockMvc mvc;
  final JsonMapper json = JsonMapper.builder().build();
  MockHttpSession admin, translator, reviewer, outsider;
  long translatorId, reviewerId, projectId;
  JsonNode detail;
  String suffix;

  @BeforeEach
  void setup() throws Exception {
    suffix = UUID.randomUUID().toString().substring(0, 8);
    admin = login("admin");
    translatorId = user("tr" + suffix, 3, 1);
    reviewerId = user("rv" + suffix, 4, 1);
    user("out" + suffix, 3, 1);
    translator = login("tr" + suffix);
    reviewer = login("rv" + suffix);
    outsider = login("out" + suffix);
    detail =
        call(
            admin,
            "/projects",
            "POST",
            m(
                "name",
                "TEST " + suffix,
                "departmentId",
                1,
                "translatorId",
                translatorId,
                "reviewerId",
                reviewerId,
                "sourceLocale",
                "en",
                "targetLocale",
                "zh-CN",
                "platform",
                "APP",
                "dueDate",
                "2026-12-31"));
    projectId = detail.get("project").get("id").asLong();
  }

  Map<String, Object> m(Object... p) {
    Map<String, Object> b = new LinkedHashMap<>();
    for (int i = 0; i < p.length; i += 2) b.put(p[i].toString(), p[i + 1]);
    return b;
  }

  MockHttpSession login(String username) throws Exception {
    var r =
        mvc.perform(
                post("/api/auth/login")
                    .with(csrf())
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(m("username", username, "password", PASSWORD))))
            .andReturn();
    assertEquals(200, r.getResponse().getStatus());
    return (MockHttpSession) r.getRequest().getSession(false);
  }

  long user(String name, long role, long dept) throws Exception {
    return call(
            admin,
            "/admin/users",
            "POST",
            m(
                "username",
                name,
                "displayName",
                "TEST " + name,
                "roleId",
                role,
                "departmentId",
                dept,
                "enabled",
                true,
                "password",
                PASSWORD))
        .get("id")
        .asLong();
  }

  MockHttpServletRequestBuilder req(String path, String method, Map<String, Object> b)
      throws Exception {
    var r =
        switch (method) {
          case "POST" -> post("/api" + path);
          case "PUT" -> put("/api" + path);
          case "DELETE" -> delete("/api" + path);
          case "HEAD" -> head("/api" + path);
          default -> get("/api" + path);
        };
    if (b != null)
      r.with(csrf()).contentType("application/json").content(json.writeValueAsString(b));
    return r;
  }

  MvcResult result(MockHttpSession s, String path, String method, Map<String, Object> b)
      throws Exception {
    return mvc.perform(req(path, method, b).session(s)).andReturn();
  }

  JsonNode call(MockHttpSession s, String path, String method, Map<String, Object> b)
      throws Exception {
    var r = result(s, path, method, b);
    assertEquals(200, r.getResponse().getStatus(), r.getResponse().getContentAsString());
    return json.readTree(r.getResponse().getContentAsString());
  }

  void fail(
      MockHttpSession s, String path, String method, Map<String, Object> b, int status, String code)
      throws Exception {
    var r = result(s, path, method, b);
    assertEquals(status, r.getResponse().getStatus(), r.getResponse().getContentAsString());
    assertEquals(code, json.readTree(r.getResponse().getContentAsString()).get("code").asString());
  }

  String path() {
    return "/projects/" + projectId;
  }

  long version() {
    return detail.get("project").get("version").asLong();
  }

  void refresh() throws Exception {
    detail = call(admin, path(), "GET", null);
  }

  void importOne(String source) throws Exception {
    detail =
        call(
            admin,
            path() + "/import",
            "POST",
            m(
                "version",
                version(),
                "replace",
                false,
                "rows",
                List.of(
                    m(
                        "key",
                        "greeting",
                        "source",
                        source,
                        "context",
                        "Greeting",
                        "maxLength",
                        0))));
  }

  void start() throws Exception {
    detail = call(admin, path() + "/actions/start", "POST", m("version", version()));
  }

  JsonNode first() {
    return detail.get("segments").get(0);
  }

  JsonNode translate(String value, boolean submit) throws Exception {
    refresh();
    var s = first();
    var out =
        call(
            translator,
            path() + "/segments/" + s.get("id").asLong(),
            "PUT",
            m("version", s.get("version").asLong(), "target", value, "submit", submit));
    refresh();
    return out;
  }

  void approve(String note) throws Exception {
    refresh();
    var s = first();
    call(
        reviewer,
        path() + "/segments/" + s.get("id").asLong() + "/review",
        "POST",
        m("version", s.get("version").asLong(), "approve", true, "note", note));
    refresh();
  }

  long release() throws Exception {
    detail =
        call(
            admin, path() + "/deliveries", "POST", m("version", version(), "name", "TEST release"));
    return detail.get("deliveries").get(0).get("id").asLong();
  }

  @Test
  void completeTranslationAndImmutableDownload() throws Exception {
    importOne("Welcome {name}!");
    start();
    translate("欢迎 {name}！", true);
    approve("");
    long id = release();
    String jsonPath = path() + "/deliveries/" + id + "/json";
    var download = result(translator, jsonPath, "GET", null);
    assertEquals(200, download.getResponse().getStatus());
    assertEquals(
        "欢迎 {name}！",
        json.readTree(download.getResponse().getContentAsString(StandardCharsets.UTF_8))
            .get("greeting")
            .asString());
    assertEquals(200, result(reviewer, jsonPath, "HEAD", null).getResponse().getStatus());
    detail = call(admin, path() + "/actions/close", "POST", m("version", version()));
    assertEquals("CLOSED", detail.get("project").get("state").asString());
    fail(
        translator,
        path() + "/segments/" + first().get("id").asLong(),
        "PUT",
        m("version", first().get("version").asLong(), "target", "x", "submit", false),
        409,
        "PROJECT_LOCKED");
  }

  @Test
  void importPreviewDoesNotPersist() throws Exception {
    var before = version();
    var r =
        call(
            admin,
            path() + "/import/preview",
            "POST",
            m("content", "{\"hello\":\"Hello\"}", "replace", false));
    assertEquals(1, r.get("added").asInt());
    refresh();
    assertEquals(before, version());
    assertTrue(detail.get("segments").isEmpty());
  }

  @Test
  void duplicateJsonRejectsWholeBatch() throws Exception {
    fail(
        admin,
        path() + "/import",
        "POST",
        m("version", version(), "content", "{\"a\":\"Hello\",\"a\":\"World\"}", "replace", false),
        400,
        "INVALID_JSON");
    refresh();
    assertTrue(detail.get("segments").isEmpty());
  }

  @Test
  void nestedJsonIsExplicitlyRejected() throws Exception {
    fail(
        admin,
        path() + "/import",
        "POST",
        m("version", version(), "content", "{\"a\":{\"b\":\"hello\"}}", "replace", false),
        400,
        "FLAT_JSON_ONLY");
  }

  @Test
  void invalidRowDoesNotPersistFirstRow() throws Exception {
    fail(
        admin,
        path() + "/import",
        "POST",
        m(
            "version",
            version(),
            "replace",
            false,
            "rows",
            List.of(m("key", "ok", "source", "Hello"), m("key", "invalid key", "source", "World"))),
        400,
        "INVALID_KEY");
    refresh();
    assertTrue(detail.get("segments").isEmpty());
  }

  @Test
  void duplicateCaseKeysRejected() throws Exception {
    fail(
        admin,
        path() + "/import",
        "POST",
        m(
            "version",
            version(),
            "replace",
            false,
            "rows",
            List.of(m("key", "OK", "source", "Hello"), m("key", "ok", "source", "World"))),
        400,
        "DUPLICATE_KEY");
  }

  @Test
  void translatorCannotImportSources() throws Exception {
    fail(
        translator,
        path() + "/import",
        "POST",
        m("version", version(), "replace", false, "content", "{\"a\":\"hello\"}"),
        403,
        "FORBIDDEN");
  }

  @Test
  void unassignedCannotReadProject() throws Exception {
    fail(outsider, path(), "GET", null, 403, "OUT_OF_SCOPE");
    var rows = call(outsider, "/projects", "GET", null);
    assertEquals(0, rows.get("total").asInt());
  }

  @Test
  void adminCannotImpersonateAssignedTranslator() throws Exception {
    importOne("Hello");
    start();
    var s = first();
    fail(
        admin,
        path() + "/segments/" + s.get("id").asLong(),
        "PUT",
        m("version", s.get("version").asLong(), "target", "你好", "submit", true),
        409,
        "TRANSLATOR_ONLY");
  }

  @Test
  void placeholderLossBlocksApproval() throws Exception {
    importOne("Hello {name}");
    start();
    translate("你好", true);
    var s = first();
    fail(
        reviewer,
        path() + "/segments/" + s.get("id").asLong() + "/review",
        "POST",
        m("version", s.get("version").asLong(), "approve", true, "note", "Cannot waive blocker"),
        409,
        "QA_BLOCKED");
  }

  @Test
  void warningRequiresExactVersionReason() throws Exception {
    importOne("Version 2");
    start();
    translate("版本三", true);
    var s = first();
    fail(
        reviewer,
        path() + "/segments/" + s.get("id").asLong() + "/review",
        "POST",
        m("version", s.get("version").asLong(), "approve", true, "note", ""),
        409,
        "WAIVER_REQUIRED");
    approve("TEST intentional wording, numeric meaning checked");
    assertEquals("APPROVED", first().get("status").asString());
  }

  @Test
  void reviewerReturnsThenTranslatorResubmits() throws Exception {
    importOne("Hello");
    start();
    translate("你好", true);
    var s = first();
    call(
        reviewer,
        path() + "/segments/" + s.get("id").asLong() + "/review",
        "POST",
        m("version", s.get("version").asLong(), "approve", false, "note", "TEST improve tone"));
    refresh();
    assertEquals("CHANGES", first().get("status").asString());
    translate("您好", true);
    approve("");
  }

  @Test
  void staleTranslationCannotOverwrite() throws Exception {
    importOne("Hello");
    start();
    var s = first();
    translate("你好", false);
    fail(
        translator,
        path() + "/segments/" + s.get("id").asLong(),
        "PUT",
        m("version", s.get("version").asLong(), "target", "stale", "submit", false),
        409,
        "VERSION_CONFLICT");
    refresh();
    assertEquals("你好", first().get("target").asString());
  }

  @Test
  void sourceUpdateInvalidatesReviewPreservesDelivery() throws Exception {
    importOne("Hello");
    start();
    translate("你好", true);
    approve("");
    long releaseId = release();
    String old =
        result(admin, path() + "/deliveries/" + releaseId + "/json", "GET", null)
            .getResponse()
            .getContentAsString();
    importOne("Hello again");
    assertEquals("DRAFT", first().get("status").asString());
    assertTrue(first().get("reviewedBy").isNull());
    assertEquals(
        old,
        result(admin, path() + "/deliveries/" + releaseId + "/json", "GET", null)
            .getResponse()
            .getContentAsString());
    fail(
        admin,
        path() + "/actions/close",
        "POST",
        m("version", version()),
        409,
        "UNRELEASED_CHANGES");
  }

  @Test
  void removedKeysArchivedAndReaddedNeedReview() throws Exception {
    importOne("Hello");
    long old = first().get("id").asLong();
    detail =
        call(
            admin,
            path() + "/import",
            "POST",
            m("version", version(), "replace", true, "content", "{\"other\":\"Other\"}"));
    assertEquals(1, detail.get("segments").size());
    importOne("Hello");
    assertEquals(old, detail.get("segments").get(0).get("id").asLong());
    assertEquals(2, detail.get("segments").size());
  }

  @Test
  void termsFreezeWhenStartedAndTriggerWarning() throws Exception {
    importOne("Account ready");
    detail =
        call(
            admin,
            path() + "/terms",
            "POST",
            m(
                "version",
                version(),
                "source",
                "Account",
                "target",
                "账户",
                "note",
                "TEST approved term"));
    start();
    fail(
        admin,
        path() + "/terms",
        "POST",
        m("version", version(), "source", "ready", "target", "就绪", "note", ""),
        409,
        "GLOSSARY_FROZEN");
    translate("账号就绪", true);
    assertEquals(
        1,
        call(reviewer, path() + "/segments/" + first().get("id").asLong(), "GET", null)
            .get("qa")
            .get("warnings")
            .size());
  }

  @Test
  void pauseLocksTranslationAndResumeWorks() throws Exception {
    importOne("Hello");
    start();
    detail = call(admin, path() + "/actions/pause", "POST", m("version", version()));
    var s = first();
    fail(
        translator,
        path() + "/segments/" + s.get("id").asLong(),
        "PUT",
        m("version", s.get("version").asLong(), "target", "你好", "submit", true),
        409,
        "PROJECT_LOCKED");
    detail = call(admin, path() + "/actions/resume", "POST", m("version", version()));
    translate("你好", true);
  }

  @Test
  void releaseRequiresReviewedContent() throws Exception {
    importOne("Hello");
    start();
    translate("你好", false);
    fail(
        admin,
        path() + "/deliveries",
        "POST",
        m("version", version(), "name", "TEST"),
        409,
        "REVIEW_REQUIRED");
  }

  @Test
  void revisionHistoryIsRealAndOrdered() throws Exception {
    importOne("Hello");
    start();
    translate("你好", false);
    translate("您好", true);
    approve("");
    var history =
        call(translator, path() + "/segments/" + first().get("id").asLong(), "GET", null)
            .get("history");
    assertEquals(4, history.size());
    assertEquals("APPROVED", history.get(0).get("action").asString());
    assertEquals("你好", history.get(2).get("target").asString());
  }

  @Test
  void exactMemoryOnlyShowsVisibleApprovedRows() throws Exception {
    importOne("Hello");
    start();
    translate("你好", true);
    approve("");
    var second =
        call(
            admin,
            "/projects",
            "POST",
            m(
                "name",
                "TEST memory " + suffix,
                "departmentId",
                1,
                "translatorId",
                translatorId,
                "reviewerId",
                reviewerId,
                "sourceLocale",
                "en",
                "targetLocale",
                "zh-CN",
                "platform",
                "APP",
                "dueDate",
                "2026-12-31"));
    long id = second.get("project").get("id").asLong();
    second =
        call(
            admin,
            "/projects/" + id + "/import",
            "POST",
            m(
                "version",
                second.get("project").get("version").asLong(),
                "replace",
                false,
                "content",
                "{\"same\":\"Hello\"}"));
    long sid = second.get("segments").get(0).get("id").asLong();
    var suggestions =
        call(translator, "/projects/" + id + "/segments/" + sid + "/memory", "GET", null);
    assertTrue(suggestions.size() >= 1);
    assertEquals("你好", suggestions.get(0).get("target").asString());
  }

  @Test
  void disabledUserAndResetPasswordRevokeSession() throws Exception {
    var users = call(admin, "/admin/users", "GET", null);
    JsonNode u = null;
    for (var x : users) if (x.get("id").asLong() == translatorId) u = x;
    var b =
        m(
            "version",
            u.get("version").asLong(),
            "username",
            u.get("username").asString(),
            "displayName",
            "TEST translator",
            "roleId",
            3,
            "departmentId",
            1,
            "enabled",
            false,
            "password",
            "");
    call(admin, "/admin/users/" + translatorId, "PUT", b);
    fail(translator, "/auth/me", "GET", null, 401, "UNAUTHENTICATED");
    b.put("enabled", true);
    b.put("version", u.get("version").asLong() + 1);
    b.put("password", "Aa9" + UUID.randomUUID());
    call(admin, "/admin/users/" + translatorId, "PUT", b);
    fail(translator, "/auth/me", "GET", null, 401, "UNAUTHENTICATED");
  }

  @Test
  void lastAdministratorProtected() throws Exception {
    var users = call(admin, "/admin/users", "GET", null);
    JsonNode a = null;
    for (var x : users) if (x.get("id").asLong() == 1) a = x;
    fail(
        admin,
        "/admin/users/1",
        "PUT",
        m(
            "version",
            a.get("version").asLong(),
            "username",
            "admin",
            "displayName",
            "TEST admin",
            "roleId",
            1,
            "departmentId",
            1,
            "enabled",
            false,
            "password",
            ""),
        409,
        "LAST_ADMIN");
    assertEquals(200, result(admin, "/auth/me", "GET", null).getResponse().getStatus());
  }

  @Test
  void selfReviewRejectedAfterReassignment() throws Exception {
    importOne("Hello");
    start();
    translate("你好", true);
    var r =
        call(
            admin,
            "/admin/roles",
            "POST",
            m(
                "name",
                "TEST dual " + suffix,
                "scope",
                "ASSIGNED",
                "permissions",
                List.of("projects", "translate", "review")));
    var users = call(admin, "/admin/users", "GET", null);
    JsonNode u = null;
    for (var x : users) if (x.get("id").asLong() == translatorId) u = x;
    call(
        admin,
        "/admin/users/" + translatorId,
        "PUT",
        m(
            "version",
            u.get("version").asLong(),
            "username",
            u.get("username").asString(),
            "displayName",
            "TEST dual",
            "roleId",
            r.get("id").asLong(),
            "departmentId",
            1,
            "enabled",
            true,
            "password",
            ""));
    detail =
        call(
            admin,
            path(),
            "PUT",
            m(
                "version",
                version(),
                "name",
                "TEST reassigned",
                "dueDate",
                "2026-12-31",
                "translatorId",
                1,
                "reviewerId",
                translatorId));
    var s = first();
    fail(
        translator,
        path() + "/segments/" + s.get("id").asLong() + "/review",
        "POST",
        m("version", s.get("version").asLong(), "approve", true, "note", ""),
        409,
        "SELF_REVIEW");
  }

  @Test
  void simultaneousVersionWritesOnlyOneSucceeds() throws Exception {
    importOne("Hello");
    start();
    var s = first();
    long sid = s.get("id").asLong(), v = s.get("version").asLong();
    var pool = Executors.newFixedThreadPool(2);
    try {
      Callable<Integer> run =
          () ->
              result(
                      translator,
                      path() + "/segments/" + sid,
                      "PUT",
                      m("version", v, "target", "你好", "submit", false))
                  .getResponse()
                  .getStatus();
      var a = pool.submit(run);
      var b = pool.submit(run);
      var values =
          new ArrayList<>(List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS)));
      Collections.sort(values);
      assertEquals(List.of(200, 409), values);
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void csrfRequiredAndSecretsNotReturned() throws Exception {
    assertEquals(
        403,
        mvc.perform(
                post("/api/projects").session(admin).contentType("application/json").content("{}"))
            .andReturn()
            .getResponse()
            .getStatus());
    String users = result(admin, "/admin/users", "GET", null).getResponse().getContentAsString();
    assertFalse(users.contains("passwordHash"));
    assertFalse(users.contains("$2a$"));
  }

  @Test
  void crossDepartmentReadAndDownloadDenied() throws Exception {
    var dept =
        call(
            admin,
            "/admin/departments",
            "POST",
            m("name", "TEST remote " + suffix, "zone", "UTC", "enabled", true));
    String name = "mgr" + suffix;
    user(name, 2, dept.get("id").asLong());
    var remote = login(name);
    fail(remote, path(), "GET", null, 403, "OUT_OF_SCOPE");
    importOne("Hello");
    start();
    translate("你好", true);
    approve("");
    long id = release();
    fail(remote, path() + "/deliveries/" + id + "/json", "GET", null, 403, "OUT_OF_SCOPE");
  }

  @Test
  void tooLongTargetBlockedByCodePoints() throws Exception {
    detail =
        call(
            admin,
            path() + "/import",
            "POST",
            m(
                "version",
                version(),
                "replace",
                false,
                "rows",
                List.of(m("key", "tiny", "source", "Hi", "maxLength", 2))));
    start();
    translate("你好呀", true);
    var s = first();
    fail(
        reviewer,
        path() + "/segments/" + s.get("id").asLong() + "/review",
        "POST",
        m("version", s.get("version").asLong(), "approve", true, "note", ""),
        409,
        "QA_BLOCKED");
  }

  @Test
  void accentedTermsRemainDistinctLiteralEntries() throws Exception {
    detail =
        call(
            admin,
            path() + "/terms",
            "POST",
            m("version", version(), "source", "cafe", "target", "咖啡馆", "note", "TEST plain"));
    detail =
        call(
            admin,
            path() + "/terms",
            "POST",
            m("version", version(), "source", "café", "target", "咖啡店", "note", "TEST accented"));
    assertEquals(2, detail.get("terms").size());
    fail(
        admin,
        path() + "/terms",
        "POST",
        m("version", version(), "source", "CAFE", "target", "重复", "note", ""),
        409,
        "DUPLICATE_TERM");
  }
}
