package com.shouzhong.zxing.demo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.shouzhong.zxing.Callback;
import com.shouzhong.zxing.IViewFinder;
import com.shouzhong.zxing.ZXingScannerView;

public class ScannerTopActivity extends AppCompatActivity {

    private ZXingScannerView zXingScannerView;
    private TextView tv;
    private Vibrator vibrator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner_top);
        tv = findViewById(R.id.tv);
        zXingScannerView = findViewById(R.id.zxing);
        zXingScannerView.setViewFinder(new ViewFinder(this));
        zXingScannerView.setCallback(new Callback() {
            @Override
            public void result(String s, String path) {
                Log.e("==================", s);
                tv.setText(s);
                startVibrator();
                zXingScannerView.restartPreviewAfterDelay(2000);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        zXingScannerView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        zXingScannerView.onPause();
    }

    @Override
    protected void onDestroy() {
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }
        super.onDestroy();
    }

    private void startVibrator() {
        if (vibrator == null)
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(300);
    }

    class ViewFinder extends View implements IViewFinder {

        private Rect framingRect;//扫码框所占区域
        private float widthRatio = 1f;//扫码框宽度占view总宽度的比例
        private float heightWidthRatio = 0.6f;//扫码框的高宽比
        private int leftOffset = -1;//扫码框相对于左边的偏移量，若为负值，则扫码框会水平居中
        private int topOffset = 0;//扫码框相对于顶部的偏移量，若为负值，则扫码框会竖直居中

        private int laserColor = 0xff008577;// 扫描线颜色

        private Paint laserPaint;// 扫描线

        private int position;

        public ViewFinder(Context context) {
            super(context);
            willNotDraw();
            laserPaint = new Paint();
            laserPaint.setColor(laserColor);
            laserPaint.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
            updateFramingRect();
        }

        @Override
        public void onDraw(Canvas canvas) {
            if (getFramingRect() == null) {
                return;
            }
            drawLaser(canvas);
        }

        private void drawLaser(Canvas canvas) {
            Rect framingRect = getFramingRect();
            int top = framingRect.top + 10 + position;
            canvas.drawRect(framingRect.left + 10, top, framingRect.right - 10, top + 5, laserPaint);
            position = framingRect.bottom - framingRect.top - 25 < position ? 0 : position + 2;
            //区域刷新
            postInvalidateDelayed(20, framingRect.left + 10, framingRect.top + 10, framingRect.right - 10, framingRect.bottom - 10);
        }

        /**
         * 设置framingRect的值（扫码框所占的区域）
         */
        private synchronized void updateFramingRect() {
            Point viewSize = new Point(getWidth(), getHeight());
            int width, height;
            width = (int) (getWidth() * widthRatio);
            height = (int) (heightWidthRatio * width);

            int left, top;
            if (leftOffset < 0) {
                left = (viewSize.x - width) / 2;//水平居中
            } else {
                left = leftOffset;
            }
            if (topOffset < 0) {
                top = (viewSize.y - height) / 2;//竖直居中
            } else {
                top = topOffset;
            }
            framingRect = new Rect(left, top, left + width, top + height);
        }

        @Override
        public Rect getFramingRect() {
            return framingRect;
        }
    }
}
