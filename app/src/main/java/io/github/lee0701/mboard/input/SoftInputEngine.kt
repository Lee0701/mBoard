package io.github.lee0701.mboard.input

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.view.View
import io.github.lee0701.mboard.keyboard.Keyboard

interface SoftInputEngine: InputEngine, Keyboard.Listener {
    fun initView(context: Context): View
    fun getView(): View

    fun onComputeInsets(inputView: View, outInsets: InputMethodService.Insets?)

}