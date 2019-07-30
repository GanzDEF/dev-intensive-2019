package ru.skillbranch.devintensive.ui.custom

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.text.TextPaint
import android.util.AttributeSet
import androidx.annotation.ColorRes
import androidx.annotation.Dimension
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import ru.skillbranch.devintensive.R
import kotlin.math.min
import kotlin.math.roundToInt


class CircleImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    defStyle: Int = 0
) : AppCompatImageView(context, attrs, defStyle) {

    companion object {
        private const val DEFAULT_BORDER_WIDTH = 2
        private const val DEFAULT_BORDER_COLOR = Color.WHITE
    }

    private val drawableRect = RectF()
    private val borderRect = RectF()

    private val shaderMatrix = Matrix()
    private val bitmapPaint = Paint()
    private val borderPaint = Paint()
    private val circleBackgroundPaint = Paint()

    private var borderColor = DEFAULT_BORDER_COLOR
    private var borderWidthPx = convertDpToPx(DEFAULT_BORDER_WIDTH)

    private var bitmap: Bitmap? = null
    private var bitmapShader: BitmapShader? = null
    private var bitmapWidth: Int = 0
    private var bitmapHeight: Int = 0

    private var drawableRadius: Float = 0F
    private var borderRadius: Float = 0F

    private var colorFilter: ColorFilter? = null

    private var isReady: Boolean = false
    private var isSetupPending: Boolean = false
    private var isBorderOverlay = false

    private var textPaint: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private var text:String? = null

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.CircleImageView, defStyle, 0)

        borderWidthPx = a.getDimensionPixelSize(R.styleable.CircleImageView_cv_borderWidth, borderWidthPx)
        borderColor = DEFAULT_BORDER_COLOR
        text = a.getString(R.styleable.CircleImageView_cv_text)

        textPaint.textSize = 46f * resources.displayMetrics.scaledDensity
        textPaint.color = Color.WHITE

        a.recycle()

        super.setScaleType(ScaleType.CENTER_CROP)
        isReady = true

        if (isSetupPending) {
            setup()
            isSetupPending = false
        }
    }

    fun getBorderColor() = borderColor

    fun setIntBorderColor(color: Int) {
        if (color != borderColor) {
            borderColor = color
            borderPaint.color = borderColor
            invalidate()
        }
    }

    fun setBorderColor(@ColorRes colorId: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setIntBorderColor(resources.getColor(colorId, context.theme))
        } else {
            setIntBorderColor(resources.getColor(colorId))

        }
    }

    fun setBorderColor(hex:String){
        setIntBorderColor(Color.parseColor(hex))
    }

    private fun convertDpToPx(dp:Int):Int = (dp * context.applicationContext.resources.displayMetrics.density + 0.5f).toInt()
    private fun convertPxToDp(px:Int):Int = (px / context.applicationContext.resources.displayMetrics.density).toInt()

    @Dimension
    fun getBorderWidth() = convertPxToDp(borderWidthPx)

    @Dimension
    fun setBorderWidth(dp: Int) {
        val newWidth = convertDpToPx(dp)
        if (newWidth != borderWidthPx){
            borderWidthPx = newWidth
            setup()
        }
    }

    fun setText(newText:String?){
        text = newText
        setup()
    }

    override fun onDraw(canvas: Canvas) {
        bitmap?:return

        if (borderWidthPx > 0) canvas.drawCircle(borderRect.centerX(), borderRect.centerY(), borderRadius, borderPaint)

        canvas.drawCircle(drawableRect.centerX(), drawableRect.centerY(), drawableRadius, bitmapPaint)

        if (text != null) {
            val centerX = (width * 0.5f).roundToInt()
            val centerY = (height * 0.5f).roundToInt()
            val textWidth = textPaint.measureText(text) * 0.5f
            val textBaseLineHeight = textPaint.fontMetrics.ascent * -0.4f
            canvas.drawText(text ?: return, centerX - textWidth, centerY + textBaseLineHeight, textPaint)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setup()
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPadding(left, top, right, bottom)
        setup()
    }

    override fun setPaddingRelative(start: Int, top: Int, end: Int, bottom: Int) {
        super.setPaddingRelative(start, top, end, bottom)
        setup()
    }

    override fun setImageBitmap(bm: Bitmap) {
        super.setImageBitmap(bm)
        initializeBitmap()
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        initializeBitmap()
    }

    override fun setImageResource(@DrawableRes resId: Int) {
        super.setImageResource(resId)
        initializeBitmap()
    }

    override fun setImageURI(uri: Uri?) {
        super.setImageURI(uri)
        initializeBitmap()
    }

    override fun setColorFilter(cf: ColorFilter) {
        if (cf === colorFilter) return

        colorFilter = cf
        applyColorFilter()
        invalidate()
    }

    override fun getColorFilter(): ColorFilter? = colorFilter

    private fun applyColorFilter() {
        bitmapPaint.colorFilter = colorFilter
    }

    private fun getBitmapFromDrawable(drawable: Drawable?): Bitmap? {
        with(drawable){
            return when (this) {
                null -> null
                is BitmapDrawable -> bitmap
                else -> {
                    val width = if (this is ColorDrawable) 1 else intrinsicWidth
                    val height = if (this is ColorDrawable) 1 else intrinsicHeight
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    setBounds(0, 0, canvas.width, canvas.height)
                    draw(canvas)
                    bitmap
                }
            }
        }
    }

    private fun initializeBitmap() {
        bitmap = getBitmapFromDrawable(drawable)
        setup()
    }

    private fun setup() {
        if (!isReady) {
            isSetupPending = true
            return
        }

        if (width == 0 && height == 0) return

        if (bitmap == null) {
            invalidate()
            return
        }

        bitmapShader = BitmapShader(bitmap ?: return, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

        bitmapPaint.isAntiAlias = true
        bitmapPaint.shader = bitmapShader

        with(borderPaint){
            style = Paint.Style.STROKE
            isAntiAlias = true
            color = borderColor
            strokeWidth = borderWidthPx.toFloat()
        }

        with(circleBackgroundPaint) {
            style = Paint.Style.FILL
            isAntiAlias = true
            color = Color.TRANSPARENT
        }

        bitmapHeight = (bitmap ?: return).height
        bitmapWidth = (bitmap ?: return).width

        borderRect.set(calculateBounds())
        borderRadius = min((borderRect.height() - borderWidthPx) / 2.0f, (borderRect.width() - borderWidthPx) / 2.0f)

        drawableRect.set(borderRect)
        if (!isBorderOverlay && borderWidthPx > 0) {
            drawableRect.inset(borderWidthPx - 1.0f, borderWidthPx - 1.0f)
        }
        drawableRadius = min(drawableRect.height() / 2.0f, drawableRect.width() / 2.0f)

        applyColorFilter()
        updateShaderMatrix()
        invalidate()
    }

    private fun calculateBounds(): RectF {
        val availableWidth = width - paddingLeft - paddingRight
        val availableHeight = height - paddingTop - paddingBottom

        val sideLength = min(availableWidth, availableHeight)

        val left = paddingLeft + (availableWidth - sideLength) / 2f
        val top = paddingTop + (availableHeight - sideLength) / 2f

        return RectF(left, top, left + sideLength, top + sideLength)
    }

    private fun updateShaderMatrix() {
        val scale: Float
        var dx = 0f
        var dy = 0f

        shaderMatrix.set(null)

        if (bitmapWidth * drawableRect.height() > drawableRect.width() * bitmapHeight) {
            scale = drawableRect.height() / bitmapHeight.toFloat()
            dx = (drawableRect.width() - bitmapWidth * scale) * 0.5f
        } else {
            scale = drawableRect.width() / bitmapWidth.toFloat()
            dy = (drawableRect.height() - bitmapHeight * scale) * 0.5f
        }

        shaderMatrix.setScale(scale, scale)
        shaderMatrix.postTranslate((dx + 0.5f).toInt() + drawableRect.left, (dy + 0.5f).toInt() + drawableRect.top)

        (bitmapShader ?: return).setLocalMatrix(shaderMatrix)
    }
}