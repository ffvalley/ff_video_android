package com.ffvalley.demo.ijkplayer;

import android.content.Context;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ffvalley.demo.R;
import com.ffvalley.demo.constant.CommonConstant;
import com.ffvalley.demo.exception.VideoException;

import tv.danmaku.ijk.media.lib.SettingConfigs;
import tv.danmaku.ijk.media.lib.media.AndroidMediaController;
import tv.danmaku.ijk.media.lib.media.IjkVideoView;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;


public class IjkPlayerVideoWindow {

    private static final String TAG = "IjkPlayerVideoWindow.class";
    private static final String LIB_IJKPLAYER_SO = "libijkplayer.so";

    private Builder mBuilder;

    private LayoutInflater mLayoutInflater;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayoutParams;
    private View mWindowLayoutView; // 最外层view

    private IjkVideoView mIjkVideoIvv;
    private TableLayout mIjkVideoTl;
    private TextView mIjkVideoStatusTv;


    private IjkPlayerVideoWindow(Context context, Builder builder) {
        mBuilder = builder;
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mLayoutParams = new WindowManager.LayoutParams();
    }

    public static final class Builder {
        private Context bContext;
        private SettingConfigs bSettings;
        private AndroidMediaController bMediaController;

        public Builder(Context context) {
            this.bContext = context;
        }

        // 设置播放库
        public Builder setPlayLib(SettingConfigs settings, AndroidMediaController mediaController) {
            if (settings == null)
                throw new VideoException("setPlayLib - settings参数，" + CommonConstant.EXCEPTION_MESSAGE_NOT_NULL);
            if (mediaController == null)
                throw new VideoException("setPlayLib - mediaController参数，" + CommonConstant.EXCEPTION_MESSAGE_NOT_NULL);

            bSettings = settings;
            bMediaController = mediaController;

            IjkMediaPlayer.loadLibrariesOnce(null);
            IjkMediaPlayer.native_profileBegin(LIB_IJKPLAYER_SO);
            return this;
        }

        public IjkPlayerVideoWindow build() {
            if (bContext == null) {
                throw new VideoException("Builder - context参数，" + CommonConstant.EXCEPTION_MESSAGE_NOT_NULL);
            } else if (bMediaController == null) {
                throw new VideoException("setPlayLib - bMediaController参数，" + CommonConstant.EXCEPTION_MESSAGE_NOT_NULL);
            }
            return new IjkPlayerVideoWindow(bContext, this);
        }
    }

    // 初始化视频播放Window
    public void initWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        mLayoutParams.format = PixelFormat.RGBA_8888;
        mLayoutParams.gravity = Gravity.CENTER;
        mLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mLayoutParams.width = RelativeLayout.LayoutParams.MATCH_PARENT;
        mLayoutParams.height = RelativeLayout.LayoutParams.MATCH_PARENT;

        mWindowLayoutView = mLayoutInflater.inflate(R.layout.ijkplayer_window_video, null);
        mWindowManager.addView(mWindowLayoutView, mLayoutParams);


        mIjkVideoStatusTv = mWindowLayoutView.findViewById(R.id.ijk_video_status_tv);

        mIjkVideoIvv = mWindowLayoutView.findViewById(R.id.ijk_video_ivv);
        mIjkVideoTl = mWindowLayoutView.findViewById(R.id.ijk_video_tl);
        mIjkVideoIvv.setMediaController(mBuilder.bMediaController);
        mIjkVideoIvv.setHudView(mIjkVideoTl);
    }

    // 加载视频源
    public void loadVideo(String videoUrl) {
        if (mWindowLayoutView == null)
            throw new VideoException(CommonConstant.EXCEPTION_MESSAGE_03);

        if (TextUtils.isEmpty(videoUrl)) {
            showTipView(CommonConstant.EXCEPTION_MESSAGE_02);
        } else {
            mIjkVideoIvv.setVideoURI(Uri.parse(videoUrl));
            mIjkVideoIvv.start();
        }
    }

    // 设置窗口播放状态提示显示样式
    private void showTipView(String message) {
        mIjkVideoStatusTv.setVisibility(View.VISIBLE);
        mIjkVideoStatusTv.setText(message);
    }

}
