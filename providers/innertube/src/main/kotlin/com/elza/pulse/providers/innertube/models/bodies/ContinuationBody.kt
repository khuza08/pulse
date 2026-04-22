package com.elza.pulse.providers.innertube.models.bodies

import com.elza.pulse.providers.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class ContinuationBody(
    val context: Context = Context.DefaultWeb,
    val continuation: String
)
