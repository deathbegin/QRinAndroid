package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;


public class lsb {
    TextView textView;
    String originimgpath;
    ImageView afterimg;
    EditText editText;
    //隐藏信息的起始位移
    static int startingoffset = 0;

    /**
     * @param textView
     * @param originimgpath
     * @param afterimg
     * @param editText
     * @return
     * @Title: lsb
     * @Description 构造方法
     * @author zw
     * @date 2019/11/24 11:20
     */
    public lsb(TextView textView, String originimgpath, ImageView afterimg, EditText editText) {
        //传入控件
        this.textView = textView;
        this.originimgpath = originimgpath;
        this.afterimg = afterimg;
        this.editText = editText;
    }

    /**
     * @param
     * @author zw
     * @Title:
     * @return
     * @date 2019/11/24 11:01
     */
    public class position {
        public int x;
        public int y;
    }

    /**
     * @param
     * @return java.lang.String
     * @Title: go
     * @Description 加密
     * @author zw
     * @date 2019/11/24 11:21
     */
    public String go() throws IOException {
        //从path中获取bitmap
        Bitmap secretImg = BitmapFactory.decodeFile(originimgpath);

        //读入要加密的文本
        String secretdata = "";
        secretdata = editText.getText().toString();
        System.out.println("需要加密的内容：" + secretdata);
        //隐藏信息
        secretImg = addText(secretImg, secretdata, startingoffset);
        //保存隐藏信息后的图片
        String path;
        path = savePNG(secretImg, "gg");
        //显示保存后的图片
        afterimg.setImageBitmap(secretImg);
        textView.setText("加密内容：\n" + secretdata);
        return path;
    }

    /**
     * @param path
     * @return void
     * @Title: back
     * @Description 解密
     * @author zw
     * @date 2019/11/24 11:21
     */
    public void back(String path) throws IOException {
        Bitmap secretImg = BitmapFactory.decodeFile(path);
        //提取信息
        String res = decode(secretImg, startingoffset);
        textView.setText(textView.getText().toString() + "\n解密内容:\n" + res);
    }

    /**
     * @param bitmapimg
     * @param data
     * @param offset
     * @return android.graphics.Bitmap
     * @Title: addText
     * @Description 具体加密操作
     * @author zw
     * @date 2019/11/24 11:24
     */
    private Bitmap addText(Bitmap bitmapimg, String data, int offset) {
        //讲隐藏内容转化为byte数组
        byte[] addition = data.getBytes();
        byte[] len = IntToByte(addition.length);

        // 获取图片的宽和高
        final int height = bitmapimg.getHeight();
        final int width = bitmapimg.getWidth();
        int i = 0, j = 0;

        int imageValue = bitmapimg.getPixel(i, j);            // 获得像素点(i,j)的R/G/B的值（0-255， 8比特位）
        int NextimageValue = bitmapimg.getPixel(i + 1, j); //注意有8位，前两位好像是透明度，后六位才是rgb的int值
        //存放符合要求的点
        ArrayList positionlist = new ArrayList();

        //使bitmap可以修改
        bitmapimg = bitmapimg.copy(Bitmap.Config.ARGB_8888, true);
        //按列循环
        while ((i + 2) < (width - 1)) {
            //为了防止恢复时因相近色出现问题
            //提前将相近色也转变为可加密色
            // 即<=0x010101的都变成0x000000
            // &0xffffff 排除透明度的干扰
            if (((imageValue & 0xffffff) <= 0x010101) && ((NextimageValue & 0xffffff) == 0)) {
                bitmapimg.setPixel(i, j, Color.argb(0xff, 0x00, 0x00, 0x00));
                imageValue = bitmapimg.getPixel(i, j);
                NextimageValue = bitmapimg.getPixel(i + 1, j);
            }
            //将符合要求的点加入数组备用
            if (((imageValue & 0xffffff) == 0) && ((NextimageValue & 0xffffff) == 0)) {
                position position1 = new position();
                position1.x = i;
                position1.y = j;
                positionlist.add(position1);
            }
            //下一个点
            if (j < (height - 1)) {
                j++;
            } else if ((i + 2) < (width - 1)) {
                // 高度(y坐标)遍历完后，移动x(j)坐标。
                i = i + 2;
                j = 0;
            } else
                break;
            //获取下一个点的Color值
            imageValue = bitmapimg.getPixel(i, j);
            NextimageValue = bitmapimg.getPixel(i + 1, j);
        }
        //测试
        System.out.println(positionlist.size());
        System.out.println("需要隐藏的信息长度:" + addition.length);
        System.out.println("byte:" + addition.length);

//        1.先隐藏文本长度
        bitmapimg = encodeText(bitmapimg, len, offset, positionlist);
//        2.隐藏文本的实际内容
        bitmapimg = encodeText(bitmapimg, addition, offset + 12, positionlist);
        //返回用以保存(好像可以不用)
        return bitmapimg;
    }

