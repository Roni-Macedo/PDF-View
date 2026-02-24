package com.example.pdfview.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import androidx.core.graphics.createBitmap

class PdfComposeRenderer(private val context: Context) {

    private var pdfRenderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null
    var currentPageIndex = 0
        private set

    val pageCount: Int
        get() = pdfRenderer?.pageCount ?: 0

    fun openPdfFromRaw(rawResId: Int) {
        val file = File(context.cacheDir, "temp.pdf")
        context.resources.openRawResource(rawResId).use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        openPdfFromFile(file)
    }

    fun openPdfFromUri(uri: Uri) {
        val fileDescriptor = context.contentResolver
            .openFileDescriptor(uri, "r")
        pdfRenderer = PdfRenderer(fileDescriptor!!)
    }

    private fun openPdfFromFile(file: File) {
        val descriptor = ParcelFileDescriptor.open(
            file,
            ParcelFileDescriptor.MODE_READ_ONLY
        )
        pdfRenderer = PdfRenderer(descriptor)
    }

    fun renderPage(): Bitmap? {
        pdfRenderer?.let { renderer ->
            if (currentPageIndex >= renderer.pageCount) return null

            currentPage?.close()
            currentPage = renderer.openPage(currentPageIndex)

            val bitmap = createBitmap(currentPage!!.width, currentPage!!.height)

            currentPage!!.render(
                bitmap,
                null,
                null,
                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
            )

            return bitmap
        }
        return null
    }

    fun nextPage(): Bitmap? {
        pdfRenderer?.let {
            if (currentPageIndex < it.pageCount - 1) {
                currentPageIndex++
            }
            return renderPage()
        }
        return null
    }

    fun previousPage(): Bitmap? {
        pdfRenderer?.let {
            if (currentPageIndex > 0) {
                currentPageIndex--
            }
            return renderPage() // sempre renderiza a página atual
        }
        return null
    }

    fun close() {
        currentPage?.close()
        pdfRenderer?.close()
    }
}