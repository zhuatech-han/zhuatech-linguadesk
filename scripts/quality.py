#!/usr/bin/env python3
# Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
"""仅在全新本机测试实例建立TEST数据并验证真实流程。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。"""
import argparse, hashlib, json, os, secrets, time
from pathlib import Path
from urllib.parse import urlparse
import requests

ROOT = Path(__file__).resolve().parents[1]
COUNT = 0


def check(ok, label):
    global COUNT
    if not ok:
        raise AssertionError(label)
    COUNT += 1


class Session:
    """真实同源HTTP会话，读取CSRF并禁止自动重放写请求。知华科技 https://www.zhuatech.cn/。"""

    def __init__(self, base):
        self.base = base
        self.session = requests.Session()

    def call(self, method, path, body=None, status=200, code=None, raw=False):
        headers = {}
        if method not in ["GET", "HEAD"]:
            csrf = self.session.get(self.base + "/api/auth/csrf", timeout=20)
            check(csrf.status_code == 200, "CSRF bootstrap")
            data = csrf.json()
            headers[data["header"]] = data["token"]
        response = self.session.request(
            method, self.base + "/api" + path, json=body, headers=headers, timeout=30
        )
        check(
            response.status_code == status,
            f"{method} {path}: expected {status}, got {response.status_code}; {response.text[:160] if status != 200 else ''}",
        )
        if code:
            check(response.json().get("code") == code, "expected business error")
        return response if raw else response.json()

    def login(self, username, password):
        return self.call(
            "POST", "/auth/login", {"username": username, "password": password}
        )


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", default="http://127.0.0.1:8126")
    parser.add_argument("--env-file", default=str(ROOT / ".env"))
    parser.add_argument("--state", default=str(ROOT / "output/quality-state.json"))
    args = parser.parse_args()
    url = urlparse(args.base)
    if url.hostname not in ["127.0.0.1", "localhost", "::1"] or url.scheme not in [
        "http",
        "https",
    ]:
        parser.error("QA accepts only a disposable localhost instance")
    env = dict(
        line.split("=", 1)
        for line in Path(args.env_file).read_text().splitlines()
        if "=" in line and not line.startswith("#")
    )
    admin = Session(args.base)
    health = requests.get(args.base + "/actuator/health", timeout=20)
    check(health.status_code == 200 and health.json()["status"] == "UP", "health")
    admin.call("GET", "/projects", status=401, code="UNAUTHENTICATED")
    denial = requests.post(args.base + "/api/projects", json={}, timeout=20)
    check(denial.status_code == 403, "CSRF denied")
    profile = admin.login(env["ADMIN_USERNAME"], env["ADMIN_PASSWORD"])
    check(profile["scope"] == "ALL", "bootstrap scope")
    check(
        admin.call("GET", "/projects")["total"] == 0,
        "Refuse QA unless the database is empty",
    )
    users = admin.call("GET", "/admin/users")
    check(
        len(users) == 1 and "passwordHash" not in users[0],
        "no shared accounts or hashes",
    )
    original = users[0]
    admin.call(
        "PUT",
        f"/admin/users/{original['id']}",
        {**original, "enabled": False, "password": ""},
        409,
        "LAST_ADMIN",
    )
    role = admin.call("GET", "/admin/roles")[0]
    admin.call(
        "PUT",
        "/admin/roles/1",
        {**role, "permissions": [p for p in role["permissions"] if p != "projects"]},
        409,
        "LAST_ADMIN",
    )
    department = admin.call("GET", "/admin/departments")[0]
    admin.call(
        "PUT",
        "/admin/departments/1",
        {**department, "enabled": False},
        409,
        "LAST_ADMIN",
    )
    remote = admin.call(
        "POST",
        "/admin/departments",
        {"name": "TEST 国际产品组", "zone": "Europe/Berlin", "enabled": True},
    )
    passwords, accounts = {}, {}
    for username, name, role_id, department_id in [
        ("test-manager", "TEST 项目经理", 2, 1),
        ("test-translator", "TEST 译员", 3, 1),
        ("test-reviewer", "TEST 审校", 4, 1),
        ("test-outsider", "TEST 未指派译员", 3, 1),
        ("test-remote", "TEST 其他团队经理", 2, remote["id"]),
        ("test-reader", "TEST 只读成员", 5, 1),
    ]:
        password = "Aa9" + secrets.token_hex(16)
        passwords[username] = password
        accounts[username] = admin.call(
            "POST",
            "/admin/users",
            {
                "username": username,
                "displayName": name,
                "roleId": role_id,
                "departmentId": department_id,
                "enabled": True,
                "password": password,
            },
        )
    sessions = {name: Session(args.base) for name in accounts}
    for name, session in sessions.items():
        session.login(name, passwords[name])
    manager, translator, reviewer, outsider, remote_session = [
        sessions[x]
        for x in [
            "test-manager",
            "test-translator",
            "test-reviewer",
            "test-outsider",
            "test-remote",
        ]
    ]
    manager.call("GET", "/admin/users", status=403, code="FORBIDDEN")
    translator.call("GET", "/reports", status=403, code="FORBIDDEN")
    base = {
        "departmentId": 1,
        "sourceLocale": "en",
        "targetLocale": "zh-CN",
        "platform": "APP",
        "translatorId": accounts["test-translator"]["id"],
        "reviewerId": accounts["test-reviewer"]["id"],
        "dueDate": "2026-12-31",
    }
    manager.call(
        "POST",
        "/projects",
        {**base, "name": "TEST invalid", "reviewerId": base["translatorId"]},
        409,
        "ASSIGNEE_INVALID",
    )
    detail = manager.call(
        "POST", "/projects", {**base, "name": "TEST 移动应用 · 首发交付验证"}
    )
    pid = detail["project"]["id"]
    path = f"/projects/{pid}"
    manager.call(
        "POST",
        path + "/import",
        {
            "version": detail["project"]["version"],
            "replace": False,
            "content": '{"same":"A","same":"B"}',
        },
        400,
        "INVALID_JSON",
    )
    manager.call(
        "POST",
        path + "/import",
        {
            "version": detail["project"]["version"],
            "replace": False,
            "rows": [
                {"key": "valid", "source": "Hello"},
                {"key": "invalid key", "source": "No"},
            ],
        },
        400,
        "INVALID_KEY",
    )
    check(not manager.call("GET", path)["segments"], "bad imports are atomic")
    values = [
        ("account.welcome", "Welcome {name}", "欢迎 {name}", "登录后的欢迎语", 30),
        ("account.title", "Account settings", "账户设置", "设置页标题", 15),
        ("cart.items", "{{count}} items", "{{count}} 件商品", "保留双花括号变量", 30),
        ("billing.total", "Total: %1$s", "合计：%1$s", "保留金额占位符", 30),
        ("release.note", "Version 2", "版本 2", "更新版本号", 20),
        ("button.save", "Save", "保存", "操作按钮", 4),
        (
            "notice.line",
            " First line\nSecond line ",
            " 第一行\n第二行 ",
            "保留空白及换行",
            0,
        ),
        ("greeting.symbol", "Hi 👋", "你好 👋", "Unicode 文案", 8),
    ]
    rows = [
        {"key": k, "source": s, "context": c, "maxLength": n}
        for k, s, target, c, n in values
    ]
    preview = manager.call(
        "POST", path + "/import/preview", {"replace": False, "rows": rows}
    )
    check(
        preview["total"] == 8 and not manager.call("GET", path)["segments"],
        "preview changes nothing",
    )
    detail = manager.call(
        "POST",
        path + "/import",
        {"version": detail["project"]["version"], "replace": False, "rows": rows},
    )
    detail = manager.call(
        "POST",
        path + "/terms",
        {
            "version": detail["project"]["version"],
            "source": "Account",
            "target": "账户",
            "note": "TEST approved wording",
        },
    )
    detail = manager.call(
        "POST", path + "/actions/start", {"version": detail["project"]["version"]}
    )
    outsider.call("GET", path, status=403, code="OUT_OF_SCOPE")
    remote_session.call("GET", path, status=403, code="OUT_OF_SCOPE")
    translator.call("POST", path + "/import", {}, 403, "FORBIDDEN")
    manager.call(
        "POST",
        path + "/terms",
        {
            "version": detail["project"]["version"],
            "source": "Save",
            "target": "保存",
            "note": "",
        },
        409,
        "GLOSSARY_FROZEN",
    )
    first = detail["segments"][0]
    changed = translator.call(
        "PUT",
        path + f"/segments/{first['id']}",
        {"version": first["version"], "target": "欢迎", "submit": True},
    )
    reviewer.call(
        "POST",
        path + f"/segments/{first['id']}/review",
        {
            "version": changed["segment"]["version"],
            "approve": True,
            "note": "TEST cannot waive",
        },
        409,
        "QA_BLOCKED",
    )
    translator.call(
        "PUT",
        path + f"/segments/{first['id']}",
        {"version": first["version"], "target": "stale", "submit": False},
        409,
        "VERSION_CONFLICT",
    )
    detail = manager.call("GET", path)
    for segment, value in zip(detail["segments"], values):
        result = translator.call(
            "PUT",
            path + f"/segments/{segment['id']}",
            {"version": segment["version"], "target": value[2], "submit": True},
        )
        review = reviewer.call(
            "POST",
            path + f"/segments/{segment['id']}/review",
            {"version": result["segment"]["version"], "approve": True, "note": ""},
        )
        check(review["segment"]["status"] == "APPROVED", "approved exact target")
    detail = manager.call("GET", path)
    release = manager.call(
        "POST",
        path + "/deliveries",
        {"version": detail["project"]["version"], "name": "TEST v1.0 首发文案"},
    )
    delivery = release["deliveries"][0]
    payload = manager.call(
        "GET", path + f"/deliveries/{delivery['id']}/json", raw=True
    ).content
    check(hashlib.sha256(payload).hexdigest() == delivery["sha256"], "release SHA")
    check(
        json.loads(payload) == {x[0]: x[2] for x in values},
        "complete exact JSON payload",
    )
    remote_session.call(
        "GET",
        path + f"/deliveries/{delivery['id']}/json",
        status=403,
        code="OUT_OF_SCOPE",
    )
    first = release["segments"][0]
    updated = manager.call(
        "POST",
        path + "/import",
        {
            "version": release["project"]["version"],
            "replace": False,
            "rows": [
                {
                    "key": first["key"],
                    "source": "Welcome back {name}",
                    "context": first["context"],
                    "maxLength": first["maxLength"],
                }
            ],
        },
    )
    check(
        updated["segments"][0]["status"] == "DRAFT"
        and updated["segments"][0]["reviewedBy"] is None,
        "source update invalidates exact approval",
    )
    check(
        manager.call(
            "GET", path + f"/deliveries/{delivery['id']}/json", raw=True
        ).content
        == payload,
        "old snapshot immutable after source edit",
    )
    manager.call(
        "POST",
        path + "/actions/close",
        {"version": updated["project"]["version"]},
        409,
        "UNRELEASED_CHANGES",
    )
    segment = updated["segments"][0]
    result = translator.call(
        "PUT",
        path + f"/segments/{segment['id']}",
        {"version": segment["version"], "target": "欢迎回来 {name}", "submit": True},
    )
    reviewer.call(
        "POST",
        path + f"/segments/{segment['id']}/review",
        {
            "version": result["segment"]["version"],
            "approve": False,
            "note": "TEST use a formal greeting",
        },
    )
    segment = manager.call("GET", path)["segments"][0]
    result = translator.call(
        "PUT",
        path + f"/segments/{segment['id']}",
        {
            "version": segment["version"],
            "target": "欢迎再次访问 {name}",
            "submit": True,
        },
    )
    reviewer.call(
        "POST",
        path + f"/segments/{segment['id']}/review",
        {"version": result["segment"]["version"], "approve": True, "note": ""},
    )
    detail = manager.call("GET", path)
    detail = manager.call(
        "POST",
        path + "/deliveries",
        {"version": detail["project"]["version"], "name": "TEST v1.1 更新欢迎语"},
    )
    check(len(detail["deliveries"]) == 2, "two real immutable releases")
    detail = manager.call(
        "POST", path + "/actions/close", {"version": detail["project"]["version"]}
    )
    check(detail["project"]["state"] == "CLOSED", "completed state")
    # Create a real GUI work project; no fabricated seed in the application itself.
    gui = manager.call(
        "POST", "/projects", {**base, "name": "TEST 出海产品 · 多语言设置"}
    )
    gui_path = f"/projects/{gui['project']['id']}"
    gui_rows = rows + [
        {
            "key": "button.cancel",
            "source": "Cancel",
            "context": "取消按钮",
            "maxLength": 5,
        },
        {
            "key": "account.logout",
            "source": "Sign out",
            "context": "账号菜单",
            "maxLength": 8,
        },
        {
            "key": "file.upload",
            "source": "Upload file",
            "context": "文件上传按钮",
            "maxLength": 12,
        },
        {
            "key": "notify.email",
            "source": "Email notifications",
            "context": "通知偏好",
            "maxLength": 20,
        },
    ]
    gui = manager.call(
        "POST",
        gui_path + "/import",
        {"version": gui["project"]["version"], "replace": False, "rows": gui_rows},
    )
    gui = manager.call(
        "POST",
        gui_path + "/terms",
        {
            "version": gui["project"]["version"],
            "source": "Account",
            "target": "账户",
            "note": "TEST terminology",
        },
    )
    gui = manager.call(
        "POST", gui_path + "/actions/start", {"version": gui["project"]["version"]}
    )
    for segment, value in zip(gui["segments"][1:6], values[1:6]):
        r = translator.call(
            "PUT",
            gui_path + f"/segments/{segment['id']}",
            {"version": segment["version"], "target": value[2], "submit": True},
        )
        if segment["key"] != "cart.items":
            reviewer.call(
                "POST",
                gui_path + f"/segments/{segment['id']}/review",
                {"version": r["segment"]["version"], "approve": True, "note": ""},
            )
    # Manager pause/resume is real and locks translator requests.
    gui = manager.call("GET", gui_path)
    paused = manager.call(
        "POST", gui_path + "/actions/pause", {"version": gui["project"]["version"]}
    )
    segment = paused["segments"][0]
    translator.call(
        "PUT",
        gui_path + f"/segments/{segment['id']}",
        {"version": segment["version"], "target": "TEST", "submit": False},
        409,
        "PROJECT_LOCKED",
    )
    gui = manager.call(
        "POST", gui_path + "/actions/resume", {"version": paused["project"]["version"]}
    )
    check(
        translator.call("GET", "/projects?mine=translate")["total"] == 2,
        "server-side assigned filtering",
    )
    check(outsider.call("GET", "/projects")["total"] == 0, "unassigned projects hidden")
    check(
        manager.call("GET", "/projects?q=首发&size=1&page=1")["total"] == 1,
        "search pagination",
    )
    # Revoke an existing test session and reset that disposable test account through the API.
    user = accounts["test-outsider"]
    admin.call(
        "PUT", f"/admin/users/{user['id']}", {**user, "enabled": False, "password": ""}
    )
    outsider.call("GET", "/auth/me", status=401, code="UNAUTHENTICATED")
    passwords["test-outsider"] = "Aa9" + secrets.token_hex(16)
    admin.call(
        "PUT",
        f"/admin/users/{user['id']}",
        {
            **user,
            "version": user["version"] + 1,
            "enabled": True,
            "password": passwords["test-outsider"],
        },
    )
    outsider.call("GET", "/auth/me", status=401, code="UNAUTHENTICATED")
    check(
        admin.call("GET", "/reports.csv", raw=True).content.startswith(b"project,"),
        "actual scoped CSV",
    )
    check(len(manager.call("GET", "/audit")) > 20, "real audit")
    state = {
        "projectIds": [pid, gui["project"]["id"]],
        "guiProjectId": gui["project"]["id"],
        "accounts": accounts,
        "passwords": passwords,
        "checks": COUNT,
        "snapshots": {},
    }
    for project_id in state["projectIds"]:
        state["snapshots"][str(project_id)] = admin.call(
            "GET", f"/projects/{project_id}"
        )
    state["auditSnapshot"] = admin.call("GET", "/audit")
    state["checks"] = COUNT
    target = Path(args.state)
    target.parent.mkdir(parents=True, exist_ok=True)
    fd = os.open(target, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(fd, "w") as stream:
        json.dump(state, stream, ensure_ascii=False, indent=2)
    print(
        f"PASS: {COUNT} real MySQL/HTTP assertions; private QA state saved (no credentials printed)."
    )


if __name__ == "__main__":
    main()
