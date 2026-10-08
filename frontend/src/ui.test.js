// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
import test from "node:test";
import { reactive } from "vue";
import assert from "node:assert/strict";
import {
  filterSegments,
  readSourceFile,
  language,
  t,
  stateName,
  errorMessage,
  qaName,
  ask,
  confirmation,
  answerConfirmation,
  cloneRecord,
} from "./ui.js";
test("filters source, target and context without mutating translations", () => {
  const rows = [
    {
      key: "save",
      source: "Save",
      target: "保存",
      context: "toolbar",
      status: "REVIEW",
    },
    {
      key: "cancel",
      source: "Cancel",
      target: "取消",
      context: "dialog",
      status: "DRAFT",
    },
  ];
  assert.equal(filterSegments(rows, "TOOLBAR", "REVIEW")[0].key, "save");
  assert.equal(filterSegments(rows, "保存", "").length, 1);
  assert.equal(filterSegments(rows, "", "APPROVED").length, 0);
  assert.equal(rows[0].target, "保存");
});
test("source file retains whitespace and Unicode", async () => {
  const bytes = new TextEncoder().encode('{"hello":" 你好\\n "}');
  const content = await readSourceFile({
    size: bytes.length,
    arrayBuffer: async () => bytes.buffer,
  });
  assert.equal(JSON.parse(content).hello, " 你好\n ");
});
test("oversized file rejected before reading", async () => {
  let read = false;
  await assert.rejects(
    readSourceFile({
      size: 512001,
      arrayBuffer: () => {
        read = true;
      },
    }),
    /FILE_TOO_LARGE/,
  );
  assert.equal(read, false);
});
test("invalid UTF8 is rejected instead of silently replacing text", async () => {
  await assert.rejects(
    readSourceFile({
      size: 2,
      arrayBuffer: async () => new Uint8Array([0xc3, 0x28]).buffer,
    }),
    /INVALID_TEXT/,
  );
});
test("language switch translates statuses and errors without altering codes", () => {
  language.value = "en";
  assert.equal(t("中文", "English"), "English");
  assert.equal(stateName("APPROVED"), "Approved");
  assert.match(errorMessage(new Error("RESULT_UNKNOWN")), /result unknown/);
  assert.match(qaName("PLACEHOLDERS"), /Placeholder/);
  language.value = "zh";
  assert.equal(stateName("APPROVED"), "已通过");
});
test("unknown error remains actionable and includes diagnostic code", () => {
  assert.match(errorMessage(new Error("UNEXPECTED_CODE")), /UNEXPECTED_CODE/);
});

test("discard decision remains pending until explicit response and cancellation preserves caller intent", async () => {
  let chosen = null;
  const pending = ask("TEST discard?").then((v) => (chosen = v));
  await Promise.resolve();
  assert.equal(chosen, null);
  assert.equal(confirmation.value.message, "TEST discard?");
  answerConfirmation(false);
  await pending;
  assert.equal(chosen, false);
  assert.equal(confirmation.value, null);
});

test("Vue-proxied API records open an independent editable form", () => {
  const row = reactive({
    id: 7,
    name: "TEST reader",
    permissions: ["projects"],
  });
  const copy = cloneRecord(row);
  assert.equal(copy.name, row.name);
  copy.name = "TEST edit";
  copy.permissions.push("reports");
  assert.equal(row.name, "TEST reader");
  assert.deepEqual([...row.permissions], ["projects"]);
});
