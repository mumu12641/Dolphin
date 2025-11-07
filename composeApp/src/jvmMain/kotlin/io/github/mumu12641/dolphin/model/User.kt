package io.github.mumu12641.dolphin.model

import kotlinx.serialization.Serializable

@Serializable
data class User(val user: String, val pwd: String)
