<!-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2 -->
<script setup>
import { reactive, computed } from "vue";
import { X } from "@lucide/vue";
import { t, language } from "../ui.js";
const props = defineProps({
  options: { type: Object, required: true },
  project: { type: Object, default: null },
  pending: Boolean,
  perform: { type: Function, required: true },
});
const emit = defineEmits(["close"]);
const form = reactive(
  props.project
    ? { ...props.project }
    : {
        name: "",
        departmentId:
          props.options.departments.find((d) => d.enabled)?.id || "",
        sourceLocale: "en",
        targetLocale: "zh-CN",
        platform: "APP",
        translatorId: "",
        reviewerId: "",
        dueDate: new Date().toISOString().slice(0, 10),
      },
);
const people = (perm) =>
  props.options.users.filter(
    (a) =>
      a.enabled &&
      a.permissions.includes(perm) &&
      (a.scope === "ALL" || a.departmentId === Number(form.departmentId)),
  );
const translators = computed(() => people("translate"));
const reviewers = computed(() => people("review"));
/** 创建或更新实际指派；有效身份由服务端复核。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function save() {
  if (
    await props.perform(
      props.project ? `/projects/${props.project.id}` : "/projects",
      props.project ? "PUT" : "POST",
      form,
    )
  )
    emit("close");
}
</script>
<template>
  <div class="modal-backdrop">
    <section
      class="modal"
      role="dialog"
      aria-modal="true"
      :aria-label="t('项目配置', 'Project setup')"
    >
      <header>
        <h2>
          {{
            project
              ? t("编辑项目", "Edit project")
              : t("新建翻译项目", "New translation project")
          }}
        </h2>
        <button
          class="icon-button"
          :aria-label="t('关闭', 'Close')"
          :disabled="pending"
          @click="emit('close')"
        >
          <X :size="20" />
        </button>
      </header>
      <form @submit.prevent="save">
        <label
          >{{ t("项目名称", "Project name")
          }}<input
            v-model="form.name"
            required
            maxlength="160"
            autofocus
            :disabled="pending"
        /></label>
        <div class="form-grid">
          <label
            >{{ t("所属团队", "Team")
            }}<select
              v-model="form.departmentId"
              required
              :disabled="!!project || pending"
            >
              <option
                v-for="d in options.departments.filter((d) => d.enabled)"
                :key="d.id"
                :value="d.id"
              >
                {{ d.name }}
              </option>
            </select></label
          ><label
            >{{ t("平台", "Platform")
            }}<select v-model="form.platform" :disabled="!!project || pending">
              <option
                v-for="d in options.platforms.filter(
                  (d) => d.enabled || d.code === form.platform,
                )"
                :key="d.id"
                :value="d.code"
              >
                {{ language === "zh" ? d.name : d.nameEn }}
              </option>
            </select></label
          ><label
            >{{ t("源语言标签", "Source locale")
            }}<input
              v-model="form.sourceLocale"
              required
              maxlength="40"
              placeholder="en"
              :disabled="!!project || pending" /></label
          ><label
            >{{ t("目标语言标签", "Target locale")
            }}<input
              v-model="form.targetLocale"
              required
              maxlength="40"
              placeholder="zh-CN / de / fr"
              :disabled="!!project || pending" /></label
          ><label
            >{{ t("指定译员", "Assigned translator")
            }}<select
              v-model="form.translatorId"
              :aria-label="t('指定译员', 'Assigned translator')"
              required
              :disabled="pending"
            >
              <option value="" disabled>
                {{ t("选择译员", "Choose translator") }}
              </option>
              <option v-for="a in translators" :key="a.id" :value="a.id">
                {{ a.displayName }}
              </option>
            </select></label
          ><label
            >{{ t("独立审校", "Independent reviewer")
            }}<select
              v-model="form.reviewerId"
              :aria-label="t('独立审校', 'Independent reviewer')"
              required
              :disabled="pending"
            >
              <option value="" disabled>
                {{ t("选择审校", "Choose reviewer") }}
              </option>
              <option v-for="a in reviewers" :key="a.id" :value="a.id">
                {{ a.displayName }}
              </option>
            </select></label
          >
        </div>
        <label
          >{{ t("交付期限", "Due date")
          }}<input
            v-model="form.dueDate"
            type="date"
            required
            :disabled="pending"
        /></label>
        <p class="hint">
          {{
            t(
              "新实例请先在“登录账号”建立译员与审校账号。译员和审校必须不同。",
              "Create translator and reviewer accounts first in Accounts. They must be different people.",
            )
          }}
        </p>
        <footer>
          <button type="button" :disabled="pending" @click="emit('close')">
            {{ t("取消", "Cancel") }}</button
          ><button class="primary" :disabled="pending">
            {{
              pending ? t("保存中…", "Saving…") : t("保存项目", "Save project")
            }}
          </button>
        </footer>
      </form>
    </section>
  </div>
</template>
