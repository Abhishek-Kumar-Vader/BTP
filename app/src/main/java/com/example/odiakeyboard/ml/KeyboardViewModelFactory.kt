package com.example.odiakeyboard.ml

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.odiakeyboard.viewmodel.KeyboardViewModel

/**
 * ViewModelProvider.Factory that injects [OdiaSuggestionRepository] into
 * [KeyboardViewModel].
 *
 * Used by [OdiaInputMethodService] to create the ViewModel with the real ML
 * backend. The ViewModel constructor signature is unchanged — the factory just
 * satisfies the [SuggestionRepository] parameter.
 *
 * Usage inside the Service:
 * ```kotlin
 * private val viewModel: KeyboardViewModel by lazy {
 *     ViewModelProvider(
 *         lifecycleOwner,
 *         KeyboardViewModelFactory(this)
 *     )[KeyboardViewModel::class.java]
 * }
 * ```
 */
class KeyboardViewModelFactory(context: Context) : ViewModelProvider.Factory {

    // Use applicationContext to prevent Service context leaks into the ViewModel
    private val appContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == KeyboardViewModel::class.java) {
            "KeyboardViewModelFactory can only create KeyboardViewModel"
        }
        val repository = OdiaSuggestionRepository(appContext)
        return KeyboardViewModel(repository) as T
    }
}