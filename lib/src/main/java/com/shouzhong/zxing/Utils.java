package com.shouzhong.zxing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

class Utils {

    /**
     *  nv21转bitmap
     *
     * @param nv21
     * @param width
     * @param height
     * @return
     */
    static final Bitmap nv21ToBitmap(byte[] nv21, int width, int height){
        Bitmap bitmap = null;
        try {
            YuvImage image = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compressToJpeg(new Rect(0, 0, width, height), 80, stream);
            bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    /**
     * 保存图片
     *
     * @param context
     * @param bitmap
     */
    static final String saveBitmap(final Context context, Bitmap bitmap) {
        try {
            final String local = context.getExternalCacheDir().getAbsolutePath() + "/img_" + System.currentTimeMillis() + ".jpg";;
            final File file = new File(local);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
            }
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            if (null != bitmap) {
                bitmap.recycle();
            }
            return local;
        } catch (Exception e) {
            return null;
        }
    }

}
