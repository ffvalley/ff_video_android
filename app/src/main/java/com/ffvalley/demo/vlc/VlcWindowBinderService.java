package com.ffvalley.demo.vlc;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;

import org.videolan.libvlc.LibVLC;

import java.util.ArrayList;

public class VlcWindowBinderService extends Service {

    public static boolean mIsStarted = false;
    private VlcVideoWindow mVlcVideoWindow;

    @Override
    public void onCreate() {
        super.onCreate();
        mIsStarted = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new VlcWindowBinder();
    }

    @Override
    public void onDestroy() {
        if (mVlcVideoWindow != null) mVlcVideoWindow.dismiss();
        mIsStarted = false;
        super.onDestroy();
    }

    public class VlcWindowBinder extends Binder {
        public VlcWindowBinderService getService() {
            return VlcWindowBinderService.this;
        }

        public void showVideo(String playerUrl, VlcVideoType videoType) {
            createVideo(playerUrl, videoType);
        }
    }

    private void createVideo(String playerUrl, VlcVideoType videoType) {
        if (!TextUtils.isEmpty(playerUrl)) {
            ArrayList<String> args = new ArrayList<>();
            args.add("--vout=android-display");
            args.add("--rtsp-tcp");

            mVlcVideoWindow = new VlcVideoWindow.Builder(VlcWindowBinderService.this)
                    .setPlayLib(new LibVLC(VlcWindowBinderService.this, args))
                    .build();

            mVlcVideoWindow.initDialog();
            mVlcVideoWindow.loadVideo(playerUrl, videoType);
            mVlcVideoWindow.playVideo();
            mVlcVideoWindow.setOnVlcVideoListener(new OnVlcVideoListener() {
                @Override
                public void dismiss() {
                    mIsStarted = false;
                    VlcWindowBinderService.this.onDestroy();
                }
            });
        }
    }
}
