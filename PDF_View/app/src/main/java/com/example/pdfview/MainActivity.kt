package com.example.pdfview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.pdfview.ui.screens.PdfViewerScreen


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val pdfUri = intent?.data

        setContent {
            Scaffold(
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                PdfViewerScreen(
                    modifier = Modifier.padding(innerPadding),
                    initialPdfUri = pdfUri
                )

            }
        }
    }
}
