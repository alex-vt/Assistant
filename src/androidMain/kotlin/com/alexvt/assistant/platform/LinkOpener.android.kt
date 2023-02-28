package com.alexvt.assistant.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri


actual fun openLinkInChrome(link: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    intent.setPackage("com.android.chrome") // see https://stackoverflow.com/questions/12013416
    try {
        androidContext.startActivity(intent)
    } catch (exception: ActivityNotFoundException) {
        // Chrome browser presumably not installed so allow user to choose instead
        intent.setPackage(null)
        androidContext.startActivity(intent)
    }
}

lateinit var androidContext: Context // todo don't expose
