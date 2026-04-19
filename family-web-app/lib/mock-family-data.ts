export type FamilyBindingState = {
  isBound: boolean;
  bindingCode: string;
  elderName: string;
  lastBoundAt: string | null;
  bindingMethod: "手动输入" | "扫码导入";
  validationStatus: "本地占位" | "待服务端校验" | "校验通过" | "校验失败";
  validationNote: string;
  validationUpdatedAt: string | null;
};

export type FamilyProfileState = {
  name: string;
  relation: string;
  greetingStyle: string;
  commonTopics: string[];
  tabooWords: string[];
  routine: string;
};

export type FamilyReminderPlan = {
  id: string;
  title: string;
  time: string;
  frequency: string;
  channel: string;
  status: "待确认" | "已同步" | "待同步";
};

export type FamilyTodaySummary = {
  summary: string;
  riskLevel: string;
  actionHint: string;
  syncedCount: number;
  totalCount: number;
  pendingSyncCount: number;
  nextReminderTitle: string;
  nextReminderTime: string;
};

export type FamilyAlertAction =
  | { kind: "link"; label: string; href: string }
  | { kind: "tel"; label: string; phone: string }
  | { kind: "dismiss"; label: string };

export type FamilyAlertItem = {
  id: string;
  level: string;
  reason: string;
  action: string;
  source: string;
  primaryAction: FamilyAlertAction;
  secondaryAction?: FamilyAlertAction;
};

export type FamilyContact = {
  id: string;
  name: string;
  relation: string;
  phone: string;
};

export function getBindingStatusMeta(status: FamilyBindingState["validationStatus"]) {
  switch (status) {
    case "校验通过":
      return {
        label: "校验通过",
        pillClassName: "pill pill--good",
        summary: "这次绑定已经通过当前阶段校验，家属端可以按真实家庭上下文继续往下看。",
      };
    case "校验失败":
      return {
        label: "校验失败",
        pillClassName: "pill pill--danger",
        summary: "这次绑定还没通过校验，建议先回绑定页重新确认绑定码或扫码方式。",
      };
    case "待服务端校验":
      return {
        label: "待服务端校验",
        pillClassName: "pill pill--warn",
        summary: "当前是绑定成功流占位，页面可以继续验收，但后面还要接正式服务端校验。",
      };
    default:
      return {
        label: "本地占位",
        pillClassName: "pill pill--warn",
        summary: "现在还是本地占位状态，先把绑定成功流和跨页联动跑顺，再接真实校验链路。",
      };
  }
}

export const defaultElderProfile: FamilyProfileState = {
  name: "张阿姨",
  relation: "妈妈",
  greetingStyle: "温和陪伴",
  commonTopics: ["早餐", "花草", "邻里聊天"],
  tabooWords: ["别唠叨", "别催"],
  routine: "早上 7:30 起床，午饭前后最容易忘记喝水。",
};

export const defaultReminderPlans: FamilyReminderPlan[] = [
  {
    id: "med-1",
    title: "午饭后喝水",
    time: "12:30",
    frequency: "每天",
    channel: "语音播报 + 铃声",
    status: "待确认",
  },
  {
    id: "walk-1",
    title: "晚饭后散步",
    time: "18:40",
    frequency: "周一、周三、周五",
    channel: "铃声",
    status: "已同步",
  },
  {
    id: "pill-1",
    title: "晚间降压药",
    time: "20:00",
    frequency: "每天",
    channel: "语音播报",
    status: "已同步",
  },
];

export const defaultContacts: FamilyContact[] = [
  { id: "c1", name: "张阿姨", relation: "本人", phone: "13800001234" },
  { id: "c2", name: "李叔叔", relation: "邻居", phone: "13800005678" },
  { id: "c3", name: "王医生", relation: "社区医生", phone: "13800007890" },
];

export const memoryNotes = [
  "昨天她主动提到想吃清淡一点，最近胃口一般。",
  "这周提过两次“别太早打电话，我还没起床”。",
  "她最近对院子里的花特别上心，聊花草会放松很多。",
];

function toMinutes(time: string) {
  const [hour, minute] = time.split(":").map(Number);
  return hour * 60 + minute;
}

function getNearestReminder(reminders: FamilyReminderPlan[]) {
  if (reminders.length === 0) {
    return null;
  }

  const now = new Date();
  const currentMinutes = now.getHours() * 60 + now.getMinutes();
  const sorted = [...reminders].sort((left, right) => toMinutes(left.time) - toMinutes(right.time));
  const laterToday = sorted.find((item) => toMinutes(item.time) >= currentMinutes);
  return laterToday ?? sorted[0];
}

