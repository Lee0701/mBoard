package io.github.lee0701.mboard.keyboard

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.StyleRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.LinearLayoutCompat
import io.github.lee0701.mboard.KeyboardListener
import io.github.lee0701.mboard.R
import io.github.lee0701.mboard.databinding.KeyboardKeyBinding

data class Key(
    val code: Int,
    val output: String?,
    val label: String? = output,
    val icon: Int? = null,
    val width: Float = 1f,
    val type: Type = Type.Alphanumeric,
) {
    fun initView(context: Context, listener: KeyboardListener): View {
        val wrappedContext = ContextThemeWrapper(context, type.styleId)
        return KeyboardKeyBinding.inflate(LayoutInflater.from(wrappedContext)).apply {
            val key = this@Key
            if(key.label != null) label.text = key.label
            if(key.icon != null) icon.setImageResource(key.icon)
            root.layoutParams = LinearLayoutCompat.LayoutParams(
                0, LinearLayoutCompat.LayoutParams.MATCH_PARENT
            ).apply {
                weight = key.width
            }
            root.setOnClickListener {
                listener.onKey(key.code, key.output)
            }
        }.root
    }

    enum class Type(
        @StyleRes val styleId: Int,
    ) {
        Alphanumeric(R.style.Theme_Mboard_Keyboard_Key),
        Modifier(R.style.Theme_Mboard_Keyboard_Key_Mod),
        Return(R.style.Theme_Mboard_Keyboard_Key_Return),
    }
}
