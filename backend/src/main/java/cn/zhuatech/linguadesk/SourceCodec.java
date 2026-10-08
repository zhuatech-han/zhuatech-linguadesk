// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.linguadesk;

import java.nio.charset.StandardCharsets;
import java.util.*;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** 扁平JSON严格导入；完整验证后才修改数据库，不静默跳过错误行。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
public final class SourceCodec {
  static final JsonMapper JSON =
      JsonMapper.builder()
          .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
          .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .build();

  /** 已验证的源文行，上下文与Unicode字符上限跟随源文版本。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public record Row(String key, String source, String context, int maxLength) {}

  /** 接受原始JSON对象或严格行数组，拒绝嵌套、重复键、超限与未知结构。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public static List<Row> parse(Map<String, Object> b, int limit) {
    List<Row> rows = new ArrayList<>();
    if (b.containsKey("content")) {
      String content = TextRules.text(b.get("content"), 512000, true);
      if (content.getBytes(StandardCharsets.UTF_8).length > 512000)
        throw new Problem(413, "FILE_TOO_LARGE");
      try {
        var node = JSON.readTree(content);
        if (node == null || !node.isObject()) throw new Problem(400, "FLAT_JSON_ONLY");
        for (var entry : node.properties()) {
          if (!entry.getValue().isString()) throw new Problem(400, "FLAT_JSON_ONLY");
          rows.add(
              new Row(
                  TextRules.key(entry.getKey()),
                  TextRules.text(entry.getValue().asString(), 4000, true),
                  "",
                  0));
        }
      } catch (Problem e) {
        throw e;
      } catch (Exception e) {
        throw new Problem(400, "INVALID_JSON");
      }
    } else {
      if (!(b.get("rows") instanceof List<?> list)) throw new Problem(400, "INVALID_INPUT");
      for (Object item : list) {
        if (!(item instanceof Map<?, ?> r)) throw new Problem(400, "INVALID_INPUT");
        rows.add(
            new Row(
                TextRules.key(r.get("key")),
                TextRules.text(r.get("source"), 4000, true),
                TextRules.text(r.containsKey("context") ? r.get("context") : "", 1000, false),
                Rules.integer(r.containsKey("maxLength") ? r.get("maxLength") : 0, 0, 4000)));
      }
    }
    if (rows.isEmpty()) throw new Problem(400, "EMPTY_IMPORT");
    if (rows.size() > limit) throw new Problem(413, "RESOURCE_LIMIT");
    Set<String> keys = new HashSet<>();
    for (var r : rows)
      if (!keys.add(r.key.toLowerCase(Locale.ROOT))) throw new Problem(400, "DUPLICATE_KEY");
    return rows;
  }

  /** JSON交付精确保留文案空白和Unicode，不往业务载荷添加品牌。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public static String exportJson(List<Segment> rows) {
    Map<String, String> out = new LinkedHashMap<>();
    for (var s : rows) out.put(s.key, s.target);
    return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(out) + "\n";
  }

  /** 双语CSV使用标准引用并防止表格公式执行，JSON是精确交付格式。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public static String exportCsv(List<Segment> rows) {
    var out = new StringBuilder("key,source,target,context,max_length,status\r\n");
    for (var s : rows)
      out.append(Rules.csv(s.key))
          .append(',')
          .append(Rules.csv(s.source))
          .append(',')
          .append(Rules.csv(s.target))
          .append(',')
          .append(Rules.csv(s.context))
          .append(',')
          .append(s.maxLength)
          .append(',')
          .append(Rules.csv(s.status))
          .append("\r\n");
    return out.toString();
  }

  private SourceCodec() {}
}
