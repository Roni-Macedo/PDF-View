package com.example.pdfview.ui.components

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

fun getPdfName(context: Context, uri: Uri): String? {
    var name: String? = null

    val cursor = context.contentResolver.query(
        uri,
        null,
        null,
        null,
        null
    )

    cursor?.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (it.moveToFirst() && nameIndex != -1) {
            name = it.getString(nameIndex)
        }
    }

    return name
}