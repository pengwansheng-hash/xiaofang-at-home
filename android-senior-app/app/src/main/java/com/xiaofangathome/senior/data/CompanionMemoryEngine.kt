package com.xiaofangathome.senior.data

import java.util.Locale

data class CompanionContextRecall(
    val semanticMemories: List<SemanticMemoryItem>,
    val profileHint: String? = null,
    val taskHint: String? = null,
    val recentHint: String? = null,
    val emotionHint: String? = null,
    val collectionHint: String? = null,
)

data class CompanionReplyDraft(
    val intentLabel: String,
    val primary: String,
    val supportingHints: List<String>,
    val followUp: String? = null,
    val retrievedMemoryIds: List<String>,
)

object CompanionMemoryEngine {
    private const val SHORT_TERM_WINDOW_MS = 3L * 24 * 60 * 60 * 1000
    private const val EVENT_WINDOW_MS = 7L * 24 * 60 * 60 * 1000

    private val shortAckKeywords = listOf(
        "嗯", "哦", "好", "好的", "行", "可以", "收到", "知道了", "谢谢", "哈哈", "拜拜", "晚安", "早安",
    )
    private val resistanceKeywords = listOf(
        "别问", "别再问", "问这些干什么", "不想说", "不想聊", "烦不烦", "换个话题", "别打听",
    )
    private val contactKeywords = listOf("联系", "打电话", "给他打", "给她打", "拨一下", "叫家里人")
    private val jokeKeywords = listOf("笑话", "逗我", "乐一乐", "讲个段子")
    private val reminderKeywords = listOf("提醒", "记得", "吃药", "喝水", "明天", "下午", "晚上", "几点")
    private val weatherKeywords = listOf("天气", "太阳", "下雨", "刮风", "阴天", "热", "冷", "晴")
    private val smallTalkKeywords = listOf("天气不错", "今天太阳", "今天天气", "吃了吗", "睡了吗", "起床了")
    private val stablePreferenceKeywords = listOf("喜欢", "爱吃", "爱喝", "不喜欢", "不爱", "习惯", "平时都", "一直都")
    private val routineKeywords = listOf("早上", "中午", "晚上", "每天", "平时", "按时", "睡觉", "散步", "吃药", "喝水")
    private val longTermHealthKeywords = listOf("高血压", "糖尿病", "心脏", "膝盖", "腰", "关节", "老毛病")
    private val recentHealthKeywords = listOf("头晕", "胸口", "难受", "不舒服", "疼", "乏力", "咳嗽", "没劲")
    private val lowMoodKeywords = listOf("孤单", "难过", "委屈", "烦", "闷", "心里堵", "没意思", "不想动")
    private val worryKeywords = listOf("担心", "着急", "焦虑", "心慌", "放不下")
    private val lightMoodKeywords = listOf("心情不错", "挺高兴", "轻松", "还行", "挺好")
    private val familyKeywords = listOf("儿子", "女儿", "孩子", "孙子", "孙女", "老伴", "家里")
    private val profileKeywords = listOf("一个人住", "独居", "跟老伴住", "和孩子住", "退休", "自己住")
    private val experienceKeywords = listOf("以前", "年轻时", "上班", "工作", "当兵", "下乡", "厂里")

    fun shouldUseFastLocalReply(userText: String): Boolean = shouldUseFastLocalReplyV2(userText)

    fun shouldExtractSemanticMemory(userText: String): Boolean {
        val cleanText = userText.trim()
        if (cleanText.isBlank()) return false
        if (shouldUseFastLocalReplyV2(cleanText)) return false
        if (isResistanceMessage(cleanText)) return false
        if (looksLikeQuestion(cleanText)) return false
        if (isLowValueSmallTalk(cleanText)) return false

        return hasStablePreferenceSignal(cleanText) ||
            hasRoutineSignal(cleanText) ||
            hasLongTermHealthSignal(cleanText) ||
            hasRecentStateSignal(cleanText) ||
            detectDisclosureKind(cleanText) != null ||
            hasMeaningfulEventSignal(cleanText)
    }

