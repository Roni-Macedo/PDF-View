package com.example.pdfview.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

class PdfBitmapConverter(
    private val context: Context,
) {
    private var renderer: PdfRenderer? = null

    suspend fun pdfToBitmaps(contentUri: Uri): List<Bitmap> {
        return withContext(Dispatchers.IO) {
            // Fecha o renderer anterior se existir para evitar vazamento de memória
            renderer?.close()

            try {
                val pfd = context.contentResolver.openFileDescriptor(contentUri, "r")
                if (pfd != null) {
                    val currentRenderer = PdfRenderer(pfd)
                    renderer = currentRenderer

                    // Usamos async para processar as páginas em paralelo (mais rápido)
                    val bitmaps = (0 until currentRenderer.pageCount).map { index ->
                        async {
                            renderPageToBitmap(currentRenderer, index)
                        }
                    }.awaitAll()

                    // Importante: pfd e renderer devem ser fechados depois, ou mantidos
                    // conforme a necessidade. Aqui fechamos para liberar recursos.
                    currentRenderer.close()
                    pfd.close()

                    return@withContext bitmaps
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext emptyList()
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