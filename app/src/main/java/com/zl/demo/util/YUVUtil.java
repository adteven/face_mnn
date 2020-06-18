package com.zl.demo.util;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.Image;
import android.os.Environment;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Created by panes on 2018/3/30.
 */

public class YUVUtil {
    public static final int COLOR_FormatI420 = 1;
    public static final int COLOR_FormatNV21 = 2;

    private static boolean isImageFormatSupported(Image image) {
        int format = image.getFormat();
        switch (format) {
            case ImageFormat.YUV_420_888:
            case ImageFormat.NV21:
            case ImageFormat.YV12:
                return true;
        }
        return false;
    }

    public static void flipImageVertical(byte[] src, byte[] dst, int width, int height) {
        // Y
        for (int y = 0; y < height; y++) {
            System.arraycopy(src, (height - 1 - y) * width, dst, y * width, width);
        }

        // UV
        int wh = width * height;
        int halfH = height / 2;
        for (int y = 0; y < halfH; y++) {
            System.arraycopy(src, wh + y * width, dst, wh + (halfH - 1 - y) * width, width);
        }
    }

    public static byte[] getDataFromImage(Image image, int colorFormat) {
        if (colorFormat != COLOR_FormatI420 && colorFormat != COLOR_FormatNV21) {
            throw new IllegalArgumentException("only support COLOR_FormatI420 " + "and COLOR_FormatNV21");
        }
        if (!isImageFormatSupported(image)) {
            throw new RuntimeException("can't convert Image to byte array, format " + image.getFormat());
        }
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];
        int channelOffset = 0;
        int outputStride = 1;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = width * height;
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height + 1;
                        outputStride = 2;
                    }
                    break;
                case 2:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = (int) (width * height * 1.25);
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height;
                        outputStride = 2;
                    }
                    break;
            }
            ByteBuffer buffer = planes[i].getBuffer();

            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();
            int shift = (i == 0) ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
            buffer.clear();
        }
        return data;
    }


    public static void writeBytesToFile(byte[] arg, String fileName) throws IOException {
        OutputStream out = new FileOutputStream(Environment.getExternalStorageDirectory() + File.separator + fileName + ".txt");
        InputStream is = new ByteArrayInputStream(arg);
        byte[] buff = new byte[1024];
        int len = 0;
        while ((len = is.read(buff)) != -1) {
            out.write(buff, 0, len);
        }
        is.close();
        out.close();
    }

    public static byte[] YUV420spRotate90Anticlockwise(byte[] src, int width, int height) {
        byte[] dst = new byte[src.length];
        int uvHeight = height >> 1;

        // 处理Y分量
        int k = 0;
        for (int col = width - 1; col >= 0; col--) {
            for (int row = 0; row < height; row++) {
                dst[k++] = src[row * width + col];
            }
        }

        // 处理UV分量
        for (int col = width - 1; col >= 0; col -= 2) {
            for (int row = height; row < height + uvHeight; row++) {
                dst[k++] = src[row * width + col - 1]; // U
                dst[k++] = src[row * width + col]; // V
            }
        }
        return dst;
    }

    public static byte[] YUV420spRotate90Clockwise(byte[] src, int width, int height) {
        byte[] dst = new byte[src.length];
        int uvHeight = height >> 1;

        // 处理Y分量
        int k = 0;
        for (int col = 0; col < width; col++) {
            for (int row = height - 1; row >= 0; row--) {
                dst[k++] = src[row * width + col];
            }
        }

        // 处理UV分量
        for (int col = 0; col < width; col += 2) {
            for (int row = height + uvHeight - 1; row >= height; row--) {
                dst[k++] = src[row * width + col]; // U
                dst[k++] = src[row * width + col + 1]; // V
            }
        }
        return dst;
    }

    public static byte[] YUV420spMirror(byte[] src, int width, int height) {
        byte[] dst = new byte[src.length];
        int uvHeight = height >> 1;
        // 处理Y分量
        int k = 0;
        for (int row = 0; row < height; row++) {
            for (int col = width - 1; col >= 0; col--) {
                dst[k++] = src[row * width + col];
            }
        }
        // 处理UV分量
        for (int row = height; row < uvHeight + height; row++) {
            for (int col = width - 2; col >= 0; col -= 2) {
                dst[k++] = src[row * width + col];
                dst[k++] = src[row * width + col + 1];
            }
        }
        return dst;
    }


    /*
     * 获取位图的RGB数据
     */
    public static byte[] getRGBByBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int size = width * height;
        int pixels[] = new int[size];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        byte[] data = convertColorToByte(pixels);
        return data;
    }

    /*
     * 像素数组转化为RGB数组
     */
    public static byte[] convertColorToByte(int color[]) {
        if (color == null) {
            return null;
        }
        byte[] data = new byte[color.length * 3];
        for (int i = 0; i < color.length; i++) {
            data[i * 3] = (byte) (color[i] >> 16 & 0xff);
            data[i * 3 + 1] = (byte) (color[i] >> 8 & 0xff);
            data[i * 3 + 2] = (byte) (color[i] & 0xff);
        }
        return data;
    }

}
