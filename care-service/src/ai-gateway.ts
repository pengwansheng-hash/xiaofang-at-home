import { randomUUID } from "node:crypto";
import { ensureLocalEnvLoaded } from "./env.js";
import type { CarePlan, SemanticMemoryRecord, SeniorProfile, TopicBrief } from "./types.js";

type GatewayMode = "live" | "fallback";

type ChatMessageInput = {
  role: "user" | "assistant";
  content: string;
};

export type AiRuntimeStatus = {
  provider: "disabled" | "openai-compatible";
  configured: boolean;
  model: string | null;
  baseUrl: string | null;
  timeoutMs: number;
};

export type CompanionReplyInput = {
  seniorId: string;
  userText: string;
  recentMessages: ChatMessageInput[];
  profile: SeniorProfile | null;
  carePlans: CarePlan[];
  topicBriefs: TopicBrief[];
  semanticMemories?: SemanticMemoryRecord[];
  conversationContext?: CompanionReplyConversationContext | null;
};

export type CompanionReplyConversationContext = {
  preferredName?: string | null;
  communicationStyle?: string | null;
  personaPrompt?: string | null;
  commonTopics?: string[];
  tabooTopics?: string[];
  emotionHint?: string | null;
  memoryHighlights?: string[];
  reminderHint?: string | null;
  contactHint?: string | null;
  recentConversationHint?: string | null;
  collectionHint?: string | null;
};

export type CompanionReplyResult = {
  mode: GatewayMode;
  provider: string;
  model: string;
  reply: string;
  summary: string;
  usedContext: {
    preferredName: string | null;
    communicationStyle: string | null;
    nextCarePlan: string | null;
    topicBriefTitles: string[];
    commonTopics: string[];
    tabooTopics: string[];
    memoryHighlights: string[];
    emotionHint: string | null;
    reminderHint: string | null;
    contactHint: string | null;
    recentConversationHint: string | null;
  };
  generatedAt: string;
};

export type TopicBriefInput = {
  seniorId: string;
  topicHint: string;
  sourceText: string;
  sourceName: string;
  sourceUrl: string;
  profile: SeniorProfile | null;
};

export type TopicBriefResult = {
  mode: GatewayMode;
  provider: string;
  model: string;
  topicBrief: TopicBrief;
  generatedAt: string;
};

type AiGatewayConfig = {
  provider: "disabled" | "openai-compatible";
  apiKey: string | null;
  baseUrl: string | null;
  model: string | null;
  timeoutMs: number;
};

type OpenAiMessage = {
  role: "system" | "user" | "assistant";
  content: string;
};

function getNowIso() {
  return new Date().toISOString();
}

function logGatewayWarning(scope: string, detail: Record<string, unknown>) {
  console.warn(`[ai-gateway] ${scope}`, JSON.stringify(detail));
}

function normalizeText(value: string | null | undefined): string {
  return value?.trim().replace(/\s+/g, " ") ?? "";
}

function normalizePromptBlock(value: string | null | undefined): string {
  return value?.replace(/\r\n/g, "\n").trim().replace(/\n{3,}/g, "\n\n") ?? "";
}

function containsAny(text: string, keywords: string[]): boolean {
  return keywords.some((keyword) => text.includes(keyword));
}

function looksLikeQuestion(text: string): boolean {
  return text.includes("?") || text.includes("？") || text.endsWith("吗") || text.endsWith("呢");
}

function tokenize(text: string): string[] {
  const normalized = normalizeText(text).toLowerCase().replace(/[^\p{L}\p{N}]+/gu, " ");
  const baseTokens = normalized.split(/\s+/).map((item) => item.trim()).filter((item) => item.length >= 2);
  if (baseTokens.length > 0) {
    return [...new Set(baseTokens)].slice(0, 16);
  }
  const compact = normalized.replace(/\s+/g, "");
  const pairs: string[] = [];
  for (let index = 0; index < compact.length - 1; index += 1) {
    pairs.push(compact.slice(index, index + 2));
  }
  return [...new Set(pairs)].slice(0, 16);
}

