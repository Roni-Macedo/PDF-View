package com.example.pdfview

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.pdfview.ui.theme.PDFViewTheme
import com.github.barteksc.pdfviewer.PDFView


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

    AndroidView(
        modifier = modifier.fillMaxSize(),

        factory = { ctx ->

            PDFView(ctx, null).apply {

                fromUri(uri)
                    .enableSwipe(true)
                    .swipeHorizontal(false)

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

    var pdfUri by remember { mutableStateOf(initialPdfUri) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        pdfUri = uri
    }

    Scaffold(

        topBar = {
            TopAppBar(
                title = { Text("PDF Viewer") },

                actions = {
                    IconButton(
                        onClick = { launcher.launch(arrayOf("application/pdf")) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Abrir PDF"
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
        ) {

            if (pdfUri == null) {

                Button(
                    onClick = {
                        launcher.launch(arrayOf("application/pdf"))
                    },
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text("Escolher PDF")
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