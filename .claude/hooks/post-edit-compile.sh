#!/usr/bin/env bash
# =============================================================================
# PostToolUse 钩子（Edit / Write / MultiEdit）
# 作用：智能体每改一个 .java 文件，立即编译（含测试代码）。
#       编译失败时退出码 2，把编译错误回灌给智能体，使其"当场"修正，
#       避免在错误基础上继续堆代码（也能第一时间暴露幻觉出来的不存在的 API）。
# =============================================================================
input=$(cat)
path=$(printf '%s' "$input" | grep -o '"file_path"[[:space:]]*:[[:space:]]*"[^"]*"' | head -1 | sed 's/.*:[[:space:]]*"\(.*\)"/\1/')
[[ "$path" == *.java ]] || exit 0

cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0
if ! out=$(mvn -q test-compile 2>&1); then
  {
    echo "【编译失败】修改 $path 后工程无法编译，请先修复以下错误再继续："
    printf '%s\n' "$out" | grep -E 'ERROR|error:' | head -30
  } >&2
  exit 2
fi
exit 0
