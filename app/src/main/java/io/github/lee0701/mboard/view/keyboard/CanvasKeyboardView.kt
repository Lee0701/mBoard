package io.github.lee0701.mboard.view.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.android.material.color.DynamicColors
import io.github.lee0701.mboard.R
import io.github.lee0701.mboard.module.softkeyboard.Key
import io.github.lee0701.mboard.module.softkeyboard.KeyType
import io.github.lee0701.mboard.module.softkeyboard.Keyboard
import kotlin.math.roundToInt

open class CanvasKeyboardView(
    context: Context,
    attrs: AttributeSet?,
    keyboard: Keyboard,
    theme: Theme,
    listener: KeyboardListener,
): KeyboardView(context, attrs, keyboard, theme, listener) {

    private val rect = Rect()
    private val bitmapPaint = Paint()
    private val textPaint = Paint()

    private val keyMarginHorizontal: Float
    private val keyMarginVertical: Float

    private val keyboardBackground: Drawable
    private val keyBackgrounds: Map<KeyType, Pair<Drawable, ColorStateList>>
    private val keyIconTints: Map<KeyType, Int>
    private val keyLabelTextColors: Map<KeyType, Int>
    private val keyLabelTextSizes: Map<KeyType, Float>

    private val cachedKeys: MutableList<CachedKey> = mutableListOf()
    override val wrappedKeys: List<KeyWrapper> get() = cachedKeys.toList()

    init {
        textPaint.textAlign = Paint.Align.CENTER
        keyMarginHorizontal = resources.getDimension(R.dimen.key_margin_horizontal)
        keyMarginVertical = resources.getDimension(R.dimen.key_margin_vertical)

        val keyboardContext = DynamicColors.wrapContextIfAvailable(context, theme.keyboardBackground).let {
            if(it == context) ContextThemeWrapper(context, theme.keyboardBackground) else it
        }
        keyboardContext.theme.resolveAttribute(R.attr.background, typedValue, true)
        val background = ContextCompat.getDrawable(keyboardContext, typedValue.resourceId) ?: ColorDrawable(
            Color.WHITE)
        keyboardContext.theme.resolveAttribute(R.attr.backgroundTint, typedValue, true)
        val backgroundTint = ContextCompat.getColor(keyboardContext, typedValue.resourceId)
        DrawableCompat.setTint(background, backgroundTint)
        this.keyboardBackground = background

        val keyContexts = theme.keyBackground.mapValues { (_, id) ->
            DynamicColors.wrapContextIfAvailable(context, id).let {
                if(it == context) ContextThemeWrapper(context, id) else it
            }
        }
        keyBackgrounds = keyContexts.mapValues { (_, keyContext) ->
            keyContext.theme.resolveAttribute(R.attr.background, typedValue, true)
            val keyBackground = ContextCompat.getDrawable(keyContext, typedValue.resourceId) ?: ColorDrawable(
                Color.TRANSPARENT)
            keyContext.theme.resolveAttribute(R.attr.backgroundTint, typedValue, true)
            val keyBackgroundTint = ContextCompat.getColorStateList(keyContext, typedValue.resourceId) ?: ColorStateList(arrayOf(), intArrayOf())
            keyBackground to keyBackgroundTint
        }
        keyIconTints = keyContexts.mapValues { (_, keyContext) ->
            keyContext.theme.resolveAttribute(R.attr.iconTint, typedValue, true)
            ContextCompat.getColor(keyContext, typedValue.resourceId)
        }
        keyLabelTextColors = keyContexts.mapValues { (_, keyContext) ->
            keyContext.theme.resolveAttribute(android.R.attr.textColor, typedValue, true)
            ContextCompat.getColor(keyContext, typedValue.resourceId)
        }
        keyLabelTextSizes = keyContexts.mapValues { (_, keyContext) ->
            keyContext.theme.resolveAttribute(android.R.attr.textSize, typedValue, true)
            context.resources.getDimension(typedValue.resourceId)
        }

        setWillNotDraw(false)
        cacheKeys()
    }

    fun cacheKeys() {
        val rowHeight = keyboardHeight / keyboard.rows.size
        keyboard.rows.forEachIndexed { j, row ->
            val keyWidths = row.keys.map { it.width }.sum() + row.padding*2
            val keyWidthUnit = keyboardWidth / keyWidths
            var x = row.padding * keyWidthUnit
            val y = j * rowHeight
            row.keys.forEachIndexed { i, key ->
                val width = keyWidthUnit * key.width
                val height = rowHeight
                val label = key.label
                val icon = theme.keyIcon[key.iconType]?.let { ContextCompat.getDrawable(context, it) }
                cachedKeys += CachedKey(key, x.roundToInt(), y, width.roundToInt(), height, icon)
                x += width
            }
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas?) {
        if(canvas == null) return
        getLocalVisibleRect(rect)
        val bitmapCache = mutableMapOf<BitmapCacheKey, Bitmap>()

        // Draw keyboard background
        canvas.drawBitmap(keyboardBackground.toBitmap(rect.width(), rect.height()), 0f, 0f, bitmapPaint)

        // Draw key backgrounds
        cachedKeys.forEach { key ->
            val keyBackgroundOverride = key.key.backgroundType?.resId?.let { ContextCompat.getDrawable(context, it) }
            val keyBackgroundInfo = keyBackgrounds[key.key.type]
            val pressed = keyStates[key.key.code] == true
            if(keyBackgroundInfo != null) {
                val drawable = keyBackgroundOverride ?: keyBackgroundInfo.first.mutate().constantState?.newDrawable()
                val background = drawable?.apply {
                    val keyState = intArrayOf(if(pressed) android.R.attr.state_pressed else -android.R.attr.state_pressed)
                    DrawableCompat.setTint(this, keyBackgroundInfo.second.getColorForState(keyState, keyBackgroundInfo.second.defaultColor))
                } ?: keyBackgroundInfo.first
                val extendAmount = context.resources.getDimension(R.dimen.key_bg_radius)*2 + context.resources.getDimension(R.dimen.key_margin_horizontal)*2
                val extendTop = if(key.key.backgroundType?.extendTop == true) extendAmount else 0f
                val extendBottom = if(key.key.backgroundType?.extendBottom == true) extendAmount else 0f
                val x = key.x + keyMarginHorizontal
                val y = key.y + keyMarginVertical - extendTop
                val width = (key.width - keyMarginHorizontal*2)
                val height = (key.height - keyMarginVertical*2) + extendTop + extendBottom
                val bitmap = bitmapCache.getOrPut(BitmapCacheKey(width.roundToInt(), height.roundToInt(), pressed, key.key.type)) {
                    background.toBitmap(width.roundToInt(), height.roundToInt())
                }
                canvas.drawBitmap(bitmap, x, y, bitmapPaint)
            }
        }

        // Draw key foregrounds
        cachedKeys.forEach { key ->
            val baseX = key.x + key.width/2
            val baseY = key.y + key.height/2
            val tint = keyIconTints[key.key.type]
            if(key.icon != null && tint != null) {
                DrawableCompat.setTint(key.icon, tint)
                val bitmap = key.icon.toBitmap()
                val x = baseX - bitmap.width/2
                val y = baseY - bitmap.height/2
                canvas.drawBitmap(bitmap, x.toFloat(), y.toFloat(), bitmapPaint)
            }
            val textSize = keyLabelTextSizes[key.key.type]
            val textColor = keyLabelTextColors[key.key.type]
            if(key.key.label != null && textSize != null && textColor != null) {
                textPaint.color = textColor
                textPaint.textSize = textSize
                val x = baseX.toFloat()
                val y = baseY - (textPaint.descent() + textPaint.ascent())/2
                canvas.drawText(key.key.label, x, y, textPaint)
            }
        }
    }

    override fun updateLabelsAndIcons(labels: Map<Int, CharSequence>, icons: Map<Int, Drawable>) {
        val cachedKeys = this.cachedKeys.toList()
        this.cachedKeys.clear()
        this.cachedKeys += cachedKeys.map { key ->
            if(key.icon != null) {
                key.copy(icon = icons[key.key.code] ?: key.icon)
            } else {
                key.copy(key = key.key.copy(label = labels[key.key.code]?.toString()))
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(keyboardWidth.roundToInt(), keyboardHeight)
    }

    data class CachedKey(
        override val key: Key,
        override val x: Int,
        override val y: Int,
        override val width: Int,
        override val height: Int,
        override val icon: Drawable?,
    ): KeyWrapper
    data class BitmapCacheKey(
        val width: Int,
        val height: Int,
        val pressed: Boolean,
        val type: KeyType,
    )

    override fun findKey(x: Int, y: Int): KeyWrapper? {
        wrappedKeys.forEach { key ->
            if(x in key.x until key.x+key.width) {
                if(y in key.y until key.y+key.height) {
                    return key
                }
            }
        }
        return null
    }

    override fun showPopup(key: KeyWrapper, popup: KeyPopup) {
        popup.apply {
            val parentX = key.x + key.width/2
            val parentY = key.y + resources.getDimension(R.dimen.candidates_view_height).toInt() + key.height/2
            show(this@CanvasKeyboardView, key.key.label, key.icon, parentX, parentY)
        }
    }

    override fun postViewChanged() {
        invalidate()
    }
}