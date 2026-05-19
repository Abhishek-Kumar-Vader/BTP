package com.example.odiakeyboard.ime

import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.odiakeyboard.ml.KeyboardViewModelFactory
import com.example.odiakeyboard.model.toKeyboardInputType
import com.example.odiakeyboard.ui.keyboard.KeyboardScreen
import com.example.odiakeyboard.ui.theme.OdiaKeyboardTheme
import com.example.odiakeyboard.viewmodel.InputConnectionHandler
import com.example.odiakeyboard.viewmodel.KeyboardViewModel

class OdiaInputMethodService : InputMethodService() {

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    private val lifecycleOwner = ImeLifecycleOwner()

    // ── ViewModel — now created with the ML factory ───────────────────────────

    private val viewModel: KeyboardViewModel by lazy {
        ViewModelProvider(
            lifecycleOwner,
            KeyboardViewModelFactory(this),   // ← Phase 2: injects OdiaSuggestionRepository
        )[KeyboardViewModel::class.java]
    }

    // ── Haptic ────────────────────────────────────────────────────────────────

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
    }

    // ── InputConnectionHandler ────────────────────────────────────────────────

    private val icHandler = object : InputConnectionHandler {
        private fun ic(): InputConnection? = currentInputConnection

        override fun commitText(text: String) {
            ic()?.commitText(text, 1)
        }
        override fun deleteSurroundingText(beforeLength: Int, afterLength: Int) {
            ic()?.deleteSurroundingText(beforeLength, afterLength)
        }
        override fun performEditorAction(actionId: Int) {
            ic()?.performEditorAction(
                if (actionId == EditorInfo.IME_ACTION_UNSPECIFIED)
                    currentInputEditorInfo?.imeOptions
                        ?.and(EditorInfo.IME_MASK_ACTION)
                        ?: EditorInfo.IME_ACTION_DONE
                else actionId
            )
        }
        override fun sendKeyEvent(keyCode: Int, metaState: Int) {
            ic()?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode))
            ic()?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP,   keyCode))
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Service lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        lifecycleOwner.onCreate()
        lifecycleOwner.onStart()
        viewModel.attachInputConnectionHandler(icHandler)
    }

    override fun onCreateInputView(): View {
        lifecycleOwner.onResume()

        // Fix: Compose requires the owners to be set on the window's decor view
        // to handle certain cases (like when it's placed inside a parentPanel).
        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(lifecycleOwner)
            decorView.setViewTreeViewModelStoreOwner(lifecycleOwner)
            decorView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        }

        return ComposeView(this).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool
            )
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                OdiaKeyboardTheme {
                    KeyboardScreen(
                        viewModel   = viewModel,
                        onKeyHaptic = { triggerHaptic() },
                    )
                }
            }
        }
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        val inputType = attribute?.inputType?.toKeyboardInputType()
            ?: com.example.odiakeyboard.model.InputType.TEXT
        viewModel.onStartInput(inputType)
    }

    override fun onFinishInput() {
        super.onFinishInput()
        // Do NOT detach — handler safely returns null when IC is stale
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // StateFlow drives automatic recomposition — no manual action needed
    }

    override fun onDestroy() {
        viewModel.detachInputConnectionHandler()
        lifecycleOwner.onPause()
        lifecycleOwner.onStop()
        lifecycleOwner.onDestroy()
        super.onDestroy()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Haptic feedback
    // ─────────────────────────────────────────────────────────────────────────

    private fun triggerHaptic() {
        if (!isHapticFeedbackEnabled()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(20L)
            }
        } catch (_: Exception) { }
    }

    private fun isHapticFeedbackEnabled(): Boolean =
        android.provider.Settings.System.getInt(
            contentResolver,
            android.provider.Settings.System.HAPTIC_FEEDBACK_ENABLED,
            1,
        ) != 0
}