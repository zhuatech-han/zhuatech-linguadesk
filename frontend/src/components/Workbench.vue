<!-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2 -->
<script setup>
import { ref, computed, watch } from "vue";
import {
  Search,
  Check,
  Send,
  History,
  ArrowLeft,
  Download,
  Plus,
  Pause,
  Play,
  Upload,
  RefreshCw,
  Pencil,
  Trash2,
  LockKeyhole,
} from "@lucide/vue";
import { api, download } from "../api.js";
import { t, stateName, qaName, filterSegments, ask } from "../ui.js";
import ImportDialog from "./ImportDialog.vue";
import ProjectDialog from "./ProjectDialog.vue";
const props = defineProps({
  detail: { type: Object, required: true },
  profile: { type: Object, required: true },
  options: { type: Object, required: true },
  pending: Boolean,
  perform: { type: Function, required: true },
});
const emit = defineEmits(["back", "refresh", "error"]);
const project = computed(() => props.detail.project);
const tab = ref("strings");
const q = ref("");
const status = ref("");
const selectedId = ref(null);
const target = ref("");
const reviewNote = ref("");
const stored = ref(null);
const suggestions = ref([]);
const sourceModal = ref(false);
const sourceForm = ref({});
const termModal = ref(false);
const termForm = ref({});
const showImport = ref(false);
const showEdit = ref(false);
const releaseModal = ref(false);
const releaseName = ref("");
const actionModal = ref("");
const actionNote = ref("");
const showHistory = ref(false);
const has = (c) => props.profile.permissions.includes(c);
const editable = computed(() =>
  ["DRAFT", "ACTIVE", "PAUSED"].includes(project.value.state),
);
const filtered = computed(() =>
  filterSegments(props.detail.segments, q.value, status.value),
);
const selected = computed(
  () => props.detail.segments.find((s) => s.id === selectedId.value) || null,
);
const dirty = computed(
  () => selected.value && target.value !== selected.value.target,
);
const translator = computed(
  () =>
    has("translate") &&
    project.value.translatorId === props.profile.id &&
    project.value.state === "ACTIVE",
);
const reviewer = computed(
  () =>
    has("review") &&
    project.value.reviewerId === props.profile.id &&
    project.value.state === "ACTIVE",
);
const person = (id) =>
  props.options.users.find((a) => a.id === id)?.displayName || `#${id}`;
