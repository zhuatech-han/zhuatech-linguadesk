// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.linguadesk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

/** 文本边界、准确载荷及软件变量核查反例。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
class TextRulesTest {
  Segment s(String source, String target) {
    var s = new Segment();
    s.source = source;
    s.target = target;
    s.key = "test";
    return s;
  }

  List<?> blockers(Segment s) {
    return (List<?>) TextRules.qa(s, List.of()).get("blockers");
  }

  @Test
  void repeatedVariablesMustKeepTheirCounts() {
    assertTrue(blockers(s("{name} {name}", "{name}")).contains("PLACEHOLDERS"));
  }

  @Test
  void doubledBracesAreNotEquivalentToSingleBraces() {
    assertTrue(blockers(s("{{name}}", "{name}")).contains("PLACEHOLDERS"));
  }

  @Test
  void dollarPlaceholdersCannotLoseTheirDollar() {
    assertTrue(blockers(s("${name}", "{name}")).contains("PLACEHOLDERS"));
  }

  @Test
  void positionalPrintfAllowsReordering() {
    assertTrue(blockers(s("%1$s %2$d", "%2$d %1$s")).isEmpty());
  }

  @Test
  void escapedPrintfIsNotAPlaceholder() {
    assertTrue(blockers(s("%%s text", "文字")).isEmpty());
  }

  @Test
  void supplementaryCharactersCountAsOne() {
    var s = s("Hi", "😀好");
    s.maxLength = 2;
    assertTrue(blockers(s).isEmpty());
    s.maxLength = 1;
    assertTrue(blockers(s).contains("LENGTH"));
  }

  @Test
  void embeddedChineseNumbersStillWarn() {
    assertTrue(
        ((List<?>) TextRules.qa(s("Version2", "版本3"), List.of()).get("warnings"))
            .contains("NUMBERS"));
  }

  @Test
  void termChecksAreCaseSensitiveAndLiteral() {
    var term = new GlossaryTerm();
    term.source = "Account";
    term.target = "账户";
    assertEquals(
        1,
        ((List<?>) TextRules.qa(s("Account ready", "账号就绪"), List.of(term)).get("warnings")).size());
    assertEquals(
        0,
        ((List<?>) TextRules.qa(s("account ready", "账号就绪"), List.of(term)).get("warnings")).size());
  }

  @Test
  void invalidSurrogatesAndNulAreRejected() {
    assertThrows(Problem.class, () -> TextRules.text("\uD800", 4000, false));
    assertThrows(Problem.class, () -> TextRules.text("x\0", 4000, false));
  }

  @Test
  void exactJsonPreservesWhitespaceHtmlAndUnicode() {
    var segment = s("hello", " 你好\n <script>literal</script> ");
    var json = SourceCodec.JSON.readTree(SourceCodec.exportJson(List.of(segment)));
    assertEquals(segment.target, json.get("test").asString());
  }

  @Test
  void spreadsheetFormulaIsEscapedInCsvButJsonIsExact() {
    var s = s("input", "=SUM(A1:A2)");
    assertTrue(SourceCodec.exportCsv(List.of(s)).contains("'="));
    assertEquals(
        s.target,
        SourceCodec.JSON.readTree(SourceCodec.exportJson(List.of(s))).get("test").asString());
  }

  @Test
  void unsupportedJsonScalarRejected() {
    assertThrows(Problem.class, () -> SourceCodec.parse(Map.of("content", "{\"count\":3}"), 2000));
  }

  @Test
  void trailingJsonIsRejected() {
    assertThrows(
        Problem.class,
        () -> SourceCodec.parse(Map.of("content", "{\"a\":\"x\"} {\"b\":\"y\"}"), 2000));
  }

  @Test
  void keyWhitespaceIsRejectedWithoutSilentRenaming() {
    assertThrows(Problem.class, () -> TextRules.key(" leading"));
  }

  @Test
  void emptyImportHasClearInputError() {
    var error = assertThrows(Problem.class, () -> SourceCodec.parse(Map.of("content", "{}"), 2000));
    assertEquals(400, error.status);
    assertEquals("EMPTY_IMPORT", error.getMessage());
  }
}
