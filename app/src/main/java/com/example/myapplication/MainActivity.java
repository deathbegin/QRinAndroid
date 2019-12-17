package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.IOException;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, EasyPermissions.PermissionCallbacks {
    private ImageView ivTest;

    private File cameraSavePath;//拍照照片路径
    private Uri uri;
    private String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private TextView textview;
    private EditText editText;
    private ImageView show;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        Button btnGetPicFromCamera = findViewById(R.id.btn_get_pic_from_camera);
        Button btnGetPicFromPhotoAlbum = findViewById(R.id.btn_get_pic_form_photo_album);
        Button btnGetPermission = findViewById(R.id.btn_get_Permission);
        Button btnEncode = findViewById(R.id.btn_encode);
        Button btnDecode = findViewById(R.id.btn_decode);
        ivTest = findViewById(R.id.iv_test);
        textview = findViewById(R.id.textView);
        editText = findViewById(R.id.secrettext);
        show = findViewById(R.id.after);

        textview.setMovementMethod(ScrollingMovementMethod.getInstance());

//        btnGetPicFromCamera.setOnClickListener(this);
        btnGetPicFromPhotoAlbum.setOnClickListener(this);
        btnGetPermission.setOnClickListener(this);
        btnEncode.setOnClickListener(this);
        btnDecode.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            /*case R.id.btn_get_pic_from_camera:
                goCamera();
                break;*/
            case R.id.btn_get_pic_form_photo_album:
                goPhotoAlbum();
                break;
            case R.id.btn_get_Permission:
                getPermission();
                break;
            case R.id.btn_encode:
                encode();
                break;
            case R.id.btn_decode:
                try {
                    decode();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    //获取权限
    private void getPermission() {
        if (EasyPermissions.hasPermissions(this, permissions)) {
            //已经打开权限
            Toast.makeText(this, "已经申请相关权限", Toast.LENGTH_SHORT).show();
        } else {
            //没有打开相关权限、申请权限
            EasyPermissions.requestPermissions(this, "需要获取您的相册、照相使用权限", 1, permissions);
        }

    }

    //激活相册操作
    private void goPhotoAlbum() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 2);
    }

/*    //激活相机操作
    private void goCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraSavePath = new File(Environment.getExternalStorageDirectory().getPath() + "/" + System.currentTimeMillis() + ".jpg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(MainActivity.this, "com.example.myapplication.fileprovider", cameraSavePath);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            uri = Uri.fromFile(cameraSavePath);
        }
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        MainActivity.this.startActivityForResult(intent, 1);
    }*/

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //框架要求必须这么写
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    //成功打开权限
    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

        Toast.makeText(this, "相关权限获取成功", Toast.LENGTH_SHORT).show();
    }

    //用户未同意权限
    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        Toast.makeText(this, "请同意相关权限，否则功能无法使用", Toast.LENGTH_SHORT).show();
    }

    //全局照片路径
    //使得可以更新imageview里的图片
    String photoPath;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1 && resultCode == RESULT_OK) {
           /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                photoPath = String.valueOf(cameraSavePath);
            } else {
                photoPath = uri.getEncodedPath();
            }
            Log.d("拍照返回图片路径:", photoPath);
            Glide.with(MainActivity.this).load(photoPath).into(ivTest);*/
        } else if (requestCode == 2 && resultCode == RESULT_OK) {
            photoPath = getPhotoFromPhotoAlbum.getRealPathFromUri(this, data.getData());
            Glide.with(MainActivity.this).load(photoPath).into(ivTest);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    //简化获取图片的路径
    //后续可以考虑增加从目录读取功能
    String backpath = "";

    //加密按钮
    private void encode() {
        lsb lsb1 = new lsb(textview, photoPath, show, editText);
        try {
            backpath = lsb1.go();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //解密按钮
    private void decode() throws IOException {
        lsb lsb2 = new lsb(textview, photoPath, show, editText);
        if ("".equals(backpath)) {
            lsb2.back(photoPath);
        } else {
            lsb2.back(backpath);
        }
    }
}