function hasResistanceSignal(text: string): boolean {
  return containsAny(text, ["别问", "别再问", "问这些干什么", "不想说", "不想聊", "烦不烦", "换个话题", "别打听"]);
}

function hasImmediateProfileDisclosure(text: string): boolean {
  return containsAny(text, ["儿子", "女儿", "孩子", "孙子", "孙女", "老伴", "一个人住", "独居", "退休", "年轻时", "以前", "工作", "上班"]);
}

function hasCompanionTaskSignal(text: string): boolean {
  return containsAny(text, ["提醒", "记得", "吃药", "喝水", "明天", "下午", "晚上", "几点", "帮我", "联系", "打电话"]);
}

function shouldUseFastLocalCompanionReply(input: CompanionReplyInput): boolean {
  const text = normalizeText(input.userText);
  if (!text) return true;
  if (looksLikeQuestion(text)) return false;
  if (hasResistanceSignal(text)) return false;
  if (hasImmediateProfileDisclosure(text)) return false;
  if (hasCompanionTaskSignal(text)) return false;
  if (containsAny(text, ["难受", "头晕", "胸口", "担心", "焦虑", "孤单", "不舒服"])) return false;
  return text.length <= 6 && containsAny(text, ["嗯", "哦", "好", "好的", "行", "收到", "知道了", "谢谢", "哈哈", "拜拜", "晚安", "早安"]);
}

function normalizeCommunicationStyle(value: string | null | undefined): string | null {
  const trimmed = normalizeText(value);
  if (!trimmed) return null;
  if (trimmed === "patient_gentle" || trimmed === "confident_steady") {
    return trimmed;
  }
  return null;
}

function pickNextCarePlan(carePlans: CarePlan[]): string | null {
  const plan = carePlans.find((item) => item.status === "active") ?? null;
  return plan ? `${plan.schedule} 的 ${plan.title}` : null;
}

function hasKeywordOverlap(queryText: string, memory: SemanticMemoryRecord): boolean {
  const queryTokens = tokenize(queryText);
  return memory.keywords.some((keyword) => queryTokens.some((token) => token.includes(keyword) || keyword.includes(token)));
}

function pickRelevantMemories(input: CompanionReplyInput): string[] {
  const text = normalizeText(input.userText);
  const memories = input.semanticMemories ?? [];
  if (!text || looksLikeQuestion(text) && containsAny(text, ["天气", "太阳", "下雨"])) {
    return [];
  }

  const wantsProfile = hasImmediateProfileDisclosure(text);
  const wantsPreference = containsAny(text, ["喜欢", "不喜欢", "习惯", "平时", "吃", "喝"]);
  const wantsRecentState = containsAny(text, ["最近", "今天", "刚刚", "刚才", "难受", "头晕", "闷", "担心"]);

  return memories
    .filter((memory) => memory.expiresAt == null || memory.expiresAt > Date.now())
    .map((memory) => {
      if (memory.memoryLayer === "profile" && !wantsProfile && !hasKeywordOverlap(text, memory)) return null;
      if (memory.memoryLayer === "preference" && !wantsPreference && !hasKeywordOverlap(text, memory)) return null;
      if (memory.memoryLayer === "recent_state" && !wantsRecentState && !hasKeywordOverlap(text, memory)) return null;
      const overlap = memory.keywords.filter((keyword) => text.includes(keyword)).length;
      const score = overlap * 3 + (hasKeywordOverlap(text, memory) ? 2 : 0) + Math.min(memory.evidenceCount, 2);
      if (score <= 1) return null;
      return { summary: normalizeText(memory.compressedSummary || memory.summary), score };
    })
    .filter((item): item is { summary: string; score: number } => Boolean(item && item.summary))
    .sort((left, right) => right.score - left.score)
    .slice(0, 2)
    .map((item) => item.summary);
}

