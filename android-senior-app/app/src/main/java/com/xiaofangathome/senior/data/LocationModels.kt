package com.xiaofangathome.senior.data

data class LocationSample(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val provider: String,
    val sampledAt: Long,
)
