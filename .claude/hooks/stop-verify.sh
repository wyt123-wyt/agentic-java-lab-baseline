#!/usr/bin/env bash
# =============================================================================
# Stop 钩子：智能体宣布"完成"时触发的最终质量门禁。
#   ① 验收测试完整性：SHA-256 与教师发布的基线一致，且无 @Disabled
#   ② mvn verify：编译 + 全部测试 + JaCoCo 覆盖率门槛
# 任一不通过 → 退出码 2，阻止智能体结束，并把失败原因回灌给它继续修复。
# 最多连续拦截 3 次，防止无限循环；超过后放行，由人类介入。
# =============================================================================
cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0
input=$(cat)
COUNTER=.claude/.stop-attempts

fail() {
  n=$(( $(cat "$COUNTER" 2>/dev/null || echo 0) + 1 ))
  echo "$n" > "$COUNTER"
  if [ "$n" -gt 3 ]; then
    echo "$(date '+%F %T') STOP-GATE 连续 3 次未通过，交由人类处理" >> .claude/audit.log
    rm -f "$COUNTER"
    exit 0
  fi
  { echo "【质量门禁未通过（第 $n/3 次）】任务尚未完成："; echo "$1"; } >&2
  echo "$(date '+%F %T') STOP-GATE-FAIL #$n" >> .claude/audit.log
  exit 2
}

# ① 验收测试完整性
if command -v sha256sum >/dev/null 2>&1; then SHA="sha256sum"; else SHA="shasum -a 256"; fi
if ! $SHA -c ACCEPTANCE.sha256 >/dev/null 2>&1; then
  fail "验收测试或规格文件被改动（SHA-256 校验失败）。请用 git diff 检查并恢复，禁止修改验收测试。"
fi
if grep -rEn '@Disabled|@Ignore' src/test/java >/dev/null 2>&1; then
  fail "测试代码中出现 @Disabled/@Ignore：$(grep -rEn '@Disabled|@Ignore' src/test/java | head -5)"
fi

# ② 构建与测试
out=$(mvn -q verify 2>&1)
if [ $? -ne 0 ]; then
  fail "$(printf '%s\n' "$out" | grep -E 'FAIL|ERROR|Tests run|expected|Rule violated' | head -40)"
fi

rm -f "$COUNTER"
echo "$(date '+%F %T') STOP-GATE-PASS" >> .claude/audit.log
exit 0
