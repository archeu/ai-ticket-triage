#!/usr/bin/env bash
# 生成线上的公开示例（所有访客都能看到，不会被定时清理）。只能在服务器上运行。
# 做法：用一个临时邀请码把 samples/public/{zh,en} 里的邮件提交给线上应用，标记为示例，最后停用临时邀请码。
# 重新运行会用新结果替换旧示例。
set -euo pipefail
cd "$(dirname "$0")/.."
API=http://127.0.0.1:8081/api/tickets
psql() { sudo docker exec -i ticket-triage-db-1 psql -U triage -d triage -v ON_ERROR_STOP=1 "$@"; }

code="seed-$(python3 -c 'import secrets; print(secrets.token_hex(6))')"
echo "insert into invites (code, label, active, daily_limit, use_count, uses_today, created_at)
      values (:'code', '生成示例（临时）', true, 100, 0, 0, now());" | psql -q -v code="$code"

i=0
for lang in zh en; do
  for f in samples/public/"$lang"/*.txt; do
    i=$((i + 1))
    body=$(python3 -c 'import json,sys; print(json.dumps({"email": open(sys.argv[1], encoding="utf-8").read().strip(), "summaryLanguage": sys.argv[2]}, ensure_ascii=False))' "$f" "$lang")
    # 从服务器本机提交，每封用不同的 X-Real-IP，避开"每个 IP 每分钟 5 次"的限流
    status=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$API" -H 'Content-Type: application/json' \
      -H "X-Invite-Code: $code" -H "X-Visitor-Id: seed-samples-$lang" -H "X-Real-IP: 10.255.0.$i" -d "$body")
    echo "$lang/$(basename "$f") → HTTP $status"
  done
  echo "delete from tickets where sample_lang = :'lang';
        update tickets set sample_lang = :'lang', visitor_id = null where visitor_id = :'vid';" \
    | psql -q -v lang="$lang" -v vid="seed-samples-$lang"
done

echo "update invites set active = false where code = :'code';" | psql -q -v code="$code"
echo "完成，当前示例："
psql -c "select id, sample_lang as lang, category, urgency, left(summary, 32) as summary from tickets where sample_lang is not null order by id;"