/** 切换文案前明确提醒尚未保存的草稿。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function select(id) {
  if (id === selectedId.value) return;
  if (
    dirty.value &&
    !(await ask(
      t(
        "当前译文尚未保存，确定放弃并切换？",
        "Discard unsaved translation and switch?",
      ),
    ))
  )
    return;
  selectedId.value = id;
}
let loadSerial = 0;
watch(
  () => [selected.value?.id, selected.value?.version],
  async () => {
    const s = selected.value;
    target.value = s?.target || "";
    reviewNote.value = s?.reviewNote || "";
    stored.value = null;
    suggestions.value = [];
    const n = ++loadSerial;
    if (!s) return;
    try {
      const data = await api(`/projects/${project.value.id}/segments/${s.id}`);
      if (n !== loadSerial) return;
      stored.value = data;
      const memory = await api(
        `/projects/${project.value.id}/segments/${s.id}/memory`,
      );
      if (n === loadSerial) suggestions.value = memory;
    } catch (e) {
      if (n === loadSerial) emit("error", e);
    }
  },
  { immediate: true },
);
watch(
  () => props.detail.segments,
  (rows) => {
    if (!rows.some((s) => s.id === selectedId.value))
      selectedId.value = rows[0]?.id || null;
  },
  { immediate: true },
);
/** 保存与送审只提交当前文案版本；无自动重试。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
watch(filtered, (rows) => {
  if (!dirty.value && !rows.some((s) => s.id === selectedId.value))
    selectedId.value = rows[0]?.id || null;
});
async function save(submit) {
  await props.perform(
    `/projects/${project.value.id}/segments/${selected.value.id}`,
    "PUT",
    { version: selected.value.version, target: target.value, submit },
  );
}
/** 审批准确的当前文案版本；修改意见和规则提示由服务端校验。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function review(approve) {
  await props.perform(
    `/projects/${project.value.id}/segments/${selected.value.id}/review`,
    "POST",
    { version: selected.value.version, approve, note: reviewNote.value },
  );
}
/** 显式改变项目状态和留下原因，失败不关闭对话框。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function action(code) {
  if (
    await props.perform(
      `/projects/${project.value.id}/actions/${code}`,
      "POST",
      { version: project.value.version, note: actionNote.value },
    )
  ) {
    actionModal.value = "";
    actionNote.value = "";
  }
}
/** 冻结全部已审校文案的交付版本，成功才切换交付列表。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function release() {
  if (
    await props.perform(`/projects/${project.value.id}/deliveries`, "POST", {
      version: project.value.version,
      name: releaseName.value,
    })
  ) {
    releaseModal.value = false;
    releaseName.value = "";
    tab.value = "deliveries";
  }
}
/** 下载指定历史版本，不用最新译文覆盖历史文件。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function getDelivery(d, format) {
  try {
    await download(
      `/projects/${project.value.id}/deliveries/${d.id}/${format}`,
      `linguadesk-v${d.number}.${format}`,
    );
  } catch (e) {
    emit("error", e);
  }
}
/** 为真实源文准备独立表单。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
function editSource(s) {
  sourceForm.value = s
    ? { ...s }
    : { key: "", source: "", context: "", maxLength: 0 };
  sourceModal.value = true;
}
/** 提交源文、上下文和字符上限，整批校验后修改。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function saveSource() {
  const s = sourceForm.value;
  if (
    !s.id &&
    props.detail.segments.some(
      (r) => r.key.toLowerCase() === s.key.toLowerCase(),
    )
  ) {
    emit("error", new Error("DUPLICATE_KEY"));
    return;
  }
  const result = await props.perform(
    `/projects/${project.value.id}/import`,
    "POST",
    {
      version: project.value.version,
      replace: false,
      rows: [
        {
          key: s.key,
          source: s.source,
          context: s.context,
          maxLength: s.maxLength,
        },
      ],
    },
  );
  if (result) sourceModal.value = false;
}
/** 归档业务键，保留修订和历史交付。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function archiveSource() {
  const s = sourceForm.value;
  if (
    !(await ask(
      t(
        "归档此键？历史修订与已发布版本仍保留。",
        "Archive this key? History and released versions remain.",
      ),
    ))
  )
    return;
  const r = await props.perform(
    `/projects/${project.value.id}/segments/${s.id}`,
    "DELETE",
    { version: project.value.version, segmentVersion: s.version },
  );
  if (r) sourceModal.value = false;
}
/** 在草稿项目创建或编辑真实术语。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
function editTerm(term) {
  termForm.value = term ? { ...term } : { source: "", target: "", note: "" };
  termModal.value = true;
}
/** 核对项目和术语版本，开始后的词库不可改。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function saveTerm() {
  const term = termForm.value;
  const result = await props.perform(
    `/projects/${project.value.id}/terms${term.id ? "/" + term.id : ""}`,
    term.id ? "PUT" : "POST",
    { ...term, version: project.value.version, termVersion: term.version },
  );
  if (result) termModal.value = false;
}
/** 仅移除草稿术语，服务端版本冲突不覆盖。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function removeTerm(term) {
  if (!(await ask(t("确定移除此术语？", "Remove this glossary term?")))) return;
  await props.perform(
    `/projects/${project.value.id}/terms/${term.id}`,
    "DELETE",
    { version: project.value.version, termVersion: term.version },
  );
}
/** 统一未保存译稿导航保护。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function canLeave() {
  return (
    !dirty.value ||
    (await ask(t("放弃未保存译文？", "Discard unsaved translation?")))
  );
}
defineExpose({ canLeave });
/** 从业务详情返回前检查未保存译文。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function back() {
  if (
    !dirty.value ||
    (await ask(
      t("放弃未保存译文并返回？", "Discard unsaved translation and go back?"),
    ))
  )
    emit("back");
}
</script>
<template>
  <section class="project-detail">
    <button class="text-button breadcrumb" @click="back">
      <ArrowLeft :size="16" />{{ t("翻译项目", "Projects") }}
    </button>
    <header class="page-heading">
      <div>
        <div class="title-row">
          <h1>{{ project.name }}</h1>
          <span class="badge" :class="project.state.toLowerCase()">{{
            stateName(project.state)
          }}</span>
        </div>
        <div class="metadata">
          <span class="locale-pair"
            >{{ project.sourceLocale }} <span>→</span>
            {{ project.targetLocale }}</span
          ><span
            >{{ t("译员", "Translator") }}
            {{ person(project.translatorId) }}</span
          ><span
            >{{ t("审校", "Reviewer") }} {{ person(project.reviewerId) }}</span
          ><span>{{ t("期限", "Due") }} {{ project.dueDate }}</span>
        </div>
      </div>
      <div class="actions">
        <button
          :disabled="pending || dirty"
          :title="t('刷新项目', 'Refresh project')"
          @click="emit('refresh')"
        >
          <RefreshCw :size="16" /></button
        ><button
          v-if="has('manage') && editable"
          :disabled="pending || dirty"
          @click="showEdit = true"
        >
          <Pencil :size="16" />{{ t("配置", "Setup") }}</button
        ><button
          v-if="has('manage') && editable"
          :disabled="pending || dirty"
          @click="showImport = true"
        >
          <Upload :size="16" />{{ t("导入原文", "Import") }}</button
        ><button
          v-if="has('manage') && project.state === 'DRAFT'"
          class="primary"
          :disabled="pending || !detail.segments.length"
          @click="action('start')"
        >
          <Play :size="16" />{{ t("开始翻译", "Start translation") }}</button
        ><button
          v-if="has('manage') && project.state === 'ACTIVE'"
          :disabled="pending || dirty"
          @click="action('pause')"
        >
          <Pause :size="16" />{{ t("暂停", "Pause") }}</button
        ><button
          v-if="has('manage') && project.state === 'PAUSED'"
          class="primary"
          :disabled="pending"
          @click="action('resume')"
        >
          <Play :size="16" />{{ t("恢复", "Resume") }}</button
        ><button
          v-if="has('publish') && project.state === 'ACTIVE'"
          class="primary"
          :disabled="
            pending ||
            dirty ||
            !detail.counts.total ||
            detail.counts.approved !== detail.counts.total ||
            detail.deliveries[0]?.projectVersion === project.version
          "
          @click="releaseModal = true"
        >
          {{
            detail.deliveries[0]?.projectVersion === project.version
              ? t("当前版本已发布", "Version released")
              : t("发布交付版本", "Release version")
          }}
        </button>
      </div>
    </header>
    <div class="summary-line project-summary">
      <span
        >{{ t("文案", "Strings") }} <b>{{ detail.counts.total }}</b></span
      ><span
        >{{ t("已翻译", "Translated") }}
        <b>{{ detail.counts.translated }}</b></span
      ><span
        >{{ t("已审校", "Approved") }}
        <b>{{ detail.counts.approved }} / {{ detail.counts.total }}</b></span
      ><span v-if="detail.counts.blocked" class="warning-text"
        >{{ t("规则阻断", "QA blockers") }}
        <b>{{ detail.counts.blocked }}</b></span
      >
      <div class="progress">
        <i
          :style="{
            width:
              (detail.counts.total
                ? (100 * detail.counts.approved) / detail.counts.total
                : 0) + '%',
          }"
        ></i>
      </div>
    </div>
    <nav class="tabs">
      <button
        v-for="[key, zh, en] in [
          ['strings', '双语编辑', 'Bilingual editor'],
          ['glossary', '项目术语', 'Glossary'],
          ['deliveries', '交付版本', 'Deliveries'],
          ['events', '项目记录', 'Project history'],
        ]"
        :key="key"
        :class="{ active: tab === key }"
        @click="tab = key"
      >
        {{ t(zh, en)
        }}<span v-if="key === 'deliveries'">{{
          detail.deliveries.length
        }}</span>
      </button>
    </nav>
    <div v-if="tab === 'strings'" class="editor-layout">
      <aside class="string-list">
        <div class="string-search">
          <label class="search"
            ><Search :size="16" /><input
              v-model="q"
              :placeholder="t('搜索键名或文案', 'Search key or text')"
              :aria-label="t('搜索文案', 'Search strings')" /></label
          ><select
            v-model="status"
            :aria-label="t('文案状态', 'String status')"
          >
            <option value="">{{ t("全部状态", "All statuses") }}</option>
            <option
              v-for="s in [
                'UNTRANSLATED',
                'DRAFT',
                'REVIEW',
                'APPROVED',
                'CHANGES',
              ]"
              :key="s"
              :value="s"
            >
              {{ stateName(s) }}
            </option>
          </select>
          <div class="list-count">
            <span>{{ filtered.length }} {{ t("条文案", "strings") }}</span
            ><button
              v-if="has('manage') && editable"
              class="text-button"
              :disabled="pending"
              @click="editSource(null)"
            >
              <Plus :size="14" />{{ t("新增", "Add") }}
            </button>
          </div>
        </div>
        <div class="string-scroll">
          <button
            v-for="s in filtered"
            :key="s.id"
            class="string-row"
            :class="{ selected: selectedId === s.id }"
            @click="select(s.id)"
          >
            <div>
              <span class="mono">{{ s.key }}</span
              ><i class="status-dot" :class="s.status.toLowerCase()"></i>
            </div>
            <p>{{ s.source }}</p>
            <small>{{ stateName(s.status) }}</small>
          </button>
          <div v-if="!filtered.length" class="empty compact">
            {{ t("没有匹配文案", "No matching strings")
            }}<button
              v-if="!detail.segments.length && has('manage') && editable"
              class="text-button"
              @click="showImport = true"
            >
              {{ t("导入 JSON 原文", "Import source JSON") }}
            </button>
          </div>
        </div>
      </aside>
      <div v-if="selected" class="translation-panel">
        <div class="segment-heading">
          <div>
            <span class="mono">{{ selected.key }}</span
            ><span class="badge" :class="selected.status.toLowerCase()">{{
              stateName(selected.status)
            }}</span>
          </div>
          <button
            v-if="has('manage') && editable"
            class="text-button"
            :disabled="pending || dirty"
            @click="editSource(selected)"
          >
            <Pencil :size="14" />{{ t("原文与上下文", "Source & context") }}
          </button>
        </div>
        <div class="editor-body">
          <div class="field-label">
            {{ t("原文", "Source") }} <b>{{ project.sourceLocale }}</b>
          </div>
          <pre class="source-text">{{ selected.source }}</pre>
          <p v-if="selected.context" class="context">
            <b>{{ t("上下文", "Context") }}:</b> {{ selected.context }}
          </p>
          <label class="target-label"
            ><span
              >{{ t("译文", "Translation") }} <b>{{ project.targetLocale }}</b
              ><small
                >{{ [...target].length
                }}{{ selected.maxLength ? " / " + selected.maxLength : "" }}
                {{ t("字符", "characters") }}</small
              ></span
            ><textarea
              v-model="target"
              rows="7"
              :readonly="!translator"
              :disabled="pending"
              spellcheck="true"
              :placeholder="
                translator
                  ? t(
                      '输入译文，保留变量名称与次数',
                      'Translate; preserve placeholder names and counts',
                    )
                  : t('尚未填写译文', 'No translation yet')
              "
            ></textarea>
          </label>
          <div v-if="translator" class="editor-actions">
            <small>{{
              dirty
                ? t("尚未保存", "Unsaved changes")
                : t("当前版本已保存", "Current version saved")
            }}</small
            ><button :disabled="pending || !dirty" @click="save(false)">
              {{ t("保存草稿", "Save draft") }}</button
            ><button
              class="primary"
              :disabled="
                pending ||
                !target.trim() ||
                (!dirty && ['APPROVED', 'REVIEW'].includes(selected.status))
              "
              @click="save(true)"
            >
              <Send :size="15" />{{
                !dirty && selected.status === "APPROVED"
                  ? t("已通过审校", "Approved")
                  : !dirty && selected.status === "REVIEW"
                    ? t("已送审", "Submitted")
                    : t("保存并送审", "Save & submit")
              }}
            </button>
          </div>
          <p v-if="selected.status === 'CHANGES'" class="notice danger">
            <strong>{{ t("审校退回", "Changes requested") }}:</strong>
            {{ selected.reviewNote }}
          </p>
          <div
            v-if="reviewer && selected.status === 'REVIEW'"
            class="review-box"
          >
            <h3>{{ t("审校此版本", "Review this version") }}</h3>
            <label
              >{{
                t(
                  "修改意见 / 核查说明",
                  "Change request / warning explanation",
                )
              }}<textarea
                v-model="reviewNote"
                rows="3"
                maxlength="1000"
                :disabled="pending"
                :placeholder="
                  t(
                    '有规则提示时，说明为什么此译文仍然正确',
                    'Explain any warning you intentionally accept',
                  )
                "
              ></textarea>
            </label>
            <div class="actions">
              <button
                :disabled="pending || !reviewNote.trim()"
                @click="review(false)"
              >
                {{ t("退回修改", "Request changes") }}</button
              ><button
                class="primary"
                :disabled="pending || !stored || stored.qa.blockers.length > 0"
                @click="review(true)"
              >
                <Check :size="16" />{{ t("通过审校", "Approve") }}
              </button>
            </div>
          </div>
          <div v-if="selected.status === 'APPROVED'" class="approved-note">
            <Check :size="17" />{{
              t(
                "此版本已通过独立审校",
                "This version passed independent review",
              )
            }}<span v-if="selected.reviewNote">{{ selected.reviewNote }}</span>
          </div>
          <section v-if="suggestions.length" class="memory">
            <h3>{{ t("已审校的精确匹配", "Exact approved matches") }}</h3>
            <div v-for="(s, i) in suggestions" :key="i">
              <small>{{ s.project }}</small>
              <p>{{ s.target }}</p>
              <button
                v-if="translator"
                class="text-button"
                :disabled="pending"
                @click="target = s.target"
              >
                {{ t("使用并重新审校", "Use and review again") }}
              </button>
            </div>
          </section>
          <button
            class="text-button history-link"
            @click="showHistory = !showHistory"
          >
            <History :size="15" />{{ t("修订记录", "Revision history") }}
            {{ stored?.history.length || 0 }}
          </button>
          <div v-if="showHistory" class="revision-list">
            <article v-for="h in stored?.history || []" :key="h.id">
              <header>
                <strong>v{{ h.segmentVersion }} · {{ h.action }}</strong
                ><small>{{ new Date(h.createdAt).toLocaleString() }}</small>
              </header>
              <pre>{{ h.target || h.source }}</pre>
              <p v-if="h.note">{{ h.note }}</p>
            </article>
          </div>
        </div>
      </div>
      <aside v-if="selected" class="qa-panel">
        <h3>{{ t("版本核查", "Version checks") }}</h3>
        <small class="muted"
          >v{{ selected.version }} ·
          {{
            dirty
              ? t(
                  "以下结果属于已保存译文",
                  "Results refer to saved translation",
                )
              : t("已保存译文", "Saved translation")
          }}</small
        ><template v-if="stored"
          ><div
            v-for="code in stored.qa.blockers"
            :key="code"
            class="qa-item blocker"
          >
            <strong>{{ t("阻断", "Blocker") }}</strong>
            <p>{{ qaName(code) }}</p>
          </div>
          <div
            v-for="code in stored.qa.warnings"
            :key="code"
            class="qa-item warning"
          >
            <strong>{{ t("需核对", "Check") }}</strong>
            <p>{{ qaName(code) }}</p>
          </div>
          <div
            v-if="!stored.qa.blockers.length && !stored.qa.warnings.length"
            class="qa-clear"
          >
            <Check :size="18" />{{ t("规则核查无异常", "No rule issues") }}
          </div></template
        >
        <hr />
        <h3>{{ t("项目术语", "Project glossary") }}</h3>
        <dl v-if="detail.terms.length">
          <template v-for="term in detail.terms" :key="term.id"
            ><dt>{{ term.source }}</dt>
            <dd>{{ term.target }}</dd></template
          >
        </dl>
        <p v-else class="muted">{{ t("未配置术语", "No glossary terms") }}</p>
        <p class="hint">
          {{
            t(
              "规则检查不判断翻译语义。审校仍需核对原文、语境和用词。",
              "Rules do not judge meaning. Review the source, context and wording.",
            )
          }}
        </p>
      </aside>
    </div>
    <section v-else-if="tab === 'glossary'" class="panel">
      <div class="section-toolbar">
        <div>
          <h2>{{ t("项目术语", "Project glossary") }}</h2>
          <p class="muted">
            {{
              t(
                "区分大小写的字面包含检查；项目开始后冻结。",
                "Case-sensitive literal matching; frozen once the project starts.",
              )
            }}
          </p>
        </div>
        <button
          v-if="has('manage') && project.state === 'DRAFT'"
          :disabled="pending"
          @click="editTerm(null)"
        >
          <Plus :size="16" />{{ t("添加术语", "Add term") }}</button
        ><span v-else class="muted"
          ><LockKeyhole :size="14" /> {{ t("只读", "Read only") }}</span
        >
      </div>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>{{ t("源术语", "Source term") }}</th>
              <th>{{ t("目标术语", "Target term") }}</th>
              <th>{{ t("说明", "Note") }}</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="term in detail.terms" :key="term.id">
              <td>{{ term.source }}</td>
              <td>{{ term.target }}</td>
              <td>{{ term.note }}</td>
              <td
                class="actions"
                v-if="has('manage') && project.state === 'DRAFT'"
              >
                <button
                  class="icon-button"
                  :aria-label="t('编辑术语', 'Edit term')"
                  :disabled="pending"
                  @click="editTerm(term)"
                >
                  <Pencil :size="16" /></button
                ><button
                  class="icon-button"
                  :aria-label="t('删除术语', 'Delete term')"
                  :disabled="pending"
                  @click="removeTerm(term)"
                >
                  <Trash2 :size="16" />
                </button>
              </td>
              <td v-else></td>
            </tr>
          </tbody>
        </table>
        <div v-if="!detail.terms.length" class="empty">
          {{ t("尚未添加术语", "No terms yet") }}
        </div>
      </div>
    </section>
    <section v-else-if="tab === 'deliveries'" class="panel">
      <div class="section-toolbar">
        <div>
          <h2>{{ t("冻结的交付版本", "Frozen delivery versions") }}</h2>
          <p class="muted">
            {{
              t(
                "每版包含完整语言 JSON 与双语 CSV；后续编辑不会覆盖。",
                "Each release contains a complete locale JSON and bilingual CSV. Later edits never overwrite it.",
              )
            }}
          </p>
        </div>
      </div>
      <div v-for="d in detail.deliveries" :key="d.id" class="delivery-row">
        <div class="delivery-version">v{{ d.number }}</div>
        <div class="delivery-info">
          <strong>{{ d.name }}</strong
          ><small
            >{{ new Date(d.createdAt).toLocaleString() }} ·
            {{ t("项目版本", "Project version") }} {{ d.projectVersion }}</small
          ><code>SHA-256 {{ d.sha256 }}</code>
        </div>
        <div class="actions">
          <button @click="getDelivery(d, 'json')">
            <Download :size="16" /> JSON</button
          ><button @click="getDelivery(d, 'csv')">
            {{ t("双语 CSV", "Bilingual CSV") }}
          </button>
        </div>
      </div>
      <div v-if="!detail.deliveries.length" class="empty">
        {{
          t(
            "全部文案通过审校后，项目经理可发布交付版本。",
            "After all strings pass review, the manager can release a delivery.",
          )
        }}
      </div>
    </section>
    <section v-else class="panel">
      <div class="section-toolbar">
        <h2>{{ t("项目记录", "Project history") }}</h2>
      </div>
      <table>
        <thead>
          <tr>
            <th>{{ t("时间", "Time") }}</th>
            <th>{{ t("操作", "Action") }}</th>
            <th>{{ t("操作人", "Actor") }}</th>
            <th>{{ t("说明", "Note") }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="e in detail.events" :key="e.id">
            <td>{{ new Date(e.createdAt).toLocaleString() }}</td>
            <td>{{ e.action }}</td>
            <td>{{ person(e.actorId) }}</td>
            <td>{{ e.note }}</td>
          </tr>
        </tbody>
      </table>
    </section>
    <div v-if="has('manage') && editable" class="project-end-actions">
      <button
        v-if="project.state === 'ACTIVE' && detail.deliveries.length"
        class="text-button"
        :disabled="pending || dirty"
        @click="actionModal = 'close'"
      >
        {{ t("完成并关单", "Close completed project") }}</button
      ><button
        class="text-button muted"
        :disabled="pending || dirty"
        @click="actionModal = 'cancel'"
      >
        {{ t("取消项目", "Cancel project") }}
      </button>
    </div>
    <ImportDialog
      v-if="showImport"
      :project="project"
      :pending="pending"
      :perform="perform"
      @close="showImport = false"
      @error="emit('error', $event)"
    /><ProjectDialog
      v-if="showEdit"
      :project="project"
      :options="options"
      :pending="pending"
      :perform="perform"
      @close="showEdit = false"
    />
    <div v-if="sourceModal" class="modal-backdrop">
      <section
        class="modal"
        role="dialog"
        aria-modal="true"
        :aria-label="t('原文与上下文', 'Source & context')"
      >
        <header>
          <h2>{{ t("原文与上下文", "Source & context") }}</h2>
        </header>
        <form @submit.prevent="saveSource">
          <label
            >Key<input
              v-model="sourceForm.key"
              required
              maxlength="160"
              :readonly="!!sourceForm.id"
              :disabled="pending" /></label
          ><label
            >{{ t("原文", "Source")
            }}<textarea
              v-model="sourceForm.source"
              rows="4"
              required
              maxlength="4000"
              :disabled="pending"
            ></textarea></label
          ><label
            >{{ t("上下文", "Context")
            }}<textarea
              v-model="sourceForm.context"
              rows="2"
              maxlength="1000"
              :disabled="pending"
            ></textarea></label
          ><label
            >{{
              t(
                "目标字符上限（0 为不限）",
                "Target character limit (0 = unlimited)",
              )
            }}<input
              v-model.number="sourceForm.maxLength"
              type="number"
              min="0"
              max="4000"
              required
              :disabled="pending"
          /></label>
          <p class="hint">
            {{
              t(
                "变更会使原审校失效，历史交付文件不变。",
                "Changes invalidate approval; historical delivery files stay unchanged.",
              )
            }}
          </p>
          <footer>
            <button
              v-if="sourceForm.id"
              type="button"
              :disabled="pending"
              class="text-button warning-text"
              @click="archiveSource"
            >
              {{ t("归档此键", "Archive key") }}</button
            ><button
              type="button"
              :disabled="pending"
              @click="sourceModal = false"
            >
              {{ t("取消", "Cancel") }}</button
            ><button class="primary" :disabled="pending">
              {{ t("保存原文", "Save source") }}
            </button>
          </footer>
        </form>
      </section>
    </div>
    <div v-if="termModal" class="modal-backdrop">
      <section
        class="modal"
        role="dialog"
        aria-modal="true"
        :aria-label="t('项目术语', 'Project glossary')"
      >
        <header>
          <h2>{{ t("项目术语", "Project glossary") }}</h2>
        </header>
        <form @submit.prevent="saveTerm">
          <label
            >{{ t("源术语", "Source term")
            }}<input
              v-model="termForm.source"
              required
              maxlength="200"
              :disabled="pending" /></label
          ><label
            >{{ t("目标术语", "Target term")
            }}<input
              v-model="termForm.target"
              required
              maxlength="200"
              :disabled="pending" /></label
          ><label
            >{{ t("说明", "Note")
            }}<textarea
              v-model="termForm.note"
              maxlength="500"
              rows="2"
              :disabled="pending"
            ></textarea>
          </label>
          <footer>
            <button
              type="button"
              :disabled="pending"
              @click="termModal = false"
            >
              {{ t("取消", "Cancel") }}</button
            ><button class="primary" :disabled="pending">
              {{ t("保存术语", "Save term") }}
            </button>
          </footer>
        </form>
      </section>
    </div>
    <div v-if="releaseModal" class="modal-backdrop">
      <section
        class="modal"
        role="dialog"
        aria-modal="true"
        :aria-label="t('发布交付版本', 'Release delivery')"
      >
        <header>
          <h2>{{ t("发布交付版本", "Release delivery") }}</h2>
        </header>
        <form @submit.prevent="release">
          <label
            >{{ t("版本名称", "Release name")
            }}<input
              v-model="releaseName"
              required
              maxlength="120"
              :disabled="pending"
              placeholder="v1.0"
          /></label>
          <p class="hint">
            {{
              t(
                "冻结当前全部已审校文案，生成 JSON 和双语 CSV。文件将长期保留，不被后续编辑覆盖。",
                "Freeze all approved strings into JSON and bilingual CSV. Files remain unchanged by future edits.",
              )
            }}
          </p>
          <footer>
            <button
              type="button"
              :disabled="pending"
              @click="releaseModal = false"
            >
              {{ t("取消", "Cancel") }}</button
            ><button class="primary" :disabled="pending">
              {{ t("确认发布", "Confirm release") }}
            </button>
          </footer>
        </form>
      </section>
    </div>
    <div v-if="actionModal" class="modal-backdrop">
      <section
        class="modal"
        role="dialog"
        aria-modal="true"
        :aria-label="t('项目状态变更', 'Project state change')"
      >
        <header>
          <h2>
            {{
              actionModal === "cancel"
                ? t("取消项目", "Cancel project")
                : t("完成并关单", "Close completed project")
            }}
          </h2>
        </header>
        <form @submit.prevent="action(actionModal)">
          <label
            >{{ t("说明", "Reason")
            }}<textarea
              v-model="actionNote"
              rows="3"
              maxlength="1000"
              :required="actionModal === 'cancel'"
              :disabled="pending"
            ></textarea>
          </label>
          <p class="hint">
            {{
              t(
                "此动作锁定项目；现有历史版本仍可下载。",
                "This action locks the project; existing historical releases remain downloadable.",
              )
            }}
          </p>
          <footer>
            <button type="button" :disabled="pending" @click="actionModal = ''">
              {{ t("返回", "Back") }}</button
            ><button class="primary" :disabled="pending">
              {{ t("确认", "Confirm") }}
            </button>
          </footer>
        </form>
      </section>
    </div>
  </section>
</template>
