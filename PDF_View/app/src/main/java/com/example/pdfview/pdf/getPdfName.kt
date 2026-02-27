package com.example.pdfview.pdf

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

fun getPdfName(context: Context, uri: Uri?): String {
    if (uri == null) return "PDF Viewer"

    var name = "PDF Viewer"

    val cursor = context.contentResolver.query(
        uri,
        null,
        null,
        null,
        null
    )

    cursor?.use {
        val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (it.moveToFirst() && index != -1) {
            name = it.getString(index)
        }
    }

    return name
}