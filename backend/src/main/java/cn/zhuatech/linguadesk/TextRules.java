// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.linguadesk;

import java.util.*;
import java.util.regex.*;

/** 软件文案保留空白并检测变量、数字、术语和Unicode字符长度。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
public final class TextRules {
  static final Pattern TOKENS =
      Pattern.compile(
          "\\$\\{[A-Za-z_][A-Za-z0-9_.]*}|\\{\\{[A-Za-z_][A-Za-z0-9_.]*}}|\\{[A-Za-z_][A-Za-z0-9_.]*}|%(?:\\d+\\$)?[sdif]|%\\([A-Za-z_][A-Za-z0-9_]*\\)[sdif]");
  static final Pattern NUMBERS = Pattern.compile("\\d+(?:[.,]\\d+)*");

  /** 不去掉文案首尾空白；仅允许换行、回车和制表控制字符，拒绝非法代理对。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public static String text(Object v, int max, boolean required) {
    if (!(v instanceof String s) || s.length() > max || (required && s.isBlank()))
      throw new Problem(400, "INVALID_TEXT");
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t')
        throw new Problem(400, "INVALID_TEXT");
      if (Character.isHighSurrogate(c)) {
        if (++i >= s.length() || !Character.isLowSurrogate(s.charAt(i)))
          throw new Problem(400, "INVALID_TEXT");
      } else if (Character.isLowSurrogate(c)) throw new Problem(400, "INVALID_TEXT");
    }
    return s;
  }

  /** 文案键保留大小写，但同一项目不允许仅大小写不同的重复键。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public static String key(Object value) {
    String s = text(value, 160, true);
    if (!s.matches("[A-Za-z0-9][A-Za-z0-9_.:/-]{0,159}")) throw new Problem(400, "INVALID_KEY");
    return s;
  }

  /** 语言标签规范化，拒绝相同源与目标语言。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public static String locale(Object v) {
    String s = Rules.text(v, 40, true);
    if (!s.matches("[a-zA-Z]{2,3}(?:-[A-Za-z0-9]{2,8})*")) throw new Problem(400, "INVALID_LOCALE");
    return Locale.forLanguageTag(s).toLanguageTag();
  }

  /** 统计变量或数字的出现次数，不因重复变量被集合去重。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  static Map<String, Integer> tokens(String s, Pattern p) {
    var out = new TreeMap<String, Integer>();
    var m = p.matcher(s);
    while (m.find()) out.merge(m.group(), 1, Integer::sum);
    return out;
  }

  /** 校验是规则检查，不能证明翻译语义正确；阻断项不得豁免。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public static Map<String, Object> qa(Segment s, List<GlossaryTerm> terms) {
    var blockers = new ArrayList<String>();
    var warnings = new ArrayList<String>();
    if (s.target.isBlank()) blockers.add("EMPTY_TARGET");
    if (!tokens(s.source.replace("%%", ""), TOKENS)
        .equals(tokens(s.target.replace("%%", ""), TOKENS))) blockers.add("PLACEHOLDERS");
    if (s.maxLength > 0 && s.target.codePointCount(0, s.target.length()) > s.maxLength)
      blockers.add("LENGTH");
    if (!s.target.isBlank() && s.source.equals(s.target)) warnings.add("SAME_TEXT");
    if (!tokens(TOKENS.matcher(s.source).replaceAll(""), NUMBERS)
        .equals(tokens(TOKENS.matcher(s.target).replaceAll(""), NUMBERS))) warnings.add("NUMBERS");
    for (var t : terms)
      if (s.source.contains(t.source) && !s.target.contains(t.target))
        warnings.add("TERM:" + t.source + " → " + t.target);
    return Map.of(
        "blockers",
        blockers,
        "warnings",
        warnings,
        "characters",
        s.target.codePointCount(0, s.target.length()));
  }

  private TextRules() {}
}
