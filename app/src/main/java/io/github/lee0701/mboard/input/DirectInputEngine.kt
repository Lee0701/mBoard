package io.github.lee0701.mboard.input

import android.view.KeyCharacterMap
import io.github.lee0701.mboard.service.KeyboardState

class DirectInputEngine(
    override val listener: InputEngine.Listener,
): InputEngine {

    private val keyCharacterMap: KeyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)

    override fun onKey(code: Int, state: KeyboardState) {
        val char = keyCharacterMap.get(code, state.asMetaState())
        if(char > 0) listener.onCommitText(char.toChar().toString())
    }

    override fun onDelete() {
        listener.onDeleteText(1, 0)
    }

    override fun onReset() {
        listener.onFinishComposing()
    }

    override fun getLabels(state: KeyboardState): Map<Int, CharSequence> {
        return Companion.getLabels(keyCharacterMap, state)
    }

    companion object {
        fun getLabels(keyCharacterMap: KeyCharacterMap, state: KeyboardState): Map<Int, CharSequence> {
            val range = 0 .. 304
            return range.map { keyCode -> keyCode to keyCharacterMap.get(keyCode, state.asMetaState()) }
                .mapNotNull { (keyCode, label) -> (if(label == 0) null else label)?.let { keyCode to it } }.toMap()
                .mapValues { (_, label) -> label.toChar().toString() }
        }
    }
}