function buildUsedContext(input: CompanionReplyInput) {
  const context = input.conversationContext;
  return {
    preferredName: normalizeText(context?.preferredName) || normalizeText(input.profile?.preferredName) || null,
    communicationStyle: normalizeCommunicationStyle(context?.communicationStyle) || input.profile?.communicationStyle || null,
    nextCarePlan: pickNextCarePlan(input.carePlans),
    topicBriefTitles: input.topicBriefs.slice(0, 2).map((item) => normalizeText(item.title)).filter(Boolean),
    commonTopics: (context?.commonTopics ?? []).map(normalizeText).filter(Boolean).slice(0, 4),
    tabooTopics: (context?.tabooTopics ?? []).map(normalizeText).filter(Boolean).slice(0, 4),
    memoryHighlights: [
      ...pickRelevantMemories(input),
      ...((context?.memoryHighlights ?? []).map(normalizeText).filter(Boolean)),
    ].filter((item, index, list) => list.indexOf(item) === index).slice(0, 2),
    emotionHint: normalizeText(context?.emotionHint) || null,
    reminderHint: normalizeText(context?.reminderHint) || null,
    contactHint: normalizeText(context?.contactHint) || null,
    recentConversationHint: normalizeText(context?.recentConversationHint) || null,
  };
}

function buildFastLocalCompanionReply(input: CompanionReplyInput, usedContext: CompanionReplyResult["usedContext"]): CompanionReplyResult {
  const text = normalizeText(input.userText);
  const reply = (() => {
    if (text.includes("谢谢")) return "不用客气，我在。";
    if (text.includes("晚安")) return "晚安，早点休息。";
    if (text.includes("早安")) return "早安，慢慢开始今天。";
    if (text.includes("拜拜")) return "好，回头再聊。";
    return "好，我在。";
  })();

  return {
    mode: "fallback",
    provider: "local",
    model: "local-rule-fallback",
    reply,
    summary: "Returned a short local acknowledgement for a very brief message.",
    usedContext,
    generatedAt: getNowIso(),
  };
}

function buildCompanionStyleInstruction(communicationStyle: string | null): string {
  if (communicationStyle === "confident_steady") {
    return "像熟悉的朋友或晚辈那样自然聊天，利落、真诚，不端着。";
  }
  return "像熟悉的晚辈那样自然聊天，温和、平实，不用照护口吻。";
}

function buildCompanionSystemPrompt(input: CompanionReplyInput, usedContext: CompanionReplyResult["usedContext"]): string {
  const profile = input.profile;
  const personaPrompt = normalizePromptBlock(input.conversationContext?.personaPrompt);
  const instructions = [
    "你是“小芳在家”的聊天助手，目标是像普通熟人一样自然接话。",
    buildCompanionStyleInstruction(usedContext.communicationStyle),
    "不要提醒对方是老人，不要暗示对方脆弱、需要被照顾。",
    "不要机械追问心情、家庭、基本资料；除非用户主动提起或明确需要帮助。",
    "不要把记忆生硬塞回话里，只有和当前话题高度相关时才轻轻带一句。",
    "多数回复控制在 1 到 3 句，不要套模板，不要每次都反问。",
    "如果提到身体不舒服，只表达关心和轻量建议，不做诊断。",
    "如果用户表示抗拒或不想聊，立刻收口并换成轻松、中性的接话。",
    "输出必须是 JSON：{\"reply\":\"...\",\"summary\":\"...\"}。",
  ];

  const contextBlocks = [
    usedContext.preferredName ? `称呼偏好：${usedContext.preferredName}` : "",
    profile?.routineSummary ? `作息摘要：${normalizeText(profile.routineSummary)}` : "",
    usedContext.commonTopics.length > 0 ? `常见自然话题：${usedContext.commonTopics.join("、")}` : "",
    usedContext.tabooTopics.length > 0 ? `避免主动带出：${usedContext.tabooTopics.join("、")}` : "",
    usedContext.memoryHighlights.length > 0 ? `可选相关记忆：${usedContext.memoryHighlights.join("；")}` : "",
    usedContext.emotionHint ? `情绪提示：${usedContext.emotionHint}` : "",
    usedContext.reminderHint ? `提醒线索：${usedContext.reminderHint}` : "",
    usedContext.contactHint ? `联系人线索：${usedContext.contactHint}` : "",
    usedContext.recentConversationHint ? `最近对话衔接：${usedContext.recentConversationHint}` : "",
    usedContext.nextCarePlan ? `下一条提醒：${usedContext.nextCarePlan}` : "",
    input.topicBriefs.length > 0 ? `可引用的外部话题：${input.topicBriefs.slice(0, 2).map((item) => `${normalizeText(item.title)}(${normalizeText(item.summary)})`).join("；")}` : "",
    personaPrompt ? `附加设定：${personaPrompt}` : "",
  ].filter(Boolean);

  return [...instructions, ...contextBlocks].join("\n");
}

