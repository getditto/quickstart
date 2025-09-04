package integration

actual fun getEnvironmentVariable(name: String): String? {
    return System.getenv(name)
}