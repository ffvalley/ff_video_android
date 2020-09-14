package com.ffvalley.demo.vlc;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.net.Uri;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.ffvalley.demo.R;
import com.ffvalley.demo.constant.CommonConstant;
import com.ffvalley.demo.exception.VideoException;
import com.ffvalley.demo.utils.DateUtil;
import com.ffvalley.demo.utils.DisplayUtil;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;


public class VlcVideoWindow {

    private static final String TAG = "VlcVideoWindow.class";
    //    private static final int PROGRESS_MAX = 10000;
    private static final int CONSOLE_DISPLAY_DURATION = 5 * 1000;
    private static final int CONSOLE_10_S = 10 * 1000;

    private Builder mBuilder;
    private OnVlcVideoListener mOnVlcVideoListener;

    private Media mMedia;
    private long mTotalTime = 0; // 视频播放总时间

    private boolean mIsPlaying = false; // 是否正在播放视频
    private boolean mIsMinimize = false; // 是否最小化视频窗口
    private boolean mIsReceiverVolume = false; // 是否是系统广播导致音量发生变化
    private int mTempVolume = 0; // 缓存音量

    private LayoutInflater mLayoutInflater;
    private AudioManager mAudioManager;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayoutParams;
    private View mWindowLayoutView; // 最外层view
    private GestureDetector mGestureDetector;
    private int mVideoWidth = 0;
    private int mVideoHeight = 0;

    private Button mVlcCloseBtn;
    private View mVlcConsole; // 控制台控件

    private boolean mNeedPaint = true;
    private SurfaceView mSurfaceView;
    private RelativeLayout mVideoZoomRl;
    private View mVideoZoomView;
    private RelativeLayout mVideoSwitchRl;
    private View mVideoSwitchView;
    private RelativeLayout mVideoVolumeRl;
    private View mVideoVolumeView;

    private RelativeLayout mVideoForwardRl;
    private RelativeLayout mVideoBackwardRl;
    private SeekBar mVideoProgressSb;
    private SeekBar mVideoVolumeSb;
    private TextView mVideoCurrentTimeTv;
    private TextView mVideoTotalTimeTv;
    private TextView mVlcVideoStatusTv;
    private LinearLayout mVideoControlLl;

