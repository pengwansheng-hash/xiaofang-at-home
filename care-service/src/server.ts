import { createServer, type IncomingMessage, type ServerResponse } from "node:http";
import { randomUUID } from "node:crypto";
import {
  generateCompanionReply,
  generateTopicBrief,
  getAiRuntimeStatus,
  type CompanionReplyConversationContext,
} from "./ai-gateway.js";
import { readStore, writeStore } from "./store.js";
import type {
  CarePlan,
  CarePlanEvent,
  FamilyBinding,
  SemanticMemoryRecord,
  SeniorProfile,
  SeniorSyncPacket,
} from "./types.js";

type JsonRecord = Record<string, unknown>;

const port = Number(process.env.PORT ?? "3301");

function sendJson(response: ServerResponse, statusCode: number, data: unknown) {
  response.statusCode = statusCode;
  response.setHeader("Content-Type", "application/json; charset=utf-8");
  response.setHeader("Access-Control-Allow-Origin", "*");
  response.setHeader("Access-Control-Allow-Headers", "Content-Type");
  response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,OPTIONS");
  response.end(JSON.stringify(data, null, 2));
}

async function readJsonBody(request: IncomingMessage): Promise<JsonRecord> {
  const chunks: Buffer[] = [];

  for await (const chunk of request) {
    chunks.push(Buffer.from(chunk));
  }

  if (chunks.length === 0) {
    return {};
  }

  return JSON.parse(Buffer.concat(chunks).toString("utf8")) as JsonRecord;
}

function getNowIso() {
  return new Date().toISOString();
}

function notFound(response: ServerResponse) {
  sendJson(response, 404, { error: "not_found" });
}

function matchPath(pathname: string, pattern: RegExp) {
  return pathname.match(pattern);
}

function normalizeRecentMessages(value: unknown): Array<{ role: "user" | "assistant"; content: string }> {
  if (!Array.isArray(value)) {
    return [];
  }

  return value
    .map((item) => {
      if (typeof item === "string") {
        return {
          role: "user" as const,
          content: item
        };
      }

      if (typeof item === "object" && item) {
        const record = item as Record<string, unknown>;
        const role = record.role === "assistant" ? "assistant" : "user";
        const content = String(record.content ?? "").trim();
        if (!content) {
          return null;
        }
        return { role, content };
      }

      return null;
    })
    .filter((item): item is { role: "user" | "assistant"; content: string } => Boolean(item));
}

function normalizeStringArray(value: unknown): string[] {
  if (!Array.isArray(value)) {
    return [];
  }

  return value
    .map((item) => String(item ?? "").trim())
    .filter((item) => item.length > 0);
}

function normalizePositiveNumber(value: unknown, fallback: number): number {
  const numericValue = typeof value === "number" ? value : Number(value);
  return Number.isFinite(numericValue) && numericValue > 0 ? numericValue : fallback;
}

function normalizeSemanticMemoryType(value: unknown): SemanticMemoryRecord["memoryType"] | null {
  const trimmed = String(value ?? "").trim();
  if (
    trimmed === "Preference" ||
    trimmed === "Routine" ||
    trimmed === "Health" ||
    trimmed === "Family" ||
    trimmed === "Profile" ||
    trimmed === "Experience" ||
    trimmed === "Event" ||
    trimmed === "Emotion"
  ) {
    return trimmed;
  }
  return null;
}

function normalizeMemoryLayer(value: unknown): SemanticMemoryRecord["memoryLayer"] {
  const trimmed = String(value ?? "").trim();
  if (trimmed === "profile" || trimmed === "Profile") {
    return "profile";
  }
  if (trimmed === "preference" || trimmed === "Preference") {
    return "preference";
  }
  if (trimmed === "recent_state" || trimmed === "RecentState") {
    return "recent_state";
  }
  return "profile";
}

function normalizeMemoryRetention(value: unknown): SemanticMemoryRecord["retention"] {
  const trimmed = String(value ?? "").trim();
  if (trimmed === "short_term" || trimmed === "ShortTerm") {
    return "short_term";
  }
  if (trimmed === "long_term" || trimmed === "LongTerm") {
    return "long_term";
  }
  return "long_term";
}

