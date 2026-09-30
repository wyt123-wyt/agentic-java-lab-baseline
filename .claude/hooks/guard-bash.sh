#!/usr/bin/env bash
# =============================================================================
# PreToolUse 钩子（Bash）
# 作用：堵住"绕过验证"的命令行后门——跳过测试、绕过 git 钩子、
#       以及用 sed/重定向/git checkout 等方式间接改写受保护文件。
# =============================================================================
input=$(cat)
cmd=$(printf '%s' "$input" | grep -o '"command"[[:space:]]*:[[:space:]]*".*"' | head -1)

block() {
  echo "【已拦截】$1" >&2
  echo "请在不削弱验证手段的前提下解决问题；如确有必要，向人类说明并请求授权。" >&2
  echo "$(date '+%F %T') BLOCKED-BASH $1 :: $cmd" >> "${CLAUDE_PROJECT_DIR:-.}/.claude/audit.log" 2>/dev/null
  exit 2
}

# 1) 跳过测试 / 放宽质量门禁
if printf '%s' "$cmd" | grep -Eq -- '-DskipTests|-Dmaven\.test\.skip|-Djacoco\.skip|-Dpit\.skip|-Dsurefire\.skip|-Dmaven\.test\.failure\.ignore|-DtestFailureIgnore'; then
  block "禁止跳过或忽略测试（检测到 skip/ignore 类参数）"
fi
# 2) 绕过 git 钩子、推送远程
if printf '%s' "$cmd" | grep -Eq -- '--no-verify|git[[:space:]]+push'; then
  block "禁止 --no-verify 或 git push，提交与推送由人类在审查后完成"
fi
# 3) 通过命令行间接改写受保护文件
if printf '%s' "$cmd" | grep -Eq 'acceptance/|SPEC\.md|pom\.xml|ACCEPTANCE\.sha256|\.claude/|drill/'; then
  if printf '%s' "$cmd" | grep -Eq 'sed[[:space:]]+-i|perl[[:space:]]+-[a-z]*i|>[^&]|[[:space:]]tee[[:space:]]|(^|[[:space:];&|])(mv|rm|cp|truncate)[[:space:]]|git[[:space:]]+(checkout|restore|apply|stash|reset)|patch[[:space:]]'; then
    block "禁止通过命令行修改受保护文件（规格/验收测试/构建脚本/钩子）"
  fi
fi
exit 0
