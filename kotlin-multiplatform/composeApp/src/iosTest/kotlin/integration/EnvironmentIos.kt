package integration

import platform.posix.getenv
import kotlinx.cinterop.toKString
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual fun getEnvironmentVariable(name: String): String? {
    return getenv(name)?.toKString()
}