function buildCompanionUserPrompt(input: CompanionReplyInput): string {
  const recentMessages = input.recentMessages.slice(-4).map((message) => `${message.role === "assistant" ? "小芳" : "用户"}：${normalizeText(message.content)}`);
  return [
    recentMessages.length > 0 ? `最近对话：\n${recentMessages.join("\n")}` : "",
    `当前用户消息：${normalizeText(input.userText)}`,
    "请直接返回 JSON，不要带 Markdown。",
  ].filter(Boolean).join("\n\n");
}

function buildFallbackReply(input: CompanionReplyInput, usedContext: CompanionReplyResult["usedContext"]): string {
  const text = normalizeText(input.userText);
  if (!text) {
    return "我在，你慢慢说。";
  }
  if (hasResistanceSignal(text)) {
    return "好，那这话先放下，我们不聊这些了。";
  }
  if (containsAny(text, ["难受", "头晕", "不舒服", "疼"])) {
    return "听着有点难受，先别硬撑着。要是越发不舒服，尽快让家里人知道。";
  }
  if (containsAny(text, ["孤单", "难过", "烦", "闷", "担心", "焦虑"])) {
    return "听着这会儿心里有点堵，我先陪你把这口气缓下来。";
  }
  if (containsAny(text, ["天气", "太阳", "下雨"])) {
    return looksLikeQuestion(text)
      ? "天气这类信息我这边未必实时准，出门前看一下手机天气更稳妥。"
      : "这会儿天气确实挺显眼。";
  }
  if (containsAny(text, ["儿子", "女儿", "孩子", "孙子", "老伴", "家里"])) {
    return "听着家里这阵子有点让人挂心，会惦记是正常的。";
  }
  const relevantMemory = usedContext.memoryHighlights[0];
  if (relevantMemory) {
    return `我还记着你提过 ${relevantMemory.replace(/^偏好：|^家人：|^情况：|^近况：|^情绪：|^身体：/, "")}，这会儿我接着听你说。`;
  }
  return "我听着呢，这事可以慢慢聊。";
}

function readConfig(): AiGatewayConfig {
  ensureLocalEnvLoaded();
  const apiKey = process.env.AI_API_KEY?.trim() || null;
  const baseUrl = process.env.AI_BASE_URL?.trim().replace(/\/+$/, "") || null;
  const model = process.env.AI_MODEL?.trim() || null;
  const timeoutMs = Number(process.env.AI_TIMEOUT_MS ?? "6500");
  const configured = Boolean(apiKey && baseUrl && model);
  return {
    provider: configured ? "openai-compatible" : "disabled",
    apiKey,
    baseUrl,
    model,
    timeoutMs: Number.isFinite(timeoutMs) && timeoutMs > 0 ? timeoutMs : 6500,
  };
}

export function getAiRuntimeStatus(): AiRuntimeStatus {
  const config = readConfig();
  return {
    provider: config.provider,
    configured: config.provider !== "disabled",
    model: config.model,
    baseUrl: config.baseUrl,
    timeoutMs: config.timeoutMs,
  };
}

