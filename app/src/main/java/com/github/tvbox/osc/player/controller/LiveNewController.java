package com.github.tvbox.osc.player.controller;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.tvbox.osc.R;

import org.jetbrains.annotations.NotNull;

/**
 * 横竖切换新版直播控制器
 */

public class LiveNewController extends BaseController {
    protected ProgressBar mLoading;
    private final int minSwipeDistance = 100;      //最小识别位移(px)
    private float mDownX;
    private float mDownY;

    public LiveNewController(@NotNull Context context) {
        super(context);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.player_live_control_view;
    }

    @Override
    protected void initView() {
        super.initView();
        mLoading = findViewById(R.id.loading);
    }

    public interface LiveControlListener {
        void setting();

        void playStateChanged(int playState);

        void changeSource(int direction);

        void changeChannel(int direction);
    }

    private LiveNewController.LiveControlListener listener = null;

    public void setListener(LiveNewController.LiveControlListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onPlayStateChanged(int playState) {
        super.onPlayStateChanged(playState);
        listener.playStateChanged(playState);
    }

    /**
     * 直播:竖向滑动用于换台,禁用父类基于竖向滑动的亮度/音量调节以避免手势冲突
     */
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return true;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        mDownX = e.getX();
        mDownY = e.getY();
        return super.onDown(e);
    }

    /**
     * 抬手时按位移判定换台/换源:GestureDetector 的 fling 依赖速度估计,
     * 慢速滑动会被漏判,直播又没有其它竖向手势,故位移过阈值即触发
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = super.onTouchEvent(event);
        if (event.getActionMasked() == MotionEvent.ACTION_UP && listener != null) {
            float dx = event.getX() - mDownX;
            float dy = event.getY() - mDownY;
            if (Math.abs(dy) > Math.abs(dx)) {
                if (dy < -minSwipeDistance) {
                    listener.changeChannel(1);
                } else if (dy > minSwipeDistance) {
                    listener.changeChannel(-1);
                }
            } else {
                if (dx > minSwipeDistance) {
                    listener.changeSource(1);
                } else if (dx < -minSwipeDistance) {
                    listener.changeSource(-1);
                }
            }
        }
        return handled;
    }

    @Override
    public boolean onBackPressed() {
        if (mControlWrapper.isFullScreen()) {
            return stopFullScreen();
        }
        return super.onBackPressed();
    }
}
