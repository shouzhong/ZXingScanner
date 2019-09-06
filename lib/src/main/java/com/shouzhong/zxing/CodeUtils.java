package com.shouzhong.zxing;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2018/07/31.
 *
 *
 */

public class CodeUtils {

    /**
     * 识别图片，建议在子线程运行
     *
     * @param bmp
     * @return
     */
    public static String decode(Bitmap bmp) throws Exception {
        if (bmp == null) throw new Exception("图片不存在");
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        Map<DecodeHintType, Object> hints = new HashMap<>();
        List<BarcodeFormat> decodeFormats = new ArrayList<>();
        decodeFormats.add(BarcodeFormat.QR_CODE);
        decodeFormats.add(BarcodeFormat.CODABAR);
        decodeFormats.add(BarcodeFormat.CODE_39);
        decodeFormats.add(BarcodeFormat.CODE_93);
        decodeFormats.add(BarcodeFormat.CODE_128);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
        hints.put(DecodeHintType.TRY_HARDER, BarcodeFormat.QR_CODE);
        hints.put(DecodeHintType.CHARACTER_SET, "utf-8");
        MultiFormatReader reader = new MultiFormatReader();//初始化解析对象
        try {
            int[] pixels = new int[width * height];
            bmp.getPixels(pixels, 0, width, 0, 0, width, height);
            //新建一个RGBLuminanceSource对象
            RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            //将图片转换成二进制图片
            BinaryBitmap binaryBitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));
            Result result = reader.decode(binaryBitmap, hints);//开始解析
            return result.getText();
        } catch (Exception e) {}
        Matrix m = new Matrix();
        m.setRotate(90, (float) bmp.getWidth() / 2, (float) bmp.getHeight() / 2);
        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
        width = bmp.getWidth();
        height = bmp.getHeight();
        int[] pixels = new int[width * height];
        bmp.getPixels(pixels, 0, width, 0, 0, width, height);
        //新建一个RGBLuminanceSource对象
        RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
        //将图片转换成二进制图片
        BinaryBitmap binaryBitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));
        Result result = reader.decode(binaryBitmap, hints);//开始解析
        return result.getText();
    }

    /**
     * 条码生成，建议在子线程运行
     *
     * @param contents
     * @param desiredWidth
     * @param desiredHeight
     * @return
     */
    public static Bitmap createBarcode(String contents, int desiredWidth, int desiredHeight) throws Exception {
        final int WHITE = 0xFFFFFFFF;
        final int BLACK = 0xFF000000;
        Hashtable<EncodeHintType, Object> hst = new Hashtable<>();
        hst.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        //容错级别
        hst.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        //设置空白边距的宽度
        hst.put(EncodeHintType.MARGIN, 0);
        BitMatrix result = new MultiFormatWriter().encode(contents, BarcodeFormat.CODE_128, desiredWidth, desiredHeight, hst);
        int width = result.getWidth();
        int height = result.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    /**
     * 二维码生成，建议在子线程运行
     *
     * @param string
     * @param size
     * @return
     */
    public static Bitmap createQRCode(String string, int size, Bitmap logo) throws Exception {
        final int WHITE = 0xFFFFFFFF;
        final int BLACK = 0xFF000000;
        MultiFormatWriter writer = new MultiFormatWriter();
        Hashtable<EncodeHintType, Object> hst = new Hashtable<>();
        hst.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        //容错级别
        hst.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        //设置空白边距的宽度
        hst.put(EncodeHintType.MARGIN, 0);
        BitMatrix matrix = writer.encode(string, BarcodeFormat.QR_CODE, size, size, hst);//生成二维码矩阵信息
        int width = matrix.getWidth();//矩阵高度
        int height = matrix.getHeight();//矩阵宽度
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = matrix.get(x, y) ? BLACK : WHITE;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return addLogo(bitmap, logo, 0.25f);
    }

    /**
     * 往图形中间添加logo，建议在子线程运行
     *
     * @param src
     * @param logo
     * @param scale 缩放比例，0~1
     * @return
     */
    public static Bitmap addLogo(Bitmap src, Bitmap logo, float scale) throws Exception {
        if (src == null) {
            return null;
        }
        if (logo == null) {
            return src;
        }
        //获取图片的宽高
        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();
        int logoWidth = logo.getWidth();
        int logoHeight = logo.getHeight();
        if (srcWidth == 0 || srcHeight == 0) {
            return null;
        }
        if (logoWidth == 0 || logoHeight == 0 || scale == 0.0f) {
            return src;
        }
        scale = scale < 0 || scale > 1 ? 0.25f : scale;
        //logo大小为二维码整体大小的1/5
        float scaleFactor = srcWidth * scale / logoWidth;
        Bitmap bitmap = Bitmap.createBitmap(srcWidth, srcHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(src, 0, 0, null);
        canvas.scale(scaleFactor, scaleFactor, srcWidth / 2, srcHeight / 2);
        canvas.drawBitmap(logo, (srcWidth - logoWidth) / 2, (srcHeight - logoHeight) / 2, null);
        canvas.save();
        canvas.restore();
        return bitmap;
    }
}
