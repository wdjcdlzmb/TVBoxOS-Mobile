package com.github.tvbox.osc.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.ClipboardUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.base.BaseVbFragment;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.cache.RoomDataManger;
import com.github.tvbox.osc.databinding.FragmentMyBinding;
import com.github.tvbox.osc.ui.activity.CollectActivity;
import com.github.tvbox.osc.ui.activity.DetailActivity;
import com.github.tvbox.osc.ui.activity.HistoryActivity;
import com.github.tvbox.osc.ui.activity.LiveActivity;
import com.github.tvbox.osc.ui.activity.LocalPlayActivity;
import com.github.tvbox.osc.ui.activity.MovieFoldersActivity;
import com.github.tvbox.osc.ui.activity.SettingActivity;
import com.github.tvbox.osc.ui.activity.SubscriptionActivity;
import com.github.tvbox.osc.ui.adapter.HistoryAdapter;
import com.github.tvbox.osc.ui.dialog.AboutDialog;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.Utils;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.interfaces.OnInputConfirmListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author pj567
 * @date :2021/3/9
 * @description:
 */
public class MyFragment extends BaseVbFragment<FragmentMyBinding> {

    private HistoryAdapter continueAdapter;

    @Override
    protected void init() {
        mBinding.tvVersion.setText("v"+ AppUtils.getAppVersionName());

        mBinding.rvContinueWatching.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false));
        continueAdapter = new HistoryAdapter(R.layout.item_continue_watching);
        mBinding.rvContinueWatching.setAdapter(continueAdapter);
        continueAdapter.setOnItemClickListener((adapter, view, position) -> {
            FastClickCheckUtil.check(view);
            VodInfo vodInfo = continueAdapter.getData().get(position);
            Bundle bundle = new Bundle();
            bundle.putString("id", vodInfo.id);
            bundle.putString("sourceKey", vodInfo.sourceKey);
            jumpActivity(DetailActivity.class, bundle);
        });

        mBinding.addrPlay.setOnClickListener(v ->{
            new XPopup.Builder(getContext())
                    .asInputConfirm(getString(R.string.play), "", isPush(ClipboardUtils.getText().toString())?ClipboardUtils.getText():"", getString(R.string.address), text -> {
                        if (!TextUtils.isEmpty(text)){
                            Intent newIntent = new Intent(mContext, DetailActivity.class);
                            newIntent.putExtra("id", text);
                            newIntent.putExtra("sourceKey", "push_agent");
                            newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(newIntent);
                        }
                    }, null, R.layout.dialog_input).show();
        });
        //mBinding.tvLive.setOnClickListener(v -> jumpActivity(LivePlayActivity.class));
        mBinding.tvLive.setOnClickListener(v -> jumpActivity(LiveActivity.class));

        mBinding.tvSetting.setOnClickListener(v -> jumpActivity(SettingActivity.class));

        mBinding.tvHistory.setOnClickListener(v -> jumpActivity(HistoryActivity.class));

        mBinding.tvFavorite.setOnClickListener(v -> jumpActivity(CollectActivity.class));

        mBinding.tvLocal.setOnClickListener(v -> {
            if (!XXPermissions.isGranted(mContext, Permission.MANAGE_EXTERNAL_STORAGE)) {
                showPermissionTipPopup();
            } else {
                jumpActivity(MovieFoldersActivity.class);
            }
        });

        mBinding.llSubscription.setOnClickListener(v -> jumpActivity(SubscriptionActivity.class));

        mBinding.llAbout.setOnClickListener(v -> {
            new XPopup.Builder(mActivity)
                    .asCustom(new AboutDialog(mActivity))
                    .show();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (continueAdapter != null) {
            loadContinueWatching();
        }
    }

    private void loadContinueWatching() {
        new Thread(() -> {
            List<VodInfo> records = RoomDataManger.getAllVodRecord(20);
            List<VodInfo> list = new ArrayList<>();
            for (VodInfo vodInfo : records) {
                if (vodInfo.playNote != null && !vodInfo.playNote.isEmpty()) {
                    vodInfo.note = vodInfo.playNote;
                }
                list.add(vodInfo);
            }
            if (mActivity == null) return;
            mActivity.runOnUiThread(() -> {
                continueAdapter.setNewData(list);
                int visibility = list.isEmpty() ? View.GONE : View.VISIBLE;
                mBinding.tvContinueHeader.setVisibility(visibility);
                mBinding.rvContinueWatching.setVisibility(visibility);
            });
        }).start();
    }

    private void showPermissionTipPopup(){
        new XPopup.Builder(mActivity)
                .isDarkTheme(Utils.isDarkTheme())
                .asConfirm(getString(R.string.tip),getString(R.string.storage_rw_permission_note), () -> {
                    getPermission();
                }).show();
    }

    private void getPermission(){
        XXPermissions.with(this)
                .permission(Permission.MANAGE_EXTERNAL_STORAGE)
                .request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(List<String> permissions, boolean all) {
                        if (all) {
                            jumpActivity(MovieFoldersActivity.class);
                        }else {
                            ToastUtils.showLong(R.string.partial_permission_denied);
                        }
                    }

                    @Override
                    public void onDenied(List<String> permissions, boolean never) {
                        if (never) {
                            ToastUtils.showLong(R.string.storage_perm_blocked_manual);
                            // 如果是被永久拒绝就跳转到应用权限系统设置页面
                            XXPermissions.startPermissionActivity(mActivity, permissions);
                        } else {
                            ToastUtils.showShort(R.string.permission_request_failed);
                            showPermissionTipPopup();
                        }
                    }
                });
    }

    private boolean isPush(String text) {
        return !TextUtils.isEmpty(text) && Arrays.asList("smb", "http", "https", "mitv").contains(Uri.parse(text).getScheme());
    }

}