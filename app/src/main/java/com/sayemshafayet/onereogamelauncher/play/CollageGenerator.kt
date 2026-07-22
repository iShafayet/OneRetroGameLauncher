package com.sayemshafayet.onereogamelauncher.play

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import dagger.hilt.android.qualifiers.ApplicationContext
import com.sayemshafayet.onereogamelauncher.ui.util.ImageBitmapDecoder
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class CollageInput(
    val title: String,
    val systemName: String,
    val boxArtPath: String?,
    val stars: Float,
    val reviewExcerpt: String?,
    val playtimeHours: Double,
    val sessionCount: Int,
    val statusLabel: String,
    val raEarned: Int?,
    val raTotal: Int?,
)

@Singleton
class CollageGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val WIDTH = 1080
        private const val HEIGHT = 1350
        private const val BRAND = "ORGL"
    }

    fun generate(input: CollageInput): String? {
        val outDir = File(context.filesDir, "collages").apply { mkdirs() }
        val safeName = input.title.replace(Regex("""[\\/:*?"<>|]"""), "_").take(80)
        val outFile = File(outDir, "${safeName}_${System.currentTimeMillis()}.png")
        return if (generateTo(outFile, input)) outFile.absolutePath else null
    }

    fun generateTo(outputFile: File, input: CollageInput): Boolean {
        outputFile.parentFile?.mkdirs()
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#121218"))

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D8D8E0")
            textSize = 34f
        }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#7C5CFF")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        var y = 48
        drawBoxArt(canvas, input.boxArtPath, y)
        y += 620

        canvas.drawText(trimTitle(input.title), 48f, y.toFloat(), titlePaint)
        y += 56
        canvas.drawText(input.systemName, 48f, y.toFloat(), bodyPaint)
        y += 48
        canvas.drawText(starsText(input.stars), 48f, y.toFloat(), bodyPaint)
        y += 48
        canvas.drawText(
            "${input.statusLabel} · ${formatHours(input.playtimeHours)} · ${input.sessionCount} sessions",
            48f,
            y.toFloat(),
            bodyPaint,
        )
        y += 40
        if (input.raTotal != null && input.raTotal > 0) {
            canvas.drawText(
                "RetroAchievements ${input.raEarned ?: 0}/${input.raTotal}",
                48f,
                y.toFloat(),
                bodyPaint,
            )
            y += 40
        }
        input.reviewExcerpt?.takeIf { it.isNotBlank() }?.let { excerpt ->
            y += 16
            drawWrappedText(canvas, excerpt.take(220), 48, y, WIDTH - 96, bodyPaint)
        }

        canvas.drawText(BRAND, (WIDTH - 140).toFloat(), (HEIGHT - 40).toFloat(), accentPaint)

        return runCatching {
            FileOutputStream(outputFile).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            true
        }.getOrDefault(false)
    }

    private fun drawBoxArt(canvas: Canvas, path: String?, top: Int) {
        val rect = Rect(48, top, WIDTH - 48, top + 560)
        val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2A2A35") }
        canvas.drawRoundRect(
            rect.left.toFloat(),
            rect.top.toFloat(),
            rect.right.toFloat(),
            rect.bottom.toFloat(),
            24f,
            24f,
            frame,
        )
        val bmp = ImageBitmapDecoder.decode(context, path)
        if (bmp != null) {
            canvas.save()
            canvas.clipRect(rect)
            drawCenterCrop(canvas, bmp, rect)
            canvas.restore()
            bmp.recycle()
        } else {
            val placeholder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#3A3A48")
                textSize = 42f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("No cover", rect.exactCenterX(), rect.exactCenterY(), placeholder)
        }
    }

    private fun drawCenterCrop(canvas: Canvas, bitmap: Bitmap, dest: Rect) {
        val bw = bitmap.width.toFloat()
        val bh = bitmap.height.toFloat()
        val dw = dest.width().toFloat()
        val dh = dest.height().toFloat()
        val scale = maxOf(dw / bw, dh / bh)
        val sw = bw * scale
        val sh = bh * scale
        val dx = dest.left + (dw - sw) / 2f
        val dy = dest.top + (dh - sh) / 2f
        canvas.drawBitmap(bitmap, null, RectF(dx, dy, dx + sw, dy + sh), null)
    }

    private fun drawWrappedText(canvas: Canvas, text: String, x: Int, startY: Int, maxWidth: Int, paint: Paint) {
        val words = text.split(' ')
        var line = StringBuilder()
        var y = startY
        for (word in words) {
            val test = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(test) > maxWidth) {
                canvas.drawText(line.toString(), x.toFloat(), y.toFloat(), paint)
                y += 42
                line = StringBuilder(word)
            } else {
                line = StringBuilder(test)
            }
        }
        if (line.isNotEmpty()) canvas.drawText(line.toString(), x.toFloat(), y.toFloat(), paint)
    }

    private fun trimTitle(title: String): String =
        if (title.length <= 42) title else title.take(39) + "…"

    private fun starsText(stars: Float): String {
        val full = stars.toInt()
        val half = stars - full >= 0.5f
        return buildString {
            repeat(full) { append('★') }
            if (half) append('⯨')
            append("  ")
            append(String.format("%.1f", stars))
        }
    }

    private fun formatHours(hours: Double): String =
        when {
            hours < 1.0 -> "${(hours * 60).toInt()} min"
            hours < 10 -> String.format("%.1f h", hours)
            else -> "${hours.toInt()} h"
        }
}
