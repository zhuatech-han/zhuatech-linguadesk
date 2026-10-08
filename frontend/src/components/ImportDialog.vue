<!-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2 -->
<script setup>
import { ref, watch } from "vue";
import { X, Upload } from "@lucide/vue";
import { t, readSourceFile } from "../ui.js";
const props = defineProps({
  project: { type: Object, required: true },
  pending: Boolean,
  perform: { type: Function, required: true },
});
const emit = defineEmits(["close", "error"]);
const content = ref("");
const replace = ref(false);
const preview = ref(null);
const fileInput = ref(null);
watch([content, replace], () => (preview.value = null));
/** 只读取用户选择的实际文件；字节大小和UTF8明确校验。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function choose(event) {
  const file = event.target.files?.[0];
  if (!file) return;
  try {
    content.value = await readSourceFile(file);
  } catch (e) {
    emit("error", e);
  }
  event.target.value = "";
}
/** 只校验和预览，不改变数据库。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function check() {
  preview.value = await props.perform(
    `/projects/${props.project.id}/import/preview`,
    "POST",
    { content: content.value, replace: replace.value },
  );
}
/** 确认导入使用最新项目版本，数据失败整批回滚。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function apply() {
  const result = await props.perform(
    `/projects/${props.project.id}/import`,
    "POST",
    {
      version: props.project.version,
      content: content.value,
      replace: replace.value,
    },
  );
  if (result) emit("close");
}
</script>
<template>
  <div class="modal-backdrop">
    <section
      class="modal wide"
      role="dialog"
      aria-modal="true"
      :aria-label="t('导入原文', 'Import sources')"
    >
      <header>
        <h2>{{ t("导入原文", "Import source strings") }}</h2>
        <button
          class="icon-button"
          :disabled="pending"
          :aria-label="t('关闭', 'Close')"
          @click="emit('close')"
        >
          <X :size="20" />
        </button>
      </header>
      <div class="modal-body">
        <input
          ref="fileInput"
          class="file-input"
          type="file"
          accept=".json,application/json"
          @change="choose"
        /><button :disabled="pending" @click="fileInput.click()">
          <Upload :size="16" />{{
            t("选择 JSON 文件", "Choose JSON file")
          }}</button
        ><label class="mt"
          >{{ t("或粘贴扁平 JSON", "Or paste flat JSON")
          }}<textarea
            v-model="content"
            rows="9"
            spellcheck="false"
            :disabled="pending"
            placeholder='{"welcome": "Welcome {name}", "save": "Save"}'
          ></textarea>
        </label>
        <p class="hint">
          {{
            t(
              "仅字符串值，最多 512 KB；保留文案空白。嵌套对象、数组和重复键会拒绝整批。",
              "String values only, up to 512 KB; whitespace preserved. Nested objects, arrays and duplicate keys reject the whole import.",
            )
          }}
        </p>
        <label class="check"
          ><input v-model="replace" type="checkbox" :disabled="pending" />{{
            t(
              "以此次导入替换全部原文：缺失键归档",
              "Replace all sources: archive keys missing from this import",
            )
          }}</label
        >
        <p class="hint">
          {{
            t(
              "原文改变会使审校失效；JSON 导入将上下文与字符上限恢复为默认值。历史交付保持不变。",
              "Changed sources lose approval; JSON imports reset context and character limits. Historical deliveries stay unchanged.",
            )
          }}
        </p>
        <div v-if="preview" class="import-preview">
          <strong>{{
            t("验证通过，可导入", "Validated and ready to import")
          }}</strong>
          <div class="summary-line">
            <span
              >{{ t("新增", "Added") }} <b>{{ preview.added }}</b></span
            ><span
              >{{ t("变更", "Changed") }} <b>{{ preview.changed }}</b></span
            ><span
              >{{ t("归档", "Archived") }} <b>{{ preview.removed }}</b></span
            ><span
              >{{ t("导入后总计", "Total after import") }}
              <b>{{ preview.total }}</b></span
            >
          </div>
          <table>
            <thead>
              <tr>
                <th>Key</th>
                <th>{{ t("原文预览", "Source preview") }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in preview.rows.slice(0, 5)" :key="r.key">
                <td class="mono">{{ r.key }}</td>
                <td>{{ r.source }}</td>
              </tr>
            </tbody>
          </table>
          <small v-if="preview.rows.length > 5">{{
            t("仅展示前 5 条", "First 5 rows shown")
          }}</small>
        </div>
      </div>
      <footer>
        <button :disabled="pending" @click="emit('close')">
          {{ t("取消", "Cancel") }}</button
        ><button
          v-if="!preview"
          class="primary"
          :disabled="pending || !content.trim()"
          @click="check"
        >
          {{ t("验证并预览", "Validate & preview") }}</button
        ><button v-else class="primary" :disabled="pending" @click="apply">
          {{ t("确认导入", "Confirm import") }}
        </button>
      </footer>
    </section>
  </div>
</template>
