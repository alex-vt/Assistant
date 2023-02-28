package com.alexvt.assistant.platform

import net.sourceforge.tess4j.Tesseract
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit

actual fun extractFromScreen(top: Int, bottom: Int, left: Int, right: Int): String {
    val screenBufferedImage =
        Robot().createScreenCapture(Rectangle(Toolkit.getDefaultToolkit().screenSize))
    return Tesseract().apply {
        // system installed ocr-tesseract 5 package data used.
        // See https://stackoverflow.com/questions/36166164/tesseract-for-java-setting-tessdata-prefix-for-executable-jar
        setDatapath("/usr/share/tesseract-ocr/5/tessdata")
    }.doOCR(screenBufferedImage, Rectangle(left, top, right - left, bottom - top))
}
