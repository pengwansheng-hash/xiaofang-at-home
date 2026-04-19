export type ValidationStatus = "local_placeholder" | "pending_validation" | "validated" | "failed";

export type BindingMethod = "manual" | "scan";

export type FamilyBinding = {
  bindingId: string;
  seniorId: string;
  childId: string;
  relation: string;
  bindingCode: string;
  bindingMethod: BindingMethod;
  validationStatus: ValidationStatus;
  notificationPreference: "normal" | "important_only";
  boundAt: string;
  updatedAt: string;
};

export type ImportantContact = {
  id: string;
  name: string;
  relation: string;
  phone: string;
  priority: number;
};

export type SeniorProfile = {
  seniorId: string;
  preferredName: string;
  relationLabel: string;
  interests: string[];
  hobbies: string[];
  tabooTopics: string[];
  communicationStyle: "patient_gentle" | "confident_steady";
  routineSummary: string;
  personaTags: string[];
  importantContacts: ImportantContact[];
  updatedAt: string;
};

export type CarePlan = {
  planId: string;
  seniorId: string;
  title: string;
  schedule: string;
  frequency: string;
  channel: string;
  confirmRequired: boolean;
  source: "senior_app" | "family_web" | "system";
  status: "active" | "paused";
  updatedAt: string;
};

export type CarePlanEvent = {
  eventId: string;
  planId: string;
  eventType: "completed" | "snoozed" | "skipped" | "needs_help" | "sync";
  payload: Record<string, unknown>;
  createdAt: string;
};

export type TopicBrief = {
  topicId: string;
  seniorId: string;
  title: string;
  summary: string;
  sourceName: string;
  sourceUrl: string;
  riskFlags: string[];
  generatedAt: string;
};

export type SemanticMemoryRecord = {
  seniorId: string;
  id: string;
  memoryType: "Preference" | "Routine" | "Health" | "Family" | "Profile" | "Experience" | "Event" | "Emotion";
  memoryLayer: "profile" | "preference" | "recent_state";
  retention: "long_term" | "short_term";
  title: string;
  summary: string;
  compressedSummary: string;
  keywords: string[];
  sourceText: string;
  confidence: number;
  createdAt: number;
  updatedAt: number;
  sourceCount: number;
  evidenceCount: number;
  lastAccessedAt: number;
  lastConfirmedAt: number;
  expiresAt: number | null;
};

export type ServiceStore = {
  bindings: FamilyBinding[];
  profiles: SeniorProfile[];
  carePlans: CarePlan[];
  carePlanEvents: CarePlanEvent[];
  topicBriefs: TopicBrief[];
  semanticMemories: SemanticMemoryRecord[];
};

export type SeniorSyncPacket = {
  seniorId: string;
  profile: SeniorProfile | null;
  binding: FamilyBinding | null;
  carePlans: CarePlan[];
  latestEvents: CarePlanEvent[];
  topicBriefs: TopicBrief[];
  semanticMemories: SemanticMemoryRecord[];
  generatedAt: string;
};

export type AiRuntimeResponse = {
  provider: "disabled" | "openai-compatible";
  configured: boolean;
  model: string | null;
  baseUrl: string | null;
  timeoutMs: number;
};

export type CompanionReplyApiResponse = {
  mode: "live" | "fallback";
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

export type SemanticMemorySyncApiResponse = {
  seniorId: string;
  savedCount: number;
  generatedAt: string;
};

export type TopicBriefGenerateApiResponse = {
  mode: "live" | "fallback";
  provider: string;
  model: string;
  topicBrief: TopicBrief;
  generatedAt: string;
};
