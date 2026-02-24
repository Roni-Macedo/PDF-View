package com.example.pdfview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.pdfview.ui.screens.PdfViewerScreen


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val pdfUri = intent?.data

        setContent {
            PdfViewerScreen(pdfUri)
        }
    }
}
