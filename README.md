# 智能体协同 Java 开发：监督与验证实验工程

## 目录结构
```
SPEC.md                         需求规格（人写，测试预言）
CLAUDE.md                       智能体工作守则（受保护）
ACCEPTANCE.sha256               验收测试与规格的 SHA-256 基线
pom.xml                         JUnit 5 + JaCoCo 门槛 + PIT 变异测试（受保护）
.claude/settings.json           项目级权限与钩子（可提交到 git，不含密钥）
.claude/hooks/                  四个钩子脚本（受保护）
.claude/agents/java-reviewer.md 独立审查子智能体
docs/kimi-settings.example.json 接入 Kimi K3 的用户级配置样例（复制到 ~/.claude/settings.json）
src/main/java/...               实现代码（智能体的工作区）
src/test/java/.../acceptance/   验收测试（人写，受保护）
drill/                          缺陷注入演练（仅参考工程包含）
examples/agent-weak-test/       智能体式弱测试反面教材（仅参考工程包含）
```

## 常用命令
```bash
mvn -q test                                              # 运行全部测试
mvn -q verify                                            # 测试 + 覆盖率门槛
mvn -q test-compile org.pitest:pitest-maven:mutationCoverage   # 变异测试
bash drill/mutation-drill.sh                             # 缺陷注入演练（参考工程）
sha256sum -c ACCEPTANCE.sha256                           # 手工核验验收测试未被改动
```

## Windows 说明
钩子脚本为 bash 脚本，Windows 上请安装 Git for Windows（Claude Code 在 Windows 上本身即依赖 Git Bash）。
