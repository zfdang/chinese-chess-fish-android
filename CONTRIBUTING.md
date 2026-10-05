# 开发与发布

修改通过 Pull Request 合并到 `master`。普通 PR 和直接 push 到 `master` 只运行构建检查；只有 PR 合并后才生成 GitHub Release。紧急修复也应通过 PR 合并。仅修改 `docs/` 的 PR 不触发构建或发版。Release 标签使用合并提交的 Git 提交数，序号允许不连续。

