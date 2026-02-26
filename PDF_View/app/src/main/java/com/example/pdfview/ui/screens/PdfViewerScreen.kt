package com.example.pdfview.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.pdfview.ui.components.PdfBitmapConverter

@Composable
fun PdfViewerScreen(
    modifier: Modifier = Modifier,
    initialPdfUri: Uri? = null // Renomeado para evitar conflito
) {
    val context = LocalContext.current
    val pdfBitmapConverter = remember { PdfBitmapConverter(context) }

    var selectedUri by remember { mutableStateOf(initialPdfUri) }
    var renderedPages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // --- ESSE É O ÚNICO LAUNCHED EFFECT NECESSÁRIO ---
    // Ele dispara sempre que selectedUri mudar (seja pelo valor inicial ou pelo botão)
    LaunchedEffect(selectedUri) {
        selectedUri?.let { uri ->
            isLoading = true
            renderedPages = pdfBitmapConverter.pdfToBitmaps(uri)
            isLoading = false
        }
    }

    val choosePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        // Quando o usuário escolhe o arquivo, o LaunchedEffect acima é disparado automaticamente
        selectedUri = uri
    }

    // Efeito para carregar o PDF quando a URI mudar
    LaunchedEffect(selectedUri) {
        selectedUri?.let { uri ->
            isLoading = true
            renderedPages = pdfBitmapConverter.pdfToBitmaps(uri)
            isLoading = false
        }
    }

    if (selectedUri == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = { choosePdfLauncher.launch("application/pdf") }) {
                Text(text = "Escolha um PDF")
            }
        }
    } else if (isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            LazyColumn(
                modifier = modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(renderedPages) { page ->
                    PdfPage(page = page, modifier = Modifier.padding(8.dp))
                }
            }

        }

    }
}

@Composable
fun PdfPage(
    page: Bitmap,
    modifier: Modifier = Modifier,
){
    Image(
        bitmap = page.asImageBitmap(), // CORREÇÃO AQUI
        contentDescription = null,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(page.width.toFloat() / page.height.toFloat())
    )
}