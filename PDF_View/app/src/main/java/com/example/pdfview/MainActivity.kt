package com.example.pdfview

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RawRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.pdfview.ui.theme.PDFViewTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PDFViewTheme {
                PdfFromRawScreen()
                HideSystemBars()
            }
        }
    }
}

@Composable
fun HideSystemBars() {
    val view = LocalView.current

    DisposableEffect(Unit) {
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)

        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        controller.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

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

    val bitmapCache = remember {
        mutableStateMapOf<Int, Bitmap>()
    }

    val listState = rememberLazyListState()
    val currentPage by remember {
        derivedStateOf {
            when {
                pageCount == 0 -> 0
                !listState.canScrollForward -> pageCount
                else -> listState.mostVisibleItemIndex() + 1
            }
        }
    }

    // 📄 Total de páginas
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
                    Text(
                        text = if (pageCount > 0)
                            "Página $currentPage / $pageCount"
                        else
                            "PDF"
                    )
                }
            )
        }
    ) { paddingValues ->

        if (pageCount == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.LightGray)
                .padding(paddingValues)
        ) {
            items(
                count = pageCount,
                key = { it }
            ) { pageIndex ->

                val bitmap = bitmapCache[pageIndex]

                LaunchedEffect(bitmap) {
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

                            page.close()
                            renderer.close()
                            fd.close()

                            bitmapCache[pageIndex] = bmp
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Página ${pageIndex + 1}",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Crop
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
    }
}

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
