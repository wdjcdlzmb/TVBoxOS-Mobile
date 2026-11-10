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
    private int minFlingDistance = 100;             //最小识别距离
    private int minFlingVelocity = 10;              //最小识别速度

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
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        float dx = e2.getX() - e1.getX();
        float dy = e2.getY() - e1.getY();
        if (Math.abs(dx) > Math.abs(dy)) {
            // 横向滑动:切换线路
            if (dx > minFlingDistance && Math.abs(velocityX) > minFlingVelocity) {
                listener.changeSource(1);           //右滑
            } else if (dx < -minFlingDistance && Math.abs(velocityX) > minFlingVelocity) {
                listener.changeSource(-1);          //左滑
            }
        } else {
            // 竖向滑动:切换频道(上滑下一台,下滑上一台)
            if (dy < -minFlingDistance && Math.abs(velocityY) > minFlingVelocity) {
                listener.changeChannel(1);
            } else if (dy > minFlingDistance && Math.abs(velocityY) > minFlingVelocity) {
                listener.changeChannel(-1);
            }
        }
        return false;
    }

    @Override
    public boolean onBackPressed() {
        if (mControlWrapper.isFullScreen()) {
            return stopFullScreen();
        }
        return super.onBackPressed();
    }
}
