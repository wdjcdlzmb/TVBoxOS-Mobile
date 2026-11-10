package com.github.tvbox.osc.util;

import android.view.View;
import android.view.Window;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class InsetsUtils {

    /** 开启 edge-to-edge 并按主题设置系统栏对比 */
    public static void enable(Window window, boolean lightBars) {
        WindowCompat.setDecorFitsSystemWindows(window, false);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(lightBars);
            controller.setAppearanceLightNavigationBars(lightBars);
        }
    }

    /** 状态栏高度转为顶部内边距 */
    public static void applyTopInset(final View view) {
        applyInset(view, true);
    }

    /** 导航栏高度转为底部内边距 */
    public static void applyBottomInset(final View view) {
        applyInset(view, false);
    }

    private static void applyInset(final View view, final boolean top) {
        if (view == null) return;
        final int baseLeft = view.getPaddingLeft();
        final int baseTop = view.getPaddingTop();
        final int baseRight = view.getPaddingRight();
        final int baseBottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (top) {
                v.setPadding(baseLeft, baseTop + bars.top, baseRight, baseBottom);
            } else {
                v.setPadding(baseLeft, baseTop, baseRight, baseBottom + bars.bottom);
            }
            return insets;
        });
    }

    /** 同一 view 同时消化状态栏与导航栏高度 */
    public static void applyTopAndBottomInset(final View view) {
        if (view == null) return;
        final int baseLeft = view.getPaddingLeft();
        final int baseTop = view.getPaddingTop();
        final int baseRight = view.getPaddingRight();
        final int baseBottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(baseLeft, baseTop + bars.top, baseRight, baseBottom + bars.bottom);
            return insets;
        });
    }

    /** 播放器沉浸全屏:隐藏/恢复系统栏 */
    public static void setImmersive(Window window, boolean immersive) {
        WindowInsetsControllerCompat controller = window == null ? null
                : WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller == null) return;
        if (immersive) {
            controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            controller.hide(WindowInsetsCompat.Type.systemBars());
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars());
        }
    }
}
