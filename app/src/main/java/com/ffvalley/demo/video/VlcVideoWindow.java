package com.ffvalley.demo.video;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.ffvalley.demo.R;
import com.ffvalley.demo.utils.DateUtil;
import com.ffvalley.demo.utils.DisplayUtil;
import com.ffvalley.demo.utils.NumberUtil;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;


public class VlcVideoWindow {

    private static final String TAG = "VlcVideoWindow.class";
    private static final int PROGRESS_MAX = 10000;
    private static final int CONSOLE_DISPLAY_DURATION = 5 * 1000;
    private static final int VOLUME_DEFAULT_SIZE = 50;

    private Builder mBuilder;
    private OnVlcVideoListener mOnVlcVideoListener;

    private Media mMedia;
    private long mTotalTime = 0; // 视频播放总时间

    private LayoutInflater mLayoutInflater;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayoutParams;
    private View mWindowLayoutView; // 最外层view

    private Button mVlcCloseBtn;
    private View mVlcConsole; // 控制台控件

    private SurfaceView mSurfaceView;
    private Button mVideoZoomBtn;
    private Button mVideoSwitchBtn;
    private Button mVideoForwardBtn;
    private Button mVideoBackwardBtn;
    private Button mVideoVolumeBtn;
    private SeekBar mVideoProgressSb;
    private SeekBar mVideoVolumeSb;
    private TextView mVideoCurrentTimeTv;
    private TextView mVideoTotalTimeTv;
    private TextView mVlcVideoStatusTv;
    private LinearLayout mVideoControlLl;

    private VlcVideoWindow(Context context, Builder builder) {
        mBuilder = builder;
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mLayoutParams = new WindowManager.LayoutParams();
    }

    public static final class Builder {
        private Context bContext;
        private LibVLC bLibVLC;
        private MediaPlayer bMediaPlayer;
        private IVLCVout bVlcVout;

        public Builder(Context context) {
            this.bContext = context;
        }

        // 设置播放库
        public Builder setPlayLib(LibVLC libVLC) {
            if (libVLC == null)
                throw new VlcVideoException("setPlayLib - libVLC参数，" + VlcConstant.VLC_EXCEPTION_NOT_NULL);

            bLibVLC = libVLC;
            bMediaPlayer = new MediaPlayer(libVLC);
            bVlcVout = bMediaPlayer.getVLCVout();
            return this;
        }

        public VlcVideoWindow build() {
            if (bContext == null) {
                throw new VlcVideoException("Builder - context参数，" + VlcConstant.VLC_EXCEPTION_NOT_NULL);
            } else if (bLibVLC == null) {
                throw new VlcVideoException("setPlayLib - libVLC参数，" + VlcConstant.VLC_EXCEPTION_NOT_NULL);
            }
            return new VlcVideoWindow(bContext, this);
        }
    }

    // -------------------------- 对外暴露的公共方法 strat -----------------------------------
    // 初始化视频播放Dialog
    public void initDialog() {
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

        mWindowLayoutView = mLayoutInflater.inflate(R.layout.vlc_window_video, null);
        mWindowLayoutView.setOnTouchListener(mOnTouchListener);
        mWindowManager.addView(mWindowLayoutView, mLayoutParams);

        // 关联播放器
        mSurfaceView = mWindowLayoutView.findViewById(R.id.vlc_video_sv);
        mBuilder.bVlcVout.setVideoView(mSurfaceView);
        mVlcVideoStatusTv = mWindowLayoutView.findViewById(R.id.vlc_video_status_tv);

        // 显示窗口关闭操作
        mVlcCloseBtn = mWindowLayoutView.findViewById(R.id.vlc_close_btn);
        mVlcCloseBtn.setOnClickListener(mOnClickListener);

        // 控制台view
        mVlcConsole = mWindowLayoutView.findViewById(R.id.vlc_console);
        mVideoControlLl = mWindowLayoutView.findViewById(R.id.video_control_ll);

        // 显示窗口缩放操作
        mVideoZoomBtn = mWindowLayoutView.findViewById(R.id.video_zoom_btn);
        mVideoZoomBtn.setOnClickListener(mOnClickListener);

        // 开启/停止播放视频操作
        mVideoSwitchBtn = mWindowLayoutView.findViewById(R.id.video_switch_btn);
        mVideoSwitchBtn.setOnClickListener(mOnClickListener);

        // 快进10s操作
        mVideoForwardBtn = mWindowLayoutView.findViewById(R.id.video_forward_btn);
        mVideoForwardBtn.setOnClickListener(mOnClickListener);
        // 快退10s操作
        mVideoBackwardBtn = mWindowLayoutView.findViewById(R.id.video_backward_btn);
        mVideoBackwardBtn.setOnClickListener(mOnClickListener);

        mVideoTotalTimeTv = mWindowLayoutView.findViewById(R.id.video_total_time_tv);
        mVideoCurrentTimeTv = mWindowLayoutView.findViewById(R.id.video_current_time_tv);

        // 视频进度控制
        mVideoProgressSb = mWindowLayoutView.findViewById(R.id.video_progress_sb);
        mVideoProgressSb.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
        // 音量控制
        mVideoVolumeSb = mWindowLayoutView.findViewById(R.id.video_volume_sb);
        mVideoVolumeSb.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
        mVideoVolumeBtn = mWindowLayoutView.findViewById(R.id.video_volume_btn);
        mVideoVolumeBtn.setOnClickListener(mOnClickListener);

        // 启动计时器
        mCountDownTimer.start();
    }

