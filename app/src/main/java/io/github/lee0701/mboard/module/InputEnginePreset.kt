package io.github.lee0701.mboard.module

import android.content.Context
import android.widget.Toast
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.decodeFromStream
import io.github.lee0701.converter.library.engine.HanjaConverter
import io.github.lee0701.converter.library.engine.Predictor
import io.github.lee0701.mboard.R
import io.github.lee0701.mboard.input.BasicSoftInputEngine
import io.github.lee0701.mboard.input.CodeConverterInputEngine
import io.github.lee0701.mboard.input.HangulInputEngine
import io.github.lee0701.mboard.input.HanjaConverterBuilder
import io.github.lee0701.mboard.input.HanjaConverterInputEngine
import io.github.lee0701.mboard.input.InputEngine
import io.github.lee0701.mboard.module.softkeyboard.Keyboard
import io.github.lee0701.mboard.module.table.CodeConvertTable
import io.github.lee0701.mboard.module.table.JamoCombinationTable
import io.github.lee0701.mboard.module.table.MoreKeysTable
import io.github.lee0701.mboard.service.MBoardIME
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.modules.EmptySerializersModule

@Serializable
sealed interface InputEnginePreset {

    fun inflate(ime: MBoardIME): InputEngine

    fun mutable(): Mutable

    val unifyHeight: Boolean
    val rowHeight: Int

    val autoUnlockShift: Boolean
    val showCandidatesView: Boolean

    val softKeyboard: List<String>
    val moreKeysTable: List<String>
    val codeConvertTable: List<String>

    fun loadSoftKeyboards(context: Context, names: List<String>): Keyboard {
        val resolved = names.map { filename ->
            yaml.decodeFromStream<Keyboard>(context.assets.open(filename))
        }
        return resolved.fold(Keyboard()) { acc, input -> acc + input }
    }

    fun loadConvertTable(context: Context, names: List<String>): CodeConvertTable {
        val resolved = names.map { filename ->
            yaml.decodeFromStream<CodeConvertTable>(context.assets.open(filename)) }
        return resolved.reduce { acc, input -> acc + input }
    }

    fun loadCombinationTable(context: Context, names: List<String>): JamoCombinationTable {
        val resolved = names.map { filename ->
            yaml.decodeFromStream<JamoCombinationTable>(context.assets.open(filename)) }
        return resolved.fold(JamoCombinationTable()) { acc, input -> acc + input }
    }

    fun loadMoreKeysTable(context: Context, names: List<String>): MoreKeysTable {
        val resolved = names.map { filename ->
            val refMap = yaml.decodeFromStream<MoreKeysTable.RefMap>(context.assets.open(filename))
            refMap.resolve(context.assets, yaml)
        }
        return resolved.fold(MoreKeysTable()) { acc, input -> acc + input }
    }

    @Serializable
    @SerialName("latin")
    data class Latin(
        override val softKeyboard: List<String> = listOf(),
        override val moreKeysTable: List<String> = listOf(),
        override val codeConvertTable: List<String> = listOf(),
        override val unifyHeight: Boolean = false,
        override val rowHeight: Int = 55,
        override val autoUnlockShift: Boolean = true,
        override val showCandidatesView: Boolean = false,
    ): InputEnginePreset {
        override fun inflate(ime: MBoardIME): InputEngine {
            val keyboard = loadSoftKeyboards(ime, names = softKeyboard)
            val moreKeysTable = loadMoreKeysTable(ime, names = moreKeysTable)
            val convertTable = loadConvertTable(ime, names = codeConvertTable)
            return BasicSoftInputEngine(
                keyboard = keyboard,
                getInputEngine = { listener -> CodeConverterInputEngine(convertTable, moreKeysTable, listener) },
                unifyHeight = unifyHeight,
                rowHeight = rowHeight,
                autoUnlockShift = autoUnlockShift,
                showCandidatesView = showCandidatesView,
                listener = ime,
            )
        }

        override fun mutable(): Mutable {
            return Mutable(
                type = Type.Latin,
                unifyHeight = this.unifyHeight,
                rowHeight = this.rowHeight,
                autoUnlockShift = this.autoUnlockShift,
                showCandidatesView = this.showCandidatesView,
                enableHanjaConversion = false,
                enableHanjaPrediction = false,
                softKeyboard = this.softKeyboard,
                moreKeysTable = this.moreKeysTable,
                codeConvertTable = this.codeConvertTable,
                combinationTable = listOf(),
            )
        }
    }