    fun shouldUseFastLocalReplyV2(userText: String): Boolean {
        val cleanText = userText.trim()
        if (cleanText.isBlank()) return true
        if (looksLikeQuestion(cleanText)) return false
        if (isResistanceMessage(cleanText)) return false
        if (containsContactIntent(cleanText)) return false
        if (isJokeRequest(cleanText)) return false
        if (detectDisclosureKind(cleanText) != null) return false
        if (extractEmotionLabel(cleanText) != null) return false
        if (containsAny(cleanText, reminderKeywords)) return false

        return cleanText.length <= 6 && containsAny(cleanText, shortAckKeywords)
    }

    fun recallRelevantMemories(
        userText: String,
        semanticMemories: List<SemanticMemoryItem>,
        touchedAt: Long,
        limit: Int = 2,
    ): List<SemanticMemoryItem> {
        val cleanText = userText.trim()
        if (cleanText.isBlank() || isLowValueSmallTalk(cleanText)) {
            return emptyList()
        }

        val queryTokens = tokenize(cleanText)
        if (queryTokens.isEmpty()) return emptyList()

        val allowProfile = detectDisclosureKind(cleanText) != null || containsAny(cleanText, familyKeywords + profileKeywords + experienceKeywords)
        val allowPreference = hasStablePreferenceSignal(cleanText) || hasRoutineSignal(cleanText) || containsAny(cleanText, stablePreferenceKeywords)
        val allowRecentState = hasRecentStateSignal(cleanText) || hasMeaningfulEventSignal(cleanText)

        return semanticMemories
            .asSequence()
            .filterNot { it.expiresAt != null && it.expiresAt <= touchedAt }
            .mapNotNull { memory ->
                if (memory.memoryLayer == MemoryLayer.Profile && !allowProfile && !hasKeywordOverlap(cleanText, memory)) {
                    return@mapNotNull null
                }
                if (memory.memoryLayer == MemoryLayer.Preference && !allowPreference && !hasKeywordOverlap(cleanText, memory)) {
                    return@mapNotNull null
                }
                if (memory.memoryLayer == MemoryLayer.RecentState && !allowRecentState && !hasKeywordOverlap(cleanText, memory)) {
                    return@mapNotNull null
                }

                val keywordHits = memory.keywords.count { keyword ->
                    queryTokens.any { token -> token.contains(keyword) || keyword.contains(token) }
                }
                val summaryHit = queryTokens.any { token ->
                    memory.title.contains(token) || memory.summary.contains(token) || memory.compressedSummary.contains(token)
                }
                val score = keywordHits * 3 +
                    (if (summaryHit) 2 else 0) +
                    (if (memory.evidenceCount > 1) 1 else 0)
                if (score <= 1) {
                    null
                } else {
                    memory.copy(
                        lastAccessedAt = touchedAt,
                        lastConfirmedAt = maxOf(memory.lastConfirmedAt, touchedAt),
                    ) to score
                }
            }
            .sortedWith(
                compareByDescending<Pair<SemanticMemoryItem, Int>> { it.second }
                    .thenByDescending { it.first.evidenceCount }
                    .thenByDescending { it.first.lastConfirmedAt },
            )
            .map { it.first }
            .take(limit)
            .toList()
    }

