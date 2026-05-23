package com.example.smartreceiptmanager.scanbill;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

public class ScanFrameView extends View {

    private final Paint cornerPaint, borderPaint, crosshairPaint;
    private final float cornerLen, radius;

    public ScanFrameView(Context context) { this(context, null); }
    public ScanFrameView(Context context, AttributeSet attrs) { this(context, attrs, 0); }
    public ScanFrameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        float dp = context.getResources().getDisplayMetrics().density;
        cornerLen = 24f * dp;
        radius    = 8f  * dp;

        cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cornerPaint.setColor(Color.parseColor("#00C853"));
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(4f * dp);
        cornerPaint.setStrokeCap(Paint.Cap.ROUND);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.parseColor("#4400C853"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.5f * dp);

        crosshairPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        crosshairPaint.setColor(Color.parseColor("#80FFFFFF"));
        crosshairPaint.setStyle(Paint.Style.STROKE);
        crosshairPaint.setStrokeWidth(1f * dp);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        float cx = w / 2f, cy = h / 2f;
        float cl = cornerLen;
        float ch = 20f * getResources().getDisplayMetrics().density;

        canvas.drawRoundRect(new RectF(0, 0, w, h), radius, radius, borderPaint);
        canvas.drawLine(cx - ch, cy, cx + ch, cy, crosshairPaint);
        canvas.drawLine(cx, cy - ch, cx, cy + ch, crosshairPaint);

        // Top-Left
        canvas.drawLine(0, cl, 0, 0, cornerPaint);
        canvas.drawLine(0, 0, cl, 0, cornerPaint);
        // Top-Right
        canvas.drawLine(w - cl, 0, w, 0, cornerPaint);
        canvas.drawLine(w, 0, w, cl, cornerPaint);
        // Bottom-Left
        canvas.drawLine(0, h - cl, 0, h, cornerPaint);
        canvas.drawLine(0, h, cl, h, cornerPaint);
        // Bottom-Right
        canvas.drawLine(w - cl, h, w, h, cornerPaint);
        canvas.drawLine(w, h, w, h - cl, cornerPaint);
    }
}

