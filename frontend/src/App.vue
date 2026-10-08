<!-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2 -->
<script setup>
import { ref, computed, onMounted } from "vue";
import {
  Languages,
  FolderOpen,
  PenLine,
  CheckCheck,
  ChartNoAxesCombined,
  Users,
  ShieldCheck,
  Settings,
  History,
  LogOut,
  Search,
  Plus,
  ArrowRight,
  Download,
  Menu,
  X,
  KeyRound,
  ExternalLink,
  RefreshCw,
} from "@lucide/vue";
import { api, resetApi, download } from "./api.js";
import {
  t,
  language,
  stateName,
  errorMessage,
  confirmation,
  answerConfirmation,
} from "./ui.js";
import Workbench from "./components/Workbench.vue";
import AdminPanel from "./components/AdminPanel.vue";
import ProjectDialog from "./components/ProjectDialog.vue";
const workbenchRef = ref(null);
const aboutModal = ref(false);
const profile = ref(null),
  booting = ref(true),
  pending = ref(false),
  login = ref({ username: "", password: "" }),
  loginError = ref(""),
  toast = ref(""),
  success = ref(""),
  section = ref("projects"),
  detail = ref(null),
  options = ref({ departments: [], users: [], platforms: [], warningDays: 3 }),
  projects = ref({ items: [], total: 0, page: 1, size: 20 }),
  projectQuery = ref(""),
  projectState = ref(""),
  projectSort = ref("newest"),
  projectPage = ref(1),
  reports = ref([]),
  audits = ref([]),
  auditQuery = ref(""),
  auditPage = ref(1),
  mobileNav = ref(false),
  newProject = ref(false),
  passwordModal = ref(false),
  password = ref({ oldPassword: "", newPassword: "" });
