package io.github.lee0701.mboard.preset.softkeyboard

import kotlinx.serialization.Serializable

@Serializable
enum class KeyIconType {
    Shift,
    ShiftLock,
    Caps,
    Option,
    Tab,
    Backspace,
    Language,
    Return,

    Left,
    Right,
    ExpandLeft,
    ExpandRight,
    SelectAll,
    Cut,
    Copy,
    Paste,
}