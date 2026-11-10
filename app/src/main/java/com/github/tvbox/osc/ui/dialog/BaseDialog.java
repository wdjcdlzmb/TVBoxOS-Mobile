package com.github.tvbox.osc.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;

import xyz.doikki.videoplayer.util.CutoutUtil;

public class BaseDialog extends Dialog {


    public BaseDialog(@NonNull Context context) {
        super(context, R.style.CustomDialogStyle);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    public BaseDialog(Context context, int customDialogStyle) {
        super(context, customDialogStyle);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        CutoutUtil.adaptCutoutAboveAndroidP(this, true);//设置刘海
        super.onCreate(savedInstanceState);


        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(getWindow().getAttributes());
        lp.gravity = Gravity.BOTTOM | Gravity.START | Gravity.END;
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        lp.dimAmount = 0.5f;
        getWindow().setAttributes(lp);
        getWindow().setWindowAnimations(R.style.BottomDialogAnimation); // Set the animation style
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }

    @Override
    public void show() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        super.show();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
    }
}