    fun extractSemanticMemories(
        userText: String,
        timestamp: Long,
    ): List<SemanticMemoryItem> {
        val cleanText = userText.trim()
        if (cleanText.isBlank() || looksLikeQuestion(cleanText) || isLowValueSmallTalk(cleanText)) {
            return emptyList()
        }

        val additions = mutableListOf<SemanticMemoryItem>()
        val clauses = splitClauses(cleanText)

        fun addMemory(
            type: SemanticMemoryType,
            layer: MemoryLayer,
            retention: MemoryRetention,
            title: String,
            summary: String,
            compressedSummary: String,
            confidence: Double,
            source: String,
            expiresAt: Long? = null,
        ) {
            additions += SemanticMemoryItem(
                id = "${type.name.lowercase(Locale.ROOT)}_${timestamp}_${additions.size}",
                memoryType = type,
                memoryLayer = layer,
                retention = retention,
                title = title,
                summary = summary,
                compressedSummary = compressedSummary,
                keywords = (tokenize(source) + tokenize(summary) + tokenize(compressedSummary)).distinct().take(12),
                sourceText = cleanText,
                confidence = confidence,
                createdAt = timestamp,
                updatedAt = timestamp,
                sourceCount = 1,
                evidenceCount = 1,
                lastAccessedAt = timestamp,
                lastConfirmedAt = timestamp,
                expiresAt = expiresAt,
            )
        }

        clauses.forEach { clause ->
            if (clause.isBlank() || looksLikeQuestion(clause) || isLowValueSmallTalk(clause)) return@forEach

            cleanExtractedClause(clause, listOf("喜欢", "爱吃", "爱喝", "想吃", "想喝"))?.let { preference ->
                addMemory(
                    type = SemanticMemoryType.Preference,
                    layer = MemoryLayer.Preference,
                    retention = MemoryRetention.LongTerm,
                    title = "稳定偏好",
                    summary = "提到偏好：$preference",
                    compressedSummary = "偏好：${compressClause(preference)}",
                    confidence = 0.84,
                    source = clause,
                )
            }

            cleanExtractedClause(clause, listOf("不喜欢", "别提", "不要", "不想听", "别说"))?.let { taboo ->
                addMemory(
                    type = SemanticMemoryType.Preference,
                    layer = MemoryLayer.Preference,
                    retention = MemoryRetention.LongTerm,
                    title = "交流边界",
                    summary = "提到希望避开的话题：$taboo",
                    compressedSummary = "避讳：${compressClause(taboo)}",
                    confidence = 0.82,
                    source = clause,
                )
            }

            cleanExtractedClause(clause, routineKeywords)?.let { routine ->
                addMemory(
                    type = SemanticMemoryType.Routine,
                    layer = MemoryLayer.Profile,
                    retention = MemoryRetention.LongTerm,
                    title = "稳定作息",
                    summary = "提到日常习惯：$routine",
                    compressedSummary = "作息：${compressClause(routine)}",
                    confidence = 0.78,
                    source = clause,
                )
            }

            cleanExtractedClause(clause, profileKeywords)?.let { profile ->
                addMemory(
                    type = SemanticMemoryType.Profile,
                    layer = MemoryLayer.Profile,
                    retention = MemoryRetention.LongTerm,
                    title = "长期画像",
                    summary = "提到居住或生活情况：$profile",
                    compressedSummary = "情况：${compressClause(profile)}",
                    confidence = 0.8,
                    source = clause,
                )
            }

            cleanExtractedClause(clause, familyKeywords)?.let { family ->
                addMemory(
                    type = SemanticMemoryType.Family,
                    layer = MemoryLayer.Profile,
                    retention = MemoryRetention.LongTerm,
                    title = "家庭情况",
                    summary = "提到家里情况：$family",
                    compressedSummary = "家人：${compressClause(family)}",
                    confidence = 0.8,
                    source = clause,
                )
            }

            cleanExtractedClause(clause, experienceKeywords)?.let { experience ->
                addMemory(
                    type = SemanticMemoryType.Experience,
                    layer = MemoryLayer.Profile,
                    retention = MemoryRetention.LongTerm,
                    title = "人生经历",
                    summary = "提到过往经历：$experience",
                    compressedSummary = "经历：${compressClause(experience)}",
                    confidence = 0.76,
                    source = clause,
                )
            }

            cleanExtractedClause(clause, longTermHealthKeywords)?.let { health ->
                addMemory(
                    type = SemanticMemoryType.Health,
                    layer = MemoryLayer.Profile,
                    retention = MemoryRetention.LongTerm,
                    title = "长期健康基线",
                    summary = "提到长期身体情况：$health",
                    compressedSummary = "身体：${compressClause(health)}",
                    confidence = 0.8,
                    source = clause,
                )
            }

            cleanExtractedClause(clause, recentHealthKeywords)?.let { health ->
                addMemory(
                    type = SemanticMemoryType.Health,
                    layer = MemoryLayer.RecentState,
                    retention = MemoryRetention.ShortTerm,
                    title = "近期身体状态",
                    summary = "提到近期身体状态：$health",
                    compressedSummary = "身体：${compressClause(health)}",
                    confidence = 0.86,
                    source = clause,
                    expiresAt = timestamp + EVENT_WINDOW_MS,
                )
            }

            if (containsAny(clause, lowMoodKeywords + worryKeywords) && !containsAny(clause, weatherKeywords)) {
                val emotion = extractEmotionLabel(clause) ?: "这两天情绪起伏"
                addMemory(
                    type = SemanticMemoryType.Emotion,
                    layer = MemoryLayer.RecentState,
                    retention = MemoryRetention.ShortTerm,
                    title = "近期情绪状态",
                    summary = "提到近期心情：$emotion",
                    compressedSummary = "情绪：$emotion",
                    confidence = 0.72,
                    source = clause,
                    expiresAt = timestamp + SHORT_TERM_WINDOW_MS,
                )
            }

            if (hasMeaningfulEventSignal(clause)) {
                val event = compressClause(clause)
                addMemory(
                    type = SemanticMemoryType.Event,
                    layer = MemoryLayer.RecentState,
                    retention = MemoryRetention.ShortTerm,
                    title = "近期近况",
                    summary = "提到最近发生的事：$event",
                    compressedSummary = "近况：$event",
                    confidence = 0.68,
                    source = clause,
                    expiresAt = timestamp + EVENT_WINDOW_MS,
                )
            }
        }

        return mergeSemanticMemories(
            existing = emptyList(),
            additions = additions,
            recalled = emptyList(),
            touchedAt = timestamp,
        )
    }

