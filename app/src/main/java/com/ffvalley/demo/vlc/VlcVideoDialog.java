package com.ffvalley.demo.vlc;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.ffvalley.demo.R;
import com.ffvalley.demo.constant.CommonConstant;
import com.ffvalley.demo.exception.VideoException;
import com.ffvalley.demo.utils.DateUtil;
import com.ffvalley.demo.utils.DisplayUtil;
import com.ffvalley.demo.utils.NumberUtil;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;


public class VlcVideoDialog extends Dialog {

    private static final int PROGRESS_MAX = 10000;
    private static final int CONSOLE_DISPLAY_DURATION = 5 * 1000;

    private Builder mBuilder;

    private Media mMedia;
    private long mTotalTime = 0; // 视频播放总时间

    private Window mDialogWindow;

    private Button mVlcCloseBtn;
    private View mVlcConsole; // 控制台控件

    private Button mVideoZoomBtn;
    private Button mVideoSwitchBtn;
    private Button mVideoForwardBtn;
    private Button mVideoBackwardBtn;
    private SeekBar mVideoProgressSb;
    private SeekBar mVideoVolumeSb;
    private TextView mVideoCurrentTimeTv;
    private TextView mVideoTotalTimeTv;

    private VlcVideoDialog(Context context, Builder builder) {
        this(context, -1);
        this.mBuilder = builder;
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    private VlcVideoDialog(Context context, int theme) {
        super(context, theme);
    }

    public static final class Builder {
        private Context bContext;
        private LibVLC bLibVLC;
        private MediaPlayer bMediaPlayer;
        private IVLCVout bVlcVout;
        private VlcVideoDialog bDialog;

        public Builder(Context context) {
            this.bContext = context;
        }

        // 设置播放库
        public Builder setPlayLib(LibVLC libVLC) {
            if (libVLC == null)
                throw new VideoException("setPlayLib - libVLC参数，" + CommonConstant.EXCEPTION_MESSAGE_NOT_NULL);

            bLibVLC = libVLC;
            bMediaPlayer = new MediaPlayer(libVLC);
            bVlcVout = bMediaPlayer.getVLCVout();
            return this;
        }

        public VlcVideoDialog build() {
//            final VlcVideoDialog dialog = new VlcVideoDialog(context, R.style.Dialog);
            if (bContext == null) {
                throw new VideoException("Builder - context参数，" + CommonConstant.EXCEPTION_MESSAGE_NOT_NULL);
            } else if (bLibVLC == null) {
                throw new VideoException("setPlayLib - libVLC参数，" + CommonConstant.EXCEPTION_MESSAGE_NOT_NULL);
            }
            bDialog = new VlcVideoDialog(bContext, this);
            bDialog.setCanceledOnTouchOutside(false);
            bDialog.setCancelable(false);
            bDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            return bDialog;
        }
    }

    // 初始化视频播放Dialog
    public void initDialog() {
        LayoutInflater inflater = (LayoutInflater) mBuilder.bContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View containerLayout = inflater.inflate(R.layout.vlc_window_video, null);
        mBuilder.bDialog.setContentView(containerLayout);

        mDialogWindow = mBuilder.bDialog.getWindow();
        if (mDialogWindow != null) {
//            mDialogWindow.getDecorView().setSystemUiVisibility(View.INVISIBLE);
            mDialogWindow.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            mDialogWindow.setBackgroundDrawable(new ColorDrawable(Color.GRAY));
            mDialogWindow.setLayout(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        }

        // 关联播放器
        SurfaceView surfaceView = containerLayout.findViewById(R.id.vlc_video_sv);
        mBuilder.bVlcVout.setVideoView(surfaceView);

        // 显示窗口关闭操作
        mVlcCloseBtn = containerLayout.findViewById(R.id.vlc_close_btn);
        mVlcCloseBtn.setOnClickListener(mOnClickListener);

        // 控制台view
        mVlcConsole = containerLayout.findViewById(R.id.vlc_console);

        // 显示窗口缩放操作
        mVideoZoomBtn = containerLayout.findViewById(R.id.video_zoom_btn);
        mVideoZoomBtn.setOnClickListener(mOnClickListener);

        // 开启/停止播放视频操作
        mVideoSwitchBtn = containerLayout.findViewById(R.id.video_switch_btn);
        mVideoSwitchBtn.setOnClickListener(mOnClickListener);

        // 快进10s操作
        mVideoForwardBtn = containerLayout.findViewById(R.id.video_forward_btn);
        mVideoForwardBtn.setOnClickListener(mOnClickListener);
        // 快退10s操作
        mVideoBackwardBtn = containerLayout.findViewById(R.id.video_backward_btn);
        mVideoBackwardBtn.setOnClickListener(mOnClickListener);

        mVideoTotalTimeTv = containerLayout.findViewById(R.id.video_total_time_tv);
        mVideoCurrentTimeTv = containerLayout.findViewById(R.id.video_current_time_tv);

        // 视频进度控制
        mVideoProgressSb = containerLayout.findViewById(R.id.video_progress_sb);
        mVideoProgressSb.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
        // 音量控制
        mVideoVolumeSb = containerLayout.findViewById(R.id.video_volume_sb);
        mVideoVolumeSb.setOnSeekBarChangeListener(mOnSeekBarChangeListener);

        // 启动计时器
        mCountDownTimer.start();
    }

    // 加载视频源 1-本地视频、2-流媒体视频、3-资源视频
    public void loadVideo(String videoUrl, VlcVideoType type) {
        if (mDialogWindow == null) throw new VideoException(CommonConstant.EXCEPTION_MESSAGE_03);

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
                    throw new VideoException(CommonConstant.EXCEPTION_MESSAGE_01);
                }
            } else {
                throw new VideoException(CommonConstant.EXCEPTION_MESSAGE_02);
            }
            mBuilder.bMediaPlayer.setMedia(mMedia);
            mMedia.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 播放视频操作
    @SuppressLint("UseCompatLoadingForDrawables")
    public void playVideo() {
        if (mDialogWindow == null) throw new VideoException(CommonConstant.EXCEPTION_MESSAGE_03);
        if (mMedia == null) throw new VideoException(CommonConstant.EXCEPTION_MESSAGE_04);
        mVideoSwitchBtn.setBackground(mBuilder.bContext.getResources().getDrawable(R.drawable.start));
        mVideoSwitchBtn.setTag(R.id.video_switch_btn, true);
        mBuilder.bMediaPlayer.play();
    }

    // 暂停视频操作
    @SuppressLint("UseCompatLoadingForDrawables")
    public void pauseVideo() {
        if (mDialogWindow == null) throw new VideoException(CommonConstant.EXCEPTION_MESSAGE_03);
        if (mMedia == null) throw new VideoException(CommonConstant.EXCEPTION_MESSAGE_04);

        mVideoSwitchBtn.setBackground(mBuilder.bContext.getResources().getDrawable(R.drawable.stop));
        mVideoSwitchBtn.setTag(R.id.video_switch_btn, false);
        mBuilder.bMediaPlayer.pause();

    }

    // 缩小屏幕操作
    @SuppressLint("UseCompatLoadingForDrawables")
    public void narrowScreen() {
        if (mDialogWindow == null) throw new VideoException(CommonConstant.EXCEPTION_MESSAGE_03);

        mDialogWindow.setLayout(DisplayUtil.dip2px(mBuilder.bContext, 500), DisplayUtil.dip2px(mBuilder.bContext, 320));
        // dialogWindow.setGravity(Gravity.BOTTOM | Gravity.END);
        mDialogWindow.setGravity(Gravity.TOP | Gravity.START);
        mVideoZoomBtn.setBackground(mBuilder.bContext.getResources().getDrawable(R.drawable.fullscreen));
        mVideoZoomBtn.setTag(R.id.video_zoom_btn, true);
    }

    // 放大屏幕操作
    @SuppressLint("UseCompatLoadingForDrawables")
    public void enlargeScreen() {
        if (mDialogWindow == null) throw new VideoException(CommonConstant.EXCEPTION_MESSAGE_03);

        mDialogWindow.setLayout(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        mDialogWindow.setGravity(Gravity.CENTER);
        mVideoZoomBtn.setBackground(mBuilder.bContext.getResources().getDrawable(R.drawable.minimize));
        mVideoZoomBtn.setTag(R.id.video_zoom_btn, false);
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {

            switch (v.getId()) {
                case R.id.vlc_close_btn:
                    mBuilder.bDialog.dismiss();
                    break;
                case R.id.video_zoom_btn:
                    if (mDialogWindow == null) return;
                    // tag(当前是否为最小屏幕状态) 如果是null/false - 当前最大屏幕需要缩小； 如果是true - 当前最小需要放大
                    if (v.getTag(R.id.video_zoom_btn) == null || !(boolean) v.getTag(R.id.video_zoom_btn)) {
                        narrowScreen();
                    } else {
                        enlargeScreen();
                    }
                    break;
                case R.id.video_switch_btn:
                    // tag(当前是否为播放状态) 如果是null/false - 当前未播放视频需要播放； 如果是true - 当前正在播放视频
                    if (v.getTag(R.id.video_switch_btn) == null || !(boolean) v.getTag(R.id.video_switch_btn)) {
                        playVideo();
                    } else {
                        pauseVideo();
                    }
                    break;
                case R.id.video_forward_btn:
                    // 快进
                    setMediaPosition(10 * 1000);
                    break;
                case R.id.video_backward_btn:
                    // 快退
                    setMediaPosition(-10 * 1000);
                    break;
                default:
                    break;
            }
        }
    };

    private SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (seekBar == mVideoProgressSb) {
            } else if (seekBar == mVideoVolumeSb) {
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
            } else if (seekBar == mVideoVolumeSb) {
            }
            mCountDownTimer.start();
        }
    };