function extractTextContent(content: unknown): string {
  if (typeof content === "string") return content.trim();
  if (Array.isArray(content)) {
    return content
      .map((item) => {
        if (!item || typeof item !== "object") return "";
        const chunk = item as Record<string, unknown>;
        return typeof chunk.text === "string" ? chunk.text : "";
      })
      .join("")
      .trim();
  }
  return "";
}

function extractJsonObject<T>(raw: string): T | null {
  const trimmed = raw.trim();
  if (!trimmed) return null;
  const fenced = trimmed.match(/```json\s*([\s\S]*?)```/i)?.[1] ?? trimmed;
  const direct = fenced.match(/\{[\s\S]*\}/)?.[0] ?? fenced;
  try {
    return JSON.parse(direct) as T;
  } catch {
    return null;
  }
}

async function callOpenAiCompatible(messages: OpenAiMessage[], timeoutMs: number): Promise<{ content: string; model: string }> {
  const config = readConfig();
  if (config.provider === "disabled" || !config.apiKey || !config.baseUrl || !config.model) {
    throw new Error("ai_gateway_not_configured");
  }

  const controller = new AbortController();
  const timeoutHandle = setTimeout(() => controller.abort(), timeoutMs);

  try {
    const response = await fetch(`${config.baseUrl}/chat/completions`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${config.apiKey}`,
      },
      body: JSON.stringify({
        model: config.model,
        temperature: 0.7,
        messages,
      }),
      signal: controller.signal,
    });

    if (!response.ok) {
      throw new Error(`ai_gateway_http_${response.status}`);
    }

    const data = await response.json() as {
      choices?: Array<{ message?: { content?: unknown } }>;
      model?: string;
    };
    const content = extractTextContent(data.choices?.[0]?.message?.content);
    if (!content) {
      throw new Error("ai_gateway_empty_content");
    }

    return {
      content,
      model: typeof data.model === "string" && data.model.trim() ? data.model : config.model,
    };
  } finally {
    clearTimeout(timeoutHandle);
  }
}

function shouldRetryCompanionAttempt(error: unknown, attempt: number): boolean {
  if (attempt >= 1) return false;
  const message = error instanceof Error ? error.message : String(error);
  return message.includes("AbortError") ||
    message.includes("aborted") ||
    message.includes("ai_gateway_http_429") ||
    message.includes("ai_gateway_http_5");
}

async function callOpenAiCompatibleWithRetry(messages: OpenAiMessage[], timeouts: number[]): Promise<{ content: string; model: string }> {
  let lastError: unknown = null;
  for (let attempt = 0; attempt < timeouts.length; attempt += 1) {
    try {
      return await callOpenAiCompatible(messages, timeouts[attempt]);
    } catch (error) {
      lastError = error;
      if (!shouldRetryCompanionAttempt(error, attempt)) {
        throw error;
      }
    }
  }
  throw lastError instanceof Error ? lastError : new Error("ai_gateway_failed");
}

export async function generateCompanionReply(input: CompanionReplyInput): Promise<CompanionReplyResult> {
  const config = readConfig();
  const usedContext = buildUsedContext(input);

  if (shouldUseFastLocalCompanionReply(input)) {
    return buildFastLocalCompanionReply(input, usedContext);
  }

  if (config.provider === "disabled" || !config.model) {
    return {
      mode: "fallback",
      provider: "local",
      model: "local-rule-fallback",
      reply: buildFallbackReply(input, usedContext),
      summary: "Returned a deterministic fallback reply because the live model is not configured.",
      usedContext,
      generatedAt: getNowIso(),
    };
  }

  const timeouts = [Math.min(config.timeoutMs, 5200), Math.min(config.timeoutMs, 2800)].filter((value, index, list) => value > 0 && list.indexOf(value) === index);
  const messages: OpenAiMessage[] = [
    { role: "system", content: buildCompanionSystemPrompt(input, usedContext) },
    { role: "user", content: buildCompanionUserPrompt(input) },
  ];

  try {
    const result = await callOpenAiCompatibleWithRetry(messages, timeouts);
    const parsed = extractJsonObject<{ reply?: string; summary?: string }>(result.content);
    const reply = normalizeText(parsed?.reply) || normalizeText(result.content);
    if (!reply) {
      throw new Error("ai_gateway_missing_reply");
    }

    return {
      mode: "live",
      provider: "openai-compatible",
      model: result.model,
      reply,
      summary: normalizeText(parsed?.summary) || "Generated by the live model through the AI gateway.",
      usedContext,
      generatedAt: getNowIso(),
    };
  } catch (error) {
    logGatewayWarning("companion-reply-fallback", {
      seniorId: input.seniorId,
      model: config.model,
      timeoutMs: config.timeoutMs,
      message: error instanceof Error ? error.message : String(error),
    });
    return {
      mode: "fallback",
      provider: "local",
      model: "local-rule-fallback",
      reply: buildFallbackReply(input, usedContext),
      summary: "Returned a deterministic fallback reply because the live model timed out or failed.",
      usedContext,
      generatedAt: getNowIso(),
    };
  }
}

function buildTopicBriefFallback(input: TopicBriefInput): TopicBrief {
  const source = normalizeText(input.sourceText);
  const summary = source.length <= 80 ? source : `${source.slice(0, 80)}...`;
  return {
    topicId: randomUUID(),
    seniorId: input.seniorId,
    title: normalizeText(input.topicHint) || "日常话题",
    summary: summary || "整理了一条可继续聊天的话题。",
    sourceName: input.sourceName || "ai_gateway",
    sourceUrl: input.sourceUrl || "",
    riskFlags: [],
    generatedAt: getNowIso(),
  };
}

export async function generateTopicBrief(input: TopicBriefInput): Promise<TopicBriefResult> {
  const config = readConfig();
  if (config.provider === "disabled" || !config.model) {
    return {
      mode: "fallback",
      provider: "local",
      model: "local-rule-fallback",
      topicBrief: buildTopicBriefFallback(input),
      generatedAt: getNowIso(),
    };
  }

  const messages: OpenAiMessage[] = [
    {
      role: "system",
      content: [
        "请把输入内容整理成一个适合继续聊天的话题摘要。",
        "输出 JSON：{\"title\":\"...\",\"summary\":\"...\",\"riskFlags\":[\"...\"]}",
        "title 控制在 12 个字以内，summary 控制在 60 个字以内。",
      ].join("\n"),
    },
    {
      role: "user",
      content: [
        `topicHint: ${normalizeText(input.topicHint)}`,
        `sourceText: ${normalizeText(input.sourceText)}`,
        input.profile ? `preferredName: ${normalizeText(input.profile.preferredName)}` : "",
      ].filter(Boolean).join("\n"),
    },
  ];

  try {
    const result = await callOpenAiCompatible(messages, Math.min(config.timeoutMs, 5000));
    const parsed = extractJsonObject<{ title?: string; summary?: string; riskFlags?: string[] }>(result.content);
    const topicBrief: TopicBrief = {
      topicId: randomUUID(),
      seniorId: input.seniorId,
      title: normalizeText(parsed?.title) || normalizeText(input.topicHint) || "日常话题",
      summary: normalizeText(parsed?.summary) || normalizeText(input.sourceText).slice(0, 60) || "整理了一条可继续聊天的话题。",
      sourceName: input.sourceName || "ai_gateway",
      sourceUrl: input.sourceUrl || "",
      riskFlags: Array.isArray(parsed?.riskFlags) ? parsed!.riskFlags.map(normalizeText).filter(Boolean).slice(0, 4) : [],
      generatedAt: getNowIso(),
    };

    return {
      mode: "live",
      provider: "openai-compatible",
      model: result.model,
      topicBrief,
      generatedAt: getNowIso(),
    };
  } catch (error) {
    logGatewayWarning("topic-brief-fallback", {
      seniorId: input.seniorId,
      message: error instanceof Error ? error.message : String(error),
    });
    return {
      mode: "fallback",
      provider: "local",
      model: "local-rule-fallback",
      topicBrief: buildTopicBriefFallback(input),
      generatedAt: getNowIso(),
    };
  }
}
