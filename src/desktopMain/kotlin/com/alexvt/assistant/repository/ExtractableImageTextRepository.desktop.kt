package com.alexvt.assistant.repository

import com.alexvt.assistant.AppScope
import me.tatarka.inject.annotations.Inject
import net.sourceforge.tess4j.Tesseract
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.io.File

@AppScope
@Inject
actual class ExtractableImageTextRepository {

    private val tesseractDataPath = "/usr/share/tesseract-ocr/5/tessdata"

    actual fun isExtractionAvailable(): Boolean =
        File(tesseractDataPath).exists()

    actual suspend fun extractFromScreenArea(
        top: Int,
        bottom: Int,
        left: Int,
        right: Int,
        onImageCaptured: () -> Unit,
    ): String {
        val screenBufferedImage =
            Robot().createScreenCapture(Rectangle(Toolkit.getDefaultToolkit().screenSize))
        onImageCaptured()
        return Tesseract().apply {
            // system installed ocr-tesseract 5 package data used.
            // See https://stackoverflow.com/questions/36166164/tesseract-for-java-setting-tessdata-prefix-for-executable-jar
            setDatapath(tesseractDataPath)
        }.doOCR(screenBufferedImage, Rectangle(left, top, right - left, bottom - top))
    }

}
