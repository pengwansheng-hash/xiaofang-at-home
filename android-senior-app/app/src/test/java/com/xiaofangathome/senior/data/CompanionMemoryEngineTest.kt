package com.xiaofangathome.senior.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionMemoryEngineTest {
    @Test
    fun `extractSemanticMemories builds compressed summaries`() {
        val memories = CompanionMemoryEngine.extractSemanticMemories(
            userText = "我最近喜欢喝小米粥，晚上八点后就准备睡觉了。",
            timestamp = 100L,
        )

        assertTrue(memories.any { it.memoryType == SemanticMemoryType.Preference })
        assertTrue(memories.any { it.memoryType == SemanticMemoryType.Routine })
        assertTrue(memories.all { it.compressedSummary.isNotBlank() })
    }

    @Test
    fun `extractSemanticMemories captures profile family and experience details`() {
        val memories = CompanionMemoryEngine.extractSemanticMemories(
            userText = "我一个人住，儿子在外地，孙子上初中了，我年轻时在纺织厂上班。",
            timestamp = 120L,
        )

        assertTrue(memories.any { it.memoryType == SemanticMemoryType.Profile })
        assertTrue(memories.any { it.memoryType == SemanticMemoryType.Family })
        assertTrue(memories.any { it.memoryType == SemanticMemoryType.Experience })
    }

    @Test
    fun `mergeSemanticMemories compresses repeated memories`() {
        val existing = listOf(
            SemanticMemoryItem(
                id = "memory_1",
                memoryType = SemanticMemoryType.Preference,
                memoryLayer = MemoryLayer.Preference,
                retention = MemoryRetention.LongTerm,
                title = "新的喜好摘要",
                summary = "老人最近提到自己偏好：喜欢喝小米粥",
                compressedSummary = "偏好：喜欢喝小米粥",
                keywords = listOf("喜欢", "小米粥"),
                sourceText = "我喜欢喝小米粥",
                confidence = 0.7,
                createdAt = 10L,
                updatedAt = 10L,
                sourceCount = 1,
                lastAccessedAt = 10L,
            ),
        )

        val additions = listOf(
            SemanticMemoryItem(
                id = "memory_2",
                memoryType = SemanticMemoryType.Preference,
                memoryLayer = MemoryLayer.Preference,
                retention = MemoryRetention.LongTerm,
                title = "新的喜好摘要",
                summary = "老人最近提到自己偏好：喜欢喝小米粥",
                compressedSummary = "偏好：喜欢喝小米粥",
                keywords = listOf("喜欢", "小米粥", "晚上"),
                sourceText = "晚上还是想喝小米粥",
                confidence = 0.82,
                createdAt = 20L,
                updatedAt = 20L,
                sourceCount = 1,
                lastAccessedAt = 20L,
            ),
        )

        val merged = CompanionMemoryEngine.mergeSemanticMemories(
            existing = existing,
            additions = additions,
            recalled = emptyList(),
            touchedAt = 20L,
        )

        assertEquals(1, merged.size)
        assertEquals(2, merged.first().sourceCount)
        assertEquals("偏好：喜欢喝小米粥", merged.first().compressedSummary)
    }

    @Test
    fun `buildCollectionHint asks about missing basic profile`() {
        val hint = CompanionMemoryEngine.buildCollectionHint(
            userText = "今天阳光挺好。",
            semanticMemories = emptyList(),
            patientDelicate = true,
        )

        assertTrue(hint?.contains("一个人住") == true)
    }

    @Test
    fun `buildReplyDraft uses recalled memories and preserves two stage output`() {
        val recalled = listOf(
            SemanticMemoryItem(
                id = "health_1",
                memoryType = SemanticMemoryType.Health,
                memoryLayer = MemoryLayer.RecentState,
                retention = MemoryRetention.ShortTerm,
                title = "新的身体状态摘要",
                summary = "老人最近提到身体状态：最近有点头晕",
                compressedSummary = "身体：最近头晕",
                keywords = listOf("头晕"),
                sourceText = "我最近有点头晕",
                confidence = 0.9,
                createdAt = 1L,
                updatedAt = 1L,
                sourceCount = 1,
                lastAccessedAt = 1L,
            ),
        )
        val recall = CompanionContextRecall(
            semanticMemories = recalled,
            collectionHint = "您家孩子平时都在身边吗，还是有在外地的呀？",
            taskHint = "您当前最近的一条提醒是08:00的吃药，我会一起带上。",
        )

        val draft = CompanionMemoryEngine.buildReplyDraft(
            userText = "今天还有点头晕",
            patientDelicate = false,
            recall = recall,
        )
        val reply = CompanionMemoryEngine.composeReply(draft)

        assertEquals("身体关怀", draft.intentLabel)
        assertTrue(draft.retrievedMemoryIds.contains("health_1"))
        assertTrue(draft.followUp?.contains("孩子") == true)
        assertTrue(reply.contains("头晕"))
    }

    @Test
    fun `buildCollectionHint does not reask family details already provided in current turn`() {
        val hint = CompanionMemoryEngine.buildCollectionHint(
            userText = "我现在有两个儿子，一个在成都，一个在上海，他们都很少回来。",
            semanticMemories = emptyList(),
            patientDelicate = true,
        )

        assertNull(hint)
    }

    @Test
    fun `buildRecentHint ignores unrelated previous topic`() {
        val recentHint = CompanionMemoryEngine.buildRecentHint(
            userText = "我现在有两个儿子，一个在成都，一个在上海。",
            chatMessages = listOf(
                ChatMessage(
                    id = "user_1",
                    fromAi = false,
                    content = "你能给我讲个笑话吗？",
                    timeLabel = "22:43",
                    createdAt = 1L,
                ),
                ChatMessage(
                    id = "ai_1",
                    fromAi = true,
                    content = "那我给您讲一个。",
                    timeLabel = "22:43",
                    createdAt = 2L,
                ),
            ),
            patientDelicate = true,
        )

        assertNull(recentHint)
    }

    @Test
    fun `buildReplyDraft apologizes and stops probing when user resists`() {
        val recall = CompanionContextRecall(
            semanticMemories = emptyList(),
            collectionHint = "您家孩子平时都在身边吗，还是有在外地的呀？",
        )

        val draft = CompanionMemoryEngine.buildReplyDraft(
            userText = "你问这些干什么，别再问了。",
            patientDelicate = true,
            recall = recall,
        )
        val reply = CompanionMemoryEngine.composeReply(draft)

        assertEquals("安抚收口", draft.intentLabel)
        assertFalse(reply.contains("孩子平时都在身边"))
        assertTrue(reply.contains("不聊这些") || reply.contains("不再追着问"))
    }

    @Test
    fun `buildReplyDraft treats family update as acknowledgement not contact request`() {
        val draft = CompanionMemoryEngine.buildReplyDraft(
            userText = "我现在有两个儿子，一个在成都，一个在上海，他们都很少回来。",
            patientDelicate = true,
            recall = CompanionContextRecall(semanticMemories = emptyList()),
        )
        val reply = CompanionMemoryEngine.composeReply(draft)

        assertEquals("家里近况", draft.intentLabel)
        assertFalse(reply.contains("联系哪一位"))
        assertTrue(reply.contains("家里情况") || reply.contains("惦记"))
    }

    @Test
    fun `extractSemanticMemories ignores weather small talk and questions`() {
        assertTrue(
            CompanionMemoryEngine.extractSemanticMemories(
                userText = "今天天气不错",
                timestamp = 200L,
            ).isEmpty(),
        )
        assertTrue(
            CompanionMemoryEngine.extractSemanticMemories(
                userText = "今天成都的天气怎么样？",
                timestamp = 210L,
            ).isEmpty(),
        )
    }

    @Test
    fun `extractSemanticMemories assigns long term and recent layers`() {
        val memories = CompanionMemoryEngine.extractSemanticMemories(
            userText = "我一个人住，喜欢喝小米粥，最近有点头晕。",
            timestamp = 300L,
        )

        assertTrue(memories.any { it.memoryLayer == MemoryLayer.Profile && it.retention == MemoryRetention.LongTerm })
        assertTrue(memories.any { it.memoryLayer == MemoryLayer.Preference && it.retention == MemoryRetention.LongTerm })
        assertTrue(memories.any { it.memoryLayer == MemoryLayer.RecentState && it.retention == MemoryRetention.ShortTerm })
    }
}

