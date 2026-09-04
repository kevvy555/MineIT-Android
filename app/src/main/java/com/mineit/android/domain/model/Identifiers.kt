package com.mineit.android.domain.model

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class ColonyId(val value: String) {
    init {
        require(value.isNotBlank()) { "ColonyId must not be blank." }
    }

    override fun toString(): String = value
}

@Serializable
@JvmInline
value class ShipId(val value: String) {
    init {
        require(value.isNotBlank()) { "ShipId must not be blank." }
    }

    override fun toString(): String = value
}

@Serializable
@JvmInline
value class ResourceId(val value: String) {
    init {
        require(value.isNotBlank()) { "ResourceId must not be blank." }
    }

    override fun toString(): String = value
}

@Serializable
@JvmInline
value class AbsoluteDay(val value: Int) {
    init {
        require(value >= 1) { "AbsoluteDay must be at least 1." }
    }
}
