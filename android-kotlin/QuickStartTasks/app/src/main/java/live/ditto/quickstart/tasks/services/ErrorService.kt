package live.ditto.quickstart.tasks.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ErrorService {
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun showError(message: String) {
        _errorMessage.value = message
    }

    fun errorShown() {
        _errorMessage.value = null
    }
}