    /**
     * @param bitmapimg
     * @param addition
     * @param offset
     * @param positionlist
     * @return android.graphics.Bitmap
     * @Title: encodeText
     * @author zw
     * @date 2019/11/24 11:01
     */
    private Bitmap encodeText(Bitmap bitmapimg, final byte[] addition, final int offset, ArrayList positionlist) {
        // 获取图片的宽和高
        final int height = bitmapimg.getHeight();
        final int width = bitmapimg.getWidth();
        System.out.println("长高" + height + "," + width);
        // 初始化迭代的变量
        int i = 0 / height;
        int j = 0 % height;

        //使bitmap可以修改
        bitmapimg = bitmapimg.copy(Bitmap.Config.ARGB_8888, true);

        //已用的可以隐藏信息的像素点数量。
        int k = (offset == 0) ? offset : offset - 1;  //相当于offset/
        // 判断隐藏内容和图片可以隐藏内容的大小
        if (4 * positionlist.size() >= (addition.length * 8 + offset * 8)) {
            // 遍历隐藏内容的字节数组, additon[]就是要隐藏的内容的字节
            for (final byte add : addition) {
                // 遍历当前byte的每一比特位
                for (int bit = 7; bit >= 0; bit = bit - 4) {
                    //从符合要求的点的数组中取。
                    position position2 = (position) positionlist.get(k);
                    System.out.println("k:" + k);
                    System.out.println(position2.x + " " + position2.y);

                    int color = bitmapimg.getPixel(position2.x, position2.y);
                    int red = Color.red(color);
                    int green = Color.green(color);
                    int blue = Color.blue(color);
                    int alpha = Color.alpha(color);

                    int b = (add >>> bit) & 1;
                    int newred = (red & 0xFE) | b;
                    b = (add >>> (bit - 1)) & 1;
                    int newgreen = (green & 0xFE) | b;
                    b = (add >>> (bit - 2)) & 1;
                    int newblue = (blue & 0xFE) | b;
                    b = (add >>> (bit - 3)) & 1;
                    int newalpha = (alpha & 0xFE) | b;
                    bitmapimg.setPixel(position2.x, position2.y, Color.argb(newalpha, newred, newgreen, newblue));
                    k++;
                }
            }
        }

        return bitmapimg;
    }

    /**
     * @param bitmapimg
     * @param startingoffset
     * @return java.lang.String
     * @Title: decode
     * @author zw
     * @date 2019/11/24 11:09
     */
    private String decode(Bitmap bitmapimg, int startingoffset) throws IOException {
        byte[] decode = {0};
        // 获取图片的宽和高
        final int height = bitmapimg.getHeight();
        final int width = bitmapimg.getWidth();
        int i = 0, j = 0;
        // 获得像素点(i,j)的R/G/B的值（0-255， 8比特位）
        //注意有8位，前两位好像是透明度，后六位才是rgb的int值
        int imageValue = bitmapimg.getPixel(i, j);
        int NextimageValue = bitmapimg.getPixel(i + 1, j);
        ArrayList positionlist = new ArrayList();

        while ((i + 2) < (width - 1)) {
            boolean flag = false;
            if (((imageValue & 0xffffff) <= 0x010101) && ((NextimageValue & 0xffffff) == 0)) {
                //将符合要求的点加入数组备用。
                position position1 = new position();
                position1.x = i;
                position1.y = j;
                positionlist.add(position1);
            }
            if (j < (height - 1)) {
                j++;
            } else if ((i + 2) < (width - 1)) {
                // 高度(y坐标)遍历完后，移动x(j)坐标。
                i = i + 2;
                j = 0;
            } else
                break;
            imageValue = bitmapimg.getPixel(i, j);
            NextimageValue = bitmapimg.getPixel(i + 1, j);
        }
        System.out.println("可能有隐藏信息的个数：" + positionlist.size());

        decode = decodeText(bitmapimg, startingoffset, positionlist);
        return new String(decode);
    }

