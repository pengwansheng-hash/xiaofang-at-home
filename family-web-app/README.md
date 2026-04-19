# 小芳在家子女端 H5

当前目录是「小芳在家」子女端移动 H5 工程。

## 技术路线

- Next.js App Router
- TypeScript
- 移动单栏 H5
- 优先适配微信内打开，其次兼容普通手机浏览器

## 已落地页面

- `/bind` 绑定页
- `/today` 今日状态页
- `/reminders` 提醒设置页
- `/alerts` 异常页
- `/profile` 老人资料页

## 本地启动

```bash
npm.cmd install
npm.cmd run dev
```

如果你在仓库根目录，也可以直接运行：

```cmd
.\start-xiaofang.cmd
```

默认访问地址：

```text
http://127.0.0.1:3201/today
```

## 当前阶段说明

- 当前使用 mock 数据打通子女端壳层和核心页面结构
- 绑定扫码、服务端鉴权、提醒编辑保存、异常联动与真实数据同步仍在后续任务中
