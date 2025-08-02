package com.telecommande.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PairedTvInfo(
    val ipAddress: String,
    val name: String?,
    val macAddress: String?
)