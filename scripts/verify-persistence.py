#!/usr/bin/env python3
# Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
"""核对重启或恢复后的真实业务证据和交付SHA。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。"""
import argparse, hashlib, json, os
from pathlib import Path
from urllib.parse import urlparse
from quality import ROOT, Session, check


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--base", required=True)
    p.add_argument("--state", default=str(ROOT / "output/quality-state.json"))
    p.add_argument("--refresh-snapshot", action="store_true")
    a = p.parse_args()
    if urlparse(a.base).hostname not in ["127.0.0.1", "localhost", "::1"]:
        p.error("Only the named disposable localhost instance is supported")
    path = Path(a.state)
    state = json.loads(path.read_text())
    session = Session(a.base)
    session.login("test-manager", state["passwords"]["test-manager"])
    if a.refresh_snapshot:
        state["projectIds"] = [
            row["project"]["id"]
            for row in session.call("GET", "/projects?size=100")["items"]
        ]
    actual = {
        str(pid): session.call("GET", f"/projects/{pid}") for pid in state["projectIds"]
    }
    audit = session.call("GET", "/audit")
    if a.refresh_snapshot:
        state["snapshots"], state["auditSnapshot"] = actual, audit
        path.write_text(json.dumps(state, ensure_ascii=False, indent=2))
        os.chmod(path, 0o600)
        print("Private post-GUI snapshot updated; no credentials printed.")
        return
    total = 0
    for pid, detail in actual.items():
        expected = state["snapshots"][pid]
        for key in ["project", "segments", "terms", "counts", "events", "deliveries"]:
            check(detail[key] == expected[key], f"persisted {key}")
            total += len(detail[key]) if isinstance(detail[key], list) else 1
        for delivery in detail["deliveries"]:
            payload = session.call(
                "GET", f"/projects/{pid}/deliveries/{delivery['id']}/json", raw=True
            ).content
            check(
                hashlib.sha256(payload).hexdigest() == delivery["sha256"],
                "frozen delivery SHA",
            )
            total += 1
        for segment in detail["segments"]:
            history = session.call("GET", f"/projects/{pid}/segments/{segment['id']}")[
                "history"
            ]
            check(
                len(history) >= 1
                and history[0]["segmentVersion"] == segment["version"],
                "revision linked to current version",
            )
            total += 1
    ids = {x["id"]: x for x in audit}
    for old in state["auditSnapshot"]:
        check(ids.get(old["id"]) == old, "immutable older audit")
        total += 1
    print(
        f"PASS: {total} persisted record/collection/hash checks after restart or restore."
    )


if __name__ == "__main__":
    main()