    MediaPlayer.EventListener mEventListener = new MediaPlayer.EventListener() {

        @Override
        public void onEvent(MediaPlayer.Event event) {
            updateMediaInfo();
        }
    };

    IVLCVout.OnNewVideoLayoutListener mOnNewVideoLayoutListener = new IVLCVout.OnNewVideoLayoutListener() {

        @Override
        public void onNewVideoLayout(IVLCVout vlcVout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
            initMediaInfo();
        }
    };

    // 初始化显示视频相关信息
    private void initMediaInfo() {
        if (mDialogWindow == null) throw new VideoException(CommonConstant.EXCEPTION_MESSAGE_03);
        if (mMedia == null) throw new VideoException(CommonConstant.EXCEPTION_MESSAGE_04);

        mTotalTime = mBuilder.bMediaPlayer.getLength();

        mVideoTotalTimeTv.setText(DateUtil.getHHMMSS(mTotalTime));
        mVideoProgressSb.setProgress(0);
        mVideoProgressSb.setMax(PROGRESS_MAX);

        setVolume(50);
    }

    // 更新显示视频相关信息
    private void updateMediaInfo() {
        if (mDialogWindow == null) throw new VideoException(CommonConstant.EXCEPTION_MESSAGE_03);
        if (mMedia == null) throw new VideoException(CommonConstant.EXCEPTION_MESSAGE_04);

        long currentTime = mBuilder.bMediaPlayer.getTime();

        mVideoCurrentTimeTv.setText(DateUtil.getHHMMSS(currentTime));
        String ratio = NumberUtil.ratio(currentTime, mTotalTime, PROGRESS_MAX);
        int progress = 0;
        if (NumberUtil.isNumeric(ratio)) {
            progress = Integer.parseInt(ratio);
        }
        mVideoProgressSb.setProgress(progress);
    }

