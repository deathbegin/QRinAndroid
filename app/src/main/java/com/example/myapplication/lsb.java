package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.biometrics.BiometricManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;


public class lsb {
    TextView textView;
    String originimgpath;
    ImageView afterimg;
    EditText editText;
    //隐藏信息的起始位移
    static int startingoffset = 0;

    public lsb(TextView textView, String originimgpath, ImageView afterimg, EditText editText) {
        this.textView = textView;
        this.originimgpath = originimgpath;
        this.afterimg = afterimg;
        this.editText = editText;
    }

    public String go() throws IOException {
        //从path中获取bitmap
        Bitmap secretImg = BitmapFactory.decodeFile(originimgpath);

        //读入要加密的文本
        String secretdata = "";
        secretdata = editText.getText().toString();

        //隐藏信息
        secretImg = addText(secretImg, secretdata, startingoffset);
        //保存图片
        String path;
        path = savePNG(secretImg, "gg");
        //显示保存后的图片
        afterimg.setImageBitmap(secretImg);
        textView.setText("加密内容：\n" + secretdata);
        return path;
    }

    public void back(String path) throws IOException {
        Bitmap secretImg = BitmapFactory.decodeFile(path);
        //提取信息
        String res = decode(secretImg, startingoffset);
        textView.setText(textView.getText().toString() + "\n解密内容：" + res);
    }

    private Bitmap addText(Bitmap bitmapimg, String data, int offset) {
        //讲隐藏内容转化为byte数组
        byte[] addition = data.getBytes();
//        System.out.println(Arrays.toString(addition));
        byte[] len = IntToByte(addition.length);

//        1.先隐藏文本长度
        bitmapimg = encodeText(bitmapimg, len, offset);
//        2.隐藏文本的实际内容
        bitmapimg = encodeText(bitmapimg, addition, offset + 32);

        return bitmapimg;
    }

    private Bitmap encodeText(Bitmap bitmapimg, final byte[] addition, final int offset) {
        // 获取图片的宽和高
        final int height = bitmapimg.getHeight();
        final int width = bitmapimg.getWidth();
        System.out.println("长高" + height + "," + width);
        // 初始化迭代的变量
        int i = offset / height;
        int j = offset % height;

        //使bitmap可以修改
        bitmapimg = bitmapimg.copy(Bitmap.Config.ARGB_8888, true);

        //备注： 图片是由左下角开始遍历的
        // 判断隐藏内容和图片可以隐藏内容的大小
        if ((width * height) >= (addition.length * 8 + offset)) {
            // 遍历隐藏内容的字节数组, additon[]就是要隐藏的内容的字节
            for (final byte add : addition) {
                // 遍历当前byte的每一比特位
                for (int bit = 7; bit >= 0; --bit) {
                    // 获得像素点(i,j)的R/G/B的值（0-255， 8比特位）
                    int imageValue = bitmapimg.getPixel(i, j);
                    int NextimageValue = bitmapimg.getPixel(i + 1, j);
                    System.out.println(imageValue + " " + NextimageValue);
                    while (imageValue != 0x000000 || NextimageValue != 0x0000000) {
                        if (j < (height - 1)) {
                            ++j;
                        } else if ((i + 2) < (width - 1)) {
                            // 高度(y坐标)遍历完后，移动x(j)坐标。
                            i = i + 2;
                            j = 0;
                        }
                        imageValue = bitmapimg.getPixel(i, j);
                        NextimageValue = bitmapimg.getPixel(i + 1, j);
                    }
                    int b = (add >>> bit) & 1; // 与1&取最低位，并保证最低位为0或1
                    // imageValue & 0xFFFFFFFE -> 确保imageValue的最后一位为0
                    int imageNewValue = ((imageValue & 0xFFFFFFFE) | b);
                    // 把替换后的imageValue重新设置到原来的点
                    bitmapimg.setPixel(i, j, Color.rgb((imageNewValue & 0xff0000) >> 16, (imageNewValue & 0x00ff00) >> 8, (imageNewValue & 0x0000ff)));
                    /*// 确保高度(j)不越界
                    if (j < (height - 1)) {
                        ++j;
                    } else if ((i + 2) < (width - 1)) {
                        // 高度(y坐标)遍历完后，移动x(j)坐标。
                        i = i + 2;
                        j = 0;
                    }*/
                }
            }
        }
        return bitmapimg;
    }

    private String savePNG(Bitmap bitmap, String name) {
        String path = "/storage/emulated/0/DCIM/Camera/" + name + ".png";
        //保存至绝对路径
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(file);
            if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                out.flush();
                out.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Context context = MyApplication.getContext();

        //保存图片后发送广播通知更新数据库
        // Uri uri = Uri.fromFile(file);
        // sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri uri = Uri.fromFile(file);
        intent.setData(uri);
        context.sendBroadcast(intent);
        return path;
    }

    private static String decode(Bitmap bitmapimg, int startingoffset) throws IOException {
        byte[] decode;
        decode = decodeText(bitmapimg, startingoffset);
        return new String(decode);
    }

    private static byte[] decodeText(final Bitmap bitmapimg, final int startingOffset) throws IOException {
        // 初始化变量
        final int height = bitmapimg.getHeight();
        final int width = bitmapimg.getWidth();
        final int offset = 32;
        int length = 0;
        // 提取文本内容的长度，32bit，4个Byte
        for (int i = startingOffset; i < offset; ++i) {
            final int h = i / height;
            final int w = i % height;
            final int imageValue = bitmapimg.getPixel(h, w);
            final int NextimageValue = bitmapimg.getPixel(h + 1, w);
            if (NextimageValue == 0x0) {
                // 从bit中还原int(文本的字节数组长度)
                // 1. (imageValue & 1)取imageValue的最低一比特位
                // 2. length << 1 左移
                // 3. (length << 1) | (imageValue & 1) -> 把取出的每个bit通过|操作，累加到length
                length = (length << 1) | (imageValue & 1);
            }
        }
        System.out.println("读出信息长度：\n" + length);
        // 初始化字节数组，存放提取结果
        byte[] result = new byte[length];
        // 初始化迭代变量
        int i = offset / height;
        int j = offset % height;
        // 遍历数据的所有字节
        for (int letter = 0; letter < length; ++letter) {
            // 遍历隐藏数据的每一位，取出放到当前byte中
            for (int bit = 7; bit >= 0; --bit) {
                // 获取像素点(i,j)的R/G/B的值(0~255, 8比特位)
                final int imageValue = bitmapimg.getPixel(i, j);
                final int NextimageValue = bitmapimg.getPixel(i + 1, j);
                if (NextimageValue == 0x0) {
                    // (imageValue & 1) -> 取出imageValue的最低位
                    // (result[letter] << 1) -> 左移一位
                    // (result[letter] << 1) | (imageValue & 1) -> 取出imagevalue的最低位，放到byte的最低位上
                    // 循环8次，还原成一个字节Byte
                    result[letter] = (byte) ((result[letter] << 1) | (imageValue & 1));
                }

                if (j < (height - 1)) {
                    ++j;
                } else if ((i + 2) < (width - 1)) {
                    i += 2;
                    j = 0;
                }
            }
        }
        return result;
    }

    //int转byte数组
    private static byte[] IntToByte(int num) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) ((num >> 24) & 0xff);
        bytes[1] = (byte) ((num >> 16) & 0xff);
        bytes[2] = (byte) ((num >> 8) & 0xff);
        bytes[3] = (byte) (num & 0xff);
        return bytes;
    }

}
