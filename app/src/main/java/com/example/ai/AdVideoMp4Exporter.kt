package com.example.ai

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.sin

enum class SharePlatform(
    val labelEn: String,
    val labelBn: String,
    val packageCandidates: List<String>
) {
    WHATSAPP("WhatsApp", "হোয়াটসঅ্যাপ", listOf("com.whatsapp", "com.whatsapp.w4b")),
    FACEBOOK("Facebook", "ফেসবুক", listOf("com.facebook.katana", "com.facebook.lite", "com.facebook.orca")),
    INSTAGRAM("Instagram", "ইনস্টাগ্রাম", listOf("com.instagram.android")),
    ANY_APP("All Apps", "যেকোনো অ্যাপ", emptyList())
}

object AdVideoMp4Exporter {
    private const val VIRTUAL_WIDTH = 720f
    private const val VIRTUAL_HEIGHT = 1280f
    private const val WIDTH = 576   // 16 * 36 (9:16 H.264 macroblock aligned)
    private const val HEIGHT = 1024 // 16 * 64 (9:16 H.264 macroblock aligned)
    private const val FPS = 15
    private const val DURATION_SECONDS = 16
    private const val TOTAL_FRAMES = FPS * DURATION_SECONDS
    private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
    private const val BIT_RATE = 1_600_000
    private const val TIMEOUT_US = 20_000L

    suspend fun exportCommercialMp4(
        context: Context,
        storyboard: BengaliAdStoryboard,
        productBitmap: Bitmap?,
        onProgress: (Float) -> Unit = {}
    ): File = withContext(Dispatchers.Default) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outFile = File(exportDir, "smartfarm_ad_${System.currentTimeMillis()}.mp4")
        if (outFile.exists()) outFile.delete()

        val codecCandidates = findAvcEncoderCandidates()
        var lastError: Throwable? = null

        for (codecName in codecCandidates) {
            if (outFile.exists()) outFile.delete()
            val result = runCatching {
                encodeMp4WithCodec(
                    outFile = outFile,
                    codecName = codecName,
                    storyboard = storyboard,
                    productBitmap = productBitmap,
                    onProgress = onProgress
                )
            }
            if (result.isSuccess && outFile.exists() && outFile.length() > 0L) {
                return@withContext outFile
            }
            lastError = result.exceptionOrNull()
        }

