package com.ffvalley.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;

import com.ffvalley.demo.ijkplayer.IjkPlayerVideoWindow;
import com.ffvalley.demo.vlc.OnVlcVideoListener;
import com.ffvalley.demo.vlc.VlcVideoWindow;
import com.ffvalley.demo.constant.CommonConstant;
import com.ffvalley.demo.vlc.VlcVideoType;

import org.videolan.libvlc.LibVLC;

import java.util.ArrayList;

import pub.devrel.easypermissions.EasyPermissions;
import tv.danmaku.ijk.media.lib.SettingConfigs;
import tv.danmaku.ijk.media.lib.media.AndroidMediaController;

public class MainActivity extends AppCompatActivity {

    //    String mPlayerUrl = "demo.mpg"; // ASSET资源文件
//    VlcVideoType mVideoType = VlcVideoType.ASSET_VIDEO;
//    String mPlayerUrl = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Titanic.mkv"; // 本地视频文件
//    VlcVideoType mVideoType = VlcVideoType.LOCAL_VIDEO;
    String mPlayerUrl = "http://videoconverter.vivo.com.cn/201706/655_1498479540118.mp4.main.m3u8"; // rtsp流媒体文件
    VlcVideoType mVideoType = VlcVideoType.RTSP_VIDEO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.open_video_01).setOnClickListener(mOnClickListener);
        findViewById(R.id.open_video_02).setOnClickListener(mOnClickListener);

        EasyPermissions.requestPermissions(this, "need use storage", 200
                , Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (R.id.open_video_01 == v.getId()) {
                startVLCPlayer();
            } else if (R.id.open_video_02 == v.getId()) {
                startIjkPlayer();
            }
        }
    };

    // start模式开启VLC悬浮框频频播放器
    private void startVLCPlayer() {
        ArrayList<String> args = new ArrayList<>();
        args.add("--vout=android-display");
        args.add("--rtsp-tcp");

        VlcVideoWindow mVlcVideoWindow = new VlcVideoWindow.Builder(MainActivity.this)
                .setPlayLib(new LibVLC(MainActivity.this, args))
                .build();

        mVlcVideoWindow.initWindow();
        mVlcVideoWindow.loadVideo(mPlayerUrl, mVideoType);
        mVlcVideoWindow.playVideo();
        mVlcVideoWindow.setOnVlcVideoListener(new OnVlcVideoListener() {
            @Override
            public void dismiss() {
            }
        });
    }

    // start模式开启IjkPlayer悬浮框频频播放器
    private void startIjkPlayer() {
        SettingConfigs settings = new SettingConfigs(this);
        AndroidMediaController mediaController = new AndroidMediaController(this, false);

        IjkPlayerVideoWindow mIjkPlayerVideoWindow = new IjkPlayerVideoWindow.Builder(MainActivity.this)
                .setPlayLib(settings, mediaController)
                .build();
        mIjkPlayerVideoWindow.initWindow();
        mIjkPlayerVideoWindow.loadVideo("http://qthttp.apple.com.edgesuite.net/1010qwoeiuryfg/sl.m3u8");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
