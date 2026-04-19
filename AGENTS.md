# AGENTS.md

# 子项目章程

本目录是独立子项目，默认按完整产品交付流程推进，而不是直接写代码。

## 默认规则
- 先判断任务类型，再决定产品、设计、研发、测试和发版动作。
- 任何影响产品行为、页面交互、接口行为或提醒规则的改动，都必须同步更新本子项目文档。
- `docs/engineering/tasks.md` 是本子项目默认开发计划，任务开始前更新，完成后同步状态。
- 对双端协作、端云边界、模型能力、推送链路等高返工风险任务，默认先补齐目标、边界、风险、降级与验收方式讨论。
- 对页面、组件和交互类任务，默认把 Figma 作为设计协作层；若暂时没有 Figma 文件，至少先补齐 UI 规格和线框。

## 强制顺序

### 新产品想法 / 新子项目
1. 澄清目标用户、核心场景、价值和首版边界
2. 更新 `docs/product/prd.md`
3. 更新 `docs/design/ui-spec.md`
4. 更新 `docs/design/wireframes.md`
5. 更新 `docs/engineering/architecture.md` 与 `docs/engineering/api-spec.md`
6. 更新 `docs/engineering/tasks.md`
7. 待方向确认后再进入代码实现

### 新功能需求 / 功能变更
1. 更新 `docs/product/prd.md`
2. 如涉及交互，更新 `docs/design/ui-spec.md`
3. 如涉及页面或流程结构，更新 `docs/design/wireframes.md`
4. 更新 `docs/engineering/tasks.md`
5. 如技术路线、模型能力、第三方平台或消息链路不稳，先完成一轮路线和风险确认
6. 修改代码
7. 补充测试
8. 更新 `docs/qa/test-report.md`
9. 更新 `CHANGELOG.md`
10. 如接近发版，更新 `docs/releases/release-notes.md`

## 单一事实源
- `docs/product/prd.md`
- `docs/product/decisions.md`
- `docs/design/ui-spec.md`
- `docs/engineering/tasks.md`
- `docs/engineering/api-spec.md`
- `CHANGELOG.md`

## 沟通
- 默认用中文
- 先说判断和方案，再说执行
- 有明显边界不确定时，先澄清，不擅自扩需求