        if (outFile.exists()) outFile.delete()
        throw (lastError ?: IllegalStateException("Unable to encode MP4 video on this device."))
    }

    private fun findAvcEncoderCandidates(): List<String?> {
        val list = android.media.MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS)
        val matching = list.codecInfos
            .filter { info ->
                info.isEncoder && info.supportedTypes.any { it.equals(MIME_TYPE, ignoreCase = true) }
            }
        // Prefer deterministic software AVC encoders first (reliable across all emulators & phones),
        // then any remaining hardware/vendor AVC encoders, plus null default fallback.
        val software = matching.filter {
            val lower = it.name.lowercase()
            lower.startsWith("c2.android.") || lower.startsWith("omx.google.")
        }.map { it.name }
        val hardware = matching.filterNot {
            val lower = it.name.lowercase()
            lower.startsWith("c2.android.") || lower.startsWith("omx.google.")
        }.map { it.name }
        return (software + hardware + listOf(null)).distinct()
    }

    private fun chooseSupportedYuvColorFormat(encoder: MediaCodec): Int {
        val supported = runCatching {
            encoder.codecInfo.getCapabilitiesForType(MIME_TYPE).colorFormats.toSet()
        }.getOrDefault(emptySet())
        return when {
            supported.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible) ->
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
            supported.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) ->
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
            supported.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) ->
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
            else -> MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
        }
    }

    private fun encodeMp4WithCodec(
        outFile: File,
        codecName: String?,
        storyboard: BengaliAdStoryboard,
        productBitmap: Bitmap?,
        onProgress: (Float) -> Unit
    ) {
        val encoder = if (codecName != null) {
            MediaCodec.createByCodecName(codecName)
        } else {
            MediaCodec.createEncoderByType(MIME_TYPE)
        }
        val colorFormat = chooseSupportedYuvColorFormat(encoder)
        val format = MediaFormat.createVideoFormat(MIME_TYPE, WIDTH, HEIGHT).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            setInteger(MediaFormat.KEY_FRAME_RATE, FPS)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        var encoderStarted = false
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var muxerStopped = false
        var pendingOutputFormat: MediaFormat? = null
        var trackIndex = -1
        var samplesWritten = 0
        val bufferInfo = MediaCodec.BufferInfo()

        val frameBitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val frameCanvas = Canvas(frameBitmap)
        val pixels = IntArray(WIDTH * HEIGHT)
        val yuvScratch = ByteArray(WIDTH * HEIGHT * 3 / 2)

        try {
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()
            encoderStarted = true
            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val accentColor = try {
                Color.parseColor(storyboard.accentColorHex)
            } catch (_: Exception) {
                Color.parseColor("#1B5E20")
            }

            fun drainEncoder(waitForEos: Boolean) {
                var emptyTries = 0
                val maxEmptyTries = if (waitForEos) 150 else 2
                while (true) {
                    val outputIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                    when {
                        outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            emptyTries++
                            if (!waitForEos || emptyTries >= maxEmptyTries) break
                        }
                        outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            pendingOutputFormat = encoder.outputFormat
                        }
                        outputIndex >= 0 -> {
                            emptyTries = 0
                            val encodedData = encoder.getOutputBuffer(outputIndex)
                            val isConfig = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0
                            if (encodedData != null && !isConfig && bufferInfo.size > 0) {
                                if (!muxerStarted) {
                                    val activeFormat = pendingOutputFormat ?: encoder.outputFormat
                                    trackIndex = muxer!!.addTrack(activeFormat)
                                    muxer.start()
                                    muxerStarted = true
                                }
                                if (muxerStarted && trackIndex >= 0) {
                                    encodedData.position(bufferInfo.offset)
                                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                    muxer!!.writeSampleData(trackIndex, encodedData, bufferInfo)
                                    samplesWritten++
                                }
                            }
                            val sawEos = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                            encoder.releaseOutputBuffer(outputIndex, false)
                            if (sawEos) break
                        }
                    }
                }
            }

            fun dequeueInputAndSubmit(ptsUs: Long, eos: Boolean, fillData: ((Int) -> Int)?) {
                var attempts = 0
                while (attempts < 150) {
                    val inputIndex = encoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inputIndex >= 0) {
                        if (eos) {
                            encoder.queueInputBuffer(inputIndex, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        } else {
                            val submittedSize = fillData?.invoke(inputIndex) ?: 0
                            encoder.queueInputBuffer(inputIndex, 0, submittedSize, ptsUs, 0)
                        }
                        return
                    }
                    drainEncoder(waitForEos = false)
                    attempts++
                }
                error("Timed out waiting for MediaCodec input buffer")
            }

            for (frameIdx in 0 until TOTAL_FRAMES) {
                val elapsedMs = (frameIdx * 1000L) / FPS
                val ptsUs = (frameIdx * 1_000_000L) / FPS

                frameCanvas.save()
                frameCanvas.scale(WIDTH / VIRTUAL_WIDTH, HEIGHT / VIRTUAL_HEIGHT)
                drawAdFrame(
                    canvas = frameCanvas,
                    elapsedMs = elapsedMs,
                    totalMs = DURATION_SECONDS * 1000L,
                    storyboard = storyboard,
                    productBitmap = productBitmap,
                    accentColor = accentColor
                )
                frameCanvas.restore()

                frameBitmap.getPixels(pixels, 0, WIDTH, 0, 0, WIDTH, HEIGHT)

                dequeueInputAndSubmit(ptsUs = ptsUs, eos = false) { inputIndex ->
                    val image = runCatching { encoder.getInputImage(inputIndex) }.getOrNull()
                    if (image != null) {
                        fillYuv420ImageFromArgb(pixels, WIDTH, HEIGHT, image)
                        yuvScratch.size
                    } else {
                        val buf = encoder.getInputBuffer(inputIndex)
                            ?: error("Null input buffer from MediaCodec")
                        if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
                            argbToNv12(pixels, WIDTH, HEIGHT, yuvScratch)
                        } else {
                            argbToI420(pixels, WIDTH, HEIGHT, yuvScratch)
                        }
                        buf.clear()
                        val bytesToPut = minOf(buf.remaining(), yuvScratch.size)
                        buf.put(yuvScratch, 0, bytesToPut)
                        bytesToPut
                    }
                }
                drainEncoder(waitForEos = false)

                if (frameIdx % 4 == 0) {
                    onProgress((frameIdx + 1).toFloat() / TOTAL_FRAMES.toFloat())
                }
            }

            val endPtsUs = (TOTAL_FRAMES * 1_000_000L) / FPS
            dequeueInputAndSubmit(ptsUs = endPtsUs, eos = true, fillData = null)
            drainEncoder(waitForEos = true)

            if (!muxerStarted || samplesWritten <= 0) {
                error("Encoder completed with zero muxed video samples")
            }
            muxer!!.stop()
            muxerStopped = true
            onProgress(1f)
        } finally {
            frameBitmap.recycle()
            if (encoderStarted) {
                runCatching { encoder.stop() }
            }
            runCatching { encoder.release() }
            if (muxerStarted && !muxerStopped && samplesWritten > 0) {
                runCatching {
                    muxer?.stop()
                    muxerStopped = true
                }
            }
            runCatching { muxer?.release() }
            if (!muxerStopped) {
                runCatching { if (outFile.exists()) outFile.delete() }
            }
        }
    }

    private fun fillYuv420ImageFromArgb(
        argb: IntArray,
        width: Int,
        height: Int,
        image: android.media.Image
    ) {
        val planes = image.planes
        val yPlane = planes[0]
        val uPlane = planes[1]
        val vPlane = planes[2]

        val yBuf = yPlane.buffer
        val uBuf = uPlane.buffer
        val vBuf = vPlane.buffer

        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride
        val uRowStride = uPlane.rowStride
        val uPixelStride = uPlane.pixelStride
        val vRowStride = vPlane.rowStride
        val vPixelStride = vPlane.pixelStride

        for (j in 0 until height) {
            val yRowOffset = j * yRowStride
            val uvRowOffsetU = (j / 2) * uRowStride
            val uvRowOffsetV = (j / 2) * vRowStride
            val srcRow = j * width
            for (i in 0 until width) {
                val c = argb[srcRow + i]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF

                val yVal = (((66 * r + 129 * g + 25 * b + 128) shr 8) + 16).coerceIn(16, 235)
                yBuf.put(yRowOffset + i * yPixelStride, yVal.toByte())

                if ((j and 1) == 0 && (i and 1) == 0) {
                    val uVal = (((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128).coerceIn(16, 240)
                    val vVal = (((112 * r - 94 * g - 18 * b + 128) shr 8) + 128).coerceIn(16, 240)
                    val uvCol = i / 2
                    uBuf.put(uvRowOffsetU + uvCol * uPixelStride, uVal.toByte())
                    vBuf.put(uvRowOffsetV + uvCol * vPixelStride, vVal.toByte())
                }
            }
        }
    }

    private fun argbToI420(argb: IntArray, width: Int, height: Int, out: ByteArray) {
        val frameSize = width * height
        var yIndex = 0
        var uIndex = frameSize
        var vIndex = frameSize + frameSize / 4
        var index = 0
        for (j in 0 until height) {
            for (i in 0 until width) {
                val c = argb[index++]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                val yVal = (((66 * r + 129 * g + 25 * b + 128) shr 8) + 16).coerceIn(16, 235)
                out[yIndex++] = yVal.toByte()
                if ((j and 1) == 0 && (i and 1) == 0) {
                    val uVal = (((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128).coerceIn(16, 240)
                    val vVal = (((112 * r - 94 * g - 18 * b + 128) shr 8) + 128).coerceIn(16, 240)
                    out[uIndex++] = uVal.toByte()
                    out[vIndex++] = vVal.toByte()
                }
            }
        }
    }

    private fun argbToNv12(argb: IntArray, width: Int, height: Int, out: ByteArray) {
        val frameSize = width * height
        var yIndex = 0
        var uvIndex = frameSize
        var index = 0
        for (j in 0 until height) {
            for (i in 0 until width) {
                val c = argb[index++]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                val yVal = (((66 * r + 129 * g + 25 * b + 128) shr 8) + 16).coerceIn(16, 235)
                out[yIndex++] = yVal.toByte()
                if ((j and 1) == 0 && (i and 1) == 0) {
                    val uVal = (((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128).coerceIn(16, 240)
                    val vVal = (((112 * r - 94 * g - 18 * b + 128) shr 8) + 128).coerceIn(16, 240)
                    out[uvIndex++] = uVal.toByte()
                    out[uvIndex++] = vVal.toByte()
                }
            }
        }
    }

    private fun drawAdFrame(
        canvas: Canvas,
        elapsedMs: Long,
        totalMs: Long,
        storyboard: BengaliAdStoryboard,
        productBitmap: Bitmap?,
        accentColor: Int
    ) {
        val w = VIRTUAL_WIDTH
        val h = VIRTUAL_HEIGHT
        val progress = (elapsedMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)

        val darkAccent = darkenColor(accentColor, 0.32f)
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, w, h,
                intArrayOf(darkAccent, accentColor, Color.parseColor("#0B2611")),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Subtle animated spotlight circles
        val pulse = 0.5f + 0.5f * sin(progress * Math.PI * 6.0).toFloat()
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(38, 255, 255, 255)
        }
        canvas.drawCircle(w * 0.85f, h * 0.14f, 140f + pulse * 24f, glowPaint)
        canvas.drawCircle(w * 0.12f, h * 0.86f, 120f + (1f - pulse) * 20f, glowPaint)

        // Top Farm Brand Header
        drawPill(
            canvas = canvas,
            cx = w / 2f,
            cy = 72f,
            pillWidth = w - 96f,
            pillHeight = 64f,
            bgColor = Color.argb(210, 255, 255, 255),
            borderColor = accentColor
        )
        drawCenteredText(
            canvas = canvas,
            text = "${storyboard.farmName.ifBlank { "স্মার্ট খামার" }} • ${storyboard.promoBadgeBn}",
            x = 64f,
            y = 48f,
            maxWidth = (w - 128f).toInt(),
            textSize = 24f,
            color = darkAccent,
            bold = true
        )

        when {
            // Scene 1: 0s - 3.5s Product Spotlight
            elapsedMs < 3500L -> {
                val sceneProgress = elapsedMs / 3500f
                drawProductPhotoFrame(
                    canvas = canvas,
                    bitmap = productBitmap,
                    left = 72f,
                    top = 148f,
                    right = w - 72f,
                    bottom = 660f,
                    zoomScale = 1.0f + sceneProgress * 0.08f,
                    badgeText = storyboard.priceText
                )
                drawLeftAlignedText(
                    canvas = canvas,
                    text = storyboard.hookHeadlineBn,
                    x = 64f,
                    y = 700f,
                    maxWidth = (w - 128f).toInt(),
                    textSize = 44f,
                    color = Color.WHITE,
                    bold = true
                )
                drawLeftAlignedText(
                    canvas = canvas,
                    text = storyboard.productName,
                    x = 64f,
                    y = 900f,
                    maxWidth = (w - 128f).toInt(),
                    textSize = 32f,
                    color = Color.parseColor("#B9F6CA"),
                    bold = true
                )
            }

            // Scene 2: 3.5s - 7.0s Quality Benefits + Price
            elapsedMs < 7000L -> {
                drawProductPhotoFrame(
                    canvas = canvas,
                    bitmap = productBitmap,
                    left = 96f,
                    top = 136f,
                    right = w - 96f,
                    bottom = 450f,
                    zoomScale = 1.04f,
                    badgeText = storyboard.priceText
                )
                drawCenteredText(
                    canvas = canvas,
                    text = "কেন আমাদের ${storyboard.productName} সেরা?",
                    x = 56f,
                    y = 478f,
                    maxWidth = (w - 112f).toInt(),
                    textSize = 32f,
                    color = Color.parseColor("#FFE082"),
                    bold = true
                )
                val benefits = storyboard.benefitsBn.take(3)
                benefits.forEachIndexed { idx, benefit ->
                    val cardTop = 550f + idx * 165f
                    drawRoundedCard(canvas, 56f, cardTop, w - 56f, cardTop + 140f, Color.argb(235, 255, 255, 255))
                    drawLeftAlignedText(
                        canvas = canvas,
                        text = "✓  $benefit",
                        x = 84f,
                        y = cardTop + 26f,
                        maxWidth = (w - 168f).toInt(),
                        textSize = 26f,
                        color = Color.parseColor("#1B1B1B"),
                        bold = true
                    )
                }
            }

            // Scene 3: 7.0s - 11.5s App Screen-Record Ordering Tutorial ("Click Here -> Order Now")
            elapsedMs < 11500L -> {
                val subProgress = ((elapsedMs - 7000L) / 4500f).coerceIn(0f, 1f)
                val subStep = when {
                    subProgress < 0.34f -> 0
                    subProgress < 0.68f -> 1
                    else -> 2
                }

                drawCenteredText(
                    canvas = canvas,
                    text = "📱 অ্যাপ থেকে যেভাবে অর্ডার করবেন (স্ক্রিন রেকর্ড)",
                    x = 40f,
                    y = 128f,
                    maxWidth = (w - 80f).toInt(),
                    textSize = 27f,
                    color = Color.parseColor("#FFE082"),
                    bold = true
                )

                // Phone bezel frame
                val phoneLeft = 110f
                val phoneTop = 198f
                val phoneRight = w - 110f
                val phoneBottom = 940f
                drawRoundedCard(canvas, phoneLeft - 12f, phoneTop - 12f, phoneRight + 12f, phoneBottom + 12f, Color.parseColor("#1E293B"))
                drawRoundedCard(canvas, phoneLeft, phoneTop, phoneRight, phoneBottom, Color.parseColor("#F4F7F2"))

                // Phone status bar inside screen recording
                val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
                canvas.drawRect(phoneLeft, phoneTop, phoneRight, phoneTop + 62f, barPaint)
                drawLeftAlignedText(
                    canvas = canvas,
                    text = "Smart Farm Shop",
                    x = phoneLeft + 24f,
                    y = phoneTop + 16f,
                    maxWidth = 360,
                    textSize = 22f,
                    color = Color.WHITE,
                    bold = true
                )

                val pWidth = (phoneRight - phoneLeft - 48f).toInt()
                when (subStep) {
                    0 -> {
                        // Product card in phone + click hand on "এখানে ক্লিক করুন / কার্টে যোগ করুন"
                        drawRoundedCard(canvas, phoneLeft + 20f, phoneTop + 96f, phoneRight - 20f, phoneTop + 440f, Color.WHITE)
                        drawLeftAlignedText(
                            canvas = canvas,
                            text = storyboard.productName,
                            x = phoneLeft + 36f,
                            y = phoneTop + 130f,
                            maxWidth = pWidth - 20,
                            textSize = 26f,
                            color = Color.parseColor("#111827"),
                            bold = true
                        )
                        drawLeftAlignedText(
                            canvas = canvas,
                            text = storyboard.priceText.ifBlank { "তাজা খামার পণ্য" },
                            x = phoneLeft + 36f,
                            y = phoneTop + 218f,
                            maxWidth = pWidth - 20,
                            textSize = 28f,
                            color = accentColor,
                            bold = true
                        )
                        drawPill(
                            canvas,
                            cx = w / 2f,
                            cy = phoneTop + 360f,
                            pillWidth = (phoneRight - phoneLeft) - 80f,
                            pillHeight = 74f,
                            bgColor = accentColor,
                            borderColor = Color.parseColor("#FFEB3B")
                        )
                        drawCenteredText(
                            canvas = canvas,
                            text = "এখানে ক্লিক করুন • কার্টে যোগ করুন",
                            x = phoneLeft + 44f,
                            y = phoneTop + 340f,
                            maxWidth = pWidth - 16,
                            textSize = 22f,
                            color = Color.WHITE,
                            bold = true
                        )
                        drawTapIndicator(canvas, w / 2f + 40f, phoneTop + 368f, pulse)
                    }
                    1 -> {
                        // Checkout screen in phone + click hand on "এখনই অর্ডার করুন"
                        drawRoundedCard(canvas, phoneLeft + 20f, phoneTop + 96f, phoneRight - 20f, phoneTop + 420f, Color.WHITE)
                        drawLeftAlignedText(
                            canvas = canvas,
                            text = "ডেলিভারি ঠিকানা:\nঢাকা / আপনার ঠিকানা\nপেমেন্ট: বিকাশ / নগদ / ক্যাশ",
                            x = phoneLeft + 36f,
                            y = phoneTop + 132f,
                            maxWidth = pWidth - 20,
                            textSize = 23f,
                            color = Color.parseColor("#1F2937"),
                            bold = false
                        )
                        drawPill(
                            canvas,
                            cx = w / 2f,
                            cy = phoneTop + 510f,
                            pillWidth = (phoneRight - phoneLeft) - 70f,
                            pillHeight = 82f,
                            bgColor = Color.parseColor("#D32F2F"),
                            borderColor = Color.parseColor("#FFD54F")
                        )
                        drawCenteredText(
                            canvas = canvas,
                            text = "এখনই অর্ডার করুন",
                            x = phoneLeft + 40f,
                            y = phoneTop + 490f,
                            maxWidth = pWidth - 12,
                            textSize = 26f,
                            color = Color.WHITE,
                            bold = true
                        )
                        drawTapIndicator(canvas, w / 2f + 24f, phoneTop + 515f, pulse)
                    }
                    else -> {
                        // Order confirmed + Farm notification
                        drawRoundedCard(canvas, phoneLeft + 24f, phoneTop + 140f, phoneRight - 24f, phoneTop + 460f, Color.parseColor("#E8F5E9"))
                        drawCenteredText(
                            canvas = canvas,
                            text = "✓ অর্ডার সম্পন্ন হয়েছে!\n\nসরাসরি খামার অ্যাকাউন্টে নোটিফিকেশন পৌঁছে গেছে!",
                            x = phoneLeft + 44f,
                            y = phoneTop + 210f,
                            maxWidth = pWidth - 24,
                            textSize = 26f,
                            color = Color.parseColor("#1B5E20"),
                            bold = true
                        )
                    }
                }

                // Bottom caption inside screen-record scene
                val stepCaption = storyboard.orderStepsBn.getOrNull(subStep)
                    ?: storyboard.orderStepsBn.lastOrNull().orEmpty()
                drawRoundedCard(canvas, phoneLeft + 18f, phoneBottom - 160f, phoneRight - 18f, phoneBottom - 20f, Color.WHITE)
                drawLeftAlignedText(
                    canvas = canvas,
                    text = stepCaption,
                    x = phoneLeft + 34f,
                    y = phoneBottom - 142f,
                    maxWidth = pWidth - 12,
                    textSize = 21f,
                    color = Color.parseColor("#1B1B1B"),
                    bold = true
                )
            }

            // Scene 4: 11.5s - 16s Ending Outro: How to Download the App + How to Order Online
            else -> {
                drawProductPhotoFrame(
                    canvas = canvas,
                    bitmap = productBitmap,
                    left = 150f,
                    top = 128f,
                    right = w - 150f,
                    bottom = 350f,
                    zoomScale = 1.04f,
                    badgeText = storyboard.priceText
                )
                drawCenteredText(
                    canvas = canvas,
                    text = "📲 অ্যাপ ডাউনলোড ও অনলাইনে অর্ডার নিয়ম",
                    x = 40f,
                    y = 374f,
                    maxWidth = (w - 80f).toInt(),
                    textSize = 27f,
                    color = Color.parseColor("#FFE082"),
                    bold = true
                )

                // Outro Card 1: How to Download the App
                drawRoundedCard(canvas, 52f, 434f, w - 52f, 670f, Color.argb(242, 255, 255, 255))
                drawLeftAlignedText(
                    canvas = canvas,
                    text = "১. কিভাবে অ্যাপ ডাউনলোড করবেন:",
                    x = 78f,
                    y = 456f,
                    maxWidth = (w - 156f).toInt(),
                    textSize = 25f,
                    color = darkAccent,
                    bold = true
                )
                drawLeftAlignedText(
                    canvas = canvas,
                    text = "▶ Google Play Store বা শেয়ার লিংক থেকে 'Smart Farm' অ্যাপ ইনস্টল করুন এবং কাস্টমার হিসেবে ফ্রি অ্যাকাউন্ট খুলুন।",
                    x = 78f,
                    y = 508f,
                    maxWidth = (w - 156f).toInt(),
                    textSize = 22f,
                    color = Color.parseColor("#1B1B1B"),
                    bold = false
                )

                // Outro Card 2: How to Order Online
                drawRoundedCard(canvas, 52f, 696f, w - 52f, 978f, Color.parseColor("#E8F5E9"))
                drawLeftAlignedText(
                    canvas = canvas,
                    text = "২. কিভাবে অনলাইনে অর্ডার করবেন:",
                    x = 78f,
                    y = 718f,
                    maxWidth = (w - 156f).toInt(),
                    textSize = 25f,
                    color = Color.parseColor("#1B5E20"),
                    bold = true
                )
                drawLeftAlignedText(
                    canvas = canvas,
                    text = storyboard.downloadAndOrderOutroBn.ifBlank {
                        BengaliAdStoryboard.DEFAULT_DOWNLOAD_AND_ORDER_OUTRO_BN
                    },
                    x = 78f,
                    y = 768f,
                    maxWidth = (w - 156f).toInt(),
                    textSize = 22f,
                    color = Color.parseColor("#111827"),
                    bold = true
                )

                drawCenteredText(
                    canvas = canvas,
                    text = "${storyboard.farmName} • ${storyboard.productName}",
                    x = 56f,
                    y = 1008f,
                    maxWidth = (w - 112f).toInt(),
                    textSize = 25f,
                    color = Color.parseColor("#B9F6CA"),
                    bold = true
                )
            }
        }

        // Persistent Bottom Commercial CTA Button Banner
        drawPill(
            canvas = canvas,
            cx = w / 2f,
            cy = h - 140f,
            pillWidth = w - 88f,
            pillHeight = 104f,
            bgColor = Color.parseColor("#D32F2F"),
            borderColor = Color.parseColor("#FFD54F")
        )
        drawCenteredText(
            canvas = canvas,
            text = storyboard.callToActionBn.ifBlank { "এখনই অর্ডার করুন • এখানে ক্লিক করুন" },
            x = 64f,
            y = h - 170f,
            maxWidth = (w - 128f).toInt(),
            textSize = 28f,
            color = Color.WHITE,
            bold = true
        )

        // Bottom Timeline Progress Bar
        val trackPaint = Paint().apply { color = Color.argb(70, 255, 255, 255) }
        val fillPaint = Paint().apply { color = Color.parseColor("#FFD54F") }
        canvas.drawRect(56f, h - 52f, w - 56f, h - 42f, trackPaint)
        canvas.drawRect(56f, h - 52f, 56f + (w - 112f) * progress, h - 42f, fillPaint)
    }

    private fun drawTapIndicator(canvas: Canvas, cx: Float, cy: Float, pulse: Float) {
        val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 5f
            color = Color.argb(((1f - pulse) * 230).toInt().coerceIn(40, 230), 255, 235, 59)
        }
        canvas.drawCircle(cx, cy, 24f + pulse * 26f, ripplePaint)
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFEB3B") }
        canvas.drawCircle(cx, cy, 14f, dotPaint)
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 34f
        }
        canvas.drawText("👆", cx - 12f, cy + 44f, textPaint)
    }

    private fun drawProductPhotoFrame(
        canvas: Canvas,
        bitmap: Bitmap?,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        zoomScale: Float,
        badgeText: String
    ) {
        val rect = RectF(left, top, right, bottom)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawRoundRect(rect, 34f, 34f, bgPaint)

        val clipPath = Path().apply { addRoundRect(RectF(left + 8f, top + 8f, right - 8f, bottom - 8f), 28f, 28f, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(clipPath)
        if (bitmap != null && !bitmap.isRecycled) {
            val boxW = right - left - 16f
            val boxH = bottom - top - 16f
            val bmpScale = maxOf(boxW / bitmap.width.toFloat(), boxH / bitmap.height.toFloat()) * zoomScale
            val drawW = bitmap.width * bmpScale
            val drawH = bitmap.height * bmpScale
            val dx = (left + 8f) + (boxW - drawW) / 2f
            val dy = (top + 8f) + (boxH - drawH) / 2f
            val dest = RectF(dx, dy, dx + drawW, dy + drawH)
            canvas.drawBitmap(bitmap, null, dest, Paint(Paint.FILTER_BITMAP_FLAG))
        } else {
            val placeholderPaint = Paint().apply { color = Color.parseColor("#E8F5E9") }
            canvas.drawRect(rect, placeholderPaint)
            drawCenteredText(
                canvas = canvas,
                text = "🌾 খাঁটি খামার পণ্য",
                x = left + 20f,
                y = (top + bottom) / 2f - 28f,
                maxWidth = (right - left - 40f).toInt(),
                textSize = 36f,
                color = Color.parseColor("#1B5E20"),
                bold = true
            )
        }
        canvas.restore()

        if (badgeText.isNotBlank()) {
            drawPill(
                canvas = canvas,
                cx = right - 128f,
                cy = bottom - 34f,
                pillWidth = 220f,
                pillHeight = 56f,
                bgColor = Color.parseColor("#FF6F00"),
                borderColor = Color.WHITE
            )
            drawCenteredText(
                canvas = canvas,
                text = badgeText,
                x = right - 230f,
                y = bottom - 50f,
                maxWidth = 204,
                textSize = 23f,
                color = Color.WHITE,
                bold = true
            )
        }
    }

    private fun drawRoundedCard(canvas: Canvas, l: Float, t: Float, r: Float, b: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        canvas.drawRoundRect(RectF(l, t, r, b), 24f, 24f, paint)
    }

    private fun drawPill(canvas: Canvas, cx: Float, cy: Float, pillWidth: Float, pillHeight: Float, bgColor: Int, borderColor: Int) {
        val rect = RectF(cx - pillWidth / 2f, cy - pillHeight / 2f, cx + pillWidth / 2f, cy + pillHeight / 2f)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(rect, pillHeight / 2f, pillHeight / 2f, fill)
        canvas.drawRoundRect(rect, pillHeight / 2f, pillHeight / 2f, stroke)
    }

    private fun drawCenteredText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Int,
        textSize: Float,
        color: Int,
        bold: Boolean
    ) {
        drawTextLayout(canvas, text, x, y, maxWidth, textSize, color, bold, Layout.Alignment.ALIGN_CENTER)
    }

    private fun drawLeftAlignedText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Int,
        textSize: Float,
        color: Int,
        bold: Boolean
    ) {
        drawTextLayout(canvas, text, x, y, maxWidth, textSize, color, bold, Layout.Alignment.ALIGN_NORMAL)
    }

    private fun drawTextLayout(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Int,
        textSize: Float,
        color: Int,
        bold: Boolean,
        alignment: Layout.Alignment
    ) {
        if (maxWidth <= 0 || text.isBlank()) return
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.textSize = textSize
            this.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, maxWidth)
            .setAlignment(alignment)
            .setLineSpacing(6f, 1.0f)
            .build()
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        val r = ((Color.red(color) * (1f - factor))).toInt().coerceIn(0, 255)
        val g = ((Color.green(color) * (1f - factor))).toInt().coerceIn(0, 255)
        val b = ((Color.blue(color) * (1f - factor))).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    fun saveMp4ToMoviesGallery(context: Context, mp4File: File): Uri? {
        if (!mp4File.exists() || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, mp4File.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/SmartFarmAds")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        return runCatching {
            resolver.openOutputStream(uri)?.use { out ->
                mp4File.inputStream().use { input -> input.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        }.getOrElse {
            resolver.delete(uri, null, null)
            null
        }
    }

    fun buildShareCaption(storyboard: BengaliAdStoryboard?): String {
        if (storyboard == null) {
            return "Smart Farm অ্যাপ ডাউনলোড করে সরাসরি খামার থেকে তাজা পণ্য অনলাইনে অর্ডার করুন!"
        }
        return buildString {
            appendLine(storyboard.hookHeadlineBn)
            append("${storyboard.productName} • ${storyboard.farmName}")
            if (storyboard.priceText.isNotBlank()) append(" (${storyboard.priceText})")
            appendLine()
            appendLine("✅ ${storyboard.downloadAndOrderOutroBn}")
            append("🛒 ${storyboard.callToActionBn}")
        }
    }

    fun isPackageInstalledForVideoShare(context: Context, packageName: String): Boolean {
        val probe = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            setPackage(packageName)
        }
        return probe.resolveActivity(context.packageManager) != null
    }

    fun createTargetedShareVideoIntent(
        context: Context,
        mp4File: File,
        storyboard: BengaliAdStoryboard? = null,
        platform: SharePlatform = SharePlatform.ANY_APP
    ): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", mp4File)
        val caption = buildShareCaption(storyboard)
        val baseSendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, storyboard?.productName ?: "Smart Farm Product Ad Video")
            putExtra(Intent.EXTRA_TEXT, caption)
            clipData = android.content.ClipData.newRawUri("Smart Farm Video Ad", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (context !is android.app.Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        val installedPkg = platform.packageCandidates.firstOrNull { isPackageInstalledForVideoShare(context, it) }
        return if (installedPkg != null) {
            baseSendIntent.setPackage(installedPkg)
        } else {
            Intent.createChooser(baseSendIntent, "Share Product Ad Video").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (context !is android.app.Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        }
    }

    fun launchShareVideo(
        context: Context,
        mp4File: File,
        storyboard: BengaliAdStoryboard? = null,
        platform: SharePlatform = SharePlatform.ANY_APP
    ): Boolean {
        val targeted = createTargetedShareVideoIntent(context, mp4File, storyboard, platform)
        val launched = runCatching {
            context.startActivity(targeted)
            true
        }.getOrElse { false }
        if (launched) return true

        val fallback = createTargetedShareVideoIntent(context, mp4File, storyboard, SharePlatform.ANY_APP)
        return runCatching {
            context.startActivity(fallback)
            true
        }.getOrElse { false }
    }

    fun createShareVideoIntent(context: Context, mp4File: File): Intent =
        createTargetedShareVideoIntent(context, mp4File, storyboard = null, platform = SharePlatform.ANY_APP)
}
