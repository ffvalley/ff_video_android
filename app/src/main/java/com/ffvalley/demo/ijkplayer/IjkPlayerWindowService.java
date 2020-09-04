package com.ffvalley.demo.ijkplayer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import tv.danmaku.ijk.media.lib.SettingConfigs;
import tv.danmaku.ijk.media.lib.media.AndroidMediaController;

public class IjkPlayerWindowService extends Service {
    public static boolean mIsStarted = false;
    private IjkPlayerVideoWindow mIjkPlayerVideoWindow;

    @Override
    public void onCreate() {
        super.onCreate();
        mIsStarted = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showVideo();
        return super.onStartCommand(intent, flags, startId);
    }

    // 开启ijkplayer视频播放
    private void showVideo() {
        SettingConfigs settings = new SettingConfigs(this);
        AndroidMediaController mediaController = new AndroidMediaController(this, false);

        mIjkPlayerVideoWindow = new IjkPlayerVideoWindow.Builder(IjkPlayerWindowService.this)
                .setPlayLib(settings, mediaController)
                .build();
        mIjkPlayerVideoWindow.initDialog();
        mIjkPlayerVideoWindow.loadVideo("http://qthttp.apple.com.edgesuite.net/1010qwoeiuryfg/sl.m3u8");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
//        if (mIjkPlayerVideoWindow != null) mIjkPlayerVideoWindow.dismiss();
        mIsStarted = false;
        super.onDestroy();
    }

}