const has = (c) => profile.value?.permissions.includes(c);
const iconMap = {
  projects: FolderOpen,
  workbench: PenLine,
  review: CheckCheck,
  reports: ChartNoAxesCombined,
  users: Users,
  roles: ShieldCheck,
  settings: Settings,
  audit: History,
};
const title = computed(
  () =>
    ({
      projects: t("翻译项目", "Translation projects"),
      workbench: t("我的译稿", "My translations"),
      review: t("审校队列", "Review queue"),
      reports: t("进度报表", "Progress reports"),
      audit: t("操作记录", "Audit trail"),
    })[section.value] || "",
);
/** 请求失败保留明确结果，不把尚未确认的写入当成功。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
function fail(e) {
  toast.value = errorMessage(e);
  success.value = "";
  if (e.status === 401) {
    profile.value = null;
    detail.value = null;
    resetApi();
    loginError.value = toast.value;
  }
}
/** 按当前身份读取项目选项，不缓存跨身份目录。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function loadOptions() {
  options.value = await api("/options");
}
/** 服务端执行范围、筛选与分页，汇总来自同一范围。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function loadProjects() {
  const mine =
    section.value === "workbench"
      ? "translate"
      : section.value === "review"
        ? "review"
        : "";
  projects.value = await api(
    `/projects?q=${encodeURIComponent(projectQuery.value)}&state=${encodeURIComponent(projectState.value)}&sort=${projectSort.value}&page=${projectPage.value}&size=20&mine=${mine}`,
  );
}
/** 读取所选业务页面的真实持久化状态。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function loadSection() {
  if (!profile.value) return;
  try {
    if (["projects", "workbench", "review"].includes(section.value)) {
      await loadProjects();
      await loadOptions();
    } else if (section.value === "reports") {
      reports.value = await api("/reports");
      await loadOptions();
    } else if (section.value === "audit") audits.value = await api("/audit");
  } catch (e) {
    fail(e);
  }
}
/** 离开编辑台先检查未保存稿，避免静默丢失。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function navigate(code) {
  if (pending.value) return;
  if (workbenchRef.value && !(await workbenchRef.value.canLeave())) return;
  section.value = code;
  detail.value = null;
  mobileNav.value = false;
  projectPage.value = 1;
  await loadSection();
}
/** 显式刷新业务详情及实时权限，不自动覆盖写入。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function refresh() {
  try {
    if (detail.value) {
      detail.value = await api(`/projects/${detail.value.project.id}`);
      await loadOptions();
    } else await loadSection();
    profile.value = await api("/auth/me");
  } catch (e) {
    fail(e);
  }
}
/** 只读取后端允许访问的项目，并载入当前指派。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function openProject(id) {
  toast.value = "";
  try {
    detail.value = await api("/projects/" + id);
    await loadOptions();
  } catch (e) {
    fail(e);
  }
}
/** 写操作没有自动重放，成功后重新读取服务端状态。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function perform(path, method, body) {
  if (pending.value) return null;
  pending.value = true;
  toast.value = "";
  success.value = "";
  try {
    const result = await api(path, { method, body });
    if (result?.project && !path.endsWith("/preview")) detail.value = result;
    else if (
      detail.value &&
      path.startsWith(`/projects/${detail.value.project.id}/`) &&
      !path.endsWith("/preview")
    )
      detail.value = await api(`/projects/${detail.value.project.id}`);
    success.value = t("操作已完成", "Action completed");
    return result;
  } catch (e) {
    fail(e);
    return null;
  } finally {
    pending.value = false;
  }
}
/** 当前账号密码认证；成功后清除表单密码和旧CSRF。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function signIn() {
  if (pending.value) return;
  pending.value = true;
  loginError.value = "";
  try {
    resetApi();
    profile.value = await api("/auth/login", {
      method: "POST",
      body: login.value,
    });
    login.value.password = "";
    section.value = profile.value.menus[0]?.code || "projects";
    resetApi();
    await loadSection();
  } catch (e) {
    loginError.value = errorMessage(e);
  } finally {
    pending.value = false;
  }
}
/** 退出前处理未保存译稿，并失效当前会话。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function signOut() {
  if (workbenchRef.value && !(await workbenchRef.value.canLeave())) return;
  if (await perform("/auth/logout", "POST", {})) {
    profile.value = null;
    detail.value = null;
    success.value = "";
    resetApi();
  }
}
/** 调用旧密码校验接口；成功后须重新登录。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function changePassword() {
  if (await perform("/auth/password", "POST", password.value)) {
    passwordModal.value = false;
    password.value = { oldPassword: "", newPassword: "" };
    profile.value = null;
    resetApi();
  }
}
/** 查询变更从第一页重新读取。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function searchProjects() {
  projectPage.value = 1;
  await loadProjects().catch(fail);
}
/** 在当前筛选下翻页，不伪造客户端结果。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function pageProjects(n) {
  projectPage.value = n;
  await loadProjects().catch(fail);
}
/** 使用鉴权预检与浏览器原生下载。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function reportDownload() {
  try {
    await download("/reports.csv", "linguadesk-progress.csv");
  } catch (e) {
    fail(e);
  }
}
const visibleAudits = computed(() =>
  audits.value.filter((r) =>
    `${r.actor} ${r.action} ${r.objectId}`
      .toLowerCase()
      .includes(auditQuery.value.toLowerCase()),
  ),
);
const auditRows = computed(() =>
  visibleAudits.value.slice((auditPage.value - 1) * 20, auditPage.value * 20),
);
const reportCounts = computed(() =>
  reports.value.reduce(
    (a, r) => {
      a.total += r.counts.total;
      a.approved += r.counts.approved;
      a.blocked += r.counts.blocked;
      a.overdue += r.overdue ? 1 : 0;
      return a;
    },
    { total: 0, approved: 0, blocked: 0, overdue: 0 },
  ),
);
onMounted(async () => {
  try {
    profile.value = await api("/auth/me");
    section.value = profile.value.menus[0]?.code || "projects";
    await loadSection();
  } catch (e) {
    if (e.status !== 401) loginError.value = errorMessage(e);
  } finally {
    booting.value = false;
  }
});
</script>
<template>
  <div v-if="booting" class="boot">{{ t("正在连接…", "Connecting…") }}</div>
  <main v-else-if="!profile" class="login-page">
    <header class="login-top">
      <a href="https://www.zhuatech.cn/" target="_blank" rel="noopener"
        ><img src="/brand/logo.jpg" alt="知华科技" /><b>知华科技</b></a
      ><button
        class="text-button"
        @click="language = language === 'zh' ? 'en' : 'zh'"
      >
        {{ language === "zh" ? "English" : "中文" }}
      </button>
    </header>
    <section class="login-card">
      <div class="login-product">
        <Languages :size="23" /><span>LinguaDesk</span>
      </div>
      <h1>{{ t("登录翻译工作区", "Sign in to your workspace") }}</h1>
      <p class="muted">
        {{ t("软件多语言文案协作", "Software localization workspace") }}
      </p>
      <form @submit.prevent="signIn">
        <label
          >{{ t("账号", "Username")
          }}<input
            v-model="login.username"
            required
            autocomplete="username"
            maxlength="60"
            :disabled="pending"
            autofocus /></label
        ><label
          >{{ t("密码", "Password")
          }}<input
            v-model="login.password"
            required
            type="password"
            autocomplete="current-password"
            maxlength="128"
            :disabled="pending"
        /></label>
        <p v-if="loginError" role="alert" class="notice danger">
          {{ loginError }}
        </p>
        <button class="primary login-submit" :disabled="pending">
          {{ pending ? t("登录中…", "Signing in…") : t("登录", "Sign in") }}
        </button>
      </form>
      <p class="login-help">
        {{
          t(
            "账号由实例管理员分配，开通账号请联系实例管理员。",
            "Accounts are assigned by the instance administrator. Contact them to request access.",
          )
        }}
      </p>
    </section>
    <footer class="login-footer">
      {{
        t(
          "源码公开、非商业使用 · 商用须授权",
          "Public source for non-commercial use · Commercial use requires authorization",
        )
      }}<a href="https://www.zhuatech.cn/" target="_blank" rel="noopener">{{
        t("知华科技官网", "ZhiHua Technology")
      }}</a>
    </footer>
  </main>
  <div v-else class="application">
    <header class="topbar">
      <button
        class="icon-button nav-toggle"
        :aria-label="t('打开菜单', 'Open menu')"
        @click="mobileNav = !mobileNav"
      >
        <Menu :size="20" /></button
      ><a
        class="brand"
        href="https://www.zhuatech.cn/"
        target="_blank"
        rel="noopener"
        ><img src="/brand/logo.jpg" alt="知华科技" /><strong>LinguaDesk</strong
        ><span>{{ t("知华科技", "ZhiHua Technology") }}</span></a
      >
      <div class="topbar-user">
        <button
          class="text-button"
          @click="language = language === 'zh' ? 'en' : 'zh'"
        >
          {{ language === "zh" ? "English" : "中文" }}</button
        ><span class="avatar">{{ profile.displayName.slice(0, 1) }}</span
        ><span class="user-name"
          >{{ profile.displayName }}<small>{{ profile.role }}</small></span
        ><button
          class="icon-button"
          :disabled="pending"
          :title="t('修改密码', 'Change password')"
          :aria-label="t('修改密码', 'Change password')"
          @click="passwordModal = true"
        >
          <KeyRound :size="17" /></button
        ><button
          class="icon-button"
          :disabled="pending"
          :title="t('退出', 'Sign out')"
          :aria-label="t('退出', 'Sign out')"
          @click="signOut"
        >
          <LogOut :size="18" />
        </button>
      </div>
    </header>
    <div
      v-if="mobileNav"
      class="sidebar-shade"
      @click="mobileNav = false"
    ></div>
    <aside class="sidebar" :class="{ open: mobileNav }">
      <div class="nav-label">{{ t("工作区", "WORKSPACE") }}</div>
      <nav>
        <button
          v-for="m in profile.menus"
          :key="m.id"
          :class="{ active: section === m.code }"
          :disabled="pending"
          @click="navigate(m.code)"
        >
          <component :is="iconMap[m.code] || FolderOpen" :size="18" /><span>{{
            language === "zh" ? m.name : m.nameEn
          }}</span>
        </button>
      </nav>
      <footer>
        <a href="https://www.zhuatech.cn/" target="_blank" rel="noopener"
          >{{ t("商业授权与定制", "Licensing & customization") }}
          <ExternalLink :size="12" /></a
        ><small>{{ t("微信 zhuatech / zhuatech2", "han@zhuatech.cn") }}</small
        ><small>{{
          t("非商业源码学习版", "Non-commercial source edition")
        }}</small
        ><button class="text-button about-button" @click="aboutModal = true">
          {{ t("关于系统", "About") }}
        </button>
      </footer>
    </aside>
    <main class="workspace">
      <div
        v-if="toast || success"
        class="status-banner"
        :class="toast ? 'error' : 'success'"
        role="status"
      >
        <span>{{ toast || success }}</span
        ><button
          class="icon-button"
          :aria-label="t('关闭消息', 'Dismiss message')"
          @click="
            toast = '';
            success = '';
          "
        >
          <X :size="16" />
        </button>
      </div>
      <Workbench
        ref="workbenchRef"
        v-if="detail"
        :detail="detail"
        :profile="profile"
        :options="options"
        :pending="pending"
        :perform="perform"
        @back="
          detail = null;
          loadSection();
        "
        @refresh="refresh"
        @error="fail"
      />
      <template
        v-else-if="['projects', 'workbench', 'review'].includes(section)"
        ><header class="page-heading">
          <div>
            <h1>{{ title }}</h1>
            <p class="muted">
              {{
                section === "projects"
                  ? t(
                      "原文、译文、审校与交付版本。",
                      "Source strings, translations, reviews and deliveries.",
                    )
                  : section === "workbench"
                    ? t(
                        "仅显示指定你为译员的项目。",
                        "Projects assigned to you as translator.",
                      )
                    : t(
                        "仅显示指定你为审校的项目。",
                        "Projects assigned to you as reviewer.",
                      )
              }}
            </p>
          </div>
          <div class="actions">
            <button :disabled="pending" @click="refresh">
              <RefreshCw :size="16" />{{ t("刷新", "Refresh") }}</button
            ><button
              v-if="has('manage')"
              class="primary"
              :disabled="pending"
              @click="newProject = true"
            >
              <Plus :size="16" />{{ t("新建项目", "New project") }}
            </button>
          </div>
        </header>
        <div v-if="projects.summary" class="report-summary overview">
          <div>
            <small>{{ t("匹配项目", "Matching projects") }}</small
            ><strong>{{ projects.total }}</strong>
          </div>
          <div>
            <small>{{ t("文案总量", "Total strings") }}</small
            ><strong>{{ projects.summary.total }}</strong>
          </div>
          <div>
            <small>{{ t("已通过审校", "Approved strings") }}</small
            ><strong>{{ projects.summary.approved }}</strong>
          </div>
          <div>
            <small>{{ t("存在规则阻断", "Strings with blockers") }}</small
            ><strong>{{ projects.summary.blocked }}</strong>
          </div>
        </div>
        <div class="panel">
          <form class="filter-bar" @submit.prevent="searchProjects">
            <label class="search"
              ><Search :size="17" /><input
                v-model="projectQuery"
                :placeholder="t('搜索项目名称', 'Search project name')"
                :aria-label="t('搜索项目', 'Search projects')" /></label
            ><select
              v-model="projectState"
              :aria-label="t('项目状态', 'Project state')"
              @change="searchProjects"
            >
              <option value="">{{ t("全部状态", "All states") }}</option>
              <option
                v-for="s in [
                  'DRAFT',
                  'ACTIVE',
                  'PAUSED',
                  'CLOSED',
                  'CANCELLED',
                ]"
                :key="s"
                :value="s"
              >
                {{ stateName(s) }}
              </option></select
            ><select
              v-model="projectSort"
              :aria-label="t('项目排序', 'Project sort')"
              @change="searchProjects"
            >
              <option value="newest">
                {{ t("最新创建", "Newest first") }}
              </option>
              <option value="due">
                {{ t("期限顺序", "Due date") }}
              </option></select
            ><button>{{ t("搜索", "Search") }}</button
            ><span class="muted filter-count"
              >{{ projects.total }} {{ t("个项目", "projects") }}</span
            >
          </form>
          <div class="table-wrap">
            <table class="project-table">
              <thead>
                <tr>
                  <th>{{ t("项目", "Project") }}</th>
                  <th>{{ t("语言对", "Locales") }}</th>
                  <th>{{ t("状态", "State") }}</th>
                  <th>{{ t("审校进度", "Review progress") }}</th>
                  <th>{{ t("期限", "Due") }}</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="r in projects.items" :key="r.project.id">
                  <td>
                    <button
                      class="project-name"
                      @click="openProject(r.project.id)"
                    >
                      {{ r.project.name }}</button
                    ><small
                      >{{ r.counts.total }} {{ t("条文案", "strings") }}</small
                    >
                  </td>
                  <td class="mono locale-cell">
                    {{ r.project.sourceLocale }} → {{ r.project.targetLocale }}
                  </td>
                  <td>
                    <span
                      class="badge"
                      :class="r.project.state.toLowerCase()"
                      >{{ stateName(r.project.state) }}</span
                    >
                  </td>
                  <td>
                    <div class="progress-cell">
                      <span
                        >{{ r.counts.approved }} / {{ r.counts.total }}</span
                      >
                      <div class="progress">
                        <i
                          :style="{
                            width:
                              (r.counts.total
                                ? (100 * r.counts.approved) / r.counts.total
                                : 0) + '%',
                          }"
                        ></i>
                      </div>
                    </div>
                  </td>
                  <td>
                    {{ r.project.dueDate
                    }}<small v-if="r.dueSoon" class="warning-text">{{
                      t("即将到期", "Due soon")
                    }}</small>
                  </td>
                  <td>
                    <button
                      class="icon-button"
                      :aria-label="
                        t('打开项目', 'Open project') + ' ' + r.project.name
                      "
                      @click="openProject(r.project.id)"
                    >
                      <ArrowRight :size="17" />
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-if="!projects.items.length" class="empty">
              <FolderOpen :size="28" />
              <h3>{{ t("暂无项目", "No projects yet") }}</h3>
              <p>
                {{
                  has("manage")
                    ? t(
                        "先建立译员和审校账号，再新建项目并导入原文。",
                        "Create translator and reviewer accounts, then add a project and import source strings.",
                      )
                    : t(
                        "项目经理指派后，项目会显示在这里。",
                        "Projects will appear when a manager assigns them to you.",
                      )
                }}
              </p>
            </div>
          </div>
          <footer class="pagination">
            <span
              >{{ projectPage }} /
              {{ Math.max(1, Math.ceil(projects.total / 20)) }}</span
            ><button
              :disabled="projectPage <= 1"
              @click="pageProjects(projectPage - 1)"
            >
              {{ t("上一页", "Previous") }}</button
            ><button
              :disabled="projectPage * 20 >= projects.total"
              @click="pageProjects(projectPage + 1)"
            >
              {{ t("下一页", "Next") }}
            </button>
          </footer>
        </div></template
      >
      <AdminPanel
        v-else-if="['users', 'roles', 'settings'].includes(section)"
        :section="section"
        :pending="pending"
        :perform="perform"
        @error="fail"
      />
      <section v-else-if="section === 'reports'">
        <header class="page-heading">
          <div>
            <h1>{{ title }}</h1>
            <p class="muted">
              {{
                t(
                  "依据当前可见项目汇总，规则核查不是语义质量评分。",
                  "Current visible projects; rule checks are not semantic quality scores.",
                )
              }}
            </p>
          </div>
          <button @click="reportDownload">
            <Download :size="16" />{{ t("导出 CSV", "Export CSV") }}
          </button>
        </header>
        <div class="report-summary">
          <div>
            <small>{{ t("可见项目", "Visible projects") }}</small
            ><strong>{{ reports.length }}</strong>
          </div>
          <div>
            <small>{{
              t("通过审校 / 总文案", "Approved / total strings")
            }}</small
            ><strong
              >{{ reportCounts.approved }}
              <span>/ {{ reportCounts.total }}</span></strong
            >
          </div>
          <div>
            <small>{{ t("存在规则阻断", "Strings with blockers") }}</small
            ><strong>{{ reportCounts.blocked }}</strong>
          </div>
          <div>
            <small>{{ t("已逾期项目", "Overdue projects") }}</small
            ><strong>{{ reportCounts.overdue }}</strong>
          </div>
        </div>
        <div class="panel table-wrap">
          <table>
            <thead>
              <tr>
                <th>{{ t("项目", "Project") }}</th>
                <th>{{ t("状态", "State") }}</th>
                <th>{{ t("文案", "Strings") }}</th>
                <th>{{ t("翻译", "Translated") }}</th>
                <th>{{ t("审校", "Approved") }}</th>
                <th>{{ t("阻断 / 提示", "Blockers / warnings") }}</th>
                <th>{{ t("期限", "Due") }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in reports" :key="r.project.id">
                <td>
                  <button
                    class="project-name"
                    @click="openProject(r.project.id)"
                  >
                    {{ r.project.name }}</button
                  ><small
                    >{{ r.project.sourceLocale }} →
                    {{ r.project.targetLocale }}</small
                  >
                </td>
                <td>{{ stateName(r.project.state) }}</td>
                <td>{{ r.counts.total }}</td>
                <td>{{ r.counts.translated }}</td>
                <td>{{ r.counts.approved }}</td>
                <td>{{ r.counts.blocked }} / {{ r.counts.warnings }}</td>
                <td :class="{ 'warning-text': r.overdue }">
                  {{ r.project.dueDate
                  }}<small v-if="r.overdue">{{ t("已逾期", "Overdue") }}</small>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-if="!reports.length" class="empty">
            {{ t("暂无项目数据", "No project data") }}
          </div>
        </div>
      </section>
      <section v-else-if="section === 'audit'">
        <header class="page-heading">
          <div>
            <h1>{{ title }}</h1>
            <p class="muted">
              {{
                t(
                  "只读的账号、业务与系统操作审计。",
                  "Read-only audit of account, business and system operations.",
                )
              }}
            </p>
          </div>
          <button @click="loadSection">{{ t("刷新", "Refresh") }}</button>
        </header>
        <div class="panel">
          <div class="section-toolbar">
            <label class="search"
              ><Search :size="16" /><input
                v-model="auditQuery"
                :placeholder="
                  t('搜索操作人、动作或对象', 'Search actor, action or object')
                "
                :aria-label="t('搜索审计', 'Search audit')"
                @input="auditPage = 1" /></label
            ><span>{{ visibleAudits.length }} {{ t("条", "events") }}</span>
          </div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>{{ t("时间", "Time") }}</th>
                  <th>{{ t("操作人", "Actor") }}</th>
                  <th>{{ t("动作", "Action") }}</th>
                  <th>{{ t("对象", "Object") }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="r in auditRows" :key="r.id">
                  <td>{{ new Date(r.createdAt).toLocaleString() }}</td>
                  <td>{{ r.actor }}</td>
                  <td>{{ r.action }}</td>
                  <td>{{ r.objectId }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <footer class="pagination">
            <span
              >{{ auditPage }} /
              {{ Math.max(1, Math.ceil(visibleAudits.length / 20)) }}</span
            ><button :disabled="auditPage <= 1" @click="auditPage--">
              {{ t("上一页", "Previous") }}</button
            ><button
              :disabled="auditPage * 20 >= visibleAudits.length"
              @click="auditPage++"
            >
              {{ t("下一页", "Next") }}
            </button>
          </footer>
        </div>
      </section>
      <section v-else class="empty">
        {{
          t(
            "此菜单暂不可用，请刷新账号权限。",
            "Menu unavailable. Refresh your account permissions.",
          )
        }}
      </section>
    </main>
    <ProjectDialog
      v-if="newProject"
      :options="options"
      :pending="pending"
      :perform="perform"
      @close="newProject = false"
    />
    <div v-if="confirmation" class="modal-backdrop confirmation-backdrop">
      <section
        class="modal"
        role="alertdialog"
        aria-modal="true"
        :aria-label="t('确认操作', 'Confirm action')"
      >
        <header>
          <h2>{{ t("确认操作", "Confirm action") }}</h2>
        </header>
        <div class="modal-body">
          <p>{{ confirmation.message }}</p>
        </div>
        <footer>
          <button @click="answerConfirmation(false)">
            {{ t("保留并返回", "Keep and return") }}</button
          ><button class="primary" @click="answerConfirmation(true)">
            {{ t("确认继续", "Continue") }}
          </button>
        </footer>
      </section>
    </div>
    <div v-if="aboutModal" class="modal-backdrop">
      <section
        class="modal"
        role="dialog"
        aria-modal="true"
        :aria-label="t('关于系统', 'About')"
      >
        <header>
          <h2>LinguaDesk</h2>
          <button
            class="icon-button"
            :aria-label="t('关闭', 'Close')"
            @click="aboutModal = false"
          >
            <X :size="20" />
          </button>
        </header>
        <div class="modal-body">
          <div class="about-brand">
            <img src="/brand/logo.jpg" alt="知华科技" /><strong>{{
              t(
                "知华科技（上海如静知华信息科技有限公司）",
                "ZhiHua Technology (Shanghai Rujing Zhihua Information Technology Co., Ltd.)",
              )
            }}</strong>
          </div>
          <p class="hint">
            {{
              t(
                "公开源码学习版。仅限个人学习、研究与非商业交流，未经公司书面授权不得商用。",
                "Public source edition for personal learning, research and non-commercial exchange. Commercial use requires written authorization.",
              )
            }}
          </p>
          <p class="mt">
            <a href="https://www.zhuatech.cn/" target="_blank" rel="noopener"
              >https://www.zhuatech.cn/</a
            >
          </p>
          <template v-if="language === 'zh'"
            ><p class="mt">商业授权或深度定制开发请联系知华科技。</p>
            <div class="contact-images">
              <figure>
                <img
                  src="/brand/wechat-zhuatech.png"
                  alt="知华科技微信 zhuatech"
                />
                <figcaption>zhuatech</figcaption>
              </figure>
              <figure>
                <img
                  src="/brand/wechat-zhuatech2.png"
                  alt="知华科技微信 zhuatech2"
                />
                <figcaption>zhuatech2</figcaption>
              </figure>
            </div></template
          >
          <div v-else class="about-links">
            <a href="mailto:han@zhuatech.cn">han@zhuatech.cn</a
            ><a href="mailto:jack@zhuatech.cn">jack@zhuatech.cn</a
            ><a
              href="https://wa.me/8617521234993"
              target="_blank"
              rel="noopener"
              >WhatsApp +86 17521234993</a
            >
          </div>
        </div>
      </section>
    </div>
    <div v-if="passwordModal" class="modal-backdrop">
      <section
        class="modal"
        role="dialog"
        aria-modal="true"
        :aria-label="t('修改密码', 'Change password')"
      >
        <header>
          <h2>{{ t("修改密码", "Change password") }}</h2>
        </header>
        <form @submit.prevent="changePassword">
          <label
            >{{ t("原密码", "Current password")
            }}<input
              v-model="password.oldPassword"
              type="password"
              required
              autocomplete="current-password"
              :disabled="pending" /></label
          ><label
            >{{ t("新密码", "New password")
            }}<input
              v-model="password.newPassword"
              type="password"
              required
              autocomplete="new-password"
              :disabled="pending"
          /></label>
          <p class="hint">
            {{
              t(
                "12–72 字节，包含大写、小写字母和数字。修改后需重新登录。",
                "12–72 bytes with uppercase, lowercase and digits. Sign in again after changing.",
              )
            }}
          </p>
          <footer>
            <button
              type="button"
              :disabled="pending"
              @click="passwordModal = false"
            >
              {{ t("取消", "Cancel") }}</button
            ><button class="primary" :disabled="pending">
              {{ t("保存新密码", "Save new password") }}
            </button>
          </footer>
        </form>
      </section>
    </div>
  </div>
</template>
