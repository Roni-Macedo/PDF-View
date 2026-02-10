package com.example.pdfview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RawRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.example.pdfview.ui.components.VerticalScrollbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val pdfUri = intent?.data

        setContent {

            if (pdfUri != null) {
                PdfFromUriScreen(pdfUri)
            } else {
                PdfFromRawScreen()
            }
        }
    }
}

/*
O que faz:

Copia um PDF de res/raw para a pasta cache

Necessário porque:

PdfRenderer não lê direto de raw

Ele precisa de um File
 */
fun copyPdfFromRawToCache(
    context: Context,
    @RawRes rawResId: Int,
    fileName: String
): File {
    val file = File(context.cacheDir, fileName)

    context.resources.openRawResource(rawResId).use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    return file
}

/*
O que faz:

Recebe um PDF vindo de outro app (Google Drive, Files, etc)

Lê via ContentResolver

Copia para cacheDir

Retorna um File utilizável pelo PdfRenderer

📌 Mesmo objetivo da função anterior, só muda a origem.
 */
fun copyPdfFromUriToCache(
    context: Context,
    uri: Uri,
    fileName: String = "external.pdf"
): File {
    val file = File(context.cacheDir, fileName)

    context.contentResolver.openInputStream(uri)?.use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    }

    return file
}

/*
O que faz:

Tela que renderiza PDF externo

Usa remember(uri) para:

copiar uma única vez

evitar copiar de novo em recomposição

Chama o viewer real:
 */
@Composable
fun PdfFromUriScreen(uri: Uri) {
    val context = LocalContext.current

    val pdfFile = remember(uri) {
        copyPdfFromUriToCache(context, uri)
    }

    SimplePdfViewer(file = pdfFile)
}

/*
O que faz:

Descobre qual página está mais visível na tela

Analisa:

offset do item

tamanho

viewport visível

Retorna o índice da página dominante

📌 Usado para:

contador de páginas

scrollbar

indicador flutuante

👉 Essa função é ouro para UX.
 */
fun LazyListState.mostVisibleItemIndex(): Int {
    val layoutInfo = layoutInfo
    if (layoutInfo.visibleItemsInfo.isEmpty()) return 0

    return layoutInfo.visibleItemsInfo.maxBy { item ->
        val start = maxOf(item.offset, 0)
        val end = minOf(
            item.offset + item.size,
            layoutInfo.viewportEndOffset
        )
        end - start
    }.index
}

@Composable
fun PdfFromRawScreen(
) {
    val context = LocalContext.current

    val pdfFile = remember {
        copyPdfFromRawToCache(
            context = context,
            rawResId = R.raw.teste2,
            fileName = "teste.pdf"
        )
    }

    SimplePdfViewer(file = pdfFile)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimplePdfViewer(file: File) {
    var pageCount by remember { mutableIntStateOf(0) }
    val bitmapCache = remember { mutableStateMapOf<Int, Bitmap>() }
    val listState = rememberLazyListState()

    val currentPage by remember {
        derivedStateOf {
            when {
                pageCount == 0 -> 0

                // 🔥 ESSA LINHA RESOLVE O 4/5
                !listState.canScrollForward -> pageCount

                else -> listState.mostVisibleItemIndex() + 1
            }
        }
    }

    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            pageCount = renderer.pageCount
            renderer.close()
            fd.close()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = if (pageCount > 0) "Página $currentPage / $pageCount" else "PDF")
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.LightGray),
            ) {
                items(count = pageCount, key = { it }) { pageIndex ->
                    val bitmap = bitmapCache[pageIndex]
                    LaunchedEffect(pageIndex) {
                        if (bitmap == null) {
                            withContext(Dispatchers.IO) {
                                val fd = ParcelFileDescriptor.open(
                                    file,
                                    ParcelFileDescriptor.MODE_READ_ONLY
                                )
                                val renderer = PdfRenderer(fd)
                                val page = renderer.openPage(pageIndex)
                                val bmp = createBitmap(page.width, page.height)
                                page.render(
                                    bmp,
                                    null,
                                    null,
                                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                                )

                                Log.d(
                                    "PDF_BITMAP",
                                    "Página $pageIndex | " +
                                            "Tamanho: ${bmp.width}x${bmp.height} | " +
                                            "Memória: ${bmp.byteCount / 1024 / 1024} MB"
                                )

                                // 🔥 LOG DA MEMÓRIA TOTAL DO APP
                                val runtime = Runtime.getRuntime()
                                val usedMB =
                                    (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024

                                Log.d("APP_MEMORY", "Memória usada: $usedMB MB")

                                page.close()
                                renderer.close()
                                fd.close()

                                Log.d("PDF_RENDER", "Página $pageIndex fechada corretamente")

                                bitmapCache[pageIndex] = bmp
                            }
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.FillWidth // Ajustado para preencher largura
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                }

            }
            VerticalScrollbar(
                listState = listState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp),
                currentPage = currentPage,
                pageCount = pageCount
            )
        }
    }
}
