package io.github.lee0701.mboard.ime

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import io.github.lee0701.mboard.input.*
import io.github.lee0701.mboard.layout.SoftKeyboardLayout
import io.github.lee0701.mboard.layout.HangulLayout
import io.github.lee0701.mboard.layout.SymbolLayout

class MboardIME: InputMethodService(), InputEngine.Listener {

    private var inputView: FrameLayout? = null
    private var inputEngineSwitcher: InputEngineSwitcher? = null

    private val layout = SoftKeyboardLayout.LAYOUT_QWERTY_MOBILE

    override fun onCreate() {
        super.onCreate()
        val engines = listOf(
            BasicSoftInputEngine(
                { SoftKeyboardLayout.LAYOUT_QWERTY_MOBILE },
                { DirectInputEngine(it) },
                this
            ),
            BasicSoftInputEngine(
                { SoftKeyboardLayout.LAYOUT_QWERTY_SEBEOLSIK_390_MOBILE },
                { HangulInputEngine(HangulLayout.LAYOUT_HANGUL_SEBEOL_390, HangulLayout.COMB_SEBEOL_390, it) },
                this
            ),
            BasicSoftInputEngine(
                { SoftKeyboardLayout.LAYOUT_QWERTY_MOBILE_WITH_SEMICOLON },
                { CodeConverterInputEngine(SymbolLayout.LAYOUT_SYMBOLS_G, it) },
                this
            ),
        )
        val table = arrayOf(
            intArrayOf(0, 2),
            intArrayOf(1, 2),
        )
        val switcher = InputEngineSwitcher(engines, table)
        switcher.initViews(this)
        this.inputEngineSwitcher = switcher
    }

    override fun onCreateInputView(): View {
        val inputView = FrameLayout(this, null)
        val currentInputEngine = inputEngineSwitcher?.getCurrentEngine()
        val keyboardView =
            if(currentInputEngine is SoftInputEngine) currentInputEngine.getView()
            else null
        if(keyboardView != null) inputView.addView(keyboardView)
        this.inputView = inputView
        return inputView
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
    }

    override fun onSystemKey(code: Int): Boolean {
        return when(code) {
            KeyEvent.KEYCODE_LANGUAGE_SWITCH -> {
                inputEngineSwitcher?.nextLanguage()
                updateView()
                true
            }
            KeyEvent.KEYCODE_SYM -> {
                inputEngineSwitcher?.nextLanguage()
                updateView()
                true
            }
            else -> false
        }
    }

    override fun onEditorAction(code: Int) {
        if(!sendDefaultEditorAction(true)) sendDownUpKeyEvents(code)
    }

    override fun onComposingText(text: CharSequence) {
        val inputConnection = currentInputConnection ?: return
        inputConnection.setComposingText(text, 1)
    }

    override fun onFinishComposing() {
        val inputConnection = currentInputConnection ?: return
        inputConnection.finishComposingText()
    }

    override fun onCommitText(text: CharSequence) {
        val inputConnection = currentInputConnection ?: return
        inputConnection.commitText(text, 1)
    }

    override fun onDeleteText(beforeLength: Int, afterLength: Int) {
        val inputConnection = currentInputConnection ?: return
        inputConnection.deleteSurroundingText(beforeLength, afterLength)
    }

    override fun onComputeInsets(outInsets: Insets?) {
        val inputView = this.inputView ?: return
        val currentEngine = inputEngineSwitcher?.getCurrentEngine()
        if(currentEngine is SoftInputEngine) currentEngine.onComputeInsets(inputView, outInsets)
        else return super.onComputeInsets(outInsets)
    }

    private fun updateView() {
        val inputView = inputView ?: return
        val inputEngine = inputEngineSwitcher?.getCurrentEngine()
        inputView.removeAllViews()
        if(inputEngine is SoftInputEngine) {
            inputView.addView(inputEngine.getView())
            inputEngine.onReset()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}