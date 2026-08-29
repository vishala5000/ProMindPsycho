package com.example.quotevideogenerator

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var quotesEditText: EditText
    private lateinit var generateButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView

    private val videoWidth = 1080
    private val videoHeight = 1920
    private val duration = 8
    private val fps = 30
    private val textAreaWidth = 700
    private val textAreaHeight = 700
    private val maxFontSize = 72
    private val minFontSize = 24

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        quotesEditText = findViewById(R.id.quotesEditText)
        generateButton = findViewById(R.id.generateButton)
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.statusText)

        generateButton.setOnClickListener { startGeneration() }
    }

    private fun startGeneration() {
        val quotes = quotesEditText.text.toString()
            .split(Regex("\\r?\\n"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (quotes.isEmpty()) {
            statusText.text = "Enter at least one quote."
            statusText.setTextColor(Color.RED)
            return
        }

        generateButton.isEnabled = false
        progressBar.visibility = View.VISIBLE
        progressBar.progress = 0
        statusText.setTextColor(Color.WHITE)
        statusText.text = "Starting..."

        thread {
            try {
                prepareAssets()
                val workDir = File(cacheDir, "quote_generator")
                if (workDir.exists()) workDir.deleteRecursively()
                val imagesDir = File(workDir, "images").apply { mkdirs() }
                val videosDir = File(workDir, "videos").apply { mkdirs() }

                quotes.forEachIndexed { index, quote ->
                    val number = index + 1
                    runOnUiThread {
                        statusText.text = "Generating $number / ${quotes.size}"
                        progressBar.progress = ((index.toFloat() / quotes.size) * 100).toInt()
                    }

                    val image = File(imagesDir, "$number.png")
                    createFrame(quote, image)

                    val output = File(videosDir, "$number.mp4")
                    runFfmpeg(image, output)

                    if (!output.exists() || output.length() == 0L) {
                        throw RuntimeException("Video $number.mp4 was not created")
                    }
                }

                val zipFile = File(workDir, "videos.zip")
                zipVideos(videosDir, zipFile)
                saveToMovies(videosDir)
                saveZipToDownloads(zipFile)

                runOnUiThread {
                    progressBar.progress = 100
                    statusText.setTextColor(Color.rgb(52, 211, 153))
                    statusText.text = "✓ Complete: ${quotes.size} videos + videos.zip saved"
                    generateButton.isEnabled = true
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusText.setTextColor(Color.RED)
                    statusText.text = "❌ ${e.message}"
                    generateButton.isEnabled = true
                }
            }
        }
    }

    private fun prepareAssets() {
        val required = listOf("bg.png", "bg.mp3", "font.ttf")
        required.forEach {
            if (!File(filesDir, it).exists()) copyAsset(it, File(filesDir, it))
        }
    }

    private fun copyAsset(name: String, destination: File) {
        assets.open(name).use { input ->
            FileOutputStream(destination).use { output -> input.copyTo(output) }
        }
    }

    private fun createFrame(quote: String, output: File) {
        val bg = android.graphics.BitmapFactory.decodeFile(File(filesDir, "bg.png").absolutePath)
            ?: throw RuntimeException("Could not load bg.png")

        val canvasBitmap = Bitmap.createBitmap(videoWidth, videoHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(canvasBitmap)

        val srcRatio = bg.width.toFloat() / bg.height
        val dstRatio = videoWidth.toFloat() / videoHeight
        val srcRect = if (srcRatio > dstRatio) {
            val cropWidth = (bg.height * dstRatio).toInt()
            android.graphics.Rect((bg.width - cropWidth) / 2, 0, (bg.width + cropWidth) / 2, bg.height)
        } else {
            val cropHeight = (bg.width / dstRatio).toInt()
            android.graphics.Rect(0, (bg.height - cropHeight) / 2, bg.width, (bg.height + cropHeight) / 2)
        }
        val dstRect = android.graphics.Rect(0, 0, videoWidth, videoHeight)
        canvas.drawBitmap(bg, srcRect, dstRect, Paint(Paint.ANTI_ALIAS_FLAG))
        bg.recycle()

        val typeface = Typeface.createFromFile(File(filesDir, "font.ttf"))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = typeface
        }

        var bestLines: List<String> = listOf(quote)
        var bestSize = minFontSize
        var bestHeight = Float.MAX_VALUE

        for (size in maxFontSize downTo minFontSize) {
            paint.textSize = size.toFloat()
            val lines = wrapText(paint, quote, textAreaWidth.toFloat())
            val lineHeight = size * 1.20f
            val totalHeight = lines.size * lineHeight
            if (totalHeight <= textAreaHeight && lines.all { paint.measureText(it) <= textAreaWidth }) {
                bestLines = lines
                bestSize = size
                bestHeight = totalHeight
                break
            }
        }

        paint.textSize = bestSize.toFloat()
        val lineHeight = bestSize * 1.20f
        val startY = videoHeight / 2f - bestHeight / 2f - paint.ascent()

        bestLines.forEachIndexed { index, line ->
            val y = startY + index * lineHeight
            canvas.drawText(line, videoWidth / 2f, y, paint)
        }

        FileOutputStream(output).use { canvasBitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        canvasBitmap.recycle()
    }

    private fun wrapText(paint: Paint, text: String, maxWidth: Float): List<String> {
        val result = mutableListOf<String>()
        var current = ""
        text.split(Regex("\\s+")).forEach { word ->
            val test = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(test) <= maxWidth) {
                current = test
            } else {
                if (current.isNotEmpty()) result.add(current)
                current = word
            }
        }
        if (current.isNotEmpty()) result.add(current)
        return result
    }

    private fun runFfmpeg(image: File, output: File) {
        val music = File(filesDir, "bg.mp3")
        val command = "-y -loop 1 -i '${image.absolutePath}' -stream_loop -1 -i '${music.absolutePath}' " +
                "-t 8 -vf scale=1080:1920 -r 30 -c:v libx264 -preset ultrafast -crf 18 " +
                "-pix_fmt yuv420p -c:a aac -b:a 192k -af volume=1.0 -movflags +faststart '${output.absolutePath}'"

        val session = FFmpegKit.execute(command)
        if (!ReturnCode.isSuccess(session.returnCode)) {
            throw RuntimeException("FFmpeg failed for ${output.name}\n${session.failStackTrace ?: session.allLogsAsString}")
        }
    }

    private fun zipVideos(videosDir: File, zipFile: File) {
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            videosDir.listFiles()?.filter { it.extension.equals("mp4", true) }?.sortedBy { it.name.toIntOrNull() ?: 0 }?.forEach { file ->
                zos.putNextEntry(ZipEntry(file.name))
                FileInputStream(file).use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }

    private fun saveToMovies(videosDir: File) {
        videosDir.listFiles()?.filter { it.extension.equals("mp4", true) }?.forEach { file ->
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/QuoteVideoGenerator")
            }
            val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw RuntimeException("Could not save ${file.name}")
            contentResolver.openOutputStream(uri).use { output ->
                FileInputStream(file).use { input -> input.copyTo(output!!) }
            }
        }
    }

    private fun saveZipToDownloads(zipFile: File) {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "videos.zip")
            put(MediaStore.Downloads.MIME_TYPE, "application/zip")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/QuoteVideoGenerator")
        }
        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw RuntimeException("Could not save videos.zip")
        contentResolver.openOutputStream(uri).use { output ->
            FileInputStream(zipFile).use { input -> input.copyTo(output!!) }
        }
    }
}
