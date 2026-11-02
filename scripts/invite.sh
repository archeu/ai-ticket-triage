#!/usr/bin/env bash
# 邀请链接管理（在服务器上运行，直接操作线上数据库）
# 用法：
#   ./scripts/invite.sh create "Upwork 客户 Acme" [每天次数，默认 20]   生成一个邀请链接
#   ./scripts/invite.sh list                                          查看所有邀请码和使用情况
#   ./scripts/invite.sh revoke <邀请码>                                停用一个邀请码
set -euo pipefail
SITE="https://triage.ainiya.de"
psql() { sudo docker exec -i ticket-triage-db-1 psql -U triage -d triage -v ON_ERROR_STOP=1 "$@"; }

case "${1:-}" in
  create)
    label="${2:?请写上这个链接给谁用，例如：./scripts/invite.sh create \"Upwork 客户 Acme\"}"
    limit="${3:-20}"
    [[ "$limit" =~ ^[0-9]+$ ]] || { echo "每天次数必须是数字"; exit 1; }
    code=$(python3 -c 'import secrets; print(secrets.token_urlsafe(12))')
    # 用 psql 变量传值（:'label'），自动转义，备注里有引号也不会出错
    echo "insert into invites (code, label, active, daily_limit, use_count, uses_today, created_at)
          values (:'code', :'label', true, $limit, 0, 0, now());" | psql -q -v code="$code" -v label="$label"
    echo "已生成（$label，每天最多 $limit 次）："
    echo "$SITE/?invite=$code"
    ;;
  list)
    psql -c 'select code as "邀请码", label as "给谁", case when active then $$启用$$ else $$停用$$ end as "状态",
                    daily_limit as "每天上限", use_count as "累计使用",
                    to_char(last_used_at at time zone $$Asia/Bangkok$$, $$MM-DD HH24:MI$$) as "最近使用"
             from invites order by created_at desc;'
    ;;
  revoke)
    code="${2:?请写上要停用的邀请码：./scripts/invite.sh revoke <邀请码>}"
    result=$(echo "update invites set active = false where code = :'code';" | psql -v code="$code")
    [[ "$result" == "UPDATE 1" ]] && echo "已停用：$code" || echo "没找到这个邀请码：$code"
    ;;
  *)
    sed -n '2,6p' "$0"
    ;;
esac
