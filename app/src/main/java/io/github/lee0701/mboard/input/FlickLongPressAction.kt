package io.github.lee0701.mboard.input

import io.github.lee0701.mboard.service.KeyboardState

sealed interface FlickLongPressAction {
    fun onKey(code: Int, keyboardState: KeyboardState, inputEngine: InputEngine)

    object None: FlickLongPressAction {
        override fun onKey(code: Int, keyboardState: KeyboardState, inputEngine: InputEngine) {
        }
    }

    object Repeat: FlickLongPressAction {
        override fun onKey(code: Int, keyboardState: KeyboardState, inputEngine: InputEngine) {
            // This action will be intercepted and be processed by Soft Keyboard
        }
    }

    object Shifted: FlickLongPressAction {
        override fun onKey(code: Int, keyboardState: KeyboardState, inputEngine: InputEngine) {
            inputEngine.onKey(code, makeShiftOn(keyboardState))
        }
    }

    object Symbols: FlickLongPressAction {
        override fun onKey(code: Int, keyboardState: KeyboardState, inputEngine: InputEngine) {
            if(inputEngine is BasicSoftInputEngine) {
                inputEngine.onReset()
                inputEngine.symbolsInputEngine?.onKey(code, keyboardState)
            }
        }
    }

    object ShiftedSymbols: FlickLongPressAction {
        override fun onKey(code: Int, keyboardState: KeyboardState, inputEngine: InputEngine) {
            if(inputEngine is BasicSoftInputEngine) {
                inputEngine.onReset()
                inputEngine.symbolsInputEngine?.onKey(code, makeShiftOn(keyboardState))
            }
        }
    }

    object AlternativeLanguage: FlickLongPressAction {
        override fun onKey(code: Int, keyboardState: KeyboardState, inputEngine: InputEngine) {
            if(inputEngine is BasicSoftInputEngine) {
                inputEngine.onReset()
                inputEngine.alternativeInputEngine?.onKey(code, keyboardState)
            }
        }
    }

    companion object {
        fun of(value: String): FlickLongPressAction {
            return when(value) {
                "repeat" -> Repeat
                "symbol" -> Symbols
                "shift" -> Shifted
                "shift_symbol" -> ShiftedSymbols
                "alternative_language" -> AlternativeLanguage
                else -> None
            }
        }

        fun makeShiftOn(keyboardState: KeyboardState): KeyboardState
                = keyboardState.copy(shiftState = keyboardState.shiftState.copy(pressed = true))
    }

}