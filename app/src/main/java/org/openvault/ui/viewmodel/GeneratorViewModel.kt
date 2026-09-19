package org.openvault.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.openvault.core.crypto.PasswordGenerator
import org.openvault.core.model.PasswordOptions
import org.openvault.core.model.PasswordStrength

class GeneratorViewModel : ViewModel() {

    private val _options = MutableStateFlow(PasswordOptions())
    val options: StateFlow<PasswordOptions> = _options.asStateFlow()

    private val _generatedPassword = MutableStateFlow("")
    val generatedPassword: StateFlow<String> = _generatedPassword.asStateFlow()

    private val _strength = MutableStateFlow(PasswordGenerator.calculateStrength(""))
    val strength: StateFlow<PasswordStrength> = _strength.asStateFlow()

    init {
        regenerate()
    }

    fun regenerate() {
        val newPassword = PasswordGenerator.generate(_options.value)
        _generatedPassword.value = newPassword
        _strength.value = PasswordGenerator.calculateStrength(newPassword)
    }

    fun updateLength(length: Int) {
        _options.value = _options.value.copy(length = length)
        regenerate()
    }

    fun updateWordCount(count: Int) {
        _options.value = _options.value.copy(wordCount = count)
        regenerate()
    }

    fun toggleUppercase(value: Boolean) {
        _options.value = _options.value.copy(includeUppercase = value)
        regenerate()
    }

    fun toggleLowercase(value: Boolean) {
        _options.value = _options.value.copy(includeLowercase = value)
        regenerate()
    }

    fun toggleDigits(value: Boolean) {
        _options.value = _options.value.copy(includeDigits = value)
        regenerate()
    }

    fun toggleSymbols(value: Boolean) {
        _options.value = _options.value.copy(includeSymbols = value)
        regenerate()
    }

    fun toggleAvoidAmbiguous(value: Boolean) {
        _options.value = _options.value.copy(avoidAmbiguous = value)
        regenerate()
    }

    fun setPassphraseMode(enabled: Boolean) {
        _options.value = _options.value.copy(isPassphrase = enabled)
        regenerate()
    }

    fun setSeparator(separator: String) {
        _options.value = _options.value.copy(separator = separator)
        regenerate()
    }
}
