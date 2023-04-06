package io.github.lee0701.mboard.input

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import io.github.lee0701.mboard.ime.KeyboardState
import io.github.lee0701.mboard.ime.ModifierState
import io.github.lee0701.mboard.keyboard.Keyboard

class BasicSoftInputEngine(
    initSoftKeyboard: () -> Keyboard,
    initInputEngine: (InputEngine.Listener) -> InputEngine,
    override val listener: InputEngine.Listener,
): SoftInputEngine {

    private val doubleTapGap: Int = 500

    private val softKeyboard = initSoftKeyboard()
    private val inputEngine = initInputEngine(listener)

    private var softKeyboardWrapper: Keyboard.ViewWrapper? = null

    private var keyboardState: KeyboardState = KeyboardState()
    private var shiftClickedTime: Long = 0
    private var inputHappened: Boolean = false

    override fun onKey(code: Int, state: KeyboardState) {
        inputEngine.onKey(code, state)
    }

    override fun onDelete() {
        inputEngine.onDelete()
    }

    override fun onReset() {
        keyboardState = KeyboardState()
        inputEngine.onReset()
        updateView()
    }

    override fun getLabels(state: KeyboardState): Map<Int, CharSequence> {
        return inputEngine.getLabels(state)
    }

    override fun initView(context: Context): View {
        val softKeyboardWrapper = softKeyboard.initView(context, this)
        this.softKeyboardWrapper = softKeyboardWrapper
        return softKeyboardWrapper.binding.root
    }

    override fun getView(): View {
        return softKeyboardWrapper!!.binding.root
    }

    private fun updateView() {
        updateLabels(getShiftedLabels() + inputEngine.getLabels(keyboardState))
    }

    private fun getShiftedLabels(): Map<Int, CharSequence> {
        fun label(label: String) =
            if(keyboardState.shiftState.pressed || keyboardState.shiftState.locked) label.uppercase()
            else label.lowercase()
        return softKeyboardWrapper?.keys?.associate { it.key.code to label(it.key.label.orEmpty()) }.orEmpty()
    }

    private fun updateLabels(labels: Map<Int, CharSequence>) {
        val keys = softKeyboardWrapper?.keys ?: return
        keys.map { key ->
            val label = labels[key.key.code]
            if(label != null) key.binding.label.text = label
        }
    }

    override fun onKeyDown(code: Int, output: String?) {
        when(code) {
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> onShiftKeyDown()
        }
    }

    override fun onKeyUp(code: Int, output: String?) {
        when(code) {
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> onShiftKeyUp()
        }
    }

    private fun onShiftKeyDown() {
        val lastState = keyboardState
        val lastShiftState = lastState.shiftState
        val currentShiftState = lastShiftState.copy()
        val newShiftState = currentShiftState.copy()

        keyboardState = lastState.copy(shiftState = newShiftState.copy(pressing = true))
        inputHappened = false
        updateView()
    }

    private fun onShiftKeyUp() {
        val lastState = keyboardState
        val lastShiftState = lastState.shiftState
        val currentShiftState = lastShiftState.copy(pressing = false)

        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - shiftClickedTime

        val newShiftState = if(currentShiftState.locked) {
            ModifierState()
        } else if(currentShiftState.pressed) {
            if(timeDiff < doubleTapGap) {
                ModifierState(pressed = true, locked = true)
            } else {
                ModifierState()
            }
        } else if(inputHappened) {
            ModifierState()
        } else {
            ModifierState(pressed = true)
        }

        keyboardState = lastState.copy(shiftState = newShiftState.copy(pressing = false))
        shiftClickedTime = currentTime
        inputHappened = false
        updateView()
    }

    override fun onKeyClick(code: Int, output: String?) {
        if(listener.onSystemKey(code)) return
        when(code) {
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> {
            }
            KeyEvent.KEYCODE_DEL -> {
                inputEngine.onDelete()
            }
            KeyEvent.KEYCODE_SPACE -> {
                onReset()
                listener.onCommitText(" ")
                autoUnlockShift()
            }
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                onReset()
                listener.onEditorAction(code)
                autoUnlockShift()
            }
            else -> {
                onPrintingKey(code)
                autoUnlockShift()
            }
        }
        updateView()
    }

    private fun onPrintingKey(code: Int) {
        inputEngine.onKey(code, keyboardState)
        inputHappened = true
    }

    private fun autoUnlockShift() {
        val lastState = keyboardState
        val lastShiftState = lastState.shiftState
        if(!lastShiftState.locked && !lastShiftState.pressing) {
            keyboardState = lastState.copy(shiftState = ModifierState())
        }
    }

    override fun onComputeInsets(inputView: View, outInsets: InputMethodService.Insets?) {
        if(outInsets != null) {
            outInsets.touchableInsets = InputMethodService.Insets.TOUCHABLE_INSETS_VISIBLE
            val visibleTopY = inputView.height - (softKeyboardWrapper?.binding?.root?.height ?: return)
            outInsets.visibleTopInsets = visibleTopY
            outInsets.contentTopInsets = visibleTopY
        }
    }
}