    @Serializable
    @SerialName("hangul")
    data class Hangul(
        override val softKeyboard: List<String> = listOf(),
        override val moreKeysTable: List<String> = listOf(),
        override val codeConvertTable: List<String> = listOf(),
        val combinationTable: List<String> = listOf(),
        override val unifyHeight: Boolean = false,
        override val rowHeight: Int = 55,
        override val autoUnlockShift: Boolean = true,
        override val showCandidatesView: Boolean = false,
        val enableHanjaConversion: Boolean = false,
        val enableHanjaPrediction: Boolean = false,
    ): InputEnginePreset {
        override fun inflate(ime: MBoardIME): InputEngine {
            val keyboard = loadSoftKeyboards(ime, names = softKeyboard)
            val moreKeysTable = loadMoreKeysTable(ime, names = moreKeysTable)
            val convertTable = loadConvertTable(ime, names = codeConvertTable)
            val combinationTable = loadCombinationTable(ime, names = combinationTable)
            val (converter, predictor) =
                if(enableHanjaConversion)createHanjaConverter(ime, prediction = enableHanjaPrediction)
                else (null to null)
            return BasicSoftInputEngine(
                keyboard = keyboard,
                getInputEngine = { listener ->
                    if(enableHanjaConversion) {
                        HanjaConverterInputEngine({ l ->
                            HangulInputEngine(convertTable, moreKeysTable, combinationTable, l)
                        }, converter, predictor, listener)
                    } else {
                        HangulInputEngine(convertTable, moreKeysTable, combinationTable, listener)
                    }
                },
                unifyHeight = unifyHeight,
                rowHeight = rowHeight,
                autoUnlockShift = autoUnlockShift,
                showCandidatesView = showCandidatesView,
                listener = ime,
            )
        }
        override fun mutable(): Mutable {
            return Mutable(
                type = Type.Hangul,
                unifyHeight = this.unifyHeight,
                rowHeight = this.rowHeight,
                autoUnlockShift = this.autoUnlockShift,
                showCandidatesView = this.showCandidatesView,
                enableHanjaConversion = this.enableHanjaConversion,
                enableHanjaPrediction = this.enableHanjaPrediction,
                softKeyboard = this.softKeyboard,
                moreKeysTable = this.moreKeysTable,
                codeConvertTable = this.codeConvertTable,
                combinationTable = this.combinationTable,
            )
        }
    }

    data class Mutable (
        var type: Type = Type.Latin,
        var unifyHeight: Boolean = false,
        var rowHeight: Int = 55,
        var autoUnlockShift: Boolean = true,
        var showCandidatesView: Boolean = false,
        var enableHanjaConversion: Boolean = false,
        var enableHanjaPrediction: Boolean = false,
        var softKeyboard: List<String> = listOf(),
        var moreKeysTable: List<String> = listOf(),
        var codeConvertTable: List<String> = listOf(),
        var combinationTable: List<String> = listOf(),
    ) {
        fun commit(): InputEnginePreset {
            return when(type) {
                Type.Latin -> Latin(
                    softKeyboard = softKeyboard,
                    moreKeysTable = moreKeysTable,
                    codeConvertTable = codeConvertTable,
                    unifyHeight = unifyHeight,
                    rowHeight = rowHeight,
                    showCandidatesView = showCandidatesView,
                    autoUnlockShift = autoUnlockShift,
                )
                Type.Hangul -> {
                    Hangul(
                        softKeyboard = softKeyboard,
                        moreKeysTable = moreKeysTable,
                        codeConvertTable = codeConvertTable,
                        combinationTable = combinationTable,
                        unifyHeight = unifyHeight,
                        rowHeight = rowHeight,
                        showCandidatesView = showCandidatesView,
                        autoUnlockShift = autoUnlockShift,
                    )
                }
            }
        }
    }

    enum class Type {
        Latin, Hangul
    }

    companion object {
        private val yamlConfig = YamlConfiguration(encodeDefaults = false)
        val yaml = Yaml(EmptySerializersModule(), yamlConfig)

        private fun createHanjaConverter(ime: MBoardIME, prediction: Boolean): Pair<HanjaConverter?, Predictor?> {
            if(prediction) {
                val (converter, predictor) = HanjaConverterBuilder.build(ime)
                if(converter != null && predictor != null) return converter to predictor
                else Toast.makeText(ime, R.string.msg_hanja_converter_donation_not_found, Toast.LENGTH_LONG).show()
            }

            val (converter, _) = HanjaConverterBuilder.build(ime)
            if(converter != null) return converter to null
            else Toast.makeText(ime, R.string.msg_hanja_converter_not_found, Toast.LENGTH_LONG).show()

            return null to null
        }

    }
}