    // 设置视频播放进度
    private void setProgress(int progress) {
        if (mDialogWindow == null) throw new VideoException(CommonConstant.EXCEPTION_MESSAGE_03);
        if (mMedia == null) throw new VideoException(CommonConstant.EXCEPTION_MESSAGE_04);

        mBuilder.bMediaPlayer.setTime(mTotalTime / PROGRESS_MAX * progress);
    }

    // 设置视频播放音量
    private void setVolume(int progress) {
        if (mDialogWindow == null) throw new VideoException(CommonConstant.EXCEPTION_MESSAGE_03);
        if (mMedia == null) throw new VideoException(CommonConstant.EXCEPTION_MESSAGE_04);

        mBuilder.bMediaPlayer.setVolume(progress);
    }

    // 设置播放位置 单位ms
    private void setMediaPosition(int increment) {
        if (mDialogWindow == null) throw new VideoException(CommonConstant.EXCEPTION_MESSAGE_03);
        if (mMedia == null) throw new VideoException(CommonConstant.EXCEPTION_MESSAGE_04);

        long currentTime = mBuilder.bMediaPlayer.getTime();
        mBuilder.bMediaPlayer.setTime(currentTime + increment);
    }

    // ----------------- 拖动屏幕相关方法 start -----------------------
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayoutParams;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mLayoutParams = (WindowManager.LayoutParams) mDialogWindow.getDecorView().getLayoutParams();
    }

    @Override
    public void onWindowAttributesChanged(WindowManager.LayoutParams params) {
        super.onWindowAttributesChanged(params);
        if (mBuilder != null && mDialogWindow != null) {
            mLayoutParams = params;
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        int lastX = 0, lastY = 0;
        int paramX = 0, paramY = 0;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastX = (int) event.getRawX();
                lastY = (int) event.getRawY();
                paramX = mLayoutParams.x;
                paramY = mLayoutParams.y;
                break;
            case MotionEvent.ACTION_MOVE:
                int dx = (int) event.getRawX() - lastX;
                int dy = (int) event.getRawY() - lastY;
                mLayoutParams.x = paramX - mLayoutParams.width / 2 + dx;
                mLayoutParams.y = paramY - mLayoutParams.height / 2 + dy;
                mWindowManager.updateViewLayout(mDialogWindow.getDecorView(), mLayoutParams);
                break;
            case MotionEvent.ACTION_UP:
                if (View.VISIBLE != mVlcConsole.getVisibility()) {
//                    mDialogWindow.getDecorView().setSystemUiVisibility(View.VISIBLE);
                    mVlcCloseBtn.setVisibility(View.VISIBLE);
                    mVlcConsole.setVisibility(View.VISIBLE);
                }
                // 启动计时器
                mCountDownTimer.start();
                break;
            case MotionEvent.ACTION_OUTSIDE:
                System.out.println("--------- ACTION_OUTSIDE ---------");
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
            default:
        }
        return super.onTouchEvent(event);
    }
    // ----------------- 拖动屏幕相关方法 finish -----------------------

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

    @Override
    public void dismiss() {
        mBuilder.bMediaPlayer.release();
        mBuilder.bMediaPlayer.getVLCVout().detachViews();
        mCountDownTimer.cancel();
        super.dismiss();
    }
}
