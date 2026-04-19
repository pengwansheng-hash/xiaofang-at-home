# 小芳在家

`xiaofang-at-home` 是一个独立子项目，定位为面向独居或半独居老人的 AI 照护代理。

当前目录下包含的子项目如下：
- `android-senior-app`：老人端 Android Compose 工程
- `family-web-app`：子女端移动 H5 工程，优先用于微信内打开
- `care-service`：服务端骨架，承载绑定、老人画像、提醒计划等后续能力

当前阶段的产品重点：
- 老人端 Android App 继续打通提醒、陪伴和待办主链路
- 子女端 H5 优先跑通绑定、今日状态、提醒设置和异常查看
- 14 天家庭试用闭环用来验证「提醒、熟悉感、结论回传」三项体验

## 本地启动

如果你在仓库根目录，直接运行：

```cmd
.\start-xiaofang.cmd
```

首次使用时，先在 `xiaofang-at-home/family-web-app` 执行 `npm.cmd install`。

如果你已经在 `xiaofang-at-home/family-web-app` 目录下，也可以直接运行：

```cmd
npm.cmd run dev
```

启动后默认访问地址：

```text
http://127.0.0.1:3201/today
```

## 目录说明

- `docs/product`：产品边界、需求与决策
- `docs/design`：页面、交互和线框说明
- `docs/engineering`：架构、接口与开发计划
- `docs/qa`：测试与验证记录
- `docs/releases`：发版记录

## 进度提醒

当前重点仍是把子女端 H5 和老人端 Android 两条主链路跑通，并把绑定、今日状态、提醒设置与异常查看做稳。
