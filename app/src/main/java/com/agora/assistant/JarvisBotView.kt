package com.agora.assistant

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.BlurMaskFilter
import android.view.View

/** Original geometric JARVIS mascot drawn in-app; no third-party character artwork. */
class JarvisBotView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val silver = 0xFFBFC5D0.toInt()
    private val darkSilver = 0xFF555D6B.toInt()
    private val black = 0xFF090A0E.toInt()
    private val red = 0xFFFF2639.toInt()
    private val white = 0xFFFFF4F5.toInt()

    init { setLayerType(LAYER_TYPE_SOFTWARE, null) }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val sx = width / 240f
        val sy = height / 250f
        canvas.save()
        canvas.scale(sx, sy)

        // Shoulder / chest silhouette
        paint.style = Paint.Style.FILL
        paint.color = black
        val torso = Path().apply {
            moveTo(34f, 244f); lineTo(42f, 204f); lineTo(66f, 178f)
            lineTo(174f, 178f); lineTo(198f, 204f); lineTo(206f, 244f); close()
        }
        canvas.drawPath(torso, paint)
        paint.color = darkSilver
        val armor = Path().apply {
            moveTo(45f, 240f); lineTo(54f, 207f); lineTo(78f, 190f)
            lineTo(162f, 190f); lineTo(186f, 207f); lineTo(195f, 240f); close()
        }
        canvas.drawPath(armor, paint)
        paint.color = silver
        canvas.drawRoundRect(RectF(78f, 178f, 162f, 226f), 16f, 16f, paint)
        paint.color = black
        canvas.drawRoundRect(RectF(91f, 190f, 149f, 241f), 12f, 12f, paint)

        // Neck and helmet
        paint.color = darkSilver
        canvas.drawRoundRect(RectF(96f, 147f, 144f, 190f), 12f, 12f, paint)
        paint.color = silver
        val helmet = Path().apply {
            moveTo(74f, 26f); lineTo(98f, 8f); lineTo(142f, 8f); lineTo(166f, 26f)
            lineTo(180f, 72f); lineTo(173f, 136f); lineTo(150f, 166f)
            lineTo(90f, 166f); lineTo(67f, 136f); lineTo(60f, 72f); close()
        }
        canvas.drawPath(helmet, paint)
        paint.color = darkSilver
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawPath(helmet, paint)
        paint.style = Paint.Style.FILL

        // Face plate
        paint.color = black
        val face = Path().apply {
            moveTo(82f, 34f); lineTo(158f, 34f); lineTo(169f, 82f)
            lineTo(158f, 128f); lineTo(142f, 150f); lineTo(98f, 150f)
            lineTo(82f, 128f); lineTo(71f, 82f); close()
        }
        canvas.drawPath(face, paint)
        paint.color = darkSilver
        canvas.drawRoundRect(RectF(103f, 19f, 137f, 31f), 5f, 5f, paint)

        // Eyes with red glow
        paint.color = red
        paint.maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.NORMAL)
        val leftEye = Path().apply { moveTo(78f, 83f); lineTo(112f, 75f); lineTo(105f, 94f); lineTo(83f, 98f); close() }
        val rightEye = Path().apply { moveTo(162f, 83f); lineTo(128f, 75f); lineTo(135f, 94f); lineTo(157f, 98f); close() }
        canvas.drawPath(leftEye, paint); canvas.drawPath(rightEye, paint)
        paint.maskFilter = null
        paint.color = white
        canvas.drawPath(leftEye, paint); canvas.drawPath(rightEye, paint)

        // Chin and cheek armor
        paint.color = silver
        val chin = Path().apply { moveTo(91f, 113f); lineTo(149f, 113f); lineTo(137f, 145f); lineTo(120f, 157f); lineTo(103f, 145f); close() }
        canvas.drawPath(chin, paint)
        paint.color = darkSilver
        paint.strokeWidth = 3f
        paint.style = Paint.Style.STROKE
        canvas.drawPath(chin, paint)
        paint.style = Paint.Style.FILL

        // Chest reactor
        paint.color = red
        paint.maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL)
        canvas.drawCircle(120f, 205f, 31f, paint)
        paint.maskFilter = null
        paint.color = black
        canvas.drawCircle(120f, 205f, 27f, paint)
        paint.color = red
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawCircle(120f, 205f, 22f, paint)
        paint.strokeWidth = 2f
        canvas.drawCircle(120f, 205f, 13f, paint)
        paint.style = Paint.Style.FILL
        paint.color = white
        canvas.drawCircle(120f, 205f, 6f, paint)

        // Red armor traces
        paint.color = red
        paint.strokeWidth = 3f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(55f, 213f, 77f, 224f, paint)
        canvas.drawLine(185f, 213f, 163f, 224f, paint)
        canvas.drawLine(84f, 166f, 96f, 180f, paint)
        canvas.drawLine(156f, 166f, 144f, 180f, paint)
        paint.style = Paint.Style.FILL
        canvas.restore()
    }
}
