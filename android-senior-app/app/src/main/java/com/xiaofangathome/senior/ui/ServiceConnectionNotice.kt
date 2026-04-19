package com.xiaofangathome.senior.ui

import com.xiaofangathome.senior.platform.CareServiceRuntimeResult
import com.xiaofangathome.senior.platform.SeniorPreferencesSnapshot

internal fun buildServiceConnectionNotice(
    snapshot: SeniorPreferencesSnapshot,
    runtime: CareServiceRuntimeResult?,
): String {
    val usingDefaultConnection = snapshot.careServiceBaseUrl.isBlank() && snapshot.careServiceSeniorId.isBlank()
    val testedBaseUrl = snapshot.careServiceBaseUrl
        .ifBlank { "http://10.0.2.2:3301" }
        .trimEnd('/')
    val runtimeEndpoint = "$testedBaseUrl/api/ai/runtime"

    return when {
        runtime == null && usingDefaultConnection -> "已恢复默认服务连接设置，但当前无法完成连接测试。模拟器默认用 http://10.0.2.2:3301；真机请填电脑局域网 IP 的 3301 端口。"
        runtime == null && testedBaseUrl.matches(Regex("""^https?://[^/]+:3201$""")) -> "你填的是 3201，这通常是子女端网页前端端口；老人端服务请改成 3301（真机一般是电脑局域网 IP:3301）。"
        runtime == null -> "服务连接失败，请检查 $runtimeEndpoint 是否可访问。"
        runtime.configured -> {
            val model = runtime.model?.takeIf { it.isNotBlank() } ?: "未知模型"
            "服务连接成功，AI 已就绪：$model。"
        }
        else -> "服务可达，但 AI 还没有配置好。"
    }
}
