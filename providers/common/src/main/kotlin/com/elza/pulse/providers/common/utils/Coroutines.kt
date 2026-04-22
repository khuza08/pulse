package com.elza.pulse.providers.common.utils

import kotlinx.coroutines.CancellationException

inline fun <T> runCatchingCancellable(block: () -> T) =
    runCatching(block).takeIf { it.exceptionOrNull() !is CancellationException }

inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> =
    if (isSuccess) Result.success(transform(getOrThrow()))
    else Result.failure(exceptionOrNull()!!)

inline fun <T, R> Result<T?>.mapNotNull(transform: (T) -> R?): Result<R?> =
    if (isSuccess) Result.success(getOrNull()?.let(transform))
    else Result.failure(exceptionOrNull()!!)
