#!/usr/bin/env python3
"""评测：把 eval/emails/ 里的每封邮件发给 /api/tickets（AI 分析 + 存库），检查类别和订单号是否正确。

文件名里写着正确答案，例如 03-REFUND.txt 的正确类别是 REFUND。每次改提示词都重跑一遍，
看准确率是变好还是变差。

用法：先用 ./scripts/run.sh 启动应用（dev 配置），再运行 python3 eval/run_eval.py
"""
import json
import pathlib
import re
import urllib.error
import urllib.request

API = "http://localhost:8080/api/tickets"
ORDER_RE = re.compile(r"[A-Z]\d{4,}")  # 样例邮件里订单号的格式，例如 A20261028、B88123、C5521
SAMPLES = pathlib.Path(__file__).resolve().parent / "emails"


def post(email):
    req = urllib.request.Request(API, data=json.dumps({"email": email}).encode("utf-8"),
                                 headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=120) as resp:
        return json.load(resp)


passed = 0
files = sorted(SAMPLES.glob("*.txt"))
for f in files:
    email = f.read_text(encoding="utf-8")
    expected_cat = f.stem.split("-")[1]
    m = ORDER_RE.search(email)
    expected_order = m.group(0) if m else None
    try:
        t = post(email)
    except urllib.error.HTTPError as e:
        print(f"✗ {f.name}  HTTP {e.code}: {e.read().decode('utf-8')[:200]}")
        continue
    got_order = t.get("orderNumber")
    order_ok = bool(got_order == expected_order or (got_order and expected_order and expected_order in got_order))
    cat_ok = t["category"] == expected_cat
    passed += cat_ok and order_ok
    reply = t.get("replyDraft") or ""
    print(f"{'✓' if cat_ok and order_ok else '✗'} #{t['id']} {f.name}")
    print(f"   类别 {t['category']}（预期 {expected_cat}）  紧急 {t['urgency']}  情绪 {t['sentiment']}  "
          f"订单号 {got_order}（预期 {expected_order}）")
    print(f"   摘要：{t.get('summary')}")
    print(f"   回复：{reply[:90]}{'…' if len(reply) > 90 else ''}")
print(f"全部正确 {passed} / {len(files)}")
