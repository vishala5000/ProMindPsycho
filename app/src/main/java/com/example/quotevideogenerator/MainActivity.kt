package com.quotegenerator

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

import androidx.activity.ComponentActivity

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode

import java.io.File
import java.io.FileOutputStream

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


class MainActivity : ComponentActivity() {

    companion object {

        // ====================================================
        // VIDEO
        // ====================================================

        private const val VIDEO_WIDTH = 1080

        private const val VIDEO_HEIGHT = 1920

        private const val VIDEO_DURATION = 8

        private const val VIDEO_FPS = 30


        // ====================================================
        // TEXT
        // ====================================================

        private const val TEXT_AREA_WIDTH = 700

        private const val TEXT_AREA_HEIGHT = 700

        private const val MAX_FONT_SIZE = 72f

        private const val MIN_FONT_SIZE = 24f

        private const val LINE_SPACING = 1.20f


        // ====================================================
        // ASSETS
        // ====================================================

        private const val BG_IMAGE = "bg.png"

        private const val BG_AUDIO = "bg.mp3"

        private const val FONT_FILE = "font.ttf"
    }


    private lateinit var quoteInput: EditText

    private lateinit var generateButton: Button

    private lateinit var progressBar: ProgressBar

    private lateinit var statusText: TextView


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        createUI()
    }


    // ========================================================
    // UI
    // ========================================================

    private fun createUI() {

        val root =
            LinearLayout(this)

        root.orientation =
            LinearLayout.VERTICAL

        root.setPadding(
            25,
            25,
            25,
            25
        )

        root.setBackgroundColor(
            Color.rgb(
                17,
                24,
                39
            )
        )


        val title =
            TextView(this)

        title.text =
            "Fast Bulk Quote Video Generator"

        title.textSize =
            23f

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


        val subtitle =
            TextView(this)

        subtitle.text =
            "1080 × 1920 • EXACTLY 8 Seconds • H.264 / AVC"

        subtitle.textSize =
            13f

        subtitle.setTextColor(
            Color.LTGRAY
        )

        subtitle.gravity =
            Gravity.CENTER

        root.addView(
            subtitle,
            LinearLayout.LayoutParams(
                -1,
                50
            )
        )


        quoteInput =
            EditText(this)

        quoteInput.hint =
            """
            Enter one quote per line.

            Quote one
            Quote two
            Quote three
            """.trimIndent()

        quoteInput.setHintTextColor(
            Color.GRAY
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

        quoteInput.setBackgroundColor(
            Color.rgb(
                31,
                41,
                55
            )
        )

        root.addView(
            quoteInput,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )


        val info =
            TextView(this)

        info.text =
            """
            One line = one video

            Video: 1080 × 1920
            Duration: 8 seconds
            FPS: 30
            Codec: H.264 / AVC
            Audio: AAC 192 kbps
            Music: bg.mp3
            Text area: 700 × 700
            Text: centered
            Font: font.ttf
            Background: bg.png
            """.trimIndent()

        info.textSize =
            13f

        info.setTextColor(
            Color.LTGRAY
        )

        info.setPadding(
            5,
            12,
            5,
            5
        )

        root.addView(
            info,
            LinearLayout.LayoutParams(
                -1,
                235
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
                45
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
            14f

        statusText.setTextColor(
            Color.WHITE
        )

        statusText.gravity =
            Gravity.CENTER

        root.addView(
            statusText,
            LinearLayout.LayoutParams(
                -1,
                65
            )
        )


        setContentView(
            root
        )
    }


    // ========================================================
    // START
    // ========================================================

    private fun startGeneration() {

        val text =
            quoteInput
                .text
                .toString()


        val quotes =
            text
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

                generateAll(
                    quotes
                )

                runOnUiThread {

                    statusText.text =
                        "✓ COMPLETE — Videos saved"

                    progressBar.visibility =
                        ProgressBar.GONE

                    generateButton.isEnabled =
                        true

                    Toast.makeText(
                        this,
                        "Videos and ZIP created.",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (
                error: Exception
            ) {

                runOnUiThread {

                    statusText.text =
                        "❌ ERROR"

                    progressBar.visibility =
                        ProgressBar.GONE

                    generateButton.isEnabled =
                        true

                    Toast.makeText(
                        this,
                        error.message
                            ?: "Generation failed.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        }.start()
    }


    // ========================================================
    // GENERATE ALL
    // ========================================================

    private fun generateAll(
        quotes: List<String>
    ) {

        val workDir =
            File(
                cacheDir,
                "quote_video_generator"
            )


        if (
            workDir.exists()
        ) {

            workDir.deleteRecursively()
        }


        workDir.mkdirs()


        val videoFiles =
            ArrayList<File>()


        for (
            i in quotes.indices
        ) {

            val number =
                i + 1


            runOnUiThread {

                statusText.text =
                    "Generating $number/${quotes.size}"
            }


            val imageFile =
                File(
                    workDir,
                    "$number.png"
                )


            createQuoteImage(
                quotes[i],
                imageFile
            )


            val videoFile =
                File(
                    workDir,
                    "$number.mp4"
                )


            createVideoWithFFmpeg(
                imageFile,
                videoFile
            )


            videoFiles.add(
                videoFile
            )


            imageFile.delete()
        }


        runOnUiThread {

            statusText.text =
                "Creating ZIP..."
        }


        val zipFile =
            File(
                workDir,
                "videos.zip"
            )


        createZip(
            videoFiles,
            zipFile
        )


        saveOutputFiles(
            videoFiles,
            zipFile
        )
    }


    // ========================================================
    // CREATE QUOTE IMAGE
    // ========================================================

    private fun createQuoteImage(
        quote: String,
        outputFile: File
    ) {

        val bitmap =
            Bitmap.createBitmap(
                VIDEO_WIDTH,
                VIDEO_HEIGHT,
                Bitmap.Config.ARGB_8888
            )


        val canvas =
            Canvas(bitmap)


        val background =
            loadAssetBitmap(
                BG_IMAGE
            )


        val fitted =
            centerCrop(
                background,
                VIDEO_WIDTH,
                VIDEO_HEIGHT
            )


        canvas.drawBitmap(
            fitted,
            0f,
            0f,
            null
        )


        background.recycle()

        fitted.recycle()


        val paint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            )


        paint.color =
            Color.WHITE


        paint.textAlign =
            Paint.Align.CENTER


        paint.typeface =
            loadFont()


        val lines =
            fitText(
                quote,
                paint
            )


        val fontSize =
            paint.textSize


        val lineHeight =
            fontSize *
            LINE_SPACING


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


        for (
            line in lines
        ) {

            canvas.drawText(
                line,
                centerX,
                baseline,
                paint
            )


            baseline +=
                lineHeight
        }


        FileOutputStream(
            outputFile
        ).use {

            bitmap.compress(
                Bitmap.CompressFormat.PNG,
                100,
                it
            )
        }


        bitmap.recycle()
    }


    // ========================================================
    // TEXT FIT
    // ========================================================

    private fun fitText(
        text: String,
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
                    text,
                    paint
                )


            val height =
                lines.size *
                size *
                LINE_SPACING


            if (
                height <=
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
            text,
            paint
        )
    }


    // ========================================================
    // TEXT WRAP
    // ========================================================

    private fun wrapText(
        text: String,
        paint: Paint
    ): List<String> {

        val words =
            text.split(
                Regex("\\s+")
            )


        val lines =
            ArrayList<String>()


        var current =
            ""


        for (
            word in words
        ) {

            val candidate =
                if (
                    current.isEmpty()
                ) {

                    word

                } else {

                    "$current $word"
                }


            if (
                paint.measureText(
                    candidate
                ) <=
                TEXT_AREA_WIDTH
            ) {

                current =
                    candidate

            } else {

                if (
                    current.isNotEmpty()
                ) {

                    lines.add(
                        current
                    )
                }


                /*
                 * Handle a word that itself
                 * is wider than 700 px.
                 */

                if (
                    paint.measureText(
                        word
                    ) <=
                    TEXT_AREA_WIDTH
                ) {

                    current =
                        word

                } else {

                    var part =
                        ""


                    for (
                        character in word
                    ) {

                        val test =
                            part +
                            character


                        if (
                            paint.measureText(
                                test
                            ) <=
                            TEXT_AREA_WIDTH
                        ) {

                            part =
                                test

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
    // FONT
    // ========================================================

    private fun loadFont(): Typeface {

        val destination =
            File(
                cacheDir,
                FONT_FILE
            )


        if (
            !destination.exists()
        ) {

            assets.open(
                FONT_FILE
            ).use { input ->

                FileOutputStream(
                    destination
                ).use { output ->

                    input.copyTo(
                        output
                    )
                }
            }
        }


        return Typeface.createFromFile(
            destination
        )
    }


    // ========================================================
    // LOAD IMAGE
    // ========================================================

    private fun loadAssetBitmap(
        name: String
    ): Bitmap {

        return assets.open(
            name
        ).use {

            BitmapFactory
                .decodeStream(it)
        }
    }


    // ========================================================
    // CENTER CROP
    // ========================================================

    private fun centerCrop(
        bitmap: Bitmap,
        width: Int,
        height: Int
    ): Bitmap {

        val scale =
            maxOf(
                width.toFloat() /
                    bitmap.width,

                height.toFloat() /
                    bitmap.height
            )


        val newWidth =
            (bitmap.width * scale)
                .toInt()


        val newHeight =
            (bitmap.height * scale)
                .toInt()


        val scaled =
            Bitmap.createScaledBitmap(
                bitmap,
                newWidth,
                newHeight,
                true
            )


        val left =
            (newWidth - width) / 2


        val top =
            (newHeight - height) / 2


        return Bitmap.createBitmap(
            scaled,
            left,
            top,
            width,
            height
        )
    }


    // ========================================================
    // FFMPEG VIDEO
    // ========================================================

    private fun createVideoWithFFmpeg(
        imageFile: File,
        outputFile: File
    ) {

        /*
         * Copy bg.mp3 from assets to a real filesystem path.
         */

        val musicFile =
            File(
                cacheDir,
                BG_AUDIO
            )


        if (
            !musicFile.exists()
        ) {

            assets.open(
                BG_AUDIO
            ).use { input ->

                FileOutputStream(
                    musicFile
                ).use { output ->

                    input.copyTo(
                        output
                    )
                }
            }
        }


        /*
         * FFmpeg command:
         *
         * image:
         *   loop forever
         *
         * audio:
         *   loop forever
         *
         * output:
         *   exactly 8 seconds
         *
         * video:
         *   1080x1920
         *   H.264
         *   libx264
         *
         * audio:
         *   AAC 192k
         */

        val command =
            listOf(

                "ffmpeg",

                "-y",

                "-loop",
                "1",

                "-i",
                imageFile.absolutePath,

                "-stream_loop",
                "-1",

                "-i",
                musicFile.absolutePath,

                "-t",
                VIDEO_DURATION.toString(),

                "-vf",
                "scale=1080:1920:force_original_aspect_ratio=decrease," +
                        "pad=1080:1920:(ow-iw)/2:(oh-ih)/2",

                "-r",
                VIDEO_FPS.toString(),

                "-c:v",
                "libx264",

                "-preset",
                "ultrafast",

                "-crf",
                "18",

                "-pix_fmt",
                "yuv420p",

                "-c:a",
                "aac",

                "-b:a",
                "192k",

                "-ar",
                "44100",

                "-ac",
                "2",

                "-shortest",

                "-movflags",
                "+faststart",

                outputFile.absolutePath
            )


        val ffmpegCommand =
            command.joinToString(
                " "
            ) {
                shellQuote(it)
            }


        val session =
            FFmpegKit.execute(
                ffmpegCommand
            )


        if (
            !ReturnCode.isSuccess(
                session.returnCode
            )
        ) {

            val logs =
                session.allLogsAsString


            throw RuntimeException(
                "FFmpeg failed:\n$logs"
            )
        }


        if (
            !outputFile.exists()
            ||
            outputFile.length() == 0L
        ) {

            throw RuntimeException(
                "FFmpeg did not create the video."
            )
        }
    }


    // ========================================================
    // SHELL QUOTE
    // ========================================================

    private fun shellQuote(
        value: String
    ): String {

        return "'" +
                value.replace(
                    "'",
                    "'\\''"
                ) +
                "'"
    }


    // ========================================================
    // ZIP
    // ========================================================

    private fun createZip(
        videos: List<File>,
        zipFile: File
    ) {

        ZipOutputStream(
            FileOutputStream(
                zipFile
            )
        ).use { zip ->

            for (
                video in videos
            ) {

                val entry =
                    ZipEntry(
                        video.name
                    )


                zip.putNextEntry(
                    entry
                )


                video.inputStream().use {
                    it.copyTo(
                        zip
                    )
                }


                zip.closeEntry()
            }
        }
    }


    // ========================================================
    // SAVE OUTPUT
    // ========================================================

    private fun saveOutputFiles(
        videos: List<File>,
        zipFile: File
    ) {

        for (
            video in videos
        ) {

            saveToMediaStore(
                video,
                "Movies/QuoteVideoGenerator",
                "video/mp4"
            )
        }


        saveToMediaStore(
            zipFile,
            "Download/QuoteVideoGenerator",
            "application/zip"
        )
    }


    private fun saveToMediaStore(
        file: File,
        relativePath: String,
        mimeType: String
    ) {

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
                mimeType ==
                "video/mp4"
            ) {

                MediaStore.Video.Media
                    .EXTERNAL_CONTENT_URI

            } else {

                MediaStore.Files
                    .getContentUri(
                        "external"
                    )
            }


        val uri =
            contentResolver.insert(
                collection,
                values
            )


        if (
            uri == null
        ) {

            throw RuntimeException(
                "Could not save ${file.name}"
            )
        }


        contentResolver
            .openOutputStream(uri)
            .use { output ->

                if (
                    output == null
                ) {

                    throw RuntimeException(
                        "Could not open output stream."
                    )
                }


                file.inputStream().use {
                    it.copyTo(
                        output
                    )
                }
            }
    }
}
