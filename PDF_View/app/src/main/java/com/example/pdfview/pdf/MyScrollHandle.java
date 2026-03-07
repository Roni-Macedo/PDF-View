package com.example.pdfview.pdf;

import android.content.Context;
import android.graphics.Color;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;

public class MyScrollHandle extends DefaultScrollHandle {

    private PDFView pdfView;

    public MyScrollHandle(Context context) {
        super(context);
    }

    @Override
    public void setupLayout(PDFView pdfView) {
        super.setupLayout(pdfView);

        this.pdfView = pdfView; // salva o pdfView

        getBackground().setTint(Color.parseColor("#EDF2FA")); // cor da barra
    }

    @Override
    public void setPageNum(int pageNum) {
        if (pdfView != null) {
            int total = pdfView.getPageCount();
            textView.setText(pageNum + " / " + total);
        }
    }
}