export function buildFamilyTodaySummary(reminders: FamilyReminderPlan[]): FamilyTodaySummary {
  const totalCount = reminders.length;
  const pendingSyncCount = reminders.filter((item) => item.status === "待同步").length;
  const syncedCount = reminders.filter((item) => item.status !== "待同步").length;
  const nextReminder = getNearestReminder(reminders);

  if (!nextReminder) {
    return {
      summary: "今天还没有安排提醒，先从 1 到 2 条最关键的开始配。",
      riskLevel: "待补计划",
      actionHint: "先补一条最关键的提醒，例如喝水或晚间用药。",
      syncedCount,
      totalCount,
      pendingSyncCount,
      nextReminderTitle: "还没有提醒",
      nextReminderTime: "--:--",
    };
  }

  if (pendingSyncCount > 0) {
    return {
      summary: `今天一共有 ${totalCount} 条提醒，其中 ${pendingSyncCount} 条刚改过，还没同步到老人端。`,
      riskLevel: "需要同步",
      actionHint: `先盯住 ${nextReminder.time} 的“${nextReminder.title}”，然后把刚改过的提醒尽快同步过去。`,
      syncedCount,
      totalCount,
      pendingSyncCount,
      nextReminderTitle: nextReminder.title,
      nextReminderTime: nextReminder.time,
    };
  }

  return {
    summary: `今天一共有 ${totalCount} 条提醒，最近一条是 ${nextReminder.time} 的“${nextReminder.title}”。`,
    riskLevel: "今天平稳",
    actionHint: `先看住 ${nextReminder.time} 的“${nextReminder.title}”，暂时不用高频打扰。`,
    syncedCount,
    totalCount,
    pendingSyncCount,
    nextReminderTitle: nextReminder.title,
    nextReminderTime: nextReminder.time,
  };
}

export function buildFamilyAlerts(
  binding: FamilyBindingState,
  reminders: FamilyReminderPlan[],
  contacts: FamilyContact[],
): FamilyAlertItem[] {
  if (!binding.isBound) {
    return [
      {
        id: "bind-required",
        level: "待绑定",
        source: "绑定状态",
        reason: "还没连上老人端，现在这页看到的只是示例骨架。",
        action: "先去绑定页输入 6 位绑定码，再决定今天要不要介入。",
        primaryAction: { kind: "link", label: "现在去绑定", href: "/bind" },
        secondaryAction: { kind: "dismiss", label: "稍后处理" },
      },
    ];
  }

  const nextAlerts: FamilyAlertItem[] = [];
  const pendingSync = reminders.filter((item) => item.status === "待同步");
  const pendingConfirm = reminders.filter((item) => item.status === "待确认");
  const primaryContact = contacts[0] ?? defaultContacts[0];

  if (binding.validationStatus === "校验失败") {
    nextAlerts.push({
      id: `binding-failed-${binding.bindingCode}`,
      level: "校验失败",
      source: "绑定状态",
      reason: `当前绑定码 ${binding.bindingCode} 还没有通过校验。`,
      action: "先回绑定页重新确认绑定码或扫码方式，再决定今天是不是继续按当前家庭上下文介入。",
      primaryAction: { kind: "link", label: "回绑定页重试", href: "/bind" },
      secondaryAction: { kind: "dismiss", label: "稍后处理" },
    });
  } else if (binding.validationStatus === "待服务端校验" || binding.validationStatus === "本地占位") {
    nextAlerts.push({
      id: `binding-validation-${binding.bindingCode}`,
      level: binding.validationStatus === "本地占位" ? "本地占位" : "待校验",
      source: "绑定状态",
      reason: `当前家庭绑定来自${binding.bindingMethod}，${binding.validationNote || "还没有经过服务端正式校验。"} `,
      action: "首版可以先继续看今天状态和提醒设置，但后续仍要补扫码或服务端校验接口。",
      primaryAction: { kind: "link", label: "回绑定页确认", href: "/bind" },
      secondaryAction: { kind: "dismiss", label: "稍后处理" },
    });
  }

  if (pendingSync.length > 0) {
    nextAlerts.push({
      id: `sync-${pendingSync.length}-${pendingSync[0]?.id ?? "none"}`,
      level: "需要同步",
      source: "提醒计划",
      reason: `刚改过 ${pendingSync.length} 条提醒，还没回到老人端。`,
      action: "先去提醒页确认改动，再决定今晚要不要额外打电话提醒。",
      primaryAction: { kind: "link", label: "去提醒页处理", href: "/reminders" },
      secondaryAction: { kind: "dismiss", label: "稍后处理" },
    });
  }

  if (pendingConfirm.length > 0) {
    const focusReminder = getNearestReminder(pendingConfirm) ?? pendingConfirm[0];
    nextAlerts.push({
      id: `confirm-${focusReminder.id}-${focusReminder.time}`,
      level: "今晚跟进",
      source: "待确认提醒",
      reason: `${focusReminder.time} 的“${focusReminder.title}”还没看到确认结果。`,
      action: `如果今晚还没回音，先打给 ${primaryContact.name} 确认一下情况。`,
      primaryAction: { kind: "tel", label: `现在联系 ${primaryContact.name}`, phone: primaryContact.phone },
      secondaryAction: { kind: "link", label: "先看今日状态", href: "/today" },
    });
  }

  if (reminders.length === 0) {
    nextAlerts.push({
      id: "setup-reminders",
      level: "待补计划",
      source: "提醒计划",
      reason: "已经绑定成功，但今天还没有一条正式提醒。",
      action: "先补 1 到 2 条最关键的提醒，再观察老人端今天的执行情况。",
      primaryAction: { kind: "link", label: "现在去补提醒", href: "/reminders" },
      secondaryAction: { kind: "dismiss", label: "稍后处理" },
    });
  }

  return nextAlerts;
}
