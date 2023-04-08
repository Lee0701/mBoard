package io.github.lee0701.mboard.service

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import io.github.lee0701.mboard.R
import io.github.lee0701.mboard.input.*
import io.github.lee0701.mboard.view.candidates.BasicCandidatesViewManager
import kotlin.math.roundToInt

class MBoardIME: InputMethodService(), InputEngine.Listener, BasicCandidatesViewManager.Listener, OnSharedPreferenceChangeListener {

    private var inputView: ViewGroup? = null
    private var defaultCandidatesViewManager: BasicCandidatesViewManager? = null
    private var inputEngineSwitcher: InputEngineSwitcher? = null

    override fun onCreate() {
        super.onCreate()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        reload(sharedPreferences)
    }

    private fun reload(sharedPreferences: SharedPreferences, force: Boolean = false) {
        val hangulPresetKey = sharedPreferences.getString("layout_hangul_preset", "layout_3set_390")!!
        val latinPresetKey = sharedPreferences.getString("layout_latin_preset", "layout_qwerty")!!

        val engines = listOf(
            InputEnginePresets.of(latinPresetKey, this),
            InputEnginePresets.of(hangulPresetKey, this),
            InputEnginePresets.SymbolsG(this),
        ).map { it ?: DirectInputEngine(this) }

        val table = arrayOf(
            intArrayOf(0, 2),
            intArrayOf(1, 2),
        )
        val switcher = InputEngineSwitcher(engines, table)
        this.inputEngineSwitcher = switcher

        defaultCandidatesViewManager = BasicCandidatesViewManager(this)

        if(force) setInputView(onCreateInputView())
    }

    override fun onCreateInputView(): View {
        inputEngineSwitcher?.initViews(this)
        val inputView = LinearLayout(this, null)
        inputView.orientation = LinearLayout.VERTICAL
        inputView.removeAllViews()
        val currentInputEngine = inputEngineSwitcher?.getCurrentEngine()

        val candidatesView = defaultCandidatesViewManager?.initView(this)
        if(candidatesView != null) {
            inputView.addView(candidatesView)
        }

        val keyboardView =
            if(currentInputEngine is SoftInputEngine) currentInputEngine.initView(this)
            else null
        if(keyboardView != null) {
            inputView.addView(keyboardView)
            val typedValue = TypedValue()
            keyboardView.context.theme.resolveAttribute(R.attr.background, typedValue, true)
            val color = ContextCompat.getColor(this, typedValue.resourceId)
            setNavBarColor(color)
        }
        this.inputView = inputView
        return inputView
    }

    override fun onCandidates(list: List<Candidate>) {
        val sorted = list.sortedByDescending { it.score }
        defaultCandidatesViewManager?.showCandidates(sorted)
    }

    override fun onItemClicked(candidate: Candidate) {
        onComposingText(candidate.text)
        onFinishComposing()
        inputEngineSwitcher?.getCurrentEngine()?.onReset()
        onCandidates(listOf())
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        val inputEngine = inputEngineSwitcher?.getCurrentEngine()
        inputEngine?.onReset()
        if(inputEngine is SoftInputEngine) inputEngine.onResetView()
    }

    override fun onFinishInput() {
        super.onFinishInput()
    }

    override fun onSystemKey(code: Int): Boolean {
        val inputEngine = inputEngineSwitcher?.getCurrentEngine()
        return when(code) {
            KeyEvent.KEYCODE_LANGUAGE_SWITCH -> {
                inputEngineSwitcher?.nextLanguage()
                if(inputEngine is SoftInputEngine) inputEngine.onResetView()
                updateView()
                true
            }
            KeyEvent.KEYCODE_SYM -> {
                inputEngineSwitcher?.nextExtra()
                if(inputEngine is SoftInputEngine) inputEngine.onResetView()
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
        if(currentEngine is SoftInputEngine) {
            val candidatesHeight = resources.getDimension(R.dimen.candidates_view_height).roundToInt()
            currentEngine.onComputeInsets(inputView, outInsets)
            if(outInsets == null) return
            outInsets.visibleTopInsets -= candidatesHeight
            outInsets.contentTopInsets -= candidatesHeight
        }
        else return super.onComputeInsets(outInsets)
    }

    private fun setNavBarColor(@ColorInt color: Int) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.window?.navigationBarColor = color
        }
    }

    private fun updateView() {
        setInputView(onCreateInputView())
    }

    override fun onDestroy() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroy()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if(sharedPreferences != null) reload(sharedPreferences, true)
    }
}