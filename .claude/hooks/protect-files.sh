#!/usr/bin/env bash
# =============================================================================
# PreToolUse 钩子（Edit / Write / MultiEdit / NotebookEdit）
# 作用：阻止智能体修改"验证基础设施"——规格、验收测试、构建脚本、钩子自身。
# 约定：退出码 2 = 阻止本次工具调用，stderr 内容会作为反馈交给智能体。
# 说明：不依赖 jq，直接在原始 JSON 文本里匹配 file_path，Windows Git Bash 同样可用。
# =============================================================================
input=$(cat)
path=$(printf '%s' "$input" | grep -o '"file_path"[[:space:]]*:[[:space:]]*"[^"]*"' | head -1 | sed 's/.*:[[:space:]]*"\(.*\)"/\1/')
path=${path//\\\\//}          # 统一 Windows 反斜杠

PROTECTED_REGEX='(^|/)(SPEC\.md|pom\.xml|ACCEPTANCE\.sha256|CLAUDE\.md)$|/src/test/java/.*/acceptance/|(^|/)\.claude/|(^|/)drill/'

if [[ -n "$path" && "$path" =~ $PROTECTED_REGEX ]]; then
  {
    echo "【已拦截】$path 属于受保护的验证基础设施，智能体无权修改。"
    echo "如果你认为规格或验收测试本身有错误，请停止编码，向人类说明理由并给出证据，由人类决定是否修改。"
    echo "绝不允许通过修改测试、跳过测试或放宽断言的方式让构建通过。"
  } >&2
  echo "$(date '+%F %T') BLOCKED-EDIT $path" >> "${CLAUDE_PROJECT_DIR:-.}/.claude/audit.log" 2>/dev/null
  exit 2
fi
exit 0