    fun mergeSemanticMemories(
        existing: List<SemanticMemoryItem>,
        additions: List<SemanticMemoryItem>,
        recalled: List<SemanticMemoryItem>,
        touchedAt: Long,
    ): List<SemanticMemoryItem> {
        val merged = linkedMapOf<String, SemanticMemoryItem>()

        fun upsert(memory: SemanticMemoryItem) {
            if (memory.expiresAt != null && memory.expiresAt <= touchedAt) return
            val key = normalizedMemoryKey(memory)
            val current = merged[key]
            if (current == null) {
                merged[key] = memory
                return
            }

            val sourceCount = maxOf(current.sourceCount, current.evidenceCount) + memory.sourceCount
            merged[key] = current.copy(
                title = chooseBetterSummary(current.title, memory.title),
                summary = chooseBetterSummary(current.summary, memory.summary),
                compressedSummary = chooseBetterCompressedSummary(current.compressedSummary, memory.compressedSummary),
                keywords = (current.keywords + memory.keywords).distinct().take(12),
                sourceText = chooseBetterSummary(memory.sourceText, current.sourceText),
                confidence = maxOf(current.confidence, memory.confidence),
                updatedAt = maxOf(current.updatedAt, memory.updatedAt, touchedAt),
                sourceCount = sourceCount,
                evidenceCount = maxOf(current.evidenceCount, memory.evidenceCount, sourceCount),
                lastAccessedAt = maxOf(current.lastAccessedAt, memory.lastAccessedAt, touchedAt),
                lastConfirmedAt = maxOf(current.lastConfirmedAt, memory.lastConfirmedAt),
                expiresAt = listOfNotNull(current.expiresAt, memory.expiresAt).maxOrNull(),
            )
        }

        existing.forEach { upsert(it) }
        recalled.forEach { memory ->
            upsert(
                memory.copy(
                    lastAccessedAt = touchedAt,
                    lastConfirmedAt = maxOf(memory.lastConfirmedAt, touchedAt),
                ),
            )
        }
        additions.forEach { upsert(it) }

        return merged.values
            .sortedWith(
                compareByDescending<SemanticMemoryItem> { it.lastAccessedAt }
                    .thenByDescending { it.evidenceCount }
                    .thenByDescending { it.updatedAt },
            )
            .take(40)
    }

