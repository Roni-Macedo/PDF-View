package com.example.pdfview

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import com.example.pdfview.ui.theme.PDFViewTheme
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle


class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val pdfUri = intent?.data


        setContent {
            PDFViewTheme() {
                PdfViewerScreen(initialPdfUri = pdfUri)
            }
        }
    }
}

@Composable
fun PdfViewer(
    uri: Uri,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current
    val backgroundColor = MaterialTheme.colorScheme.primary
    val androidColor = backgroundColor.toArgb()

    AndroidView(
        modifier = modifier.fillMaxSize(),

        factory = { ctx ->

            PDFView(ctx, null).apply {

                // cor entre as páginas
                setBackgroundColor(androidColor)

                fromUri(uri)
                    .enableSwipe(true)
                    .swipeHorizontal(false)

                    .scrollHandle(MyScrollHandle(ctx))

                    // zoom
                    .enableDoubletap(true)

                    .defaultPage(0)

                    .spacing(10)

                    // performance
                    .enableAntialiasing(true)

                    // gestos
                    .pageFling(true)
                    .pageSnap(true)

                    .load()
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(initialPdfUri: Uri?) {

    val context = LocalContext.current

    var pdfUri by remember { mutableStateOf(initialPdfUri) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        pdfUri = uri
    }

    val pdfName = remember(pdfUri) {
        pdfUri?.let { getPdfName(context, it) } ?: "VIZUALIZAR PDF"
    }

    Scaffold(

        topBar = {
            CenterAlignedTopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(
                        text = pdfName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },

                navigationIcon = {
                    IconButton(onClick = { launcher.launch(arrayOf("application/pdf")) }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Localized description"
                        )
                    }
                }
            )
        }

    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.secondary)
        ) {

            if (pdfUri == null) {

                Button(
                    onClick = {
                        launcher.launch(arrayOf("application/pdf"))
                    },
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text("Escolher PDF", color = Color.Black)
                }

            } else {

                PdfViewer(
                    uri = pdfUri!!,
                    modifier = Modifier.fillMaxSize()
                )

            }
        }
    }
}

fun getPdfName(context: Context, uri: Uri): String {
    var name = "PDF"

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

class MyScrollHandle(context: Context?) : DefaultScrollHandle(context) {
    init {
        setBackgroundColor(Color.Red.toArgb()) // cor da barra
    }
}