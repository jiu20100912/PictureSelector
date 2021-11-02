package com.luck.picture.lib.tools;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Log;
import android.view.Surface;

import androidx.exifinterface.media.ExifInterface;

import com.luck.picture.lib.PictureContentResolver;
import com.luck.picture.lib.config.PictureMimeType;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author：luck
 * @date：2020-01-15 18:22
 * @describe：BitmapUtils
 */
public class BitmapUtils {

    /**
     * 判断拍照 图片是否旋转
     *
     * @param context
     * @param isCameraRotateImage
     * @param path
     */
    public static void rotateImage(Context context, boolean isCameraRotateImage, String path) {
        try {
            if (isCameraRotateImage) {
                int degree = readPictureDegree(context, path);
                if (degree > 0) {
                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inSampleSize = 2;
                    File file = new File(path);
                    Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
                    bitmap = rotatingImage(bitmap, degree);
                    if (bitmap != null) {
                        saveBitmapFile(bitmap, file);
                        bitmap.recycle();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 旋转Bitmap
     *
     * @param bitmap
     * @param angle
     * @return
     */
    public static Bitmap rotatingImage(Bitmap bitmap, int angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    /**
     * 保存Bitmap至本地
     *
     * @param bitmap
     * @param file
     */
    public static void saveBitmapFile(Bitmap bitmap, File file) {
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(file));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos);
            bos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            PictureFileUtils.close(bos);
        }
    }


    /**
     * 读取图片属性：旋转的角度
     *
     * @param context
     * @param filePath 图片绝对路径
     * @return degree旋转的角度
     */
    public static int readPictureDegree(Context context, String filePath) {
        ExifInterface exifInterface;
        InputStream inputStream = null;
        try {
            if (PictureMimeType.isContent(filePath)) {
                inputStream = PictureContentResolver.getContentResolverOpenInputStream(context, Uri.parse(filePath));
                exifInterface = new ExifInterface(inputStream);
            } else {
                exifInterface = new ExifInterface(filePath);
            }
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        } finally {
            PictureFileUtils.close(inputStream);
        }
    }

    /**
     * 读取图片属性：旋转的角度
     *
     * @param inputStream 数据流
     * @return degree旋转的角度
     */
    public static int readPictureDegree(InputStream inputStream) {
        try {
            ExifInterface exifInterface = new ExifInterface(inputStream);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static Bitmap bitmapClip(Context mContext, String imgPath, boolean front, int rotation) {
        Log.d("wld__________rotation", "rotation:" + rotation);
        Bitmap bitmap = BitmapFactory.decodeFile(imgPath);
        Log.d("wld__________bitmap", "width:" + bitmap.getWidth() + "--->height:" + bitmap.getHeight());
        Matrix matrix = pictureDegree(imgPath, front);
        int width = ScreenUtils.getScreenWidth2(mContext);
        int height = ScreenUtils.getScreenHeight2(mContext);
        Log.d("wld__________screen", "width:" + width + "--->height:" + height);
        double screenRatio = height * 1. / width;//屏幕的宽高比
        if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {//vertical camera
            double bitmapRatio = bitmap.getHeight() * 1. / bitmap.getWidth();//基本上都是16/9
            if (bitmapRatio > screenRatio) {//胖的手机
                int clipHeight = (int) (bitmap.getWidth() * screenRatio);
                bitmap = Bitmap.createBitmap(bitmap, 0, (bitmap.getHeight() - clipHeight) >> 1, bitmap.getWidth(), clipHeight, matrix, true);
            } else {//瘦长的手机
                int clipWidth = (int) (bitmap.getHeight() / screenRatio);
                bitmap = Bitmap.createBitmap(bitmap, (bitmap.getWidth() - clipWidth) >> 1, 0, clipWidth, bitmap.getHeight(), matrix, true);
            }
        } else {//horienzatal camera
            double bitmapRatio = bitmap.getWidth() * 1. / bitmap.getHeight();//基本上都是16/9
            if (bitmapRatio > screenRatio) {
                int clipW = (int) (bitmap.getHeight() * screenRatio);
                bitmap = Bitmap.createBitmap(bitmap, (bitmap.getWidth() - clipW) >> 1, 0, clipW, bitmap.getWidth(), matrix, true);
            } else {
                int clipH = (int) (bitmap.getWidth() / screenRatio);
                bitmap = Bitmap.createBitmap(bitmap, 0, (bitmap.getHeight() - clipH) >> 1, bitmap.getWidth(), clipH, matrix, true);
            }
        }
        return bitmap;
    }

    private static Matrix pictureDegree(String imgPath, boolean front) {
        Matrix matrix = new Matrix();
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(imgPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (exif == null)
            return matrix;
        int degree = 0;
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                degree = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                degree = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                degree = 270;
                break;
            default:
                break;
        }
        Log.d("wld__________degree", "degree:" + degree);
        matrix.postRotate(degree);
        if (front) {
            matrix.postScale(-1, 1);
        }
        return matrix;
    }
}
