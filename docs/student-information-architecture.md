# 学生端信息架构映射

本文件记录七大一级板块的最终归属。`已接入`表示复用项目中的现有页面与接口；`待建设`仅保留信息架构位置，不提供页面、数据或接口。

## 首页

| 二级功能 | 状态 | 现有实现 |
| --- | --- | --- |
| 今日任务 | 已接入 | `HomeView.vue` 今日计划、勾选、新增与进度逻辑 |
| 目标岗位 | 已接入 | `HomeView.vue` 目标岗位摘要，详情进入 `/career/gap` |
| 继续学习 | 已接入 | `HomeView.vue` 最近课程，完整学习进入 `/learning/python` |
| 实习推荐 | 已接入 | `HomeView.vue` 真实外部岗位摘要，完整列表进入 `/employment` |
| 成长动态 | 已接入 | `HomeView.vue` 技能、项目与面试准备摘要 |

## 成长中心

| 二级功能 | 状态 | 新入口 | 复用实现 |
| --- | --- | --- | --- |
| 成长树 | 已接入 | `/growth/tree` | `CareerNebulaView.vue`、原 `/career/nebula` |
| 技能树 | 已接入现有能力视图 | `/growth/skills` | `ProfileRadarView.vue` |
| 课程成绩 | 已接入 | `/growth/grades` | `ExamPapersView.vue` 及成绩、历史子页面 |
| 证书 | 待建设 | 无可点击路由 | 未创建假页面或数据 |
| 项目 | 待建设 | 无可点击路由 | 未创建假成果数据 |
| 校园活动 | 已接入 | `/growth/campus-activity` | `CampusActivitiesView.vue` 及详情、报名、签到页面和原活动 API |
| 校园地图 | 已接入 | `/growth/campus-map` | `MapView.vue` 及原地图 API、交互逻辑 |
| 能力档案 | 已接入 | `/growth/profile` | `ProfileRadarView.vue` 及原画像 API |

## 岗位探索

| 二级功能 | 状态 | 新入口 | 复用实现 |
| --- | --- | --- | --- |
| 岗位星图 | 已接入 | `/career/star-map` | `CareerNebulaView.vue`、`CareerPlanetView.vue` |
| 岗位搜索 | 已接入 | `/career/search` | `JobExplorationView.vue` |
| 岗位画像 | 已接入现有岗位详情 | `/career/profile` | `JobExplorationView.vue?view=profile` |
| 技能要求 | 已接入现有岗位详情 | `/career/skills` | `JobExplorationView.vue?view=skills` |
| 我的能力差距 | 已接入现有目标与匹配信息 | `/career/gap` | `JobExplorationView.vue?view=gap` |

## 学习实践

| 二级功能 | 状态 | 新入口 | 复用实现 |
| --- | --- | --- | --- |
| Python学习 | 已接入 | `/learning/python` | `PythonLearningShell.vue`、学习计划与知识图谱页面 |
| 题库 | 已接入 | `/learning/question-bank` | `PythonQuestionBankView.vue`、练习与测试页面 |
| 项目实训 | 已接入现有学习计划 | `/learning/python/plan` | `PythonLearningView.vue` 中的项目实战内容 |
| 专项训练 | 已接入 | `/learning/special-training` | `LearningResourceView.vue` |
| 商业沙盘 | 待建设 | 无可点击路由 | 未创建假页面或数据 |
| 企业模拟 | 待建设 | 无可点击路由 | 未创建假页面或数据 |

## 实习就业

| 二级功能 | 状态 | 新入口 | 复用实现 |
| --- | --- | --- | --- |
| 实习雷达 | 已接入现有推荐能力 | `/employment/radar` | `HotJobsView.vue` |
| 多平台岗位聚合 | 已接入当前真实来源 | `/employment/aggregate` | `HotJobsView.vue` 中已有外部岗位链接 |
| 本地就业专区 | 待建设 | 无可点击路由 | 未伪造本地岗位数据 |
| 校招 | 已接入当前招聘数据 | `/employment/campus-recruitment` | `HotJobsView.vue`，不伪造校招分类 |
| 校友企业 | 待建设 | 无可点击路由 | 未伪造校友企业数据 |

## 校友社区

| 二级功能 | 状态 | 说明 |
| --- | --- | --- |
| 学长学姐成长路径 | 待建设 | 未接入真实案例，不创建假入口 |
| 经验分享 | 待建设 | 未接入真实内容，不创建假入口 |
| 就业案例 | 待建设 | 未接入真实案例，不创建假入口 |
| 问答交流 | 待建设 | 未实现社区讨论，不创建假入口 |

## AI求职

| 二级功能 | 状态 | 新入口 | 复用实现 |
| --- | --- | --- | --- |
| 智能简历 | 已接入 | `/ai-career/resume` | `ResumeView.vue`、工作台、设计器与填写向导 |
| AI润色 | 待建设 | 无可点击路由 | 未发起假 AI 调用 |
| 岗位定制简历 | 待建设 | 无可点击路由 | 未创建假定制流程 |
| AI模拟面试 | 已接入 | `/ai-career/interview` | `InterviewShell.vue` 下的完整面试流程 |
| 面试报告 | 已接入 | `/ai-career/report` | 现有 AI 历史、评估结果与面试报告流程 |

## 兼容入口

原 `/map`、`/activities`、`/career/nebula/*`、`/jobs/explore`、`/jobs/hot`、`/ai-tools/resume/*`、`/interview/*` 均继续工作；同时补充 `/campus-map`、`/campus-activity`、`/python-learning`、`/ai-interview`、`/star-map` 的兼容跳转。接口、数据库、权限和后台管理路由未调整。
