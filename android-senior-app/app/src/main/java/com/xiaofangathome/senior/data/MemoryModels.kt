package com.xiaofangathome.senior.data

enum class SemanticMemoryType {
    Preference,
    Routine,
    Health,
    Family,
    Profile,
    Experience,
    Event,
    Emotion,
}

enum class MemoryLayer {
    Profile,
    Preference,
    RecentState,
}

enum class MemoryRetention {
    LongTerm,
    ShortTerm,
}

data class SemanticMemoryItem(
    val id: String,
    val memoryType: SemanticMemoryType,
    val memoryLayer: MemoryLayer,
    val retention: MemoryRetention,
    val title: String,
    val summary: String,
    val compressedSummary: String,
    val keywords: List<String>,
    val sourceText: String,
    val confidence: Double,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
    val sourceCount: Int = 1,
    val evidenceCount: Int = sourceCount,
    val lastAccessedAt: Long = createdAt,
    val lastConfirmedAt: Long = updatedAt,
    val expiresAt: Long? = null,
)