    private VlcVideoWindow(Context context, Builder builder) {
        mBuilder = builder;
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);// 获取系统的Window管理者
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE); // 获取系统的Audio管理者
        registerSystemVolumeReceiver();
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
                throw new VideoException("setPlayLib - libVLC参数，" + CommonConstant.EXCEPTION_MESSAGE_NOT_NULL);

            bLibVLC = libVLC;
            bMediaPlayer = new MediaPlayer(libVLC);
            bMediaPlayer.setVideoScale(MediaPlayer.ScaleType.SURFACE_BEST_FIT);
            bVlcVout = bMediaPlayer.getVLCVout();
            return this;
        }

        public VlcVideoWindow build() {
            if (bContext == null) {
                throw new VideoException("Builder - context参数，" + CommonConstant.EXCEPTION_MESSAGE_NOT_NULL);
            } else if (bLibVLC == null) {
                throw new VideoException("setPlayLib - libVLC参数，" + CommonConstant.EXCEPTION_MESSAGE_NOT_NULL);
            }
            return new VlcVideoWindow(bContext, this);
        }
    }

    // -------------------------- 对外暴露的公共方法 strat -----------------------------------
    // 初始化视频播放Dialog
    public void initWindow() {
//        mLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        mLayoutParams.format = PixelFormat.RGBA_8888;
        mLayoutParams.gravity = Gravity.CENTER;
        mLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mLayoutParams.width = RelativeLayout.LayoutParams.MATCH_PARENT;
        mLayoutParams.height = RelativeLayout.LayoutParams.MATCH_PARENT;

        mWindowLayoutView = mLayoutInflater.inflate(R.layout.vlc_window_video, null);
        mWindowLayoutView.setOnTouchListener(mOnTouchListener);
        mWindowLayoutView.setBackgroundColor(Color.BLACK);
        mWindowManager.addView(mWindowLayoutView, mLayoutParams);

        // 关联播放器
        mSurfaceView = mWindowLayoutView.findViewById(R.id.vlc_video_sv);
        mSurfaceView.getHolder().addCallback(mSurfaceHolderCallback);
        mBuilder.bVlcVout.setVideoView(mSurfaceView);
        mVlcVideoStatusTv = mWindowLayoutView.findViewById(R.id.vlc_video_status_tv);

        mGestureDetector = new GestureDetector(mBuilder.bContext, new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (mIsMinimize) {
                    enlargeScreen();
                } else {
                    narrowScreen();
                }
                return super.onDoubleTap(e);
            }
        });

        // 显示窗口关闭操作
        mVlcCloseBtn = mWindowLayoutView.findViewById(R.id.vlc_close_btn);
        mVlcCloseBtn.setOnClickListener(mOnClickListener);

        // 控制台view
        mVlcConsole = mWindowLayoutView.findViewById(R.id.vlc_console);
        mVideoControlLl = mWindowLayoutView.findViewById(R.id.video_control_ll);

        // 显示窗口缩放操作
        mVideoZoomRl = mWindowLayoutView.findViewById(R.id.video_zoom_rl);
        mVideoZoomView = mWindowLayoutView.findViewById(R.id.video_zoom_btn);
        mVideoZoomRl.setOnClickListener(mOnClickListener);

        // 开启/停止播放视频操作
        mVideoSwitchRl = mWindowLayoutView.findViewById(R.id.video_switch_rl);
        mVideoSwitchView = mWindowLayoutView.findViewById(R.id.video_switch_btn);
        mVideoSwitchRl.setOnClickListener(mOnClickListener);

        // 快进10s操作
        mVideoForwardRl = mWindowLayoutView.findViewById(R.id.video_forward_rl);
        mVideoForwardRl.setOnClickListener(mOnClickListener);
        // 快退10s操作
        mVideoBackwardRl = mWindowLayoutView.findViewById(R.id.video_backward_rl);
        mVideoBackwardRl.setOnClickListener(mOnClickListener);

        mVideoTotalTimeTv = mWindowLayoutView.findViewById(R.id.video_total_time_tv);
        mVideoCurrentTimeTv = mWindowLayoutView.findViewById(R.id.video_current_time_tv);

        // 视频进度控制
        mVideoProgressSb = mWindowLayoutView.findViewById(R.id.video_progress_sb);
        mVideoProgressSb.setOnSeekBarChangeListener(mOnSeekBarChangeListener);

        // 音量控制
        mVideoVolumeSb = mWindowLayoutView.findViewById(R.id.video_volume_sb);
        mVideoVolumeSb.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
        mVideoVolumeRl = mWindowLayoutView.findViewById(R.id.video_volume_rl);
        mVideoVolumeView = mWindowLayoutView.findViewById(R.id.video_volume_btn);
        mVideoVolumeRl.setOnClickListener(mOnClickListener);

        // 启动计时器
        mCountDownTimer.start();
    }

    // 加载视频源 1-本地视频、2-流媒体视频、3-资源视频
    public void loadVideo(String videoUrl, VlcVideoType type) {
        if (mWindowLayoutView == null)
            throw new VideoException(CommonConstant.EXCEPTION_MESSAGE_03);

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
                    showTipView(CommonConstant.EXCEPTION_MESSAGE_01);
                }
            } else {
                showTipView(CommonConstant.EXCEPTION_MESSAGE_02);
            }
            mBuilder.bMediaPlayer.setMedia(mMedia);
            mMedia.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 变更视频播放源
    public void changeVideoSource(String videoUrl, VlcVideoType type) {
        if (mWindowLayoutView == null)
            throw new VideoException(CommonConstant.EXCEPTION_MESSAGE_03);

        try {
            if (!TextUtils.isEmpty(videoUrl)) {
                if (VlcVideoType.LOCAL_VIDEO == type) {
                    mMedia = new Media(mBuilder.bLibVLC, videoUrl);
                } else if (VlcVideoType.RTSP_VIDEO == type) {
                    mMedia = new Media(mBuilder.bLibVLC, Uri.parse(videoUrl));
                } else if (VlcVideoType.ASSET_VIDEO == type) {
                    mMedia = new Media(mBuilder.bLibVLC, mBuilder.bContext.getAssets().openFd(videoUrl));
                } else {
                    showTipView(CommonConstant.EXCEPTION_MESSAGE_01);
                }
            } else {
                showTipView(CommonConstant.EXCEPTION_MESSAGE_02);
            }
            mVideoProgressSb.setProgress(0);
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
            Log.w(TAG, CommonConstant.EXCEPTION_MESSAGE_03);
            return;
        }
        if (mMedia == null) {
            Log.w(TAG, CommonConstant.EXCEPTION_MESSAGE_04);
            return;
        }

        mVideoSwitchView.setBackground(mBuilder.bContext.getResources().getDrawable(R.drawable.start));
        mIsPlaying = true;

        if (mBuilder.bMediaPlayer.getMedia() == null || mBuilder.bMediaPlayer.getMedia().getState() != 4) {
            mBuilder.bMediaPlayer.setMedia(mMedia);
        }
        mBuilder.bMediaPlayer.play();
    }

    // 暂停视频操作
    @SuppressLint("UseCompatLoadingForDrawables")
    public void pauseVideo() {
        if (mWindowLayoutView == null) {
            Log.w(TAG, CommonConstant.EXCEPTION_MESSAGE_03);
            return;
        }
        if (mMedia == null) {
            Log.w(TAG, CommonConstant.EXCEPTION_MESSAGE_04);
            return;
        }

        mVideoSwitchView.setBackground(mBuilder.bContext.getResources().getDrawable(R.drawable.stop));
        mIsPlaying = false;
        mBuilder.bMediaPlayer.pause();

    }

    // 获取视频是否播放的状态
    public boolean getPlayingStatus() {
        return mIsPlaying;
    }

    // 缩小屏幕操作
    @SuppressLint("UseCompatLoadingForDrawables")
    public void narrowScreen() {
        if (mWindowLayoutView == null) {
            Log.w(TAG, CommonConstant.EXCEPTION_MESSAGE_03);
            return;
        }

        mLayoutParams.x = DisplayUtil.dip2px(mBuilder.bContext, 500) / 2;
        mLayoutParams.y = DisplayUtil.dip2px(mBuilder.bContext, 320) / 2;
        mLayoutParams.width = DisplayUtil.dip2px(mBuilder.bContext, 500);
        mLayoutParams.height = DisplayUtil.dip2px(mBuilder.bContext, 320);
        mWindowManager.updateViewLayout(mWindowLayoutView, mLayoutParams);

        mVideoZoomView.setBackground(mBuilder.bContext.getResources().getDrawable(R.drawable.fullscreen));
        mIsMinimize = true;
        updateSurfaceFrame();
    }

    // 放大屏幕操作
    @SuppressLint("UseCompatLoadingForDrawables")
    public void enlargeScreen() {
        if (mWindowLayoutView == null) {
            Log.w(TAG, CommonConstant.EXCEPTION_MESSAGE_03);
            return;
        }

        mLayoutParams.width = RelativeLayout.LayoutParams.MATCH_PARENT;
        mLayoutParams.height = RelativeLayout.LayoutParams.MATCH_PARENT;
        mLayoutParams.gravity = Gravity.CENTER;
        mWindowManager.updateViewLayout(mWindowLayoutView, mLayoutParams);

        mVideoZoomView.setBackground(mBuilder.bContext.getResources().getDrawable(R.drawable.minimize));
        mIsMinimize = false;
        updateSurfaceFrame();
    }

    // 获取视频是否小屏幕播放状态
    public boolean getZoomStatus() {
        return mIsMinimize;
    }

    // 设置视频播放音量
    @SuppressLint("UseCompatLoadingForDrawables")
    public void setVolume(int progress, boolean isSetSystemVolume) {
        if (mWindowLayoutView == null) {
            Log.w(TAG, CommonConstant.EXCEPTION_MESSAGE_03);
            return;
        }
        if (mMedia == null) {
            Log.w(TAG, CommonConstant.EXCEPTION_MESSAGE_04);
            return;
        }

        mVideoVolumeSb.setProgress(progress);
        if (isSetSystemVolume) {
            setSystemVolume(progress);
        }

        ViewGroup.LayoutParams layoutParams = mVideoVolumeView.getLayoutParams();
        if (progress == 0) {
            mVideoVolumeView.setBackground(mBuilder.bContext.getResources().getDrawable(R.drawable.mute));
            layoutParams.width = DisplayUtil.dip2px(mBuilder.bContext, 14);
            layoutParams.height = DisplayUtil.dip2px(mBuilder.bContext, 20);
        } else {
            mVideoVolumeView.setBackground(mBuilder.bContext.getResources().getDrawable(R.drawable.vol_up));
            layoutParams.width = DisplayUtil.dip2px(mBuilder.bContext, 21);
            layoutParams.height = DisplayUtil.dip2px(mBuilder.bContext, 18);
        }
        mVideoVolumeView.setLayoutParams(layoutParams);
        mIsReceiverVolume = false;
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
                    if (mOnVlcVideoListener != null)
                        mOnVlcVideoListener.dismiss();
                    dismiss();
                    break;
                case R.id.video_zoom_rl:
                    if (mIsMinimize) {
                        enlargeScreen();
                    } else {
                        narrowScreen();
                    }
                    mCountDownTimer.start();
                    break;
                case R.id.video_switch_rl:
                    if (mIsPlaying) {
                        pauseVideo();
                    } else {
                        playVideo();
                    }
                    mCountDownTimer.start();
                    break;
                case R.id.video_forward_rl:
                    // 快进
                    setMediaPosition(CONSOLE_10_S);
                    mCountDownTimer.start();
                    break;
                case R.id.video_backward_rl:
                    // 快退
                    setMediaPosition(-CONSOLE_10_S);
                    mCountDownTimer.start();
                    break;
                case R.id.video_volume_rl:
                    // 音量控制开关
                    if (mVideoVolumeSb != null) {
                        if (mVideoVolumeSb.getProgress() == 0) {
                            setVolume(mTempVolume, true);
                        } else {
                            mTempVolume = getCurrentSystemVolume();
                            setVolume(0, true);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    };

    // 触摸事件监听
    private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
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
            mGestureDetector.onTouchEvent(event);
            return true;
        }
    };

    private SurfaceHolder mSurfaceHolder;

    // Surface事件监听
    private SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            mSurfaceHolder = holder;
            if (mNeedPaint) {
                mNeedPaint = false;
                Canvas canvas = holder.lockCanvas();
                canvas.drawColor(Color.BLACK);
                holder.unlockCanvasAndPost(canvas);
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
        }
    };

    // 进度条事件监听
    private SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (seekBar == mVideoVolumeSb) {
                if (mIsReceiverVolume) {
                    setVolume(progress, false);
                } else {
                    setVolume(progress, true);
                }
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
    private IVLCVout.OnNewVideoLayoutListener mOnNewVideoLayoutListener = new IVLCVout.OnNewVideoLayoutListener() {

        @Override
        public void onNewVideoLayout(IVLCVout vlcVout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
            if (mVideoWidth != visibleWidth || mVideoHeight != visibleHeight) {
                mVideoWidth = visibleWidth;
                mVideoHeight = visibleHeight;
                updateSurfaceFrame();
            }

            initMediaInfo();
        }
    };

    // 视频实时事件监听
    private MediaPlayer.EventListener mEventListener = new MediaPlayer.EventListener() {

        @SuppressLint("UseCompatLoadingForDrawables")
        @Override
        public void onEvent(MediaPlayer.Event event) {

            switch (event.type) {
                case MediaPlayer.Event.MediaChanged:
                    break;
                case MediaPlayer.Event.EndReached:
                    mVideoSwitchView.setBackground(mBuilder.bContext.getResources().getDrawable(R.drawable.stop));
                    mIsPlaying = false;
                    showTipView(CommonConstant.EXCEPTION_MESSAGE_06);
                    break;
                case MediaPlayer.Event.Stopped:
                    break;
                case MediaPlayer.Event.EncounteredError:
                    mIsPlaying = false;
                    mVideoSwitchView.setBackground(mBuilder.bContext.getResources().getDrawable(R.drawable.stop));
                    showTipView(CommonConstant.EXCEPTION_MESSAGE_05);
                    break;
                case MediaPlayer.Event.Opening:
                    showTipView(CommonConstant.EXCEPTION_MESSAGE_07);
                    break;
                case MediaPlayer.Event.Buffering:
                    break;
                case MediaPlayer.Event.Playing:
                    break;
                case MediaPlayer.Event.Paused:
                    break;
                case MediaPlayer.Event.TimeChanged:
                    updateMediaInfo();
                    break;
                case MediaPlayer.Event.LengthChanged:
                    break;
                case MediaPlayer.Event.PositionChanged:
                    break;
                case MediaPlayer.Event.Vout:
                    break;
                case MediaPlayer.Event.ESAdded:
                    break;
                case MediaPlayer.Event.ESDeleted:
                    break;
                case MediaPlayer.Event.ESSelected:
                    break;
                case MediaPlayer.Event.SeekableChanged:
                    break;
                case MediaPlayer.Event.PausableChanged:
                    break;
                case MediaPlayer.Event.RecordChanged:
                    break;
            }
        }
    };

    // 总时间 CONSOLE_DISPLAY_DURATION，间隔 1000s 回调一次 onTick
    private CountDownTimer mCountDownTimer = new CountDownTimer(CONSOLE_DISPLAY_DURATION, 1000) {
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

    // 注册系统音量变化广播
    private void registerSystemVolumeReceiver() {
        SystemVolumeReceiver volumeReceiver = new SystemVolumeReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.media.VOLUME_CHANGED_ACTION");
        mBuilder.bContext.registerReceiver(volumeReceiver, filter);
    }

    // 系统音量广播接收者
    private class SystemVolumeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals("android.media.VOLUME_CHANGED_ACTION")) {
                mIsReceiverVolume = true;
                setVolume(getCurrentSystemVolume(), false);
            }
        }
    }

    // 更新视频屏幕显示大小
    private void updateSurfaceFrame() {
        int sw, sh;
        if (mIsMinimize) {
            sw = DisplayUtil.dip2px(mBuilder.bContext, 500);
//            sh = DisplayUtil.dip2px(mBuilder.bContext, 320);
        } else {
            Display defaultDisplay = mWindowManager.getDefaultDisplay();
            sw = defaultDisplay.getWidth();
//            sh = defaultDisplay.getHeight();
        }

        int displayHeight = (int) (sw * mVideoHeight * 1.0f / mVideoWidth);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(sw, displayHeight);
        mSurfaceView.setLayoutParams(params);
    }

    // 初始化显示视频相关信息
    private void initMediaInfo() {
        if (mBuilder.bMediaPlayer.isSeekable()) {
            mVideoProgressSb.setVisibility(View.VISIBLE);
            mVideoControlLl.setVisibility(View.VISIBLE);

            mTotalTime = mBuilder.bMediaPlayer.getLength();
            mVideoTotalTimeTv.setText(DateUtil.getHHMMSS(mTotalTime));
            mVideoProgressSb.setProgress(0);
            mVideoProgressSb.setMax((int) mTotalTime);
        } else {
            mVideoProgressSb.setVisibility(View.GONE);
            mVideoControlLl.setVisibility(View.GONE);
        }

        int currentSystemVolume = getCurrentSystemVolume();
        mIsReceiverVolume = true;
        setVolume(currentSystemVolume, false);
    }

    // 更新显示视频相关信息
    private void updateMediaInfo() {
        if (mVlcVideoStatusTv.getVisibility() == View.VISIBLE) {
            mVlcVideoStatusTv.setVisibility(View.GONE);
        }

        if (mBuilder.bMediaPlayer.isSeekable()) {
            long currentTime = mBuilder.bMediaPlayer.getTime();
            mVideoCurrentTimeTv.setText(DateUtil.getHHMMSS(currentTime));
            mVideoProgressSb.setProgress((int) currentTime);
        }
    }

    // 设置窗口播放状态提示显示样式
    private void showTipView(String message) {
        mVlcVideoStatusTv.setVisibility(View.VISIBLE);
        mVlcVideoStatusTv.setText(message);
    }

    // 设置视频播放进度
    private void setProgress(int progress) {
        mBuilder.bMediaPlayer.setTime(progress);
    }

    // 设置播放位置(快进/快退) 单位ms
    private void setMediaPosition(int increment) {
        long currentTime = mBuilder.bMediaPlayer.getTime();
        if (CONSOLE_10_S < currentTime && currentTime < mTotalTime - CONSOLE_10_S) {
            mBuilder.bMediaPlayer.setTime(currentTime + increment);
        } else if (currentTime < CONSOLE_10_S) {
            if (increment > 0) {
                mBuilder.bMediaPlayer.setTime(currentTime + increment);
            } else {
                mBuilder.bMediaPlayer.setTime(0);
            }
        } else if (currentTime > mTotalTime - CONSOLE_10_S) {
            if (increment > 0) {
                mBuilder.bMediaPlayer.setTime(mTotalTime - 1000);
            } else {
                mBuilder.bMediaPlayer.setTime(currentTime + increment);
            }
        }
    }

    private int getCurrentSystemVolume() {
        return 100 * mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    private void setSystemVolume(int volume) {
        volume = volume * mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 100;
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
    }
    // -------------------------- 本地私有方法 finish -----------------------------------
}
