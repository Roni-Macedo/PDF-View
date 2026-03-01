package com.example.pdfview.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PdfBitmapConverter(
    private val context: Context,
) {
    private var renderer: PdfRenderer? = null

    suspend fun pdfToBitmaps(contentUri: Uri): List<Bitmap> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Abrimos o descritor de arquivo
                context.contentResolver.openFileDescriptor(contentUri, "r")?.use { pfd ->
                    // 2. Criamos o renderer (o .use garante que ele será fechado ao fim)
                    PdfRenderer(pfd).use { renderer ->
                        // 3. Mapeamos as páginas de forma SEQUENCIAL (sem async)
                        // O renderer NÃO aceita chamadas paralelas
                        (0 until renderer.pageCount).map { index ->
                            renderPageToBitmap(renderer, index)
                        }
                    }
                } ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    private fun renderPageToBitmap(pdfRenderer: PdfRenderer, index: Int): Bitmap {
        val page = pdfRenderer.openPage(index)

        // Criamos o bitmap com a resolução original da página
        val bitmap = createBitmap(page.width, page.height)

        // No Android nativo (PdfRenderer), precisamos pintar o fundo de branco,
        // caso contrário, páginas com fundo transparente ficarão pretas.
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        page.render(
            bitmap,
            null, // destClip
            null, // transform
            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
        )

        page.close()
        return bitmap
    }
}