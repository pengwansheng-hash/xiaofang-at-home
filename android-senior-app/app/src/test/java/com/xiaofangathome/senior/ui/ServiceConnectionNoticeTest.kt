package com.xiaofangathome.senior.ui

import com.xiaofangathome.senior.platform.CareServiceRuntimeResult
import com.xiaofangathome.senior.platform.SeniorPreferencesSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceConnectionNoticeTest {
    @Test
    fun `failure notice points to the exact runtime endpoint`() {
        val snapshot = SeniorPreferencesSnapshot(
            careServiceBaseUrl = "http://192.168.0.50:3301",
            careServiceSeniorId = "senior-zhang",
        )

        val notice = buildServiceConnectionNotice(snapshot, null)

        assertTrue(notice.contains("http://192.168.0.50:3301/api/ai/runtime"))
    }

    @Test
    fun `failure notice warns when the web frontend port is used`() {
        val snapshot = SeniorPreferencesSnapshot(
            careServiceBaseUrl = "http://192.168.0.50:3201",
            careServiceSeniorId = "senior-zhang",
        )

        val notice = buildServiceConnectionNotice(snapshot, null)

        assertTrue(notice.contains("3201"))
        assertTrue(notice.contains("3301"))
        assertTrue(notice.contains("网页前端端口") || notice.contains("子女端"))
    }

    @Test
    fun `success notice reports the configured model`() {
        val snapshot = SeniorPreferencesSnapshot(
            careServiceBaseUrl = "http://192.168.0.50:3301",
            careServiceSeniorId = "senior-zhang",
        )
        val runtime = CareServiceRuntimeResult(
            provider = "openai-compatible",
            configured = true,
            model = "glm-5",
            baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
            timeoutMs = 20_000,
        )

        val notice = buildServiceConnectionNotice(snapshot, runtime)

        assertEquals("服务连接成功，AI 已就绪：glm-5。", notice)
    }
}
