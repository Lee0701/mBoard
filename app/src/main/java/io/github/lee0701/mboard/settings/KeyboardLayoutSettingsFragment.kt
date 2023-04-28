package io.github.lee0701.mboard.settings

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.INVISIBLE
import androidx.recyclerview.widget.RecyclerView.VISIBLE
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.charleskorn.kaml.decodeFromStream
import io.github.lee0701.mboard.R
import io.github.lee0701.mboard.preset.InputEnginePreset
import io.github.lee0701.mboard.preset.InputViewComponentType
import io.github.lee0701.mboard.preset.PresetLoader
import io.github.lee0701.mboard.settings.KeyboardLayoutPreferenceDataStore.Companion.KEY_DEFAULT_HEIGHT
import io.github.lee0701.mboard.settings.KeyboardLayoutPreferenceDataStore.Companion.KEY_ENGINE_TYPE
import io.github.lee0701.mboard.settings.KeyboardLayoutPreferenceDataStore.Companion.KEY_HANJA_ADDITIONAL_DICTIONARIES
import io.github.lee0701.mboard.settings.KeyboardLayoutPreferenceDataStore.Companion.KEY_HANJA_CONVERSION
import io.github.lee0701.mboard.settings.KeyboardLayoutPreferenceDataStore.Companion.KEY_HANJA_PREDICTION
import io.github.lee0701.mboard.settings.KeyboardLayoutPreferenceDataStore.Companion.KEY_HANJA_SORT_BY_CONTEXT
import io.github.lee0701.mboard.settings.KeyboardLayoutPreferenceDataStore.Companion.KEY_INPUT_HEADER
import io.github.lee0701.mboard.settings.KeyboardLayoutPreferenceDataStore.Companion.KEY_LAYOUT_PRESET
import io.github.lee0701.mboard.settings.KeyboardLayoutPreferenceDataStore.Companion.KEY_ROW_HEIGHT
import io.github.lee0701.mboard.settings.KeyboardLayoutPreferenceDataStore.Companion.KEY_SHOW_CANDIDATES
import io.github.lee0701.mboard.settings.KeyboardLayoutSettingsActivity.Companion.emptyInputEngineListener
import java.io.File
import java.util.Collections

