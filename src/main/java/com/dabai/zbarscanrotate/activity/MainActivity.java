package com.dabai.zbarscanrotate.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.dabai.zbarscanrotate.R;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final int CODE_SCAN = 0X01;
    /**
     * 6.0以上检查相机权限，没有则申请
     *
     * @return 有则返回真否则为假
     */
    private static final int TAKE_PHOTO_REQUEST_CODE = 1;
    private Button btn_start_scan;
    private TextView tv_content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化view
        initView();

    }

    private void initView() {
        btn_start_scan = (Button) findViewById(R.id.btn_start_scan);
        btn_start_scan.setOnClickListener(this);
        tv_content = (TextView) findViewById(R.id.tv_content);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null && requestCode == 0) {
            String BarcodeFormat = data.getStringExtra("BarcodeFromat");
            String DisplayContents = data.getStringExtra("DisplayContents");
            Toast.makeText(MainActivity.this, "BarcodeFormat:" + BarcodeFormat + "-----扫描结果:" + DisplayContents, Toast.LENGTH_SHORT).show();
            tv_content.setText("扫描结果:" + DisplayContents);
        } else {
            Toast.makeText(MainActivity.this, "未获取到扫描结果", Toast.LENGTH_SHORT).show();
            tv_content.setText("未获取到扫描结果");
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start_scan:
                if (checkPermission(MainActivity.this)) {
                    //进入扫描界面
                    Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
                    startActivityForResult(intent, CODE_SCAN);
                }
                break;

            default:
                break;
        }
    }

    private boolean checkPermission(Context context) {
//        int sysVersion = Integer.parseInt(Build.VERSION.SDK);
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.CAMERA},
                    TAKE_PHOTO_REQUEST_CODE);
            return false;
        } else {
            return true;
        }

    }
}
