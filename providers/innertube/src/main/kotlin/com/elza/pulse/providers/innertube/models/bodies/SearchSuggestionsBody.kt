package com.elza.pulse.providers.innertube.models.bodies

import com.elza.pulse.providers.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class SearchSuggestionsBody(
    val context: Context = Context.DefaultWeb,
    val input: String
)