    fun buildProfileHint(
        userText: String,
        commonTopics: String,
        tabooWords: String,
        patientDelicate: Boolean,
    ): String? {
        val cleanText = userText.trim()
        if (cleanText.isBlank()) return null

        val tabooList = splitTopics(tabooWords)
        val commonTopicList = splitTopics(commonTopics)
        val tabooHit = tabooList.firstOrNull { cleanText.contains(it) }
        if (tabooHit != null) {
            return "这轮别主动延展到“$tabooHit”，先顺着用户当前的话题接住。"
        }

        val commonHit = commonTopicList.firstOrNull { cleanText.contains(it) }
        if (commonHit != null) {
            return if (patientDelicate) {
                "用户正聊到常提的话题“$commonHit”，可以自然顺着聊，不必换成关怀盘问。"
            } else {
                "当前话题碰到对方常聊的“$commonHit”，顺着展开会更自然。"
            }
        }

        return null
    }

    fun buildTaskHint(
        userText: String,
        reminders: List<ReminderItem>,
        patientDelicate: Boolean,
    ): String? {
        if (!containsAny(userText, reminderKeywords)) return null
        val nextReminder = reminders.firstOrNull { it.status != ReminderStatus.Completed } ?: return null
        return if (patientDelicate) {
            "若要带到提醒，只提一句 ${nextReminder.time} 的 ${nextReminder.title} 就够了，不要像催办事项。"
        } else {
            "如果顺手回应提醒，轻轻带一句 ${nextReminder.time} 的 ${nextReminder.title}。"
        }
    }

    fun buildEmotionHint(
        userText: String,
        patientDelicate: Boolean,
    ): String? {
        val label = extractEmotionLabel(userText) ?: return null
        return when (label) {
            "有点低落或孤单" -> if (patientDelicate) "先接住对方低落的劲头，别急着追问背景。" else "先接住这股低落感，语气平一点。"
            "有点担心" -> "先安稳一下情绪，再回答内容。"
            "身体有点不舒服" -> "先关心身体状态，再给轻量建议。"
            else -> null
        }
    }

    fun buildRecentHint(
        userText: String,
        chatMessages: List<ChatMessage>,
        patientDelicate: Boolean,
    ): String? {
        val lastUserMessage = chatMessages.asReversed().firstOrNull { !it.fromAi }?.content?.trim().orEmpty()
        if (lastUserMessage.isBlank()) return null
        val currentTokens = tokenize(userText)
        val lastTokens = tokenize(lastUserMessage)
        if (currentTokens.isEmpty() || lastTokens.isEmpty()) return null
        val overlap = currentTokens.intersect(lastTokens.toSet())
        if (overlap.size < 2) return null
        return if (patientDelicate) {
            "这句和上一轮在聊同一件事，可以顺着接，不必重新起头。"
        } else {
            "还是同一话题，接续上一轮就行。"
        }
    }

    fun buildCollectionHint(
        userText: String,
        semanticMemories: List<SemanticMemoryItem>,
        patientDelicate: Boolean,
    ): String? {
        if (detectDisclosureKind(userText) != null) return null
        val hasLivingInfo = semanticMemories.any {
            it.memoryLayer == MemoryLayer.Profile && (it.memoryType == SemanticMemoryType.Profile || it.memoryType == SemanticMemoryType.Family)
        }
        if (!hasLivingInfo) {
            return if (patientDelicate) {
                "您平时是一个人住，还是和家里人一起住呀？"
            } else {
                "你平时是一个人住，还是跟家里人一起住？"
            }
        }
        return null
    }

    fun buildReplyDraft(
        userText: String,
        patientDelicate: Boolean,
        recall: CompanionContextRecall,
    ): CompanionReplyDraft = buildReplyDraftV2(userText, patientDelicate, recall)