function normalizeSemanticMemories(
  value: unknown,
  seniorId: string,
): SemanticMemoryRecord[] {
  if (!Array.isArray(value)) {
    return [];
  }

  return value
    .map((item) => {
      if (!item || typeof item !== "object") {
        return null;
      }

      const record = item as Record<string, unknown>;
      const memoryType = normalizeSemanticMemoryType(record.memoryType);
      const id = String(record.id ?? "").trim();
      if (!memoryType || !id) {
        return null;
      }

      const createdAt = normalizePositiveNumber(record.createdAt, Date.now());
      const updatedAt = normalizePositiveNumber(record.updatedAt, createdAt);
      const lastAccessedAt = normalizePositiveNumber(record.lastAccessedAt, createdAt);
      return {
        seniorId,
        id,
        memoryType,
        memoryLayer: normalizeMemoryLayer(record.memoryLayer),
        retention: normalizeMemoryRetention(record.retention),
        title: String(record.title ?? "").trim() || "未命名记忆",
        summary: String(record.summary ?? "").trim() || String(record.compressedSummary ?? "").trim() || "未命名记忆",
        compressedSummary: String(record.compressedSummary ?? "").trim() || String(record.summary ?? "").trim(),
        keywords: normalizeStringArray(record.keywords).slice(0, 12),
        sourceText: String(record.sourceText ?? "").trim(),
        confidence: normalizePositiveNumber(record.confidence, 0.5),
        createdAt,
        updatedAt,
        sourceCount: Math.max(1, Math.floor(normalizePositiveNumber(record.sourceCount, 1))),
        evidenceCount: Math.max(1, Math.floor(normalizePositiveNumber(record.evidenceCount, normalizePositiveNumber(record.sourceCount, 1)))),
        lastAccessedAt,
        lastConfirmedAt: normalizePositiveNumber(record.lastConfirmedAt, updatedAt),
        expiresAt: (() => {
          const raw = typeof record.expiresAt === "number" ? record.expiresAt : Number(record.expiresAt);
          return Number.isFinite(raw) && raw > 0 ? raw : null;
        })(),
      };
    })
    .filter((item): item is SemanticMemoryRecord => Boolean(item));
}

function replaceSemanticMemories(store: ReturnType<typeof readStore>, seniorId: string, memories: SemanticMemoryRecord[]) {
  const remaining = store.semanticMemories.filter((item) => item.seniorId !== seniorId);
  store.semanticMemories = [...remaining, ...memories].sort((left, right) => right.lastAccessedAt - left.lastAccessedAt);
}

function normalizeConversationContext(value: unknown): CompanionReplyConversationContext | null {
  if (!value || typeof value !== "object") {
    return null;
  }

  const record = value as Record<string, unknown>;
  return {
    preferredName: String(record.preferredName ?? "").trim() || null,
    communicationStyle: String(record.communicationStyle ?? "").trim() || null,
    personaPrompt: String(record.personaPrompt ?? "").trim() || null,
    commonTopics: normalizeStringArray(record.commonTopics),
    tabooTopics: normalizeStringArray(record.tabooTopics),
    emotionHint: String(record.emotionHint ?? "").trim() || null,
    memoryHighlights: normalizeStringArray(record.memoryHighlights),
    reminderHint: String(record.reminderHint ?? "").trim() || null,
    contactHint: String(record.contactHint ?? "").trim() || null,
    recentConversationHint: String(record.recentConversationHint ?? "").trim() || null,
    collectionHint: String(record.collectionHint ?? "").trim() || null,
  };
}

