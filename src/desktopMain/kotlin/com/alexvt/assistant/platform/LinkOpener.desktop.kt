package com.alexvt.assistant.platform

actual fun openLinkInChrome(link: String) {
    Runtime.getRuntime().exec(arrayOf("bash", "-c", "google-chrome $link"))
}