    fun composeReply(draft: CompanionReplyDraft): String {
        return buildList {
            add(draft.primary.trim())
            addAll(draft.supportingHints.map { it.trim() }.filter { it.isNotBlank() })
            draft.followUp?.trim()?.takeIf { it.isNotBlank() }?.let(::add)
        }
            .distinct()
            .joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun buildEmotionHintV2(
        userText: String,
        patientDelicate: Boolean,
    ): String? = buildEmotionHint(userText, patientDelicate)

    fun buildCollectionHintV2(
        userText: String,
        semanticMemories: List<SemanticMemoryItem>,
        patientDelicate: Boolean,
    ): String? {
        val cleanText = userText.trim()
        if (cleanText.isBlank()) return null
        if (looksLikeQuestion(cleanText) || isResistanceMessage(cleanText) || isLowValueSmallTalk(cleanText)) return null
        if (detectDisclosureKind(cleanText) != null) return null

        val hasPreferenceMemory = semanticMemories.any { it.memoryLayer == MemoryLayer.Preference }
        if (!hasPreferenceMemory && containsAny(cleanText, listOf("吃", "喝", "散步", "做饭", "出门"))) {
            return if (patientDelicate) {
                "您平时更喜欢在家慢慢待着，还是出去走走？"
            } else {
                "你平时更喜欢在家待着，还是出去走走？"
            }
        }
        return null
    }

    fun buildReplyDraftV2(
        userText: String,
        patientDelicate: Boolean,
        recall: CompanionContextRecall,
    ): CompanionReplyDraft {
        val cleanText = userText.trim()
        val memoryIds = recall.semanticMemories.map { it.id }
        val memoryHint = buildMemoryHint(cleanText, recall.semanticMemories)
        val support = mutableListOf<String>()

        recall.recentHint?.takeIf { it.isNotBlank() }?.let {
            if (containsAny(cleanText, listOf("还是", "刚才", "刚刚", "后来"))) {
                support += "咱们就接着刚才的话往下说。"
            }
        }
        recall.taskHint?.takeIf { containsAny(cleanText, reminderKeywords) }?.let { support += it }

        if (cleanText.isBlank()) {
            return CompanionReplyDraft(
                intentLabel = "普通回应",
                primary = if (patientDelicate) "我在呢，慢慢说。" else "我在，接着说就行。",
                supportingHints = emptyList(),
                followUp = null,
                retrievedMemoryIds = memoryIds,
            )
        }

        if (isResistanceMessage(cleanText)) {
            return CompanionReplyDraft(
                intentLabel = "安抚收口",
                primary = if (patientDelicate) {
                    "好，那咱们先不聊这些，我不再追着问了。"
                } else {
                    "行，那这话先放下，不聊这些了。"
                },
                supportingHints = emptyList(),
                followUp = if (patientDelicate) "想换个轻松点的话题也行。" else "想换个轻松点的话题就换。",
                retrievedMemoryIds = memoryIds,
            )
        }

        if (containsContactIntent(cleanText)) {
            return CompanionReplyDraft(
                intentLabel = "联系家人",
                primary = if (patientDelicate) {
                    "要是你想联系家里人，我可以先陪你把话理一理。"
                } else {
                    "要联系家里人也行，咱先把想说的理顺。"
                },
                supportingHints = listOfNotNull(recall.contactHintOrNull()),
                followUp = "你现在最想联系谁？",
                retrievedMemoryIds = memoryIds,
            )
        }

        val emotionLabel = extractEmotionLabel(cleanText)
        if (emotionLabel == "身体有点不舒服" || containsAny(cleanText, recentHealthKeywords)) {
            memoryHint?.let(support::add)
            recall.emotionHint?.let { if (!support.contains(it)) support += it }
            return CompanionReplyDraft(
                intentLabel = "身体关怀",
                primary = if (patientDelicate) {
                    "听着是有点难受，先别硬撑着。"
                } else {
                    "听着这会儿身体不太舒服，先缓一缓。"
                },
                supportingHints = support.distinct(),
                followUp = recall.collectionHint,
                retrievedMemoryIds = memoryIds,
            )
        }

        if (emotionLabel == "有点低落或孤单" || emotionLabel == "有点担心") {
            return CompanionReplyDraft(
                intentLabel = "情绪陪伴",
                primary = when (emotionLabel) {
                    "有点担心" -> if (patientDelicate) "听着你这会儿有点挂心，我陪你慢慢捋一捋。" else "听着你心里有点悬着，咱慢慢捋。"
                    else -> if (patientDelicate) "听着心里有点闷，我在这儿陪着你。" else "听着这会儿有点闷，我陪你待一会儿。"
                },
                supportingHints = listOfNotNull(memoryHint).distinct(),
                followUp = if (patientDelicate) "你先说说现在最难受的是哪一段。" else "你先说说最堵心的是哪一段。",
                retrievedMemoryIds = memoryIds,
            )
        }

        if (containsAny(cleanText, familyKeywords) || detectDisclosureKind(cleanText) == "family") {
            memoryHint?.let(support::add)
            return CompanionReplyDraft(
                intentLabel = "家里近况",
                primary = if (patientDelicate) {
                    "听着家里情况最近不算轻松，心里会惦记也正常。"
                } else {
                    "听着家里情况有点让人挂心，会惦记很正常。"
                },
                supportingHints = support.distinct(),
                followUp = null,
                retrievedMemoryIds = memoryIds,
            )
        }

        if (isJokeRequest(cleanText)) {
            return CompanionReplyDraft(
                intentLabel = "轻松逗乐",
                primary = if (patientDelicate) "行，我给你换个轻松点的。" else "那就换个轻松点的。",
                supportingHints = emptyList(),
                followUp = "你想听短一点的，还是生活里那种逗人的？",
                retrievedMemoryIds = memoryIds,
            )
        }

        if (looksLikeQuestion(cleanText)) {
            return CompanionReplyDraft(
                intentLabel = "普通问答",
                primary = if (containsAny(cleanText, weatherKeywords)) {
                    "天气这类信息我这边未必实时准，出门前看一下手机天气会更稳妥。"
                } else {
                    "这个我先顺着你的意思答一句。"
                },
                supportingHints = listOfNotNull(memoryHint).distinct(),
                followUp = null,
                retrievedMemoryIds = memoryIds,
            )
        }

        if (isLowValueSmallTalk(cleanText)) {
            return CompanionReplyDraft(
                intentLabel = "日常闲聊",
                primary = when {
                    cleanText.contains("太阳") -> "今天太阳确实挺足。"
                    cleanText.contains("天气") -> "这会儿天气倒是挺显眼。"
                    else -> if (patientDelicate) "听着挺日常的，我接着陪你聊。" else "挺生活化的一句，接着聊就行。"
                },
                supportingHints = emptyList(),
                followUp = null,
                retrievedMemoryIds = memoryIds,
            )
        }

        val followUp = recall.collectionHint
            ?.takeIf { !containsAny(cleanText, familyKeywords + profileKeywords + experienceKeywords) }
        return CompanionReplyDraft(
            intentLabel = "普通陪伴",
            primary = if (patientDelicate) "我听着呢，这事可以慢慢说。" else "我听着，这事咱慢慢聊。",
            supportingHints = listOfNotNull(memoryHint).distinct(),
            followUp = followUp,
            retrievedMemoryIds = memoryIds,
        )
    }

    private fun CompanionContextRecall.contactHintOrNull(): String? {
        return taskHint?.takeIf { it.contains("联系") } ?: recentHint?.takeIf { it.contains("联系") }
    }

    private fun buildMemoryHint(
        userText: String,
        semanticMemories: List<SemanticMemoryItem>,
    ): String? {
        val memory = semanticMemories.firstOrNull() ?: return null
        if (!hasKeywordOverlap(userText, memory)) return null
        return when (memory.memoryLayer) {
            MemoryLayer.Preference -> "还记着你提过 ${memory.compressedSummary.removePrefix("偏好：")}。"
            MemoryLayer.Profile -> "我记得你说过 ${memory.compressedSummary.removePrefix("情况：").removePrefix("家人：").removePrefix("经历：")}。"
            MemoryLayer.RecentState -> "前面提到的 ${memory.compressedSummary.removePrefix("身体：").removePrefix("情绪：").removePrefix("近况：")} 我也记着。"
        }
    }

    private fun containsAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { keyword -> keyword.isNotBlank() && text.contains(keyword, ignoreCase = false) }
    }

