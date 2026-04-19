package com.xiaofangathome.senior.ui

internal fun preferredAvatarLabel(preferredName: String): String {
    val cleanName = preferredName.trim()
    return cleanName.take(1).ifBlank { "芳" }
}

internal fun contactBadgeLabel(
    relation: String,
    name: String,
): String {
    val cleanRelation = relation.trim()
    if (cleanRelation.isNotBlank()) {
        return cleanRelation.take(2)
    }
    return name.trim().take(1).ifBlank { "家" }
}

internal fun normalizePrimaryRoute(route: String): String {
    return route.substringBefore("/")
}
