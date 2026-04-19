import type { ServiceStore } from "./types.js";

const now = "2026-04-13T09:00:00+08:00";

export const defaultStore: ServiceStore = {
  bindings: [
    {
      bindingId: "binding-demo-01",
      seniorId: "senior-zhang",
      childId: "child-demo-01",
      relation: "daughter",
      bindingCode: "426318",
      bindingMethod: "manual",
      validationStatus: "pending_validation",
      notificationPreference: "important_only",
      boundAt: now,
      updatedAt: now
    }
  ],
  profiles: [
    {
      seniorId: "senior-zhang",
      preferredName: "张阿姨",
      relationLabel: "妈妈",
      interests: ["早饭", "花草", "社区新鲜事"],
      hobbies: ["阳台种花", "晚饭后散步"],
      tabooTopics: ["催促式提醒", "沉重医疗猜测"],
      communicationStyle: "patient_gentle",
      routineSummary: "早上 7:30 起床，午饭前后容易忘记喝水，晚上 8 点后更愿意轻松聊天。",
      personaTags: ["慢节奏", "喜欢熟人感", "需要低压提醒"],
      importantContacts: [
        {
          id: "contact-01",
          name: "李女士",
          relation: "女儿",
          phone: "13800001234",
          priority: 1
        },
        {
          id: "contact-02",
          name: "王医生",
          relation: "社区医生",
          phone: "13800004567",
          priority: 2
        }
      ],
      updatedAt: now
    }
  ],
  carePlans: [
    {
      planId: "plan-01",
      seniorId: "senior-zhang",
      title: "午饭后喝水",
      schedule: "12:30",
      frequency: "daily",
      channel: "tts+ring",
      confirmRequired: true,
      source: "family_web",
      status: "active",
      updatedAt: now
    },
    {
      planId: "plan-02",
      seniorId: "senior-zhang",
      title: "晚间降压药",
      schedule: "20:00",
      frequency: "daily",
      channel: "tts",
      confirmRequired: true,
      source: "senior_app",
      status: "active",
      updatedAt: now
    }
  ],
  carePlanEvents: [],
  semanticMemories: [],
  topicBriefs: [
    {
      topicId: "topic-01",
      seniorId: "senior-zhang",
      title: "今天阳台适合给花松松土",
      summary: "结合她最近常聊花草和今天的轻松陪伴方向，先给一个生活化话题占位，而不是直接推新闻正文。",
      sourceName: "service_placeholder",
      sourceUrl: "",
      riskFlags: [],
      generatedAt: now
    }
  ]
};
