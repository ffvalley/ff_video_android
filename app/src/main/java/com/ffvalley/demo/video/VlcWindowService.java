package com.ffvalley.demo.video;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import org.videolan.libvlc.LibVLC;

import java.util.ArrayList;

public class VlcWindowService extends Service {
    public static boolean mIsStarted = false;
    private VlcVideoWindow mVlcVideoWindow;

    @Override
    public void onCreate() {
        super.onCreate();
        mIsStarted = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showLocalVideo(intent.getStringExtra(VlcConstant.VLC_PLAYER_URL_KEY)
                , (VlcVideoType) intent.getSerializableExtra(VlcConstant.VLC_VIDEO_TYPE_KEY));
        return super.onStartCommand(intent, flags, startId);
    }

    private void showLocalVideo(String playerUrl, VlcVideoType videoType) {
        if (!TextUtils.isEmpty(playerUrl)) {
            ArrayList<String> args = new ArrayList<>();
            args.add("--vout=android-display");
            args.add("--rtsp-tcp");

            mVlcVideoWindow = new VlcVideoWindow.Builder(VlcWindowService.this)
                    .setPlayLib(new LibVLC(VlcWindowService.this, args))
                    .build();

            mVlcVideoWindow.initDialog();
            mVlcVideoWindow.loadVideo(playerUrl, videoType);
            mVlcVideoWindow.playVideo();
            mVlcVideoWindow.setOnVlcVideoListener(new OnVlcVideoListener() {
                @Override
                public void dismiss() {
                    mIsStarted = false;
                    VlcWindowService.this.stopSelf();
                }
            });
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (mVlcVideoWindow != null) mVlcVideoWindow.dismiss();
        mIsStarted = false;
        super.onDestroy();
    }

}