class KeyboardLayoutSettingsFragment(
    private val fileName: String,
    private val template: String,
): PreferenceFragmentCompat(),
    KeyboardLayoutPreferenceDataStore.OnChangeListener {
    private var preferenceDataStore: KeyboardLayoutPreferenceDataStore? = null
    private var loader: PresetLoader? = null

    private var keyboardViewType: String = "canvas"
    private var themeName: String = "theme_dynamic"

    private var previewMode: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.keyboard_layout_preferences, rootKey)
        val context = context ?: return
        val loader = PresetLoader(context)
        val rootPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        this.loader = loader

        val file = File(context.filesDir, fileName)
        if(!file.exists()) {
            file.outputStream().write(context.assets.open(template).readBytes())
        }

        keyboardViewType = rootPreferences.getString("appearance_keyboard_view_type", "canvas") ?: keyboardViewType
        themeName = rootPreferences.getString("appearance_theme", "theme_dynamic") ?: themeName
        val pref = KeyboardLayoutPreferenceDataStore(context, file, this)
        this.preferenceDataStore = pref
        preferenceManager.preferenceDataStore = pref

        val rootPreference = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val defaultHeightValue = rootPreference.getFloat("appearance_keyboard_height", 55f)

        val defaultHeight = findPreference<SwitchPreference>(KEY_DEFAULT_HEIGHT)
        val rowHeight = findPreference<SliderPreference>(KEY_ROW_HEIGHT)

        val engineType = findPreference<ListPreference>(KEY_ENGINE_TYPE)
        val layoutPreset = findPreference<ListPreference>(KEY_LAYOUT_PRESET)
        val inputHeader = findPreference<PreferenceCategory>(KEY_INPUT_HEADER)

        val showCandidates = findPreference<SwitchPreference>(KEY_SHOW_CANDIDATES)
        val hanjaConversion = findPreference<SwitchPreference>(KEY_HANJA_CONVERSION)
        val hanjaPrediction = findPreference<SwitchPreference>(KEY_HANJA_PREDICTION)
        val sortByContext = findPreference<SwitchPreference>(KEY_HANJA_SORT_BY_CONTEXT)
        val additionalDictionaries = findPreference<MultiSelectListPreference>(KEY_HANJA_ADDITIONAL_DICTIONARIES)

        fun updateByDefaultHeight(newValue: Any?) {
            val enabled = newValue != true
            defaultHeight?.isChecked = !enabled
            rowHeight?.isEnabled = enabled
            if(!enabled) rowHeight?.value = defaultHeightValue
        }
        defaultHeight?.setOnPreferenceChangeListener { _, newValue ->
            updateByDefaultHeight(newValue)
            true
        }
        updateByDefaultHeight(pref.getBoolean(KEY_DEFAULT_HEIGHT, true))

        fun updateByShowCandidates(newValue: Any?) {
            val enabled = newValue == true
            showCandidates?.isChecked = enabled
            hanjaConversion?.isEnabled = enabled
            hanjaPrediction?.isEnabled = enabled
            sortByContext?.isEnabled = enabled
            additionalDictionaries?.isEnabled = enabled

            showCandidates?.isChecked = preferenceDataStore?.getBoolean(KEY_SHOW_CANDIDATES, false) == true
            hanjaConversion?.isChecked = preferenceDataStore?.getBoolean(KEY_HANJA_CONVERSION, false) == true
            hanjaPrediction?.isChecked = preferenceDataStore?.getBoolean(KEY_HANJA_PREDICTION, false) == true
            sortByContext?.isChecked = preferenceDataStore?.getBoolean(KEY_HANJA_SORT_BY_CONTEXT, false) == true
            additionalDictionaries?.values = preferenceDataStore?.getStringSet(KEY_HANJA_ADDITIONAL_DICTIONARIES, mutableSetOf()) ?: mutableSetOf()
        }
        showCandidates?.setOnPreferenceChangeListener { _, newValue ->
            updateByShowCandidates(newValue)
            true
        }
        updateByShowCandidates(pref.getBoolean(KEY_SHOW_CANDIDATES, false))

        fun updateByEngineType(newValue: Any?) {
            inputHeader?.isVisible = newValue == InputEnginePreset.Type.Hangul.name
            val (entries, values) = when(newValue) {
                InputEnginePreset.Type.Hangul.name -> {
                    R.array.preset_hangul_entries to R.array.preset_hangul_values
                }
                InputEnginePreset.Type.Latin.name -> {
                    R.array.preset_latin_entries to R.array.preset_latin_values
                }
                InputEnginePreset.Type.Symbol.name -> {
                    R.array.preset_symbol_entries to R.array.preset_symbol_values
                }
                else -> return
            }
            layoutPreset?.setEntries(entries)
            layoutPreset?.setEntryValues(values)
        }
        engineType?.setOnPreferenceChangeListener { _, newValue ->
            updateByEngineType(newValue)
            layoutPreset?.setValueIndex(0)
            true
        }
        updateByEngineType(pref.getString(KEY_ENGINE_TYPE, "Latin"))
        engineType?.isVisible = false

        layoutPreset?.setOnPreferenceChangeListener { _, newValue ->
            if(newValue !is String) return@setOnPreferenceChangeListener true
            val newLayout = InputEnginePreset.yaml
                .decodeFromStream<InputEnginePreset>(requireContext().assets.open(newValue)).layout
            true
        }
        updateKeyboardView()
    }

    private fun updateKeyboardView() {
        val preset = preferenceDataStore?.preset ?: return
        activity?.findViewById<FrameLayout>(R.id.preview_mode_frame)?.visibility = INVISIBLE
        activity?.findViewById<RecyclerView>(R.id.reorder_mode_recycler_view)?.visibility = INVISIBLE
        if(previewMode) updatePreviewMode(preset)
        else updateReorderMode(preset)
    }

    private fun updatePreviewMode(preset: InputEnginePreset) {
        val context = context ?: return
        val frame = activity?.findViewById<FrameLayout>(R.id.preview_mode_frame) ?: return
        val engine = loader?.mod(preset)?.inflate(context, emptyInputEngineListener) ?: return
        frame.removeAllViews()
        frame.addView(engine.initView(context))
        engine.onReset()
        engine.onResetComponents()
        frame.visibility = VISIBLE
    }

    private fun updateReorderMode(preset: InputEnginePreset) {
        val context = context ?: return
        val preferenceDataStore = preferenceDataStore ?: return
        val components: MutableList<InputViewComponentType> = preset.components.toMutableList()
        val recyclerView = activity?.findViewById<RecyclerView>(R.id.reorder_mode_recycler_view)

        val adapter = KeyboardLayoutPreviewAdapter(context)
        val touchHelper = ItemTouchHelper(TouchCallback { from, to ->
            Collections.swap(components, from.adapterPosition, to.adapterPosition)
            adapter.notifyItemMoved(from.adapterPosition, to.adapterPosition)
            preferenceDataStore.putComponents(components.toList())
            true
        })
        adapter.onItemLongPress = { viewHolder ->
            touchHelper.startDrag(viewHolder)
        }
        adapter.onItemMenuPress = { type, viewHolder ->
            when(type) {
                KeyboardLayoutPreviewAdapter.ItemMenuType.Remove -> {
                    val position = viewHolder.adapterPosition
                    components.removeAt(position)
                    adapter.notifyItemRemoved(position)
                    preferenceDataStore.putComponents(components.toList())
                    preferenceDataStore.update()
                }
                KeyboardLayoutPreviewAdapter.ItemMenuType.MoveUp -> {
                    val position = viewHolder.adapterPosition
                    if(position - 1 in components.indices) {
                        Collections.swap(components, position, position - 1)
                        adapter.notifyItemMoved(position, position - 1)
                    }
                }
                KeyboardLayoutPreviewAdapter.ItemMenuType.MoveDown -> {
                    val position = viewHolder.adapterPosition
                    if(position + 1 in components.indices) {
                        Collections.swap(components, position, position + 1)
                        adapter.notifyItemMoved(position, position + 1)
                    }
                }
                else -> Unit
            }
        }
        recyclerView?.apply {
            this.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            this.adapter = adapter
            touchHelper.attachToRecyclerView(this)
            adapter.submitList(components.map { preset.copy(components = listOf(it)) })
            this.visibility = VISIBLE
        }
    }

    override fun onChange(preset: InputEnginePreset) {
        val rootPreference = PreferenceManager.getDefaultSharedPreferences(context ?: return)
        preferenceDataStore?.write()
        rootPreference.edit().putBoolean("requested_restart", true).apply()
        rootPreference.edit().putBoolean("requested_restart", false).apply()
        updateKeyboardView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_keyboard_layout_setting, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val previewMode = menu.findItem(R.id.preview_mode)
        val changeOrdersMode = menu.findItem(R.id.reorder_mode)
        previewMode.isVisible = false
        changeOrdersMode.isVisible = false
        if(this.previewMode) changeOrdersMode.isVisible = true
        else previewMode.isVisible = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.preview_mode -> {
                previewMode = true
                updateKeyboardView()
                true
            }
            R.id.reorder_mode -> {
                previewMode = false
                updateKeyboardView()
                true
            }
            R.id.add_component -> {
                val dataStore = preferenceDataStore ?: return true
                val bottomSheet = ChooseNewComponentBottomSheetFragment { componentType ->
                    dataStore.putComponents(dataStore.preset.components + componentType)
                    updateKeyboardView()
                }
                bottomSheet.show(childFragmentManager, ChooseNewComponentBottomSheetFragment.TAG)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    class TouchCallback(
        val onMove: (ViewHolder, ViewHolder) -> Boolean,
    ): ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: ViewHolder,
            target: ViewHolder
        ): Boolean {
            return onMove(viewHolder, target)
        }

        override fun onSwiped(viewHolder: ViewHolder, direction: Int) = Unit

        override fun isLongPressDragEnabled(): Boolean = false
    }

    companion object {
        const val NUMBER_ROW_ID = "common/soft_%s_number.yaml"
    }
}