    // 加载视频源 1-本地视频、2-流媒体视频、3-资源视频
    public void loadVideo(String videoUrl, VlcVideoType type) {
        if (mWindowLayoutView == null) throw new VlcVideoException(VlcConstant.VLC_EXCEPTION_03);

        mBuilder.bMediaPlayer.setEventListener(mEventListener);
        mBuilder.bVlcVout.attachViews(mOnNewVideoLayoutListener);
        try {
            if (!TextUtils.isEmpty(videoUrl)) {
                if (VlcVideoType.LOCAL_VIDEO == type) {
                    mMedia = new Media(mBuilder.bLibVLC, videoUrl);
                } else if (VlcVideoType.RTSP_VIDEO == type) {
                    mMedia = new Media(mBuilder.bLibVLC, Uri.parse(videoUrl));
                } else if (VlcVideoType.ASSET_VIDEO == type) {
                    mMedia = new Media(mBuilder.bLibVLC, mBuilder.bContext.getAssets().openFd(videoUrl));
                } else {
                    showTipView(VlcConstant.VLC_EXCEPTION_01);
                }
            } else {
                showTipView(VlcConstant.VLC_EXCEPTION_02);
            }
            mBuilder.bMediaPlayer.setMedia(mMedia);
            mMedia.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 设置回调方法
    public void setOnVlcVideoListener(OnVlcVideoListener onVlcVideoListener) {
        mOnVlcVideoListener = onVlcVideoListener;
    }

    // 播放视频操作
    @SuppressLint("UseCompatLoadingForDrawables")
    public void playVideo() {
        if (mWindowLayoutView == null) {
            Log.w(TAG, VlcConstant.VLC_EXCEPTION_03);
            return;
        }
        if (mMedia == null) {
            Log.w(TAG, VlcConstant.VLC_EXCEPTION_04);
            return;
        }

        mVideoSwitchBtn.setBackground(mBuilder.bContext.getResources().getDrawable(R.drawable.start));
        mVideoSwitchBtn.setTag(R.id.video_switch_btn, true);
        if (mBuilder.bMediaPlayer.getMedia() == null || mBuilder.bMediaPlayer.getMedia().getState() != 4) {
            mBuilder.bMediaPlayer.setMedia(mMedia);
        }
        mBuilder.bMediaPlayer.play();
    }

    // 暂停视频操作
    @SuppressLint("UseCompatLoadingForDrawables")
    public void pauseVideo() {
        if (mWindowLayoutView == null) {
            Log.w(TAG, VlcConstant.VLC_EXCEPTION_03);
            return;
        }
        if (mMedia == null) {
            Log.w(TAG, VlcConstant.VLC_EXCEPTION_04);
            return;
        }

        mVideoSwitchBtn.setBackground(mBuilder.bContext.getResources().getDrawable(R.drawable.stop));
        mVideoSwitchBtn.setTag(R.id.video_switch_btn, false);
        mBuilder.bMediaPlayer.pause();

    }

    // 缩小屏幕操作
    @SuppressLint("UseCompatLoadingForDrawables")
    public void narrowScreen() {
        if (mWindowLayoutView == null) {
            Log.w(TAG, VlcConstant.VLC_EXCEPTION_03);
            return;
        }

        mLayoutParams.width = DisplayUtil.dip2px(mBuilder.bContext, 500);
        mLayoutParams.height = DisplayUtil.dip2px(mBuilder.bContext, 320);
        mLayoutParams.gravity = Gravity.TOP | Gravity.START;
        mWindowManager.updateViewLayout(mWindowLayoutView, mLayoutParams);

        mVideoZoomBtn.setBackground(mBuilder.bContext.getResources().getDrawable(R.drawable.fullscreen));
        mVideoZoomBtn.setTag(R.id.video_zoom_btn, true);
    }

    // 放大屏幕操作
    @SuppressLint("UseCompatLoadingForDrawables")
    public void enlargeScreen() {
        if (mWindowLayoutView == null) {
            Log.w(TAG, VlcConstant.VLC_EXCEPTION_03);
            return;
        }


        mLayoutParams.width = RelativeLayout.LayoutParams.MATCH_PARENT;
        mLayoutParams.height = RelativeLayout.LayoutParams.MATCH_PARENT;
        mLayoutParams.gravity = Gravity.CENTER;
        mWindowManager.updateViewLayout(mWindowLayoutView, mLayoutParams);

        mVideoZoomBtn.setBackground(mBuilder.bContext.getResources().getDrawable(R.drawable.minimize));
        mVideoZoomBtn.setTag(R.id.video_zoom_btn, false);
    }

    // 设置视频播放音量
    @SuppressLint("UseCompatLoadingForDrawables")
    public void setVolume(int progress) {
        if (mWindowLayoutView == null) {
            Log.w(TAG, VlcConstant.VLC_EXCEPTION_03);
            return;
        }
        if (mMedia == null) {
            Log.w(TAG, VlcConstant.VLC_EXCEPTION_04);
            return;
        }

        mVideoVolumeSb.setProgress(progress);
        mBuilder.bMediaPlayer.setVolume(progress);

        if (progress == 0) {
            mVideoVolumeBtn.setBackground(mBuilder.bContext.getResources().getDrawable(R.drawable.mute));
        } else {
            mVideoVolumeBtn.setBackground(mBuilder.bContext.getResources().getDrawable(R.drawable.vol_up));
        }
    }

    // 关闭视频播放窗口
    public void dismiss() {
        mBuilder.bMediaPlayer.release();
        mBuilder.bMediaPlayer.getVLCVout().detachViews();
        mCountDownTimer.cancel();
        mWindowManager.removeView(mWindowLayoutView);
    }
    // -------------------------- 对外暴露的公共方法 finish -----------------------------------

    // -------------------------- 本地私有方法 strat -----------------------------------
    // 点击事件监听
    private View.OnClickListener mOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {

            switch (v.getId()) {
                case R.id.vlc_close_btn:
                    mOnVlcVideoListener.dismiss();
                    dismiss();
                    break;
                case R.id.video_zoom_btn:
                    // tag(当前是否为最小屏幕状态) 如果是null/false - 当前最大屏幕需要缩小； 如果是true - 当前最小需要放大
                    if (v.getTag(R.id.video_zoom_btn) == null || !(boolean) v.getTag(R.id.video_zoom_btn)) {
                        narrowScreen();
                    } else {
                        enlargeScreen();
                    }
                    mCountDownTimer.start();
                    break;
                case R.id.video_switch_btn:
                    // tag(当前是否为播放状态) 如果是null/false - 当前未播放视频需要播放； 如果是true - 当前正在播放视频
                    if (v.getTag(R.id.video_switch_btn) == null || !(boolean) v.getTag(R.id.video_switch_btn)) {
                        playVideo();
                    } else {
                        pauseVideo();
                    }
                    mCountDownTimer.start();
                    break;
                case R.id.video_forward_btn:
                    // 快进
                    setMediaPosition(10 * 1000);
                    mCountDownTimer.start();
                    break;
                case R.id.video_backward_btn:
                    // 快退
                    setMediaPosition(-10 * 1000);
                    mCountDownTimer.start();
                    break;
                case R.id.video_volume_btn:
                    // 音量控制开关
                    if (mVideoVolumeSb != null) {
                        if (mVideoVolumeSb.getProgress() == 0) {
                            setVolume(VOLUME_DEFAULT_SIZE);
                        } else {
                            setVolume(0);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    };

    // 触摸事件监听
    View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        private int downX;
        private int downY;

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    downX = (int) event.getRawX();
                    downY = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    int nowX = (int) event.getRawX();
                    int nowY = (int) event.getRawY();
                    int movedX = nowX - downX;
                    int movedY = nowY - downY;
                    downX = nowX;
                    downY = nowY;
                    mLayoutParams.x = mLayoutParams.x + movedX;
                    mLayoutParams.y = mLayoutParams.y + movedY;
                    mWindowManager.updateViewLayout(view, mLayoutParams);
                    break;
                case MotionEvent.ACTION_UP:
                    if (View.VISIBLE != mVlcConsole.getVisibility()) {
                        mVlcCloseBtn.setVisibility(View.VISIBLE);
                        mVlcConsole.setVisibility(View.VISIBLE);
                    }
                    // 启动计时器
                    mCountDownTimer.start();
                    break;
                default:
                    break;
            }
            return true;
        }
    };

    // 进度条事件监听
    private SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (seekBar == mVideoVolumeSb) {
                setVolume(progress);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mCountDownTimer.cancel();
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (seekBar == mVideoProgressSb) {
                setProgress(seekBar.getProgress());
            }
            mCountDownTimer.start();
        }
    };

    // 视频初始化事件监听
    IVLCVout.OnNewVideoLayoutListener mOnNewVideoLayoutListener = new IVLCVout.OnNewVideoLayoutListener() {

        @Override
        public void onNewVideoLayout(IVLCVout vlcVout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
            initMediaInfo();
        }
    };

    // 视频实时事件监听
    MediaPlayer.EventListener mEventListener = new MediaPlayer.EventListener() {

        @SuppressLint("UseCompatLoadingForDrawables")
        @Override
        public void onEvent(MediaPlayer.Event event) {
            switch (mBuilder.bMediaPlayer.getPlayerState()) {
                case 0: // NothingSpecial
                    break;
                case 1: // Opening
                case 2: // Buffering
                    showTipView(VlcConstant.VLC_EXCEPTION_07);
                    break;
                case 3: // Playing
                    updateMediaInfo();
                    break;
                case 4: // Paused
                    break;
                case 5: // Stopped
                    break;
                case 6: // Ended
                    mVideoSwitchBtn.setBackground(mBuilder.bContext.getResources().getDrawable(R.drawable.stop));
                    mVideoSwitchBtn.setTag(R.id.video_switch_btn, false);
                    showTipView(VlcConstant.VLC_EXCEPTION_06);
                    break;
                case 7: // Error
                    showTipView(VlcConstant.VLC_EXCEPTION_05);
                    break;
            }
        }
    };

    // 总时间 CONSOLE_DISPLAY_DURATION，间隔 1000s 回调一次 onTick
    CountDownTimer mCountDownTimer = new CountDownTimer(CONSOLE_DISPLAY_DURATION, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {
        }

        @Override
        public void onFinish() {
            // 倒计时结束时的回调
//            mDialogWindow.getDecorView().setSystemUiVisibility(View.INVISIBLE);
            mVlcCloseBtn.setVisibility(View.GONE);
            mVlcConsole.setVisibility(View.GONE);
        }
    };

    // 初始化显示视频相关信息
    private void initMediaInfo() {
        if (mBuilder.bMediaPlayer.isSeekable()) {
            mVideoProgressSb.setVisibility(View.VISIBLE);
            mVideoControlLl.setVisibility(View.VISIBLE);

            mTotalTime = mBuilder.bMediaPlayer.getLength();
            mVideoTotalTimeTv.setText(DateUtil.getHHMMSS(mTotalTime));
            mVideoProgressSb.setProgress(0);
            mVideoProgressSb.setMax(PROGRESS_MAX);
        } else {
            mVideoProgressSb.setVisibility(View.GONE);
            mVideoControlLl.setVisibility(View.GONE);
        }
        setVolume(VOLUME_DEFAULT_SIZE);
    }

    // 更新显示视频相关信息
    private void updateMediaInfo() {
        if (mVlcVideoStatusTv.getVisibility() == View.VISIBLE) {
            mVlcVideoStatusTv.setVisibility(View.GONE);
        }

        if (mBuilder.bMediaPlayer.isSeekable()) {
            long currentTime = mBuilder.bMediaPlayer.getTime();

            mVideoCurrentTimeTv.setText(DateUtil.getHHMMSS(currentTime));
            String ratio = NumberUtil.ratio(currentTime, mTotalTime, PROGRESS_MAX);
            int progress = 0;
            if (NumberUtil.isNumeric(ratio)) {
                progress = Integer.parseInt(ratio);
            }
            mVideoProgressSb.setProgress(progress);
        }
    }

    // 设置窗口播放状态提示显示样式
    private void showTipView(String message) {
        mVlcVideoStatusTv.setVisibility(View.VISIBLE);
        mVlcVideoStatusTv.setText(message);
    }

    // 设置视频播放进度
    private void setProgress(int progress) {
        mBuilder.bMediaPlayer.setTime(mTotalTime / PROGRESS_MAX * progress);
    }

    // 设置播放位置(快进/快退) 单位ms
    private void setMediaPosition(int increment) {
        long currentTime = mBuilder.bMediaPlayer.getTime();
        mBuilder.bMediaPlayer.setTime(currentTime + increment);
    }
    // -------------------------- 本地私有方法 finish -----------------------------------
}
