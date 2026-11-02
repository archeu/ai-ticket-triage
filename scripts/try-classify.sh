#!/usr/bin/env bash
# 把 samples/ 里的每封邮件发给 /api/classify，对比预期类别（文件名里）和 AI 的结果。
# 这就是一个最小的"评测集"：改提示词后跑一遍，就知道准确率是变好还是变差。
cd "$(dirname "$0")/.."
pass=0; total=0
for f in samples/*.txt; do
  expected=$(basename "$f" .txt | cut -d- -f2)
  body=$(python3 -c 'import json,sys; print(json.dumps({"email": open(sys.argv[1], encoding="utf-8").read()}, ensure_ascii=False))' "$f")
  got=$(curl -sS --max-time 60 -X POST http://localhost:8080/api/classify \
        -H 'Content-Type: application/json' -d "$body" \
        | python3 -c 'import json,sys; r=json.load(sys.stdin); print(r["category"], "|", r["rawAnswer"].strip())')
  category=${got%% |*}
  total=$((total+1))
  if [[ "$category" == "$expected" ]]; then pass=$((pass+1)); mark="✓"; else mark="✗"; fi
  printf '%s %-18s 预期 %-10s 结果 %s\n' "$mark" "$(basename "$f")" "$expected" "$got"
done
echo "正确 $pass / $total"