const server = createServer(async (request, response) => {
  if (!request.url || !request.method) {
    sendJson(response, 400, { error: "invalid_request" });
    return;
  }

  if (request.method === "OPTIONS") {
    sendJson(response, 204, {});
    return;
  }

  const url = new URL(request.url, `http://${request.headers.host ?? "127.0.0.1"}`);
  const pathname = url.pathname;
  const store = readStore();

  if (request.method === "GET" && pathname === "/api/health") {
    sendJson(response, 200, {
      status: "ok",
      service: "xiaofang-care-service",
      storageMode: "json-file",
      now: getNowIso()
    });
    return;
  }

  if (request.method === "GET" && pathname === "/api/ai/runtime") {
    sendJson(response, 200, getAiRuntimeStatus());
    return;
  }

  if (request.method === "POST" && pathname === "/api/bindings") {
    const body = await readJsonBody(request);
    const binding: FamilyBinding = {
      bindingId: randomUUID(),
      seniorId: String(body.seniorId ?? "senior-unknown"),
      childId: String(body.childId ?? "child-unknown"),
      relation: String(body.relation ?? "family"),
      bindingCode: String(body.bindingCode ?? ""),
      bindingMethod: body.bindingMethod === "scan" ? "scan" : "manual",
      validationStatus: "pending_validation",
      notificationPreference: body.notificationPreference === "normal" ? "normal" : "important_only",
      boundAt: getNowIso(),
      updatedAt: getNowIso()
    };

    store.bindings.unshift(binding);
    writeStore(store);
    sendJson(response, 201, binding);
    return;
  }

  if (request.method === "POST" && pathname === "/api/bindings/scan-preview") {
    const body = await readJsonBody(request);
    sendJson(response, 200, {
      bindingCode: String(body.bindingCode ?? "593204"),
      bindingMethod: "scan",
      validationStatus: "pending_validation",
      note: "This is a local service-side preview placeholder for the scan binding flow."
    });
    return;
  }

  const bindingMatch = matchPath(pathname, /^\/api\/bindings\/([^/]+)$/);
  if (request.method === "GET" && bindingMatch) {
    const binding = store.bindings.find((item) => item.bindingId === bindingMatch[1]);
    if (!binding) {
      notFound(response);
      return;
    }
    sendJson(response, 200, binding);
    return;
  }

  const seniorMatch = matchPath(pathname, /^\/api\/seniors\/([^/]+)$/);
  if (request.method === "GET" && seniorMatch) {
    const seniorId = seniorMatch[1];
    const profile = store.profiles.find((item) => item.seniorId === seniorId);
    const binding = store.bindings.find((item) => item.seniorId === seniorId);
    const carePlans = store.carePlans.filter((item) => item.seniorId === seniorId);

    if (!profile) {
      notFound(response);
      return;
    }

    sendJson(response, 200, {
      profile,
      binding,
      carePlans
    });
    return;
  }

  const seniorSyncPacketMatch = matchPath(pathname, /^\/api\/seniors\/([^/]+)\/sync-packet$/);
  if (request.method === "GET" && seniorSyncPacketMatch) {
    const seniorId = seniorSyncPacketMatch[1];
    const profile = store.profiles.find((item) => item.seniorId === seniorId) ?? null;
    const binding = store.bindings.find((item) => item.seniorId === seniorId) ?? null;
    const carePlans = store.carePlans.filter((item) => item.seniorId === seniorId);
    const planIds = new Set(carePlans.map((item) => item.planId));
    const latestEvents = store.carePlanEvents
      .filter((item) => planIds.has(item.planId))
      .sort((left, right) => right.createdAt.localeCompare(left.createdAt))
      .slice(0, 10);
    const topicBriefs = store.topicBriefs.filter((item) => item.seniorId === seniorId);
    const semanticMemories = store.semanticMemories
      .filter((item) => item.seniorId === seniorId)
      .sort((left, right) => right.lastAccessedAt - left.lastAccessedAt)
      .slice(0, 40);

    const packet: SeniorSyncPacket = {
      seniorId,
      profile,
      binding,
      carePlans,
      latestEvents,
      topicBriefs,
      semanticMemories,
      generatedAt: getNowIso()
    };

    sendJson(response, 200, packet);
    return;
  }

  if (request.method === "PUT" && seniorMatch) {
    const seniorId = seniorMatch[1];
    const body = await readJsonBody(request);
    const index = store.profiles.findIndex((item) => item.seniorId === seniorId);

    if (index === -1) {
      notFound(response);
      return;
    }

    const current = store.profiles[index];
    const nextProfile: SeniorProfile = {
      ...current,
      preferredName: String(body.preferredName ?? current.preferredName),
      relationLabel: String(body.relationLabel ?? current.relationLabel),
      interests: Array.isArray(body.interests) ? body.interests.map(String) : current.interests,
      hobbies: Array.isArray(body.hobbies) ? body.hobbies.map(String) : current.hobbies,
      tabooTopics: Array.isArray(body.tabooTopics) ? body.tabooTopics.map(String) : current.tabooTopics,
      communicationStyle:
        body.communicationStyle === "confident_steady" ? "confident_steady" : current.communicationStyle,
      routineSummary: String(body.routineSummary ?? current.routineSummary),
      personaTags: Array.isArray(body.personaTags) ? body.personaTags.map(String) : current.personaTags,
      importantContacts: Array.isArray(body.importantContacts)
        ? (body.importantContacts as SeniorProfile["importantContacts"])
        : current.importantContacts,
      updatedAt: getNowIso()
    };

    store.profiles[index] = nextProfile;
    writeStore(store);
    sendJson(response, 200, nextProfile);
    return;
  }

  const seniorCarePlansMatch = matchPath(pathname, /^\/api\/seniors\/([^/]+)\/care-plans$/);
  if (request.method === "GET" && seniorCarePlansMatch) {
    const seniorId = seniorCarePlansMatch[1];
    sendJson(
      response,
      200,
      store.carePlans.filter((item) => item.seniorId === seniorId)
    );
    return;
  }

  const topicBriefsMatch = matchPath(pathname, /^\/api\/seniors\/([^/]+)\/topic-briefs$/);
  if (request.method === "GET" && topicBriefsMatch) {
    const seniorId = topicBriefsMatch[1];
    sendJson(
      response,
      200,
      store.topicBriefs.filter((item) => item.seniorId === seniorId)
    );
    return;
  }

  if (request.method === "POST" && pathname === "/api/ai/companion-reply") {
    const body = await readJsonBody(request);
    const seniorId = String(body.seniorId ?? "");
    const userText = String(body.userText ?? "").trim();

    if (!seniorId || !userText) {
      sendJson(response, 400, { error: "seniorId_and_userText_required" });
      return;
    }

    const profile = store.profiles.find((item) => item.seniorId === seniorId) ?? null;
    const carePlans = store.carePlans.filter((item) => item.seniorId === seniorId);
    const topicBriefs = store.topicBriefs.filter((item) => item.seniorId === seniorId);
    const semanticMemories = store.semanticMemories
      .filter((item) => item.seniorId === seniorId)
      .sort((left, right) => right.lastAccessedAt - left.lastAccessedAt)
      .slice(0, 40);
    const recentMessages = normalizeRecentMessages(body.recentMessages);
    const conversationContext = normalizeConversationContext(body.conversationContext);

    const result = await generateCompanionReply({
      seniorId,
      userText,
      recentMessages,
      profile,
      carePlans,
      topicBriefs,
      semanticMemories,
      conversationContext,
    });

    sendJson(response, 200, result);
    return;
  }

  const seniorSemanticMemoriesMatch = matchPath(pathname, /^\/api\/seniors\/([^/]+)\/semantic-memories$/);
  if (request.method === "GET" && seniorSemanticMemoriesMatch) {
    const seniorId = seniorSemanticMemoriesMatch[1];
    sendJson(
      response,
      200,
      store.semanticMemories
        .filter((item) => item.seniorId === seniorId)
        .sort((left, right) => right.lastAccessedAt - left.lastAccessedAt),
    );
    return;
  }

  if (request.method === "PUT" && seniorSemanticMemoriesMatch) {
    const seniorId = seniorSemanticMemoriesMatch[1];
    const body = await readJsonBody(request);
    const memories = normalizeSemanticMemories(body.semanticMemories ?? body.memories, seniorId);
    replaceSemanticMemories(store, seniorId, memories);
    writeStore(store);
    sendJson(response, 200, {
      seniorId,
      savedCount: memories.length,
      generatedAt: getNowIso(),
    });
    return;
  }

  if (request.method === "POST" && pathname === "/api/ai/topic-briefs/generate") {
    const body = await readJsonBody(request);
    const seniorId = String(body.seniorId ?? "");
    const topicHint = String(body.topicHint ?? "").trim();
    const sourceText = String(body.sourceText ?? "").trim();
    const sourceName = String(body.sourceName ?? "ai_gateway");
    const sourceUrl = String(body.sourceUrl ?? "");

    if (!seniorId) {
      sendJson(response, 400, { error: "seniorId_required" });
      return;
    }

    const profile = store.profiles.find((item) => item.seniorId === seniorId) ?? null;
    const result = await generateTopicBrief({
      seniorId,
      topicHint,
      sourceText,
      sourceName,
      sourceUrl,
      profile
    });

    store.topicBriefs.unshift(result.topicBrief);
    writeStore(store);
    sendJson(response, 201, result);
    return;
  }

  if (request.method === "POST" && seniorCarePlansMatch) {
    const seniorId = seniorCarePlansMatch[1];
    const body = await readJsonBody(request);
    const carePlan: CarePlan = {
      planId: randomUUID(),
      seniorId,
      title: String(body.title ?? "Untitled reminder"),
      schedule: String(body.schedule ?? "--:--"),
      frequency: String(body.frequency ?? "daily"),
      channel: String(body.channel ?? "tts"),
      confirmRequired: Boolean(body.confirmRequired ?? true),
      source: body.source === "senior_app" || body.source === "system" ? body.source : "family_web",
      status: body.status === "paused" ? "paused" : "active",
      updatedAt: getNowIso()
    };

    store.carePlans.unshift(carePlan);
    writeStore(store);
    sendJson(response, 201, carePlan);
    return;
  }

  const carePlanMatch = matchPath(pathname, /^\/api\/care-plans\/([^/]+)$/);
  if (request.method === "PUT" && carePlanMatch) {
    const planId = carePlanMatch[1];
    const body = await readJsonBody(request);
    const index = store.carePlans.findIndex((item) => item.planId === planId);

    if (index === -1) {
      notFound(response);
      return;
    }

    const current = store.carePlans[index];
    const nextPlan: CarePlan = {
      ...current,
      title: String(body.title ?? current.title),
      schedule: String(body.schedule ?? current.schedule),
      frequency: String(body.frequency ?? current.frequency),
      channel: String(body.channel ?? current.channel),
      confirmRequired: Boolean(body.confirmRequired ?? current.confirmRequired),
      status: body.status === "paused" ? "paused" : "active",
      updatedAt: getNowIso()
    };

    store.carePlans[index] = nextPlan;
    writeStore(store);
    sendJson(response, 200, nextPlan);
    return;
  }

  const carePlanEventsMatch = matchPath(pathname, /^\/api\/care-plans\/([^/]+)\/events$/);
  if (request.method === "POST" && carePlanEventsMatch) {
    const planId = carePlanEventsMatch[1];
    const body = await readJsonBody(request);
    const event: CarePlanEvent = {
      eventId: randomUUID(),
      planId,
      eventType:
        body.eventType === "completed" ||
        body.eventType === "snoozed" ||
        body.eventType === "skipped" ||
        body.eventType === "needs_help"
          ? body.eventType
          : "sync",
      payload: typeof body.payload === "object" && body.payload ? (body.payload as Record<string, unknown>) : {},
      createdAt: getNowIso()
    };

    store.carePlanEvents.unshift(event);
    writeStore(store);
    sendJson(response, 201, event);
    return;
  }

  const seniorEventsMatch = matchPath(pathname, /^\/api\/seniors\/([^/]+)\/care-plan-events$/);
  if (request.method === "GET" && seniorEventsMatch) {
    const seniorId = seniorEventsMatch[1];
    const planIds = new Set(store.carePlans.filter((item) => item.seniorId === seniorId).map((item) => item.planId));
    sendJson(
      response,
      200,
      store.carePlanEvents.filter((item) => planIds.has(item.planId))
    );
    return;
  }

  notFound(response);
});

const host = process.env.CARE_SERVICE_HOST ?? "0.0.0.0";
server.listen(port, host, () => {
  console.log(`xiaofang care service listening on http://${host}:${port}`);
});

