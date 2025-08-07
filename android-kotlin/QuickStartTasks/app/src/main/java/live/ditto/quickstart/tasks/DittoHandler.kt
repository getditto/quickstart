package live.ditto.quickstart.tasks

import live.ditto.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class DittoHandler {
    companion object {
        lateinit var ditto: Ditto
        private var _isInitialized = false
        private var pendingCallbacks = mutableListOf<() -> Unit>()
        
        val isInitialized: Boolean
            get() = _isInitialized
            
        fun markAsInitialized() {
            _isInitialized = true
            pendingCallbacks.forEach { it() }
            pendingCallbacks.clear()
        }
        
        suspend fun waitForInitialization() {
            if (_isInitialized) return
            
            suspendCancellableCoroutine<Unit> { continuation ->
                pendingCallbacks.add {
                    continuation.resume(Unit)
                }
            }
        }
    }
}
