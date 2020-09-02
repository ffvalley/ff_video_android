package com.ffvalley.demo;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.ffvalley.demo.video.VlcWindowService;
import com.ffvalley.demo.video.VlcConstant;
import com.ffvalley.demo.video.VlcVideoType;
import com.ffvalley.demo.video.VlcWindowBinderService;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {

    String mPlayerUrl = "demo.mpg"; // ASSET资源文件
    VlcVideoType mVideoType = VlcVideoType.ASSET_VIDEO;
//    String mPlayerUrl = Environment.getExternalStorageDirectory().getAbsolutePath() + "/xxxx.MP4"; // 本地视频文件
//    VlcVideoType mVideoType = VlcVideoType.LOCAL_VIDEO;
//    String mPlayerUrl = "http://videoconverter.vivo.com.cn/201706/655_1498479540118.mp4.main.m3u8"; // rtsp流媒体文件
//    VlcVideoType mVideoType = VlcVideoType.RTSP_VIDEO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.open_video_01).setOnClickListener(mOnClickListener);
        findViewById(R.id.open_video_02).setOnClickListener(mOnClickListener);
        findViewById(R.id.open_video_03).setOnClickListener(mOnClickListener);

        EasyPermissions.requestPermissions(this, "need use storage", 200
                , Manifest.permission.WRITE_EXTERNAL_STORAGE
                , Manifest.permission.SYSTEM_ALERT_WINDOW);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "授权失败", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "授权成功", Toast.LENGTH_SHORT).show();
                startVideoService();
            }
        } else if (requestCode == 1) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "授权失败", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "授权成功", Toast.LENGTH_SHORT).show();
                onBinderService();
            }

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    OnClickListener mOnClickListener = new OnClickListener() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onClick(View v) {
            if (R.id.open_video_01 == v.getId()) {
                if (!VlcWindowBinderService.mIsStarted) {
                    if (!Settings.canDrawOverlays(MainActivity.this)) {
                        Toast.makeText(MainActivity.this, "当前无权限，请授权", Toast.LENGTH_SHORT).show();
                        startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), 0);
                    } else {
                        startVideoService();
                    }
                }
            } else if (R.id.open_video_02 == v.getId()) {
                if (!VlcWindowBinderService.mIsStarted) {
                    if (!Settings.canDrawOverlays(MainActivity.this)) {
                        Toast.makeText(MainActivity.this, "当前无权限，请授权", Toast.LENGTH_SHORT).show();
                        startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), 1);
                    } else {
                        onBinderService();
                    }
                }
            } else if (R.id.open_video_03 == v.getId()) {
                if (VlcWindowBinderService.mIsStarted) {
                    unBinderService();
                }
            }
        }
    };

    private void startVideoService() {
        Intent intent = new Intent(MainActivity.this, VlcWindowService.class);
        intent.putExtra(VlcConstant.VLC_PLAYER_URL_KEY, mPlayerUrl);
        intent.putExtra(VlcConstant.VLC_VIDEO_TYPE_KEY, mVideoType);
        startService(intent);
    }

    private VlcWindowBinderService mService;
    private VlcWindowBinderService.VlcWindowBinder mBinder;
    ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mService = ((VlcWindowBinderService.VlcWindowBinder) iBinder).getService();
            mBinder = (VlcWindowBinderService.VlcWindowBinder) iBinder;

            mBinder.showVideo(mPlayerUrl, mVideoType);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    private void onBinderService() {
        Intent bindIntent = new Intent(this, VlcWindowBinderService.class);
        bindService(bindIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private void unBinderService() {
        unbindService(mServiceConnection);
    }

    @Override
    protected void onDestroy() {
        if (VlcWindowService.mIsStarted)
            MainActivity.this.stopService(new Intent(MainActivity.this, VlcWindowService.class));
        if (VlcWindowBinderService.mIsStarted)
            MainActivity.this.stopService(new Intent(MainActivity.this, VlcWindowBinderService.class));
        super.onDestroy();
    }
}
