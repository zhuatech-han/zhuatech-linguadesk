// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
import { ref } from "vue";
export const language = ref("zh");
/** 中英文界面文案，保留所有业务文本原值。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function t(zh, en) {
  return language.value === "zh" ? zh : en;
}
/** 状态不自动推断完成，仅显示服务端当前状态。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function stateName(code) {
  return (
    {
      DRAFT: t("草稿", "Draft"),
      ACTIVE: t("进行中", "Active"),
      PAUSED: t("已暂停", "Paused"),
      CLOSED: t("已关单", "Closed"),
      CANCELLED: t("已取消", "Cancelled"),
      UNTRANSLATED: t("未翻译", "Untranslated"),
      REVIEW: t("待审校", "In review"),
      APPROVED: t("已通过", "Approved"),
      CHANGES: t("需修改", "Changes requested"),
    }[code] || code
  );
}
const errors = {
  EMPTY_IMPORT: [
    "没有可导入的文案，请添加字符串键和值。",
    "No source strings. Add string keys and values.",
  ],
  UNAUTHENTICATED: [
    "会话已失效，请重新登录。",
    "Session expired. Sign in again.",
  ],
  LOGIN_FAILED: [
    "账号或密码不正确，或账号已停用。",
    "Invalid credentials or disabled account.",
  ],
  LOGIN_THROTTLED: [
    "登录尝试过多，请五分钟后重试。",
    "Too many attempts. Retry in five minutes.",
  ],
  FORBIDDEN: [
    "没有此项操作权限。",
    "You do not have permission for this action.",
  ],
  OUT_OF_SCOPE: [
    "此项目不在你的访问范围。",
    "This project is outside your scope.",
  ],
  VERSION_CONFLICT: [
    "记录已变更，请刷新后核对再操作。",
    "Record changed. Refresh and check before saving.",
  ],
  RESULT_UNKNOWN: [
    "网络中断，提交结果未知。请刷新核对，不要重复提交。",
    "Connection interrupted; result unknown. Refresh and check before resubmitting.",
  ],
  NETWORK_ERROR: [
    "连接失败，请检查网络后刷新。",
    "Connection failed. Check your network and refresh.",
  ],
  INVALID_JSON: [
    "JSON 格式错误或存在重复键，未导入任何内容。",
    "Invalid JSON or duplicate keys. Nothing imported.",
  ],
  FLAT_JSON_ONLY: [
    "仅支持扁平 JSON，值必须是字符串；不支持嵌套或数组。",
    "Use a flat JSON object with string values; no nested objects or arrays.",
  ],
  DUPLICATE_KEY: [
    "键名重复（包括仅大小写不同的键）。整批未导入。",
    "Duplicate keys, including case variants. Nothing imported.",
  ],
  INVALID_KEY: [
    "键名只允许字母、数字及 _ . : / -，最多 160 字符。",
    "Keys require letters, digits or _ . : / -, up to 160 characters.",
  ],
  INVALID_TEXT: [
    "文案为空、超长或包含不支持的控制字符。",
    "Text is empty, too long or has unsupported control characters.",
  ],
  INDEPENDENT_REVIEWER: [
    "译员和审校必须由不同账号负责。",
    "Translator and reviewer must be different accounts.",
  ],
  ASSIGNEE_INVALID: [
    "指派人员未启用、权限不足或团队范围不匹配。",
    "Assignee is disabled, lacks permission or has an incompatible team scope.",
  ],
  TRANSLATOR_ONLY: [
    "只有当前指定译员可以编辑此项目译文。",
    "Only the assigned translator may edit this project.",
  ],
  REVIEWER_ONLY: [
    "只有当前指定审校可以处理此项目译文。",
    "Only the assigned reviewer may review this project.",
  ],
  SELF_REVIEW: [
    "不能审校自己编辑的译文版本。",
    "You cannot approve a version you edited.",
  ],
  QA_BLOCKED: [
    "存在变量、长度或空译文问题，必须修改后重新审校。",
    "Fix placeholder, length or empty-target blockers before approval.",
  ],
  WAIVER_REQUIRED: [
    "存在核查提示。确认无误后填写具体说明才可通过。",
    "Warnings require a specific explanation before approval.",
  ],
  NOTE_REQUIRED: [
    "请填写原因或修改意见。",
    "Enter a reason or change request.",
  ],
  REVIEW_REQUIRED: [
    "仍有译文未通过独立审校，不能发布。",
    "Some translations lack independent approval. Release blocked.",
  ],
  UNRELEASED_CHANGES: [
    "当前版本尚未发布，请先完成审校并发布。",
    "Current changes are not released. Review and release first.",
  ],
  GLOSSARY_FROZEN: [
    "项目开始后术语已冻结。",
    "Glossary is frozen after starting the project.",
  ],
  PROJECT_LOCKED: [
    "当前项目状态不允许此操作。",
    "The project state does not permit this action.",
  ],
  STATE_INVALID: [
    "当前状态不能执行此动作，或项目尚无文案。",
    "Invalid action for this state, or no source strings yet.",
  ],
  LAST_ADMIN: [
    "必须保留一个启用的完整管理员。",
    "At least one enabled full administrator must remain.",
  ],
  PASSWORD_WEAK: [
    "密码需 12–72 字节，并包含大写、小写字母和数字。",
    "Password requires 12–72 bytes, uppercase, lowercase and a digit.",
  ],
  ACCOUNT_ASSIGNED: [
    "账号已有项目指派，不能更换团队。",
    "Account has project assignments; team cannot be changed.",
  ],
  RESOURCE_LIMIT: [
    "超出实例文案数量上限，未执行此操作。",
    "Configured resource limit exceeded. Nothing changed.",
  ],
  FILE_TOO_LARGE: ["文件需小于 512 KB。", "File must be smaller than 512 KB."],
  ALREADY_RELEASED: [
    "该版本已发布，可从交付列表下载。",
    "This version is already released. Download from deliveries.",
  ],
  SAME_LOCALE: [
    "源语言和目标语言不能相同。",
    "Source and target locales must differ.",
  ],
  CONFLICT: [
    "记录重复或仍被业务使用，请核对后再提交。",
    "Duplicate or referenced record. Check before submitting.",
  ],
  INVALID_INPUT: [
    "字段格式不正确，请检查表单。",
    "Invalid fields. Check the form.",
  ],
  OLD_PASSWORD_INVALID: ["原密码不正确。", "Current password is incorrect."],
  NOT_FOUND: ["记录不存在或已移除。", "Record not found or removed."],
  EMPTY_TARGET: ["空译文不能送审。", "Empty translations cannot be submitted."],
  DUPLICATE_TERM: [
    "此术语已存在（包括大小写变体）。",
    "Term already exists, including case variants.",
  ],
};
/** 明确错误影响，未知代码保留用于问题排查。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function errorMessage(error) {
  const a = errors[error.message];
  return a ? t(...a) : `${t("操作未完成", "Action failed")} (${error.message})`;
}
/** 规则只提供核查线索，人工审校负责语义。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function qaName(code) {
  return (
    {
      EMPTY_TARGET: t("译文为空", "Empty translation"),
      PLACEHOLDERS: t(
        "变量名称或次数不一致",
        "Placeholder names or counts differ",
      ),
      LENGTH: t("译文超过字符上限", "Translation exceeds character limit"),
      SAME_TEXT: t("译文与原文相同", "Translation equals source"),
      NUMBERS: t("数字不一致，请核对", "Numbers differ; check meaning"),
    }[code] ||
    (code.startsWith("TERM:")
      ? t("术语需核对：", "Check term: ") + code.slice(5)
      : code)
  );
}
/** 文件选择仅用于当前用户选择的JSON，不自动上传外部内容。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export async function readSourceFile(file) {
  if (file.size > 512000) throw new Error("FILE_TOO_LARGE");
  const bytes = await file.arrayBuffer();
  try {
    return new TextDecoder("utf-8", { fatal: true }).decode(bytes);
  } catch {
    throw new Error("INVALID_TEXT");
  }
}
/** 搜索及分页不会改变文案；便于可重复验证。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function filterSegments(rows, q, status) {
  const needle = q.toLocaleLowerCase();
  return rows.filter(
    (s) =>
      (!status || s.status === status) &&
      (!needle ||
        [s.key, s.source, s.target, s.context].some((v) =>
          v.toLocaleLowerCase().includes(needle),
        )),
  );
}

export const confirmation = ref(null);
/** 页面内确认避免原生弹窗阻塞，并等待用户明确保留或继续。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function ask(message) {
  if (confirmation.value) return Promise.resolve(false);
  return new Promise((resolve) => {
    confirmation.value = { message, resolve };
  });
}
/** 先关闭提示再兑现结果；取消不会改变业务或未保存译稿。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function answerConfirmation(ok) {
  const pending = confirmation.value;
  confirmation.value = null;
  pending?.resolve(ok);
}

/** 深拷贝API的JSON记录用于表单，兼容Vue响应式代理并避免修改列表原值。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function cloneRecord(row) {
  return JSON.parse(JSON.stringify(row));
}