    private fun containsContactIntent(text: String): Boolean = containsAny(text, contactKeywords)

    private fun isJokeRequest(text: String): Boolean = containsAny(text, jokeKeywords)

    private fun isResistanceMessage(text: String): Boolean = containsAny(text, resistanceKeywords)

    private fun detectDisclosureKind(text: String): String? {
        return when {
            containsAny(text, familyKeywords) -> "family"
            containsAny(text, profileKeywords) -> "profile"
            containsAny(text, experienceKeywords) -> "experience"
            else -> null
        }
    }

    private fun splitClauses(text: String): List<String> {
        return text
            .split("。", "！", "？", "；", ";", "，", ",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun cleanExtractedClause(text: String, keywords: List<String>): String? {
        if (!containsAny(text, keywords)) return null
        val clause = splitClauses(text).firstOrNull { containsAny(it, keywords) } ?: text
        if (looksLikeQuestion(clause) || isLowValueSmallTalk(clause)) return null
        return clause.trim().takeIf { it.length >= 2 }
    }

    private fun compressClause(text: String): String {
        val clean = text.replace(Regex("[。！？，,；;]"), "").trim()
        return if (clean.length <= 16) clean else clean.take(16)
    }

    private fun extractEmotionLabel(text: String): String? {
        return when {
            containsAny(text, recentHealthKeywords) -> "身体有点不舒服"
            containsAny(text, worryKeywords) -> "有点担心"
            containsAny(text, lowMoodKeywords) -> "有点低落或孤单"
            containsAny(text, lightMoodKeywords) -> "心情比较轻松"
            else -> null
        }
    }

    private fun normalizedMemoryKey(memory: SemanticMemoryItem): String {
        val seed = memory.compressedSummary.ifBlank { memory.summary }
            .lowercase(Locale.ROOT)
            .replace(Regex("\\s+"), "")
            .replace("：", ":")
        return "${memory.memoryType.name}|${memory.memoryLayer.name}|$seed"
    }

    private fun chooseBetterSummary(left: String, right: String): String {
        return when {
            right.length > left.length -> right
            left.isBlank() -> right
            else -> left
        }
    }

    private fun chooseBetterCompressedSummary(left: String, right: String): String {
        return when {
            left.isBlank() -> right
            right.isBlank() -> left
            right.length < left.length -> right
            else -> left
        }
    }

    private fun splitTopics(rawText: String): List<String> {
        return rawText
            .split("、", "，", ",", " ")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun tokenize(text: String): List<String> {
        val normalized = text
            .lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{Nd}]"), " ")
            .trim()
        if (normalized.isBlank()) return emptyList()

        val tokens = normalized.split(Regex("\\s+")).filter { it.length >= 2 }.toMutableList()
        if (tokens.isEmpty() && normalized.length >= 2) {
            normalized.windowed(size = 2, step = 1, partialWindows = false).forEach(tokens::add)
        }
        return tokens.distinct().take(16)
    }

    private fun hasKeywordOverlap(userText: String, memory: SemanticMemoryItem): Boolean {
        val tokens = tokenize(userText)
        return memory.keywords.any { keyword ->
            tokens.any { token -> token.contains(keyword) || keyword.contains(token) }
        }
    }

    private fun looksLikeQuestion(text: String): Boolean {
        return text.contains("?") || text.contains("？") || text.endsWith("吗") || text.endsWith("呢")
    }

    private fun isLowValueSmallTalk(text: String): Boolean {
        if (text.length <= 4 && containsAny(text, shortAckKeywords)) return true
        val weatherOnly = containsAny(text, weatherKeywords) && !hasMeaningfulEventSignal(text) && !containsAny(text, familyKeywords + recentHealthKeywords)
        return weatherOnly || containsAny(text, smallTalkKeywords)
    }

    private fun hasStablePreferenceSignal(text: String): Boolean {
        return containsAny(text, stablePreferenceKeywords) && !looksLikeQuestion(text)
    }

    private fun hasRoutineSignal(text: String): Boolean {
        return containsAny(text, routineKeywords) && !looksLikeQuestion(text)
    }

    private fun hasLongTermHealthSignal(text: String): Boolean {
        return containsAny(text, longTermHealthKeywords) && !looksLikeQuestion(text)
    }

    private fun hasRecentStateSignal(text: String): Boolean {
        return containsAny(text, recentHealthKeywords + lowMoodKeywords + worryKeywords) && !looksLikeQuestion(text)
    }

    private fun hasMeaningfulEventSignal(text: String): Boolean {
        if (looksLikeQuestion(text) || containsAny(text, weatherKeywords)) return false
        return containsAny(text, listOf("今天去", "刚刚", "刚才", "最近", "这两天", "出门", "回来", "看病", "买菜", "做饭", "去公园"))
    }
}