    /**
     * @param bitmapimg
     * @param startingOffset
     * @param positionlist
     * @return byte[]
     * @Title: decodeText
     * @author zw
     * @date 2019/11/24 11:09
     */
    private static byte[] decodeText(final Bitmap bitmapimg, final int startingOffset, ArrayList positionlist) throws IOException {
        // 初始化变量
        final int height = bitmapimg.getHeight();
        final int width = bitmapimg.getWidth();
        int offset = 12;
        int length = 0;
        // 提取文本内容的长度，32bit，4个Byte
        int len = 8;
        int h = 0 / height;
        int w = 0 % height;
        while ((h + 2) < (width - 1) && (len--) > 0) {
            int imageValue = bitmapimg.getPixel(h, w);
            int NextimageValue = bitmapimg.getPixel(h + 1, w);
            //找到符合要求的加密的点
            while (!(((imageValue & 0xffffff) <= 0x010101) && (NextimageValue & 0xffffff) == 0)) {
                if (w < (height - 1)) {
                    ++w;
                } else if ((h + 2) < (width - 1)) {
                    h += 1;
                    w = 0;
                }
                imageValue = bitmapimg.getPixel(h, w);
                NextimageValue = bitmapimg.getPixel(h + 1, w);
            }
            if (((imageValue & 0xffffff) <= 0x010101) && (NextimageValue & 0xffffff) == 0) {
                // 从bit中还原int(文本的字节数组长度)
                // 1. (imageValue & 1)取imageValue的最低一比特位
                // 2. length << 1 左移
                // 3. (length << 1) | (imageValue & 1) -> 把取出的每个bit通过|操作，累加到length
                System.out.println("bingo：" + h + "," + w);
                int color = bitmapimg.getPixel(h, w);
                int red = Color.red(color);
                int green = Color.green(color);
                int blue = Color.blue(color);
                int alpha = Color.alpha(color);
                //按加密的顺序从中恢复值
                length = (length << 1) | (red & 1);
                length = (length << 1) | (green & 1);
                length = (length << 1) | (blue & 1);
                length = (length << 1) | (alpha & 1);
                System.out.println("length:" + length);
            }
            //下一个
            if (w < (height - 1)) {
                w++;
            } else if ((h + 2) < (width - 1)) {
                // 高度(y坐标)遍历完后，移动x(j)坐标。
                h += 2;
                w = 0;
            } else
                break;
        }
        //测试
        System.out.println("读出信息长度：" + length);

        // 初始化字节数组，存放提取结果
        byte[] result = new byte[length];
        // 初始化迭代变量
        int i = 0 / height;
        int j = 0 % height;
        //只循环offset-1次，因为最后已经是第offset次的状态了
        //其实恢复也有了position数组后，这步可以被简化
        while ((--offset) > 0) {
            int temp = bitmapimg.getPixel(i, j);
            int Nexttemp = bitmapimg.getPixel(i + 1, j);
            while (!(((temp & 0xffffff) <= 0x010101) && (Nexttemp & 0xffffff) == 0)) {
                if (j < (height - 1)) {
                    ++j;
                } else if ((i + 2) < (width - 1)) {
                    i += 2;
                    j = 0;
                }
                temp = bitmapimg.getPixel(i, j);
                Nexttemp = bitmapimg.getPixel(i + 1, j);
            }
            //保持向后搜索
            if (j < (height - 1)) {
                ++j;
            } else if ((i + 2) < (width - 1)) {
                i += 2;
                j = 0;
            }
            System.out.printf("测试%d %d", i, j);
        }

        // 遍历原数据的所有字节
        for (int letter = 0; letter < length; ++letter) {
            // 遍历隐藏数据的每一位，取出放到当前byte中
            for (int bit = 7; bit >= 0; bit = bit - 4) {
                // 获取像素点(i,j)的R/G/B的值(0~255, 8比特位)
                int imageValue = bitmapimg.getPixel(i, j);
                int NextimageValue = bitmapimg.getPixel(i + 1, j);
                //读取下一个点，直到找到符合要求的加密的点
                while (!(((imageValue & 0xffffff) <= 0x010101) && (NextimageValue & 0xffffff) == 0)) {
                    if (j < (height - 1)) {
                        ++j;
                    } else if ((i + 2) < (width - 1)) {
                        i += 2;
                        j = 0;
                    }
                    imageValue = bitmapimg.getPixel(i, j);
                    NextimageValue = bitmapimg.getPixel(i + 1, j);
                }
                //恢复数据
                if (((imageValue & 0xffffff) <= 0x010101) && (NextimageValue & 0xffffff) == 0) {
                    //测试
                    System.out.println("解密：" + i + "," + j);
                    // (imageValue & 1) -> 取出imageValue的最低位
                    // (result[letter] << 1) -> 左移一位
                    // (result[letter] << 1) | (imageValue & 1) -> 取出imagevalue的最低位，放到byte的最低位上
                    // 循环8次，还原成一个字节Byte
                    int color = bitmapimg.getPixel(i, j);
                    int red = Color.red(color);
                    int green = Color.green(color);
                    int blue = Color.blue(color);
                    int alpha = Color.alpha(color);

                    result[letter] = (byte) ((result[letter] << 1) | (red & 1));
                    result[letter] = (byte) ((result[letter] << 1) | (green & 1));
                    result[letter] = (byte) ((result[letter] << 1) | (blue & 1));
                    result[letter] = (byte) ((result[letter] << 1) | (alpha & 1));
                }
                //保持向后搜索
                if (j < (height - 1)) {
                    ++j;
                } else if ((i + 2) < (width - 1)) {
                    i += 2;
                    j = 0;
                }
            }
            System.out.println(letter + ":" + result[letter]);
        }
        return result;
    }

    /**
     * @param num
     * @return byte[]
     * @Title: IntToByte
     * @Description int转byte, 用于隐藏输入信息的长度
     * @author zw
     * @date 2019/11/24 11:31
     */
    private static byte[] IntToByte(int num) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) ((num >> 24) & 0xff);
        bytes[1] = (byte) ((num >> 16) & 0xff);
        bytes[2] = (byte) ((num >> 8) & 0xff);
        bytes[3] = (byte) (num & 0xff);
        return bytes;
    }

    /**
     * @param bitmap
     * @param name
     * @return java.lang.String
     * @Title: savePNG
     * @Description 保存用
     * @author zw
     * @date 2019/11/24 11:31
     */
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
}