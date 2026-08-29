package com.quotegenerator

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.min


class MainActivity : ComponentActivity() {

    companion object {

        private const val VIDEO_WIDTH = 1080
        private const val VIDEO_HEIGHT = 1920

        private const val VIDEO_DURATION_SECONDS = 8

        private const val VIDEO_FPS = 30

        private const val TEXT_AREA_WIDTH = 700
        private const val TEXT_AREA_HEIGHT = 700

        private const val MAX_FONT_SIZE = 72f
        private const val MIN_FONT_SIZE = 24f

        private const val BG_IMAGE = "bg.png"
        private const val BG_MUSIC = "bg.mp3"
        private const val FONT_FILE = "font.ttf"
    }

    private lateinit var quoteInput: TextView
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var generateButton: Button


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        createInterface()
    }


    // ========================================================
    // UI
    // ========================================================

    private fun createInterface() {

        val root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL

        root.setPadding(
            30,
            30,
            30,
            30
        )

        root.setBackgroundColor(
            Color.rgb(
                17,
                24,
                39
            )
        )


        val title = TextView(this)

        title.text =
            "Fast Bulk Quote Video Generator"

        title.textSize = 24f

        title.setTextColor(
            Color.WHITE
        )

        title.gravity =
            Gravity.CENTER

        root.addView(
            title,
            LinearLayout.LayoutParams(
                -1,
                70
            )
        )


        val subtitle = TextView(this)

        subtitle.text =
            "1080 × 1920 • 8 Seconds • H.264 / AVC"

        subtitle.textSize = 14f

        subtitle.setTextColor(
            Color.LTGRAY
        )

        subtitle.gravity =
            Gravity.CENTER

        root.addView(
            subtitle,
            LinearLayout.LayoutParams(
                -1,
                60
            )
        )


        quoteInput = TextView(this)

        quoteInput.setBackgroundColor(
            Color.rgb(
                31,
                41,
                55
            )
        )

        quoteInput.setTextColor(
            Color.WHITE
        )

        quoteInput.textSize =
            17f

        quoteInput.gravity =
            Gravity.TOP or Gravity.START

        quoteInput.setPadding(
            20,
            20,
            20,
            20
        )

        quoteInput.hint =
            "Enter one quote per line..."

        quoteInput.setHintTextColor(
            Color.GRAY
        )

        quoteInput.isFocusable =
            true

        quoteInput.isClickable =
            true

        quoteInput.isLongClickable =
            true

        root.addView(
            quoteInput,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )


        val information = TextView(this)

        information.text =
            """
            One line = one video

            Video: 1080 × 1920
            Duration: exactly 8 seconds
            Codec: H.264 / AVC
            FPS: 30
            Text area: 700 × 700
            Text: centered
            Background: bg.png
            Music: bg.mp3
            Font: font.ttf
            """.trimIndent()

        information.textSize =
            14f

        information.setTextColor(
            Color.LTGRAY
        )

        information.setPadding(
            10,
            15,
            10,
            10
        )

        root.addView(
            information,
            LinearLayout.LayoutParams(
                -1,
                250
            )
        )


        progressBar =
            ProgressBar(this)

        progressBar.visibility =
            ProgressBar.GONE

        root.addView(
            progressBar,
            LinearLayout.LayoutParams(
                -1,
                50
            )
        )


        generateButton =
            Button(this)

        generateButton.text =
            "GENERATE VIDEOS"

        generateButton.setOnClickListener {

            startGeneration()
        }

        root.addView(
            generateButton,
            LinearLayout.LayoutParams(
                -1,
                65
            )
        )


        statusText =
            TextView(this)

        statusText.text =
            "Ready"

        statusText.textSize =
            15f

        statusText.setTextColor(
            Color.WHITE
        )

        statusText.gravity =
            Gravity.CENTER

        root.addView(
            statusText,
            LinearLayout.LayoutParams(
                -1,
                70
            )
        )


        setContentView(
            root
        )
    }


    // ========================================================
    // GENERATION
    // ========================================================

    private fun startGeneration() {

        val rawText =
            quoteInput.text.toString()

        val quotes =
            rawText
                .split("\n")
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotEmpty()
                }


        if (quotes.isEmpty()) {

            Toast.makeText(
                this,
                "Enter at least one quote.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        generateButton.isEnabled =
            false

        progressBar.visibility =
            ProgressBar.VISIBLE


        Thread {

            try {

                generateVideos(
                    quotes
                )

                runOnUiThread {

                    statusText.text =
                        "✓ Generation complete"

                    generateButton.isEnabled =
                        true

                    progressBar.visibility =
                        ProgressBar.GONE

                }

            } catch (error: Exception) {

                runOnUiThread {

                    statusText.text =
                        "❌ ${error.message}"

                    generateButton.isEnabled =
                        true

                    progressBar.visibility =
                        ProgressBar.GONE

                    Toast.makeText(
                        this,
                        error.message ?: "Generation failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        }.start()
    }


    // ========================================================
    // GENERATE VIDEOS
    // ========================================================

    private fun generateVideos(
        quotes: List<String>
    ) {

        val outputDirectory =
            File(
                getExternalFilesDir(
                    Environment.DIRECTORY_MOVIES
                ),
                "QuoteVideoGenerator"
            )


        if (outputDirectory.exists()) {

            outputDirectory.deleteRecursively()
        }


        outputDirectory.mkdirs()


        val videoFiles =
            ArrayList<File>()


        for (
            index in quotes.indices
        ) {

            runOnUiThread {

                statusText.text =
                    "Generating ${index + 1}/${quotes.size}"
            }


            val frame =
                createQuoteBitmap(
                    quotes[index]
                )


            val outputFile =
                File(
                    outputDirectory,
                    "${index + 1}.mp4"
                )


            createMp4(
                frame,
                outputFile
            )


            videoFiles.add(
                outputFile
            )

            frame.recycle()
        }


        val zipFile =
            File(
                outputDirectory,
                "videos.zip"
            )


        createZip(
            videoFiles,
            zipFile
        )


        saveFilesToDownloads(
            videoFiles,
            zipFile
        )
    }


    // ========================================================
    // CREATE QUOTE BITMAP
    // ========================================================

    private fun createQuoteBitmap(
        quote: String
    ): Bitmap {

        val bitmap =
            Bitmap.createBitmap(
                VIDEO_WIDTH,
                VIDEO_HEIGHT,
                Bitmap.Config.ARGB_8888
            )


        val canvas =
            Canvas(bitmap)


        val background =
            loadBitmapAsset(
                BG_IMAGE
            )


        val backgroundScaled =
            centerCrop(
                background,
                VIDEO_WIDTH,
                VIDEO_HEIGHT
            )


        canvas.drawBitmap(
            backgroundScaled,
            0f,
            0f,
            null
        )


        background.recycle()

        backgroundScaled.recycle()


        val font =
            loadTypeface()


        val paint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            )

        paint.color =
            Color.WHITE

        paint.typeface =
            font

        paint.textAlign =
            Paint.Align.CENTER


        val lines =
            fitQuoteText(
                quote,
                paint
            )


        val fontSize =
            paint.textSize


        val lineHeight =
            fontSize * 1.20f


        val totalHeight =
            lines.size *
            lineHeight


        val centerX =
            VIDEO_WIDTH / 2f


        val centerY =
            VIDEO_HEIGHT / 2f


        var baseline =
            centerY -
            totalHeight / 2f +
            fontSize


        for (line in lines) {

            canvas.drawText(
                line,
                centerX,
                baseline,
                paint
            )

            baseline +=
                lineHeight
        }


        return bitmap
    }


    // ========================================================
    // FIT TEXT
    // ========================================================

    private fun fitQuoteText(
        quote: String,
        paint: Paint
    ): List<String> {

        var size =
            MAX_FONT_SIZE


        while (
            size >= MIN_FONT_SIZE
        ) {

            paint.textSize =
                size


            val lines =
                wrapText(
                    quote,
                    paint,
                    TEXT_AREA_WIDTH.toFloat()
                )


            val lineHeight =
                size * 1.20f


            val totalHeight =
                lines.size *
                lineHeight


            if (
                totalHeight <=
                TEXT_AREA_HEIGHT
            ) {

                return lines
            }


            size -=
                2f
        }


        paint.textSize =
            MIN_FONT_SIZE


        return wrapText(
            quote,
            paint,
            TEXT_AREA_WIDTH.toFloat()
        )
    }


    // ========================================================
    // TEXT WRAP
    // ========================================================

    private fun wrapText(
        text: String,
        paint: Paint,
        maxWidth: Float
    ): List<String> {

        val words =
            text.split(
                Regex("\\s+")
            )


        val lines =
            ArrayList<String>()


        var current =
            ""


        for (word in words) {

            val test =
                if (current.isEmpty()) {

                    word

                } else {

                    "$current $word"
                }


            if (
                paint.measureText(
                    test
                ) <= maxWidth
            ) {

                current =
                    test

            } else {

                if (
                    current.isNotEmpty()
                ) {

                    lines.add(
                        current
                    )
                }


                if (
                    paint.measureText(
                        word
                    ) <= maxWidth
                ) {

                    current =
                        word

                } else {

                    var part =
                        ""


                    for (
                        character in word
                    ) {

                        val candidate =
                            part +
                            character


                        if (
                            paint.measureText(
                                candidate
                            ) <= maxWidth
                        ) {

                            part =
                                candidate

                        } else {

                            if (
                                part.isNotEmpty()
                            ) {

                                lines.add(
                                    part
                                )
                            }

                            part =
                                character.toString()
                        }
                    }


                    current =
                        part
                }
            }
        }


        if (
            current.isNotEmpty()
        ) {

            lines.add(
                current
            )
        }


        return lines
    }


    // ========================================================
    // LOAD FONT
    // ========================================================

    private fun loadTypeface(): Typeface {

        val input =
            assets.open(
                FONT_FILE
            )


        val temp =
            File(
                cacheDir,
                FONT_FILE
            )


        FileOutputStream(
            temp
        ).use { output ->

            input.copyTo(
                output
            )
        }


        input.close()


        return Typeface.createFromFile(
            temp
        )
    }


    // ========================================================
    // LOAD BITMAP
    // ========================================================

    private fun loadBitmapAsset(
        filename: String
    ): Bitmap {

        return assets.open(
            filename
        ).use {

            android.graphics.BitmapFactory
                .decodeStream(it)
        }
    }


    // ========================================================
    // CENTER CROP
    // ========================================================

    private fun centerCrop(
        bitmap: Bitmap,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap {

        val scale =
            maxOf(
                targetWidth.toFloat() /
                    bitmap.width,

                targetHeight.toFloat() /
                    bitmap.height
            )


        val width =
            (bitmap.width * scale)
                .toInt()


        val height =
            (bitmap.height * scale)
                .toInt()


        val scaled =
            Bitmap.createScaledBitmap(
                bitmap,
                width,
                height,
                true
            )


        val left =
            (width - targetWidth) / 2


        val top =
            (height - targetHeight) / 2


        return Bitmap.createBitmap(
            scaled,
            left,
            top,
            targetWidth,
            targetHeight
        )
    }


    // ========================================================
    // CREATE MP4
    // ========================================================

    private fun createMp4(
        bitmap: Bitmap,
        outputFile: File
    ) {

        /*
         * This creates an H.264 MP4 video from the quote frame.
         *
         * IMPORTANT:
         * Android MediaCodec is used here rather than Python/FFmpeg.
         */

        val mime =
            "video/avc"


        val format =
            MediaFormat.createVideoFormat(
                mime,
                VIDEO_WIDTH,
                VIDEO_HEIGHT
            )


        format.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities
                .COLOR_FormatSurface
        )


        format.setInteger(
            MediaFormat.KEY_BIT_RATE,
            8_000_000
        )


        format.setInteger(
            MediaFormat.KEY_FRAME_RATE,
            VIDEO_FPS
        )


        format.setInteger(
            MediaFormat.KEY_I_FRAME_INTERVAL,
            1
        )


        val codec =
            MediaCodec.createEncoderByType(
                mime
            )


        codec.configure(
            format,
            null,
            null,
            MediaCodec.CONFIGURE_FLAG_ENCODE
        )


        val inputSurface =
            codec.createInputSurface()


        val muxer =
            MediaMuxer(
                outputFile.absolutePath,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )


        codec.start()


        val canvas =
            inputSurface.lockCanvas(
                null
            )


        canvas.drawColor(
            Color.BLACK
        )


        canvas.drawBitmap(
            bitmap,
            0f,
            0f,
            null
        )


        inputSurface.unlockCanvasAndPost(
            canvas
        )


        val bufferInfo =
            MediaCodec.BufferInfo()


        var trackIndex =
            -1


        var muxerStarted =
            false


        val frameCount =
            VIDEO_DURATION_SECONDS *
            VIDEO_FPS


        var frames =
            1


        while (
            frames < frameCount
        ) {

            val c =
                inputSurface.lockCanvas(
                    null
                )


            c.drawBitmap(
                bitmap,
                0f,
                0f,
                null
            )


            inputSurface.unlockCanvasAndPost(
                c
            )


            frames++
        }


        /*
         * Signal end of input.
         */

        inputSurface.setPresentationTime(
            (frameCount * 1_000_000_000L) /
                VIDEO_FPS
        )


        val endCanvas =
            inputSurface.lockCanvas(
                null
            )


        endCanvas.drawBitmap(
            bitmap,
            0f,
            0f,
            null
        )


        inputSurface.unlockCanvasAndPost(
            endCanvas
        )


        /*
         * Drain encoder.
         */

        var finished =
            false


        while (!finished) {

            val outputIndex =
                codec.dequeueOutputBuffer(
                    bufferInfo,
                    10_000
                )


            when {

                outputIndex ==
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {

                    if (muxerStarted) {

                        throw RuntimeException(
                            "Encoder format changed twice."
                        )
                    }


                    val newFormat =
                        codec.outputFormat


                    trackIndex =
                        muxer.addTrack(
                            newFormat
                        )


                    muxer.start()

                    muxerStarted =
                        true
                }


                outputIndex >= 0 -> {

                    val outputBuffer =
                        codec.getOutputBuffer(
                            outputIndex
                        )


                    if (
                        outputBuffer != null &&
                        bufferInfo.size > 0 &&
                        muxerStarted
                    ) {

                        outputBuffer.position(
                            bufferInfo.offset
                        )

                        outputBuffer.limit(
                            bufferInfo.offset +
                                bufferInfo.size
                        )


                        muxer.writeSampleData(
                            trackIndex,
                            outputBuffer,
                            bufferInfo
                        )
                    }


                    codec.releaseOutputBuffer(
                        outputIndex,
                        false
                    )


                    if (
                        bufferInfo.flags and
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            != 0
                    ) {

                        finished =
                            true
                    }
                }
            }
        }


        codec.stop()

        codec.release()

        inputSurface.release()

        if (muxerStarted) {

            muxer.stop()
        }

        muxer.release()
    }


    // ========================================================
    // ZIP
    // ========================================================

    private fun createZip(
        files: List<File>,
        zipFile: File
    ) {

        ZipOutputStream(
            FileOutputStream(
                zipFile
            )
        ).use { zip ->

            for (file in files) {

                val entry =
                    ZipEntry(
                        file.name
                    )


                zip.putNextEntry(
                    entry
                )


                file.inputStream().use {
                    it.copyTo(zip)
                }


                zip.closeEntry()
            }
        }
    }


    // ========================================================
    // SAVE TO DOWNLOADS
    // ========================================================

    private fun saveFilesToDownloads(
        videos: List<File>,
        zipFile: File
    ) {

        for (video in videos) {

            saveFileToMediaStore(
                video,
                "Movies/QuoteVideoGenerator",
                "video/mp4"
            )
        }


        saveFileToMediaStore(
            zipFile,
            "Download/QuoteVideoGenerator",
            "application/zip"
        )
    }


    private fun saveFileToMediaStore(
        file: File,
        relativePath: String,
        mimeType: String
    ) {

        val resolver =
            contentResolver


        val values =
            ContentValues().apply {

                put(
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    file.name
                )

                put(
                    MediaStore.MediaColumns.MIME_TYPE,
                    mimeType
                )

                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    relativePath
                )
            }


        val collection =
            if (
                mimeType == "video/mp4"
            ) {

                MediaStore.Video.Media.EXTERNAL_CONTENT_URI

            } else {

                MediaStore.Files.getContentUri(
                    "external"
                )
            }


        val uri =
            resolver.insert(
                collection,
                values
            )


        if (uri != null) {

            resolver.openOutputStream(
                uri
            ).use { output ->

                file.inputStream().use { input ->

                    input.copyTo(
                        output!!
                    )
                }
            }
        }
    }
}
