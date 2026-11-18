package com.jesusjk0915.ahorro

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View

/**
 * Equivalente nativo del `backdrop-filter: blur()` del CSS original.
 *
 * Captura lo que ya dibujó la pantalla (el contenido de abajo), le aplica
 * un RenderEffect de desenfoque real y lo recorta a una píldora. Se coloca
 * entre el contenido y el dock, así el dock queda dibujado encima.
 */
class BackdropBlurView(context: Context) : View(context) {
    private val blurPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pill = Path()
    private var snapshot: Bitmap? = null

    init {
        setRadius(24f, 1f)
        tintPaint.alpha = 0
    }

    fun setRadius(dp: Float, density: Float) {
        val px = dp * density
        blurPaint.renderEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            RenderEffect.createBlurEffect(px, px, Shader.TileMode.CLAMP)
        } else {
            null
        }
        invalidate()
    }

    /** Tinte tipo `color-mix(in srgb, var(--surface-container) ...)`. */
    fun setTint(argb: Int, alpha: Float) {
        tintPaint.color = argb
        tintPaint.alpha = (alpha * 255f).toInt().coerceIn(0, 255)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val r = minOf(w, h) / 2f
        pill.reset()
        pill.addRoundRect(0f, 0f, w.toFloat(), h.toFloat(), r, r, Path.Direction.CW)
        snapshot = null
    }

    override fun onDraw(canvas: Canvas) {
        if (width <= 0 || height <= 0) return
        val root = rootView ?: return

        if (snapshot == null || snapshot!!.width != width || snapshot!!.height != height) {
            snapshot = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }
        val bmp = snapshot!!

        // Posiciones en pantalla ANTES de ocultar esta vista (así no se mide 0).
        val myLoc = IntArray(2)
        val rootLoc = IntArray(2)
        getLocationOnScreen(myLoc)
        root.getLocationOnScreen(rootLoc)

        val capture = Canvas(bmp)
        capture.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        val previous = visibility
        visibility = INVISIBLE // evita recursión: esta vista no se dibuja a sí misma
        try {
            capture.save()
            capture.translate(
                (rootLoc[0] - myLoc[0]).toFloat(),
                (rootLoc[1] - myLoc[1]).toFloat(),
            )
            root.draw(capture)
            capture.restore()
        } finally {
            visibility = previous
        }

        canvas.save()
        canvas.clipPath(pill)
        canvas.drawBitmap(bmp, 0f, 0f, blurPaint)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), tintPaint)
        canvas.restore()

        // El contenido de abajo puede scrollear/animarse: refrescar cada frame.
        postInvalidateOnAnimation()